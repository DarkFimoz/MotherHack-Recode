package fun.motherhack.utils.auction.ab.impl;

import fun.motherhack.utils.auction.ab.ABItems;
import lombok.experimental.UtilityClass;
import net.minecraft.item.ItemStack;

@UtilityClass
public class TalismanItems {

    public ItemStack getDedala() {
        return ABItems.dedalaTier3();
    }

    public ItemStack getExidna() {
        return ABItems.exidnaTier3();
    }

    public ItemStack getGarmonii() {
        return ABItems.garmoniiTier3();
    }

    public ItemStack getGrani() {
        return ABItems.graniTier3();
    }

    public ItemStack getHaron() {
        return ABItems.haronTier3();
    }

    public ItemStack getPhoenix() {
        return ABItems.phoenixTier3();
    }

    public ItemStack getTriton() {
        return ABItems.tritonTier3();
    }

    public ItemStack getKrush() {
        return ABItems.krush();
    }

    public ItemStack getKaratel() {
        return ABItems.karatel();
    }
}