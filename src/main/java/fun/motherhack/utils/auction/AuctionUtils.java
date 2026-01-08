package fun.motherhack.utils.auction;

import fun.motherhack.utils.Wrapper;
import fun.motherhack.utils.auction.nbt.NbtUtils;
import lombok.experimental.UtilityClass;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class AuctionUtils implements Wrapper {

    public int getPrice(NbtComponent component) {
        if (component != null) {
            NbtCompound compound = component.copyNbt().getCompound("display");
            NbtList list = compound.getList("Lore", 8);
            for (int i = 0; i < list.size(); i++) {
                String line = list.getString(i);

                if (line.contains("$")) {
                    line = line.replaceAll("§.", "");
                    Pattern pattern = Pattern.compile("\\$(\\d{1,3}(?:,\\d{3})*)");
                    Matcher matcher = pattern.matcher(line);

                    if (matcher.find()) {
                        String price = matcher.group(1).replace(",", "");
                        return Integer.parseInt(price);
                    }
                }
            }
        }

        return -1;
    }

    public boolean compareEnchantments(ItemStack stack1, ItemStack stack2) {
        NbtComponent nbt1 = stack1.getComponents().get(DataComponentTypes.CUSTOM_DATA);
        NbtComponent nbt2 = stack2.getComponents().get(DataComponentTypes.CUSTOM_DATA);
        if (nbt1 == null || nbt2 == null) return nbt1 == nbt2;
        NbtCompound enchants1 = NbtUtils.copyNbtKeys(nbt1.copyNbt(), "Enchantments");
        NbtCompound enchants2 = NbtUtils.copyNbtKeys(nbt2.copyNbt(), "Enchantments");

        return NbtUtils.matchesNbtValues(enchants1, enchants2);
    }
}