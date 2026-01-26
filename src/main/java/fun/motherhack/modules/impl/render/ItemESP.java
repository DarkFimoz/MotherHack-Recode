package fun.motherhack.modules.impl.render;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.api.events.impl.EventRender3D;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.ColorSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.Render3D;
import fun.motherhack.utils.render.fonts.Fonts;
import fun.motherhack.utils.world.WorldUtils;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4d;

import java.awt.*;

public class ItemESP extends Module {

    @Getter
    public enum ESPMode implements Nameable {
        Rect("Rect"),
        Circle("Circle"),
        None("None");

        private final String name;

        ESPMode(String name) {
            this.name = name;
        }
    }

    private final BooleanSetting shadow = new BooleanSetting("settings.itemesp.shadow", true);
    private final ColorSetting shadowColor = new ColorSetting("settings.itemesp.shadowcolor", new Color(0, 0, 0, 255));
    private final ColorSetting textColor = new ColorSetting("settings.itemesp.textcolor", new Color(255, 255, 255, 255));
    private final EnumSetting<ESPMode> espMode = new EnumSetting<>("settings.itemesp.mode", ESPMode.Rect);
    private final NumberSetting radius = new NumberSetting("settings.itemesp.radius", 1f, 0.1f, 5f, 0.1f, () -> espMode.getValue() == ESPMode.Circle);
    private final BooleanSetting useHudColor = new BooleanSetting("settings.itemesp.usehudcolor", true, () -> espMode.getValue() == ESPMode.Circle);
    private final NumberSetting colorOffset = new NumberSetting("settings.itemesp.coloroffset", 2, 1, 50, 1, () -> espMode.getValue() == ESPMode.Circle && useHudColor.getValue());
    private final ColorSetting circleColor = new ColorSetting("settings.itemesp.circlecolor", new Color(255, 255, 255, 255), () -> espMode.getValue() == ESPMode.Circle && !useHudColor.getValue());
    private final NumberSetting circlePoints = new NumberSetting("settings.itemesp.circlepoints", 12, 3, 32, 1, () -> espMode.getValue() == ESPMode.Circle);
    private final NumberSetting range = new NumberSetting("settings.itemesp.range", 100f, 10f, 5000f, 10f);

    public ItemESP() {
        super("ItemESP", Category.Render);
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck()) return;

        for (Entity ent : mc.world.getEntities()) {
            if (!(ent instanceof ItemEntity)) continue;
            if (mc.player.distanceTo(ent) > range.getValue()) continue;

            Vec3d[] vectors = getPoints(ent, e.getTickCounter().getTickDelta(true));
            Vector4d position = null;

            for (Vec3d vector : vectors) {
                vector = WorldUtils.getPosition(new Vec3d(vector.x, vector.y, vector.z));
                if (vector.z > 0 && vector.z < 1) {
                    if (position == null)
                        position = new Vector4d(vector.x, vector.y, vector.z, 0);
                    position.x = Math.min(vector.x, position.x);
                    position.y = Math.min(vector.y, position.y);
                    position.z = Math.max(vector.x, position.z);
                    position.w = Math.max(vector.y, position.w);
                }
            }

            if (position != null) {
                float posX = (float) position.x;
                float posY = (float) position.y;
                float endPosX = (float) position.z;
                float diff = (endPosX - posX) / 2f;
                String itemName = ent.getDisplayName().getString();
                float textWidth = Fonts.REGULAR.getWidth(itemName, 9f);
                float tagX = (posX + diff - textWidth / 2f);
                float rectWidth = textWidth + 4;
                float rectHeight = 10;

                if (shadow.getValue()) {
                    Render2D.drawBlurredRect(e.getContext().getMatrices(),
                            tagX - 2, posY - 13,
                            rectWidth, rectHeight,
                            1.5f, 10f,
                            shadowColor.getValue());
                }

                Render2D.drawFont(e.getContext().getMatrices(),
                        Fonts.REGULAR.getFont(9f),
                        itemName,
                        tagX, posY - 12.5f,
                        textColor.getValue());
            }
        }

        if (espMode.getValue() == ESPMode.Rect) {
            renderRectESP(e);
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;
        if (espMode.getValue() != ESPMode.Circle) return;

        for (Entity ent : mc.world.getEntities()) {
            if (!(ent instanceof ItemEntity)) continue;
            if (mc.player.distanceTo(ent) > range.getValue()) continue;
            renderCircleESP(e.getMatrixStack(), ent, e.getTickCounter().getTickDelta(true));
        }
    }

    private void renderRectESP(EventRender2D event) {
        for (Entity ent : mc.world.getEntities()) {
            if (!(ent instanceof ItemEntity)) continue;
            if (mc.player.distanceTo(ent) > range.getValue()) continue;

            Vec3d[] vectors = getPoints(ent, event.getTickCounter().getTickDelta(true));
            Vector4d position = null;

            for (Vec3d vector : vectors) {
                vector = WorldUtils.getPosition(new Vec3d(vector.x, vector.y, vector.z));
                if (vector.z > 0 && vector.z < 1) {
                    if (position == null)
                        position = new Vector4d(vector.x, vector.y, vector.z, 0);
                    position.x = Math.min(vector.x, position.x);
                    position.y = Math.min(vector.y, position.y);
                    position.z = Math.max(vector.x, position.z);
                    position.w = Math.max(vector.y, position.w);
                }
            }

            if (position != null) {
                float posX = (float) position.x;
                float posY = (float) position.y;
                float endPosX = (float) position.z;
                float endPosY = (float) position.w;

                drawRect(event.getContext(), posX, posY, endPosX, endPosY);
            }
        }
    }

    private void drawRect(DrawContext context, float posX, float posY, float endPosX, float endPosY) {
        Color black = Color.BLACK;
        Color hudColor = getHudColor();

        // Black outline
        Render2D.drawRoundedRect(context.getMatrices(), posX - 1F, posY, 1.5f, endPosY - posY + 0.5f, 0f, black);
        Render2D.drawRoundedRect(context.getMatrices(), posX - 1F, posY - 0.5f, endPosX - posX + 0.5f, 1.5f, 0f, black);
        Render2D.drawRoundedRect(context.getMatrices(), endPosX - 1f, posY, 1.5f, endPosY - posY + 0.5f, 0f, black);
        Render2D.drawRoundedRect(context.getMatrices(), posX - 1, endPosY - 1f, endPosX - posX + 0.5f, 1.5f, 0f, black);

        // Colored lines
        Render2D.drawRoundedRect(context.getMatrices(), posX - 0.5f, posY, 1f, endPosY - posY, 0f, hudColor);
        Render2D.drawRoundedRect(context.getMatrices(), posX, endPosY - 0.5f, endPosX - posX, 1f, 0f, hudColor);
        Render2D.drawRoundedRect(context.getMatrices(), posX - 0.5f, posY, endPosX - posX, 0.5f, 0f, hudColor);
        Render2D.drawRoundedRect(context.getMatrices(), endPosX - 0.5f, posY, 1f, endPosY - posY, 0f, hudColor);
    }

    private void renderCircleESP(MatrixStack stack, Entity ent, float tickDelta) {
        double x = MathHelper.lerp(tickDelta, ent.prevX, ent.getX());
        double y = MathHelper.lerp(tickDelta, ent.prevY, ent.getY());
        double z = MathHelper.lerp(tickDelta, ent.prevZ, ent.getZ());

        Vec3d pos = new Vec3d(x, y, z);
        Color color = useHudColor.getValue() ? getHudColor() : circleColor.getValue();

        // Simple circle rendering using box
        Box box = new Box(
                pos.x - radius.getValue(), y, pos.z - radius.getValue(),
                pos.x + radius.getValue(), y + 0.1, pos.z + radius.getValue()
        );

        Render3D.renderBox(stack, box, color);
    }

    private Vec3d[] getPoints(Entity ent, float tickDelta) {
        Box axisAlignedBB = getBox(ent, tickDelta);
        return new Vec3d[]{
                new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ),
                new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ),
                new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ),
                new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ),
                new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ),
                new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ),
                new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ),
                new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)
        };
    }

    private Box getBox(Entity ent, float tickDelta) {
        double x = MathHelper.lerp(tickDelta, ent.prevX, ent.getX());
        double y = MathHelper.lerp(tickDelta, ent.prevY, ent.getY());
        double z = MathHelper.lerp(tickDelta, ent.prevZ, ent.getZ());

        Box axisAlignedBB2 = ent.getBoundingBox();
        return new Box(
                axisAlignedBB2.minX - ent.getX() + x - 0.05,
                axisAlignedBB2.minY - ent.getY() + y,
                axisAlignedBB2.minZ - ent.getZ() + z - 0.05,
                axisAlignedBB2.maxX - ent.getX() + x + 0.05,
                axisAlignedBB2.maxY - ent.getY() + y + 0.15,
                axisAlignedBB2.maxZ - ent.getZ() + z + 0.05
        );
    }

    private Color getHudColor() {
        UI uiModule = MotherHack.getInstance().getModuleManager().getModule(UI.class);
        if (uiModule != null && uiModule.getTheme() != null) {
            return uiModule.getTheme().getAccentColor();
        }
        return new Color(255, 255, 255);
    }
}
