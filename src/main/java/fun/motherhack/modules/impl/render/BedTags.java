package fun.motherhack.modules.impl.render;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import fun.motherhack.MotherHack;
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

public class BedTags extends Module {

    public NumberSetting range = new NumberSetting("Range", 1000f, 10f, 1000f, 10f);

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

            // Find both parts of the bed
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

            // Render boxes for both parts of the bed
            Box headBox = BlockUtils.getBoundingBox(headPos);
            Render3D.renderBox(matrices, headBox, fillColor);
            Render3D.renderBoxOutline(matrices, headBox, outlineColor);

            Box footBox = BlockUtils.getBoundingBox(footPos);
            Render3D.renderBox(matrices, footBox, fillColor);
            Render3D.renderBoxOutline(matrices, footBox, outlineColor);
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

            // Find both parts of the bed
            BlockPos headPos = bed.getPos();
            Direction facing = mc.world.getBlockState(headPos).get(net.minecraft.block.BedBlock.FACING);
            BlockPos footPos = headPos.offset(facing.getOpposite());

            // Calculate center of the entire bed
            Vec3d bedCenter = new Vec3d(
                (headPos.getX() + footPos.getX()) / 2.0 + 0.5,
                headPos.getY() + 1.5, // Position text above the bed
                (headPos.getZ() + footPos.getZ()) / 2.0 + 0.5
            );

            Vec3d position = WorldUtils.getPosition(bedCenter);

            String name = mc.world.getBlockState(bed.getPos()).getBlock().getName().getString();
            name += " " + String.format("%.1f", distance) + "m";

            float textWidth = Fonts.REGULAR.getWidth(name, 9f);
            float centerX = (float) position.getX();
            float centerY = (float) position.getY();

            if (!(position.z > 0) || !(position.z < 1)) continue;

            // Render background
            Render2D.drawRoundedRect(e.getContext().getMatrices(),
                    centerX - textWidth / 2f - 4f,
                    centerY - 6f,
                    textWidth + 8f,
                    12f,
                    2f,
                    new Color(0, 0, 0, 150)
            );

            // Render text
            Render2D.drawFont(e.getContext().getMatrices(),
                    Fonts.REGULAR.getFont(9f),
                    name,
                    centerX - textWidth / 2f,
                    centerY - 4f,
                    Color.WHITE
            );
        }
    }
}
