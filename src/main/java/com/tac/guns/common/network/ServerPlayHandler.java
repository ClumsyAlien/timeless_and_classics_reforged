package com.tac.guns.common.network;

import com.tac.guns.Config;
import com.tac.guns.GunMod;
import com.tac.guns.Reference;
import com.tac.guns.common.Gun;
import com.tac.guns.common.ProjectileManager;
import com.tac.guns.common.ShootTracker;
import com.tac.guns.common.SpreadTracker;
import com.tac.guns.common.container.AttachmentContainer;
import com.tac.guns.common.container.InspectionContainer;
import com.tac.guns.common.container.WorkbenchContainer;
import com.tac.guns.crafting.WorkbenchRecipe;
import com.tac.guns.crafting.WorkbenchRecipes;
import com.tac.guns.entity.ProjectileEntity;
import com.tac.guns.event.GunFireEvent;
import com.tac.guns.init.ModEnchantments;
import com.tac.guns.init.ModSyncedDataKeys;
import com.tac.guns.interfaces.IProjectileFactory;
import com.tac.guns.item.GunItem;
import com.tac.guns.item.IColored;
import com.tac.guns.item.TransitionalTypes.TimelessGunItem;
import com.tac.guns.network.PacketHandler;
import com.tac.guns.network.message.MessageBulletTrail;
import com.tac.guns.network.message.MessageGunSound;
import com.tac.guns.network.message.MessageShoot;
import com.tac.guns.tileentity.WorkbenchTileEntity;
import com.tac.guns.util.GunModifierHelper;
import com.tac.guns.util.InventoryUtil;
import com.mrcrayfish.obfuscate.common.data.SyncedPlayerData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;


/**
 * Author: MrCrayfish
 */
public class ServerPlayHandler
{
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);
    private static final Predicate<LivingEntity> HOSTILE_ENTITIES = entity -> entity.getSoundCategory() == SoundCategory.HOSTILE && !Config.COMMON.aggroMobs.exemptEntities.get().contains(entity.getType().getRegistryName().toString());

    /**
     * Fires the weapon the player is currently holding.
     * This is only intended for use on the logical server.
     *
     * @param player the player for who's weapon to fire
     */
    public static void handleShoot(MessageShoot message, ServerPlayerEntity player)
    {
        if(!player.isSpectator())
        {
            World world = player.world;
            ItemStack heldItem = player.getHeldItem(Hand.MAIN_HAND);
            if(heldItem.getItem() instanceof GunItem && (Gun.hasAmmo(heldItem) || player.isCreative()))
            {
                GunItem item = (GunItem) heldItem.getItem();
                Gun modifiedGun = item.getModifiedGun(heldItem);
                if(modifiedGun != null)
                {
                    if(MinecraftForge.EVENT_BUS.post(new GunFireEvent.Pre(player, heldItem)))
                        return;

                    /* Updates the yaw and pitch with the clients current yaw and pitch */
                    player.rotationYaw = message.getRotationYaw();
                    player.rotationPitch = message.getRotationPitch();

                    ShootTracker tracker = ShootTracker.getShootTracker(player);
                    if(tracker.hasCooldown(item))
                    {
                        GunMod.LOGGER.warn(player.getName().getUnformattedComponentText() + "(" + player.getUniqueID() + ") tried to fire before cooldown finished or server is lagging? Remaining milliseconds: " + tracker.getRemaining(item));
                        return;
                    }
                    tracker.putCooldown(heldItem, item, modifiedGun);

                    if(SyncedPlayerData.instance().get(player, ModSyncedDataKeys.RELOADING))
                    {
                        SyncedPlayerData.instance().set(player, ModSyncedDataKeys.RELOADING, false);
                    }

                    if(!modifiedGun.getGeneral().isAlwaysSpread() && modifiedGun.getGeneral().getSpread() > 0.0F)
                    {
                        SpreadTracker.get(player).update(player, item);
                    }

                    int count = modifiedGun.getGeneral().getProjectileAmount();
                    Gun.Projectile projectileProps = modifiedGun.getProjectile();
                    ProjectileEntity[] spawnedProjectiles = new ProjectileEntity[count];
                    for(int i = 0; i < count; i++)
                    {
                        IProjectileFactory factory = ProjectileManager.getInstance().getFactory(projectileProps.getItem());
                        ProjectileEntity projectileEntity = factory.create(world, player, heldItem, item, modifiedGun);
                        projectileEntity.setWeapon(heldItem);
                        projectileEntity.setAdditionalDamage(Gun.getAdditionalDamage(heldItem));
                        world.addEntity(projectileEntity);
                        spawnedProjectiles[i] = projectileEntity;
                        projectileEntity.tick();
                    }
                    if(!projectileProps.isVisible())
                    {
                        MessageBulletTrail messageBulletTrail = new MessageBulletTrail(spawnedProjectiles, projectileProps, player.getEntityId());
                        PacketHandler.getPlayChannel().send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(player.getPosX(), player.getPosY(), player.getPosZ(), Config.COMMON.network.projectileTrackingRange.get(), player.world.getDimensionKey())), messageBulletTrail);
                    }

                    MinecraftForge.EVENT_BUS.post(new GunFireEvent.Post(player, heldItem));

                    if(Config.COMMON.aggroMobs.enabled.get())
                    {
                        double radius = GunModifierHelper.getModifiedFireSoundRadius(heldItem, Config.COMMON.aggroMobs.range.get());
                        double x = player.getPosX();
                        double y = player.getPosY() + 0.5;
                        double z = player.getPosZ();
                        AxisAlignedBB box = new AxisAlignedBB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
                        radius *= radius;
                        double dx, dy, dz;
                        for(LivingEntity entity : world.getEntitiesWithinAABB(LivingEntity.class, box, HOSTILE_ENTITIES))
                        {
                            dx = x - entity.getPosX();
                            dy = y - entity.getPosY();
                            dz = z - entity.getPosZ();
                            if(dx * dx + dy * dy + dz * dz <= radius)
                            {
                                entity.setRevengeTarget(Config.COMMON.aggroMobs.angerHostileMobs.get() ? player : entity);
                            }
                        }
                    }

                    boolean silenced = GunModifierHelper.isSilencedFire(heldItem);
                    ResourceLocation fireSound = silenced ? modifiedGun.getSounds().getSilencedFire() : modifiedGun.getSounds().getFire();
                    if(fireSound != null)
                    {
                        double posX = player.getPosX();
                        double posY = player.getPosY() + player.getEyeHeight();
                        double posZ = player.getPosZ();
                        float volume = GunModifierHelper.getFireSoundVolume(heldItem);
                        float pitch = 0.9F + world.rand.nextFloat() * 0.2F;
                        double radius = GunModifierHelper.getModifiedFireSoundRadius(heldItem, Config.SERVER.gunShotMaxDistance.get());
                        boolean muzzle = modifiedGun.getDisplay().getFlash() != null;
                        MessageGunSound messageSound = new MessageGunSound(fireSound, SoundCategory.PLAYERS, (float) posX, (float) posY, (float) posZ, volume, pitch, player.getEntityId(), muzzle);
                        PacketDistributor.TargetPoint targetPoint = new PacketDistributor.TargetPoint(posX, posY, posZ, radius, player.world.getDimensionKey());
                        PacketHandler.getPlayChannel().send(PacketDistributor.NEAR.with(() -> targetPoint), messageSound);
                    }

                    if(!player.isCreative())
                    {
                        CompoundNBT tag = heldItem.getOrCreateTag();
                        if(!tag.getBoolean("IgnoreAmmo"))
                        {
                            int level = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.RECLAIMED.get(), heldItem);
                            if(level == 0 || player.world.rand.nextInt(4 - MathHelper.clamp(level, 1, 2)) != 0)
                            {
                                tag.putInt("AmmoCount", Math.max(0, tag.getInt("AmmoCount") - 1));
                            }
                        }
                    }
                }
            }
            else
            {
                world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(), SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.3F, 0.8F);
            }
        }
    }

    /**
     * Crafts the specified item at the workstation the player is currently using.
     * This is only intended for use on the logical server.
     *
     * @param player the player who is crafting
     * @param id     the id of an item which is registered as a valid workstation recipe
     * @param pos    the block position of the workstation the player is using
     */
    public static void handleCraft(ServerPlayerEntity player, ResourceLocation id, BlockPos pos)
    {
        World world = player.world;

        if(player.openContainer instanceof WorkbenchContainer)
        {
            WorkbenchContainer workbench = (WorkbenchContainer) player.openContainer;
            if(workbench.getPos().equals(pos))
            {
                WorkbenchRecipe recipe = WorkbenchRecipes.getRecipeById(world, id);
                if(recipe == null)
                {
                    return;
                }

                List<ItemStack> materials = recipe.getMaterials();
                if(materials != null)
                {
                    for(ItemStack stack : materials)
                    {
                        if(!InventoryUtil.hasItemStack(player, stack))
                        {
                            return;
                        }
                    }

                    for(ItemStack stack : materials)
                    {
                        InventoryUtil.removeItemStack(player, stack);
                    }

                    WorkbenchTileEntity workbenchTileEntity = workbench.getWorkbench();

                    /* Gets the color based on the dye */
                    ItemStack stack = recipe.getItem();
                    ItemStack dyeStack = workbenchTileEntity.getInventory().get(0);
                    if(dyeStack.getItem() instanceof DyeItem)
                    {
                        DyeItem dyeItem = (DyeItem) dyeStack.getItem();
                        int color = dyeItem.getDyeColor().getColorValue();

                        if(stack.getItem() instanceof IColored && ((IColored) stack.getItem()).canColor(stack))
                        {
                            IColored colored = (IColored) stack.getItem();
                            colored.setColor(stack, color);
                            workbenchTileEntity.getInventory().set(0, ItemStack.EMPTY);
                        }
                    }

                    InventoryHelper.spawnItemStack(world, pos.getX() + 0.5, pos.getY() + 1.125, pos.getZ() + 0.5, stack);
                }
            }
        }
    }

    /**
     * @param player
     */
    public static void handleUnload(ServerPlayerEntity player)
    {
        ItemStack stack = player.getHeldItemMainhand();
        if(stack.getItem() instanceof GunItem)
        {
            CompoundNBT tag = stack.getTag();
            if(tag != null && tag.contains("AmmoCount", Constants.NBT.TAG_INT))
            {
                int count = tag.getInt("AmmoCount");
                tag.putInt("AmmoCount", 0);

                GunItem gunItem = (GunItem) stack.getItem();
                Gun gun = gunItem.getModifiedGun(stack);
                ResourceLocation id = gun.getProjectile().getItem();

                Item item = ForgeRegistries.ITEMS.getValue(id);
                if(item == null)
                {
                    return;
                }

                int maxStackSize = item.getMaxStackSize();
                int stacks = count / maxStackSize;
                for(int i = 0; i < stacks; i++)
                {
                    spawnAmmo(player, new ItemStack(item, maxStackSize));
                }

                int remaining = count % maxStackSize;
                if(remaining > 0)
                {
                    spawnAmmo(player, new ItemStack(item, remaining));
                }
            }
        }
    }

    /**
     * @param player
     * @param stack
     */
    private static void spawnAmmo(ServerPlayerEntity player, ItemStack stack)
    {
        player.inventory.addItemStackToInventory(stack);
        if(stack.getCount() > 0)
        {
            player.world.addEntity(new ItemEntity(player.world, player.getPosX(), player.getPosY(), player.getPosZ(), stack.copy()));
        }
    }

    /**
     * @param player
     */
    public static void handleAttachments(ServerPlayerEntity player)
    {
        ItemStack heldItem = player.getHeldItemMainhand();
        if(heldItem.getItem() instanceof GunItem)
        {
            NetworkHooks.openGui(player, new SimpleNamedContainerProvider((windowId, playerInventory, player1) -> new AttachmentContainer(windowId, playerInventory, heldItem), new TranslationTextComponent("container.tac.attachments")));
        }
    }

    /**
     * @param player
     */
    public static void handleInspection(ServerPlayerEntity player)
    {
        ItemStack heldItem = player.getHeldItemMainhand();
        if(heldItem.getItem() instanceof GunItem)
        {
            NetworkHooks.openGui(player, new SimpleNamedContainerProvider((windowId, playerInventory, player1) -> new InspectionContainer(windowId, playerInventory, heldItem), new TranslationTextComponent("container.tac.inspection")));
        }
    }

    /**
     * @param player
     */
    public static void handleFireMode(ServerPlayerEntity player)
    {
        ItemStack heldItem = player.getHeldItemMainhand();
        if(heldItem.getItem() instanceof TimelessGunItem)
        {
            Gun gun = ((TimelessGunItem) heldItem.getItem()).getModifiedGun(heldItem.getStack());
            int[] gunItemFireModes = heldItem.getTag().getIntArray("supportedFireModes");

            // Check if the weapon is new, add in all supported modes
            if(ArrayUtils.isEmpty(gunItemFireModes) || gunItemFireModes == null)
            {
                gunItemFireModes = gun.getGeneral().getRateSelector();
                heldItem.getTag().putIntArray("supportedFireModes", gunItemFireModes);
            }

            int locationInSupportedModes = heldItem.getTag().getIntArray("supportedFireModes")[ArrayUtils.indexOf(heldItem.getTag().getIntArray("supportedFireModes"), heldItem.getTag().getInt("CurrentFireMode"))];

            if(locationInSupportedModes == (heldItem.getTag().getIntArray("supportedFireModes").length-1))
            {
                heldItem.getTag().remove("CurrentFireMode");
                heldItem.getTag().putInt("CurrentFireMode",gunItemFireModes[0]);
            }
            else
            {
                heldItem.getTag().remove("CurrentFireMode");
                heldItem.getTag().putInt("CurrentFireMode", heldItem.getTag().getIntArray("supportedFireModes")[locationInSupportedModes+1]);
            }

        }
    }
}
