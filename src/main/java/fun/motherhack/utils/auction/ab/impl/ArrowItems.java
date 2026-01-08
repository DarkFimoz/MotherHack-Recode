package fun.motherhack.utils.auction.ab.impl;

import fun.motherhack.utils.auction.ab.ABItems;
import lombok.experimental.UtilityClass;
import net.minecraft.item.ItemStack;

@UtilityClass
public class ArrowItems {

    public ItemStack getProklyatayaStrela() {
        return ABItems.adskayaStrela();
    }

    public ItemStack getParanoiaStrela() {
        return ABItems.paranoiaStrela();
    }

    public ItemStack getProklyataya() {
        return ABItems.proklyatayaStrela();
    }

    public ItemStack getSnezhnaya() {
        return ABItems.snezhnayaStrela();
    }
}