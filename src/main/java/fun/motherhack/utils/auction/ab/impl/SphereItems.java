package fun.motherhack.utils.auction.ab.impl;

import fun.motherhack.utils.auction.ab.ABItems;
import lombok.experimental.UtilityClass;
import net.minecraft.item.ItemStack;

@UtilityClass
public class SphereItems {

    public ItemStack getAndromeda() {
        return ABItems.andromedaTier3();
    }

    public ItemStack getApollona() {
        return ABItems.apollonaTier3();
    }

    public ItemStack getAstrea() {
        return ABItems.astreaTier3();
    }

    public ItemStack getHimera() {
        return ABItems.himeraTier3();
    }

    public ItemStack getOsirisa() {
        return ABItems.osirisaTier3();
    }

    public ItemStack getPandora() {
        return ABItems.pandoraTier3();
    }

    public ItemStack getTitan() {
        return ABItems.titanTier3();
    }
}