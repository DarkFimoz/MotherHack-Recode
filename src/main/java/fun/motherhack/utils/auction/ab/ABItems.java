package fun.motherhack.utils.auction.ab;

import fun.motherhack.MotherHack;
import fun.motherhack.utils.Wrapper;
import fun.motherhack.utils.auction.nbt.NbtUtils;
import lombok.experimental.UtilityClass;
import net.minecraft.item.ItemStack;

@UtilityClass
public class ABItems implements Wrapper {

    //Пиздак блять)))) круш блядский
    public ItemStack krushHelmet() {
        return NbtUtils.loadItemStack("krushHelmet", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack krushChestplate() {
        return NbtUtils.loadItemStack("krushChestplate", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack krushLeggings() {
        return NbtUtils.loadItemStack("krushLeggings", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack krushBoots() {
        return NbtUtils.loadItemStack("krushBoots", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack krushSword() {
        return NbtUtils.loadItemStack("krushSword", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack krushPickaxe() {
        return NbtUtils.loadItemStack("krushPickaxe", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack krushTrident() {
        return NbtUtils.loadItemStack("krushTrident", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack krushCrossbow() {
        return NbtUtils.loadItemStack("krushCrossbow", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    //Бафы
    public ItemStack serka() {
        return NbtUtils.loadItemStack("serka", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack agentka() {
        return NbtUtils.loadItemStack("agentka", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack killerka() {
        return NbtUtils.loadItemStack("killerka", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack otrizhka() {
        return NbtUtils.loadItemStack("otrizhka", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack medika() {
        return NbtUtils.loadItemStack("medika", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack pobedilka() {
        return NbtUtils.loadItemStack("pobedilka", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    //Стрелы
    public ItemStack proklyatayaStrela() {
        return NbtUtils.loadItemStack("proklyatayaStrela", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack adskayaStrela() {
        return NbtUtils.loadItemStack("adskayaStrela", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack paranoiaStrela() {
        return NbtUtils.loadItemStack("paranoiaStrela", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack snezhnayaStrela() {
        return NbtUtils.loadItemStack("snezhnayaStrela", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    //Отмычки
    public ItemStack otmichkaArmor() {
        return NbtUtils.loadItemStack("otmichkaArmor", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack otmichkaResources() {
        return NbtUtils.loadItemStack("otmichkaResources", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack otmichkaSpheres() {
        return NbtUtils.loadItemStack("otmichkaSpheres", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack otmichkaTools() {
        return NbtUtils.loadItemStack("otmichkaTools", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack otmichkaWeapons() {
        return NbtUtils.loadItemStack("otmichkaWeapons", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    //Динамиты
    public ItemStack tierBlack() {
        return NbtUtils.loadItemStack("tierBlack", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack tierWhite() {
        return NbtUtils.loadItemStack("tierWhite", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    //Расходники
    public ItemStack desor() {
        return NbtUtils.loadItemStack("desor", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack plast() {
        return NbtUtils.loadItemStack("plast", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack bozhka() {
        return NbtUtils.loadItemStack("bozhka", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack snezhok() {
        return NbtUtils.loadItemStack("snezhok", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack trapka() {
        return NbtUtils.loadItemStack("trapka", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack yavka() {
        return NbtUtils.loadItemStack("yavka", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    //Талики
    public ItemStack dedalaTier1() {
        return NbtUtils.loadItemStack("dedalaTier1", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack dedalaTier2() {
        return NbtUtils.loadItemStack("dedalaTier2", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack dedalaTier3() {
        return NbtUtils.loadItemStack("dedalaTier3", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack exidnaTier1() {
        return NbtUtils.loadItemStack("exidnaTier1", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack exidnaTier2() {
        return NbtUtils.loadItemStack("exidnaTier2", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack exidnaTier3() {
        return NbtUtils.loadItemStack("exidnaTier3", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack garmoniiTier1() {
        return NbtUtils.loadItemStack("garmoniiTier1", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack garmoniiTier2() {
        return NbtUtils.loadItemStack("garmoniiTier2", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack garmoniiTier3() {
        return NbtUtils.loadItemStack("garmoniiTier3", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack graniTier1() {
        return NbtUtils.loadItemStack("graniTier1", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack graniTier2() {
        return NbtUtils.loadItemStack("graniTier2", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack graniTier3() {
        return NbtUtils.loadItemStack("graniTier3", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack haronTier1() {
        return NbtUtils.loadItemStack("haronTier1", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack haronTier2() {
        return NbtUtils.loadItemStack("haronTier2", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack haronTier3() {
        return NbtUtils.loadItemStack("haronTier3", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack phoenixTier1() {
        return NbtUtils.loadItemStack("phoenixTier1", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack phoenixTier2() {
        return NbtUtils.loadItemStack("phoenixTier2", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack phoenixTier3() {
        return NbtUtils.loadItemStack("phoenixTier3", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack tritonTier1() {
        return NbtUtils.loadItemStack("tritonTier1", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack tritonTier2() {
        return NbtUtils.loadItemStack("tritonTier2", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack tritonTier3() {
        return NbtUtils.loadItemStack("tritonTier3", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack krush() {
        return NbtUtils.loadItemStack("krush", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack karatel() {
        return NbtUtils.loadItemStack("karatel", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    //Сферы
    public ItemStack andromedaTier1() {
        return NbtUtils.loadItemStack("andromedaTier1", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack andromedaTier2() {
        return NbtUtils.loadItemStack("andromedaTier2", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack andromedaTier3() {
        return NbtUtils.loadItemStack("andromedaTier3", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack apollonaTier1() {
        return NbtUtils.loadItemStack("apollonaTier1", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack apollonaTier2() {
        return NbtUtils.loadItemStack("apollonaTier2", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack apollonaTier3() {
        return NbtUtils.loadItemStack("apollonaTier3", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack astreaTier1() {
        return NbtUtils.loadItemStack("astreaTier1", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack astreaTier2() {
        return NbtUtils.loadItemStack("astreaTier2", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack astreaTier3() {
        return NbtUtils.loadItemStack("astreaTier3", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack himeraTier1() {
        return NbtUtils.loadItemStack("himeraTier1", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack himeraTier2() {
        return NbtUtils.loadItemStack("himeraTier2", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack himeraTier3() {
        return NbtUtils.loadItemStack("himeraTier3", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack osirisaTier1() {
        return NbtUtils.loadItemStack("osirisaTier1", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack osirisaTier2() {
        return NbtUtils.loadItemStack("osirisaTier2", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack osirisaTier3() {
        return NbtUtils.loadItemStack("osirisaTier3", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack pandoraTier1() {
        return NbtUtils.loadItemStack("pandoraTier1", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack pandoraTier2() {
        return NbtUtils.loadItemStack("pandoraTier2", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack pandoraTier3() {
        return NbtUtils.loadItemStack("pandoraTier3", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack titanTier1() {
        return NbtUtils.loadItemStack("titanTier1", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack titanTier2() {
        return NbtUtils.loadItemStack("titanTier2", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }

    public ItemStack titanTier3() {
        return NbtUtils.loadItemStack("titanTier3", MotherHack.getInstance().getAbItemsDir(), mc.getNetworkHandler().getRegistryManager());
    }
}