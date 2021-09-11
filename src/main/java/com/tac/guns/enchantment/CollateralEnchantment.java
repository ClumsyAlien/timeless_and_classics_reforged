package com.tac.guns.enchantment;

import net.minecraft.inventory.EquipmentSlotType;

import net.minecraft.enchantment.Enchantment.Rarity;

/**
 * Author: MrCrayfish
 */
public class CollateralEnchantment extends GunEnchantment
{
    public CollateralEnchantment()
    {
        super(Rarity.RARE, EnchantmentTypes.GUN, new EquipmentSlotType[]{EquipmentSlotType.MAINHAND}, Type.PROJECTILE);
    }

    @Override
    public int getMinEnchantability(int level)
    {
        return 10;
    }

    @Override
    public int getMaxEnchantability(int level)
    {
        return this.getMinEnchantability(level) + 20;
    }
}
