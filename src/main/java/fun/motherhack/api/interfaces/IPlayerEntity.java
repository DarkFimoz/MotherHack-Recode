package fun.motherhack.api.interfaces;

import net.minecraft.nbt.NbtCompound;

public interface IPlayerEntity {
    void setShoulderEntityLeft(NbtCompound nbt);
    void setShoulderEntityRight(NbtCompound nbt);
}
