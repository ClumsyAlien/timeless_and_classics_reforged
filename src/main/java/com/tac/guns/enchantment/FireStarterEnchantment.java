package com.tac.guns.enchantment;

import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;

import net.minecraft.enchantment.Enchantment.Rarity;

/**
 * Author: Forked from MrCrayfish, continued by Timeless devs
 */
public class FireStarterEnchantment extends GunEnchantment
{
    public FireStarterEnchantment()
    {
        super(Rarity.VERY_RARE, EnchantmentTypes.GUN, new EquipmentSlotType[]{EquipmentSlotType.MAINHAND}, Type.PROJECTILE);
    }

    @Override
    public int getMinCost(int level)
    {
        return 15;
    }
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return false;
    }
    @Override
    public int getMaxCost(int level)
    {
        return super.getMinCost(level) + 30;
    }
}
