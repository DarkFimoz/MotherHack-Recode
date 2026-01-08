package fun.motherhack.utils.world;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.chunk.WorldChunk;
import fun.motherhack.utils.Wrapper;
import fun.motherhack.utils.math.MathUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BlockUtils implements Wrapper {

    private static final Set<Block> UNBREAKABLE = new ReferenceOpenHashSet<>(Set.of(
            Blocks.BEDROCK,
            Blocks.COMMAND_BLOCK,
            Blocks.CHAIN_COMMAND_BLOCK,
            Blocks.END_PORTAL_FRAME,
            Blocks.BARRIER
    ));

    public static boolean isHole(BlockPos pos) {
        return isHole(pos, true, false, false);
    }

    public static boolean isHole(BlockPos pos, boolean canStand, boolean checkTrap, boolean anyBlock) {
        int blockProgress = 0;
        for (Direction i : Direction.values()) {
            if (i == Direction.UP || i == Direction.DOWN) continue;
            if (anyBlock && !mc.world.isAir(pos.offset(i)) || isHard(pos.offset(i)))
                blockProgress++;
        }
        return (!checkTrap || (getBlock(pos) == Blocks.AIR && getBlock(pos.add(0, 1, 0)) == Blocks.AIR
                && getBlock(pos.add(0, 2, 0)) == Blocks.AIR))
                && blockProgress > 3 && (!canStand || getState(pos.add(0, -1, 0)).blocksMovement());
    }

    public static BlockState getState(BlockPos pos) {
        return mc.world.getBlockState(pos);
    }

    public static Block getBlock(BlockPos pos) {
        return getState(pos).getBlock();
    }

    public static boolean isHard(BlockPos pos) {
        Block block = getState(pos).getBlock();
        return block == Blocks.OBSIDIAN || block == Blocks.NETHERITE_BLOCK || block == Blocks.ENDER_CHEST || block == Blocks.BEDROCK || block == Blocks.ANVIL;
    }

    public static BlockPos getPlayerPos(PlayerEntity player) {
        return new BlockPos((int) Math.floor(player.getX()), (int) Math.floor(player.getY()), (int) Math.floor(player.getZ()));
    }

    public static BlockPos getPlayerPos(boolean fix) {
        return new BlockPosX(mc.player.getPos(), fix);
    }

    public static class BlockPosX extends BlockPos {

        public BlockPosX(double x, double y, double z) {
            super(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
        }

        public BlockPosX(double x, double y, double z, boolean fix) {
            this(x, y + (fix ? 0.3 : 0), z);
        }

        public BlockPosX(Vec3d vec3d) {
            this(vec3d.x, vec3d.y, vec3d.z);
        }

        public BlockPosX(Vec3d vec3d, boolean fix) {
            this(vec3d.x, vec3d.y, vec3d.z, fix);
        }
    }

    public static boolean isUnbreakable(BlockPos pos) {
        if (mc.world == null) return false;

        return isUnbreakable(mc.world.getBlockState(pos).getBlock());
    }

    public static boolean isUnbreakable(Block block) {
        return UNBREAKABLE.contains(block);
    }

    private static VoxelShape getOutlineShape(BlockPos pos) {
        return getState(pos).getOutlineShape(mc.world, pos);
    }

    public static Box getBoundingBox(BlockPos pos) {
        VoxelShape shape = getOutlineShape(pos);
        if (!shape.isEmpty()) {
            return shape.getBoundingBox().offset(pos);
        } else {
            return createBox(pos, 0.5, 0.5, 1, false);
        }
    }

    public static Box createBox(BlockPos blockPos, double length, double width, double height, boolean yOnCenter) {
        Vec3d pos = new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + (yOnCenter ? 0.5 : 0), blockPos.getZ() + 0.5);
        return new Box(pos.x - length, (yOnCenter ? (pos.y - height) : pos.y ), pos.z - width, pos.x + length, pos.y + height, pos.z + width);
    }

    public static List<BlockPos> getAllBlocks(PlayerEntity player, double blockRange, boolean motion) {
        List<BlockPos> allBlocks = new ArrayList<>();
        int range = (int) Math.round(blockRange);
        if (motion) player.getPos().add(new Vec3d(player.getVelocity().x, player.getVelocity().y, player.getVelocity().z));

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    allBlocks.add(player.getBlockPos().add(x, y, z));
                }
            }
        }

        return allBlocks;
    }

    public static Stream<WorldChunk> getLoadedChunks() {
        int radius = Math.max(2, mc.options.getClampedViewDistance()) + 3;
        int diameter = radius * 2 + 1;

        ChunkPos center = mc.player.getChunkPos();
        ChunkPos min = new ChunkPos(center.x - radius, center.z - radius);
        ChunkPos max = new ChunkPos(center.x + radius, center.z + radius);

        return Stream.iterate(min, pos -> {
                    int x = pos.x;
                    int z = pos.z;
                    x++;
                    if (x > max.x) {
                        x = min.x;
                        z++;
                    }
                    if(z > max.z) throw new IllegalStateException("Stream limit didn't work.");
                    return new ChunkPos(x, z);
                }).limit((long) diameter * diameter)
                .filter(c -> mc.world.isChunkLoaded(c.x, c.z))
                .map(c -> mc.world.getChunk(c.x, c.z)).filter(Objects::nonNull);
    }

    public static Stream<BlockEntity> getLoadedBlockEntities() {
        return getLoadedChunks()
                .flatMap(chunk -> chunk.getBlockEntities().values().stream());
    }

    public static ArrayList<BlockEntity> getLoadedBlockEntitiesOnArrayList() {
        return getLoadedBlockEntities().collect(Collectors.toCollection(ArrayList::new));
    }
}
