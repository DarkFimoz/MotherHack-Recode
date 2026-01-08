package fun.motherhack.utils.auction.nbt;

import fun.motherhack.utils.Wrapper;
import lombok.experimental.UtilityClass;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryWrapper;

import java.io.*;
import java.util.*;

@UtilityClass
public class NbtUtils implements Wrapper {

    public <T extends ISerializable<?>> NbtList listToTag(Iterable<T> list) {
        NbtList tag = new NbtList();
        for (T item : list) tag.add(item.toTag());
        return tag;
    }

    public <T> List<T> listFromTag(NbtList tag, ToValue<T> toItem) {
        List<T> list = new ArrayList<>(tag.size());
        for (NbtElement itemTag : tag) {
            T value = toItem.toValue(itemTag);
            if (value != null) list.add(value);
        }
        return list;
    }

    public <K, V extends ISerializable<?>> NbtCompound mapToTag(Map<K, V> map) {
        NbtCompound tag = new NbtCompound();
        for (K key : map.keySet()) tag.put(key.toString(), map.get(key).toTag());
        return tag;
    }

    public <K, V> Map<K, V> mapFromTag(NbtCompound tag, ToKey<K> toKey, ToValue<V> toValue) {
        Map<K, V> map = new HashMap<>(tag.getSize());
        for (String key : tag.getKeys()) map.put(toKey.toKey(key), toValue.toValue(tag.get(key)));
        return map;
    }

    public boolean containsAllKeys(NbtCompound source, NbtCompound required) {
        for (String key : required.getKeys()) {
            if (!source.contains(key)) {
                return false;
            }
        }
        return true;
    }

    public boolean matchesNbtValues(NbtCompound source, NbtCompound required) {
        for (String key : required.getKeys()) {
            if (!source.contains(key)) {
                return false;
            }

            NbtElement sourceElement = source.get(key);
            NbtElement requiredElement = required.get(key);

            if (!sourceElement.equals(requiredElement)) {
                return false;
            }
        }
        return true;
    }

    public NbtCompound copyNbtKeys(NbtCompound source, String... keys) {
        NbtCompound result = new NbtCompound();
        for (String key : keys) if (source.contains(key)) result.put(key, source.get(key).copy());
        return result;
    }

    public boolean hasKeyOfType(NbtCompound nbt, String key, byte type) {
        return nbt.contains(key, type);
    }

    public boolean toClipboard(ISerializable<?> serializable) {
        return toClipboard(serializable.toTag());
    }

    public boolean toClipboard(NbtCompound tag) {
        String preClipboard = mc.keyboard.getClipboard();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag, byteArrayOutputStream);
            mc.keyboard.setClipboard(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            mc.keyboard.setClipboard(preClipboard);
            return false;
        }
    }

    public boolean fromClipboard(ISerializable<?> serializable) {
        NbtCompound tag = fromClipboard();
        if (tag == null) return false;
        NbtCompound sourceTag = serializable.toTag();
        for (String key : sourceTag.getKeys()) if (!tag.contains(key)) return false;

        serializable.fromTag(tag);
        return true;
    }

    public NbtCompound fromClipboard() {
        try {
            byte[] data = Base64.getDecoder().decode(mc.keyboard.getClipboard().trim());
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            return NbtIo.readCompressed(new DataInputStream(bis), NbtSizeTracker.ofUnlimitedBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean writeToFile(NbtCompound tag, File file) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            NbtIo.writeCompressed(tag, fos);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public NbtCompound readFromFile(File file) {
        if (!file.exists()) return null;
        try (FileInputStream fis = new FileInputStream(file)) {
            return NbtIo.readCompressed(fis, NbtSizeTracker.ofUnlimitedBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean saveItemStack(String name, ItemStack stack, File directory, RegistryWrapper.WrapperLookup registries) {
        if (!directory.exists() && !directory.mkdirs()) return false;
        File file = new File(directory, name + ".nbt");
        try {
            NbtElement nbt = ItemStack.CODEC.encodeStart(registries.getOps(NbtOps.INSTANCE), stack).getOrThrow();
            if (nbt instanceof NbtCompound tag) return writeToFile(tag, file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public ItemStack loadItemStack(String name, File directory, RegistryWrapper.WrapperLookup registries) {
        try {
            File file = new File(directory, name + ".nbt");
            NbtCompound tag = readFromFile(file);
            if (tag == null) return ItemStack.EMPTY;
            Optional<ItemStack> stack = ItemStack.CODEC.parse(registries.getOps(NbtOps.INSTANCE), tag).result();
            return stack.orElse(ItemStack.EMPTY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ItemStack.EMPTY;
    }

    public interface ToKey<T> {
        T toKey(String string);
    }

    public interface ToValue<T> {
        T toValue(NbtElement tag);
    }
}