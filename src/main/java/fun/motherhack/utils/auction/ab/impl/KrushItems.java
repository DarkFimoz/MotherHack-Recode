package fun.motherhack.utils.auction.ab.impl;

import fun.motherhack.utils.auction.ab.ABItems;
import lombok.experimental.UtilityClass;
import net.minecraft.item.ItemStack;

@UtilityClass
public class KrushItems {

    public ItemStack getHelmet() {
        return ABItems.krushHelmet();
    }

    public ItemStack getChestplate() {
        return ABItems.krushChestplate();
    }

    public ItemStack getLeggings() {
        return ABItems.krushLeggings();
    }

    public ItemStack getBoots() {
        return ABItems.krushBoots();
    }

    public ItemStack getSword() {
        return ABItems.krushSword();
    }

    public ItemStack getPickaxe() {
        return ABItems.krushPickaxe();
    }

    public ItemStack getTrident() {
        return ABItems.krushTrident();
    }

    public ItemStack getCrossbow() {
        return ABItems.krushCrossbow();
    }
}