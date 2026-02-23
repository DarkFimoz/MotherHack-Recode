package fun.motherhack.modules.impl.render;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.api.events.impl.EventRender3D;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.Render3D;
import fun.motherhack.utils.render.fonts.Fonts;
import fun.motherhack.utils.world.BlockUtils;
import fun.motherhack.utils.world.WorldUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class BedTags extends Module {

    public NumberSetting range = new NumberSetting("Range", 1000f, 10f, 1000f, 10f);
    private final Map<Integer, ItemStack> bedItemCache = new HashMap<>();

    public BedTags() {
        super("BedTags", Category.Render);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        MatrixStack matrices = e.getMatrixStack();

        for (BlockEntity entity : BlockUtils.getLoadedBlockEntitiesOnArrayList()) {
            if (!(entity instanceof BedBlockEntity bed)) continue;
            if (!mc.world.getBlockState(bed.getPos()).get(net.minecraft.block.BedBlock.PART).equals(net.minecraft.block.enums.BedPart.HEAD)) continue;
            double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(bed.getPos()));
            if (distance > range.getValue()) continue;

            BlockPos headPos = bed.getPos();
            Direction facing = mc.world.getBlockState(headPos).get(net.minecraft.block.BedBlock.FACING);
            BlockPos footPos = headPos.offset(facing.getOpposite());

            int originalColor = bed.getColor().getEntityColor();
            Color fillColor = new Color(
                    (originalColor >> 16) & 0xFF,
                    (originalColor >> 8) & 0xFF,
                    originalColor & 0xFF,
                    50
            );
            Color outlineColor = new Color(bed.getColor().getEntityColor());

            // Render combined box for both parts without partition
            Box combinedBox = new Box(
                Math.min(headPos.getX(), footPos.getX()),
                headPos.getY(),
                Math.min(headPos.getZ(), footPos.getZ()),
                Math.max(headPos.getX(), footPos.getX()) + 1,
                headPos.getY() + 0.5625,
                Math.max(headPos.getZ(), footPos.getZ()) + 1
            );
            
            Render3D.renderBox(matrices, combinedBox, fillColor);
            Render3D.renderBoxOutline(matrices, combinedBox, outlineColor);
        }
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck()) return;

        for (BlockEntity entity : BlockUtils.getLoadedBlockEntitiesOnArrayList()) {
            if (!(entity instanceof BedBlockEntity bed)) continue;
            if (!mc.world.getBlockState(bed.getPos()).get(net.minecraft.block.BedBlock.PART).equals(net.minecraft.block.enums.BedPart.HEAD)) continue;
            double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(bed.getPos()));
            if (distance > range.getValue()) continue;

            BlockPos headPos = bed.getPos();
            Direction facing = mc.world.getBlockState(headPos).get(net.minecraft.block.BedBlock.FACING);
            BlockPos footPos = headPos.offset(facing.getOpposite());

            Vec3d bedCenter = new Vec3d(
                (headPos.getX() + footPos.getX()) / 2.0 + 0.5,
                headPos.getY() + 0.8,
                (headPos.getZ() + footPos.getZ()) / 2.0 + 0.5
            );

            Vec3d position = WorldUtils.getPosition(bedCenter);
            if (!(position.z > 0) || !(position.z < 1)) continue;

            ItemStack bedItem = getBedItemForColor(bed.getColor().getId());
            String distanceText = String.format("%.0fm", distance);
            
            float textWidth = Fonts.MEDIUM.getWidth(distanceText, 7f);
            float iconSize = 16f;
            float spacing = 4f;
            float totalWidth = iconSize + spacing + textWidth;
            
            float centerX = (float) position.getX();
            float centerY = (float) position.getY();
            float startX = centerX - totalWidth / 2f;

            MatrixStack matrices = e.getContext().getMatrices();
            
            // Render bed icon
            matrices.push();
            matrices.translate(startX, centerY - iconSize / 2f, 0);
            matrices.scale(1.0f, 1.0f, 1.0f);
            e.getContext().drawItem(bedItem, 0, 0);
            matrices.pop();

            // Render distance text
            Render2D.drawFont(matrices,
                    Fonts.MEDIUM.getFont(7f),
                    distanceText,
                    startX + iconSize + spacing,
                    centerY - 3f,
                    Color.WHITE
            );
        }
    }

    private ItemStack getBedItemForColor(int colorId) {
        return bedItemCache.computeIfAbsent(colorId, id -> {
            return switch (id) {
                case 0 -> new ItemStack(Items.WHITE_BED);
                case 1 -> new ItemStack(Items.ORANGE_BED);
                case 2 -> new ItemStack(Items.MAGENTA_BED);
                case 3 -> new ItemStack(Items.LIGHT_BLUE_BED);
                case 4 -> new ItemStack(Items.YELLOW_BED);
                case 5 -> new ItemStack(Items.LIME_BED);
                case 6 -> new ItemStack(Items.PINK_BED);
                case 7 -> new ItemStack(Items.GRAY_BED);
                case 8 -> new ItemStack(Items.LIGHT_GRAY_BED);
                case 9 -> new ItemStack(Items.CYAN_BED);
                case 10 -> new ItemStack(Items.PURPLE_BED);
                case 11 -> new ItemStack(Items.BLUE_BED);
                case 12 -> new ItemStack(Items.BROWN_BED);
                case 13 -> new ItemStack(Items.GREEN_BED);
                case 14 -> new ItemStack(Items.RED_BED);
                case 15 -> new ItemStack(Items.BLACK_BED);
                default -> new ItemStack(Items.WHITE_BED);
            };
        });
    }
}
