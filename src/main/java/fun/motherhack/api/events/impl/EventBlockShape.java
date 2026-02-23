package fun.motherhack.api.events.impl;

import fun.motherhack.api.events.Event;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;

public class EventBlockShape extends Event {
    private final BlockPos pos;
    private VoxelShape shape;

    public EventBlockShape(BlockPos pos, VoxelShape shape) {
        this.pos = pos;
        this.shape = shape;
    }

    public BlockPos getPos() {
        return pos;
    }

    public VoxelShape getShape() {
        return shape;
    }

    public void setShape(VoxelShape shape) {
        this.shape = shape;
    }
}
