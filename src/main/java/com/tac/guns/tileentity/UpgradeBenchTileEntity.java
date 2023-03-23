package com.tac.guns.tileentity;

import com.google.gson.GsonBuilder;
import com.tac.guns.Config;
import com.tac.guns.GunMod;
import com.tac.guns.common.Gun;
import com.tac.guns.common.container.AttachmentContainer;
import com.tac.guns.common.container.UpgradeBenchContainer;
import com.tac.guns.init.ModItems;
import com.tac.guns.init.ModTileEntities;
import com.tac.guns.item.TransitionalTypes.TimelessGunItem;
import com.tac.guns.tileentity.inventory.IStorageBlock;
import com.tac.guns.util.InventoryUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFrameItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.client.model.ModelDataManager;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.data.ForgeItemTagsProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.logging.log4j.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Time;

import static com.tac.guns.GunMod.LOGGER;

/**
 * Author: Forked from MrCrayfish, continued by Timeless devs
 */
public class UpgradeBenchTileEntity extends SyncedTileEntity implements IStorageBlock
{
    private NonNullList<ItemStack> inventory = NonNullList.withSize(2, ItemStack.EMPTY);

    public UpgradeBenchTileEntity()
    {
        super(ModTileEntities.WORKBENCH.get()/*UPGRADE_BENCH.get()*/);
    }

    @Override
    public NonNullList<ItemStack> getInventory()
    {
        return this.inventory;
    }

    @Override
    public CompoundNBT getUpdateTag()
    {
        CompoundNBT nbtTagCompound = new CompoundNBT();
        this.save(nbtTagCompound);
        return nbtTagCompound;
    }
    @Override
    public void handleUpdateTag(BlockState blockState, CompoundNBT tag)
    {
        this.load(blockState, tag);
    }
    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        BlockState blockState = level.getBlockState(worldPosition);
        this.load(blockState, pkt.getTag());   // read from the nbt in the packet
    }
    @Override
    @Nullable
    public SUpdateTileEntityPacket getUpdatePacket()
    {
        CompoundNBT nbtTagCompound = new CompoundNBT();
        this.save(nbtTagCompound);
        int tileEntityType = 42;  // arbitrary number; only used for vanilla TileEntities.
        return new SUpdateTileEntityPacket(this.worldPosition, tileEntityType, nbtTagCompound);
    }

    @Override
    public CompoundNBT save(CompoundNBT compound)
    {
        super.save(compound);

        CompoundNBT weaponBt = new CompoundNBT();
        this.inventory.get(0).save(weaponBt);
        if(this.inventory.get(0).getTag() != null)
            compound.put("weapon", weaponBt);

        CompoundNBT modules = new CompoundNBT();
        this.inventory.get(1).save(modules);
        if(this.inventory.get(1).getOrCreateTag() != null)
            compound.put("modules", modules);

        return compound;
    }

    @Override
    public void load(BlockState state, CompoundNBT compound)
    {
        super.load(state, compound);
        if(compound.contains("weapon"))
            this.inventory.set(0, ItemStack.of(compound.getCompound("weapon")));

        CompoundNBT itemStackNBT = compound.getCompound("modules");
        ItemStack readItemStack = ItemStack.of(itemStackNBT);
        this.inventory.set(1, readItemStack);
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack)
    {
        return true;
    }

    @Override
    public boolean stillValid(PlayerEntity player)
    {
        return this.level.getBlockEntity(this.worldPosition) == this && player.distanceToSqr(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ITextComponent getDisplayName()
    {
        return new TranslationTextComponent("container.tac.upgradeBench");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity playerEntity)
    {
        return new UpgradeBenchContainer(windowId, playerInventory, this);
    }
}
