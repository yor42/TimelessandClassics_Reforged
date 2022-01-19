package com.tac.guns.enchantment;

import net.minecraft.inventory.EquipmentSlotType;

/**
 * Author: Forked from MrCrayfish, continued by Timeless devs
 */
public class TriggerFingerEnchantment extends GunEnchantment
{
    public TriggerFingerEnchantment()
    {
        super(Rarity.RARE, EnchantmentTypes.SEMI_AUTO_GUN, new EquipmentSlotType[]{EquipmentSlotType.MAINHAND}, Type.WEAPON);
    }

    @Override
    public int getMaxLevel()
    {
        return 2;
    }

    @Override
    public int getMinEnchantability(int level)
    {
        return 15 + (level - 1) * 10;
    }

    @Override
    public int getMaxEnchantability(int level)
    {
        return this.getMinEnchantability(level) + 40;
    }
}