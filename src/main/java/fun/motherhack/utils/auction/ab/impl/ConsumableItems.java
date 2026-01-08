package fun.motherhack.utils.auction.ab.impl;

import fun.motherhack.utils.auction.ab.ABItems;
import lombok.experimental.UtilityClass;
import net.minecraft.item.ItemStack;

@UtilityClass
public class ConsumableItems {

    public ItemStack getDesor() {
        return ABItems.desor();
    }

    public ItemStack getPlast() {
        return ABItems.plast();
    }

    public ItemStack getBozhka() {
        return ABItems.bozhka();
    }

    public ItemStack getSnezhok() {
        return ABItems.snezhok();
    }

    public ItemStack getTrapka() {
        return ABItems.trapka();
    }

    public ItemStack getYavka() {
        return ABItems.yavka();
    }
}