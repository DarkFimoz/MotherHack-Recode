package fun.motherhack.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Fonts;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

public class Arrows extends Module {
    NumberSetting radius = new NumberSetting("Радиус", 20, 10f, 100f, 1f);
    NumberSetting size = new NumberSetting("Размер", 10, 1f, 30f, 1f);

    public Arrows() {
        super("Arrows", Category.Render);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventHandler
    public void onRender(EventRender2D event) {


        if (mc.player == null || mc.world == null) return;

        float x = (float) mc.getWindow().getScaledWidth() / 2;
        float y = (float) mc.getWindow().getScaledHeight() / 2;

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || p.isSpectator() || mc.player.distanceTo(p) < 2) continue;

            double angle = getAngle(p, mc.gameRenderer.getCamera().getPos(), mc.gameRenderer.getCamera().getYaw());
            float arrowX = x + (float) (radius.getValue() * Math.sin(angle));
            float arrowY = y - (float) (radius.getValue() * Math.cos(angle));

            render(event.getContext().getMatrices(), event.getContext(), arrowX, arrowY, (float) Math.toDegrees(angle),p);
        }
    }

    private static double getAngle(PlayerEntity player, Vec3d cameraPos, float cYaw) {
        double playerX = lerp(mc.getRenderTickCounter().getTickDelta(true), player.prevX, player.getX());
        double playerZ = lerp(mc.getRenderTickCounter().getTickDelta(true), player.prevZ, player.getZ());

        double deltaX = playerX - cameraPos.x;
        double deltaZ = playerZ - cameraPos.z;

        double angle = Math.atan2(deltaZ, deltaX);

        double yawRadians = Math.toRadians(cYaw + 90.0f);

        double angle2 = angle - yawRadians;

        while (angle2 > Math.PI) {
            angle2 -= 2 * Math.PI;
        }

        while (angle2 < -Math.PI) {
            angle2 += 2 * Math.PI;
        }

        return angle2;
    }

    private void render(MatrixStack matrices, DrawContext context, float x, float y, float r,PlayerEntity player) {

        matrices.push();
        matrices.translate(x, y, 0);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(r));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float fontWidth = Fonts.BOLD.getWidth("A",8);
        if (MotherHack.getInstance().getFriendManager().isFriend(player.getName().getString())) {
            Render2D.drawTexture(matrices, -fontWidth / 2f, 6f, size.getValue(), size.getValue(), 0, MotherHack.id("hud/arrow.png"), Color.GREEN);
        } else {
            Render2D.drawTexture(matrices, -fontWidth / 2f, 6f, size.getValue(), size.getValue(), 0, MotherHack.id("hud/arrow.png"), Color.WHITE);
        }

        RenderSystem.disableBlend();
        matrices.pop();
    }

    public static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }
}
