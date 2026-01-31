package fun.motherhack.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AntiVomit extends Module {

    private final NumberSetting dotCount = new NumberSetting("Dot Count", 12, 3, 30, 1);
    private final NumberSetting dotSize = new NumberSetting("Dot Size", 3f, 1f, 10f, 0.5f);
    private final NumberSetting smoothness = new NumberSetting("Smoothness", 0.15f, 0.05f, 0.5f, 0.01f);
    private final NumberSetting distance = new NumberSetting("Distance", 150f, 50f, 300f, 10f);
    private final BooleanSetting rainbow = new BooleanSetting("Rainbow", false);
    private final BooleanSetting useUIColor = new BooleanSetting("Use UI Color", true);
    private final NumberSetting opacity = new NumberSetting("Opacity", 0.3f, 0.1f, 1f, 0.05f);

    private final List<StabilizationDot> dots = new ArrayList<>();
    private float lastYaw = 0;
    private float lastPitch = 0;
    private final Random random = new Random();

    public AntiVomit() {
        super("AntiVomit", Category.Render);
        getSettings().add(dotCount);
        getSettings().add(dotSize);
        getSettings().add(smoothness);
        getSettings().add(distance);
        getSettings().add(rainbow);
        getSettings().add(useUIColor);
        getSettings().add(opacity);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        initializeDots();
        if (!fullNullCheck()) {
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        dots.clear();
    }

    private void initializeDots() {
        dots.clear();
        int count = dotCount.getValue().intValue();
        for (int i = 0; i < count; i++) {
            float angle = (float) (2 * Math.PI * i / count);
            float baseDistance = distance.getValue();
            float randomOffset = (random.nextFloat() - 0.5f) * 20f;
            float dist = baseDistance + randomOffset;
            dots.add(new StabilizationDot(angle, dist));
        }
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        if (!toggled) return;
        if (fullNullCheck()) return;
        if (mc.options.hudHidden) return;

        if (dots.size() != dotCount.getValue().intValue()) {
            initializeDots();
        }

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();
        float deltaYaw = MathHelper.wrapDegrees(currentYaw - lastYaw);
        float deltaPitch = currentPitch - lastPitch;
        lastYaw = currentYaw;
        lastPitch = currentPitch;

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;

        MatrixStack matrices = event.getContext().getMatrices();
        matrices.push();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        long time = System.currentTimeMillis();
        
        for (int i = 0; i < dots.size(); i++) {
            StabilizationDot dot = dots.get(i);
            dot.offsetX -= deltaYaw * 2.5f;
            dot.offsetY += deltaPitch * 2.5f;
            dot.offsetX = MathHelper.lerp(smoothness.getValue(), dot.offsetX, 0);
            dot.offsetY = MathHelper.lerp(smoothness.getValue(), dot.offsetY, 0);

            float angle = dot.angle;
            float dist = dot.distance;
            float x = centerX + (float) Math.cos(angle) * dist + dot.offsetX;
            float y = centerY + (float) Math.sin(angle) * dist + dot.offsetY;

            Color color;
            if (rainbow.getValue()) {
                float hue = (time / 3000f + i / (float) dots.size()) % 1f;
                color = Color.getHSBColor(hue, 0.8f, 1f);
            } else if (useUIColor.getValue()) {
                color = MotherHack.getInstance().getModuleManager().getModule(UI.class).getTheme().getAccentColor();
            } else {
                color = Color.WHITE;
            }

            int alpha = (int) (255 * opacity.getValue());
            int finalColor = (alpha << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();

            float size = dotSize.getValue();
            MatrixStack.Entry matrix = matrices.peek();
            
            buffer.vertex(matrix.getPositionMatrix(), x - size, y - size, 0).color(finalColor);
            buffer.vertex(matrix.getPositionMatrix(), x - size, y + size, 0).color(finalColor);
            buffer.vertex(matrix.getPositionMatrix(), x + size, y + size, 0).color(finalColor);
            buffer.vertex(matrix.getPositionMatrix(), x + size, y - size, 0).color(finalColor);
        }

        try {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } catch (Exception ex) {
            if (toggled) {
                ex.printStackTrace();
            }
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private static class StabilizationDot {
        float angle;
        float distance;
        float offsetX = 0;
        float offsetY = 0;

        StabilizationDot(float angle, float distance) {
            this.angle = angle;
            this.distance = distance;
        }
    }
}
