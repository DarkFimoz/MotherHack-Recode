package fun.motherhack.utils.auction.nbt;

import net.minecraft.nbt.NbtCompound;

public interface ISerializable<T> {
    NbtCompound toTag();
    void fromTag(NbtCompound tag);
}