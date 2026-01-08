package fun.motherhack.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.motherhack.api.events.impl.EventRender3D;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.ColorSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;

public class AngelWingsESP extends Module {
    public AngelWingsESP() {
        super("AngelWingsESP", Category.Render);
    }

    private final BooleanSetting onlyOwn = new BooleanSetting("only self", true);
    private final NumberSetting wingSpan = new NumberSetting("wing span", 1.5f, 0.6f, 6.0f, 0.05f);
    private final NumberSetting wingHeight = new NumberSetting("wing height", 0.3f, 0.3f, 3.5f, 0.05f);
    private final NumberSetting thickness = new NumberSetting("thikness", 0.3f, 0.01f, 0.4f, 0.01f);
    private final NumberSetting gradation = new NumberSetting("gradation", 8f, 8f, 128f, 1f);
    private final ColorSetting wingColor = new ColorSetting(new Color(230, 230, 255, 220));
    private final BooleanSetting renderBehind = new BooleanSetting("render behind player", false);
    private final BooleanSetting disableDepthTest = new BooleanSetting("disable depth test", false);
    
    // Анимационные переменные
    private long lastRenderTime = System.currentTimeMillis();
    private float animationTime = 0f;
    private float prevFlap = 0f;
    private float prevFlutter = 0f;
    private float currentFlap = 0f;
    private float currentFlutter = 0f;
    private final NumberSetting flapSpeed = new NumberSetting("flap speed", 0.08f, 0.01f, 0.5f, 0.01f);
    private final NumberSetting flutterSpeed = new NumberSetting("flutter speed", 0.2f, 0.05f, 1.0f, 0.01f);
    private final NumberSetting flapAmplitude = new NumberSetting("flap amplitude", 0.6f, 0.1f, 2.0f, 0.05f);
    private final NumberSetting flutterAmplitude = new NumberSetting("flutter amplitude", 0.03f, 0.01f, 0.2f, 0.01f);

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;

        // Обновление времени анимации с интерполяцией
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastRenderTime) / 1000f;
        lastRenderTime = currentTime;
        
        // Плавное обновление анимационных параметров
        animationTime += deltaTime * 20f; // Умножаем на 20 для сохранения скорости в тиках
        float tickDelta = event.getTickCounter().getTickDelta(true);
        
        // Интерполяция анимационных параметров
        prevFlap = currentFlap;
        prevFlutter = currentFlutter;
        
        // Плавные волновые функции для анимации
        float animTime = animationTime * 0.05f; // Замедляем общее время
        
        // Основной взмах с использованием косинуса для более плавного движения
        currentFlap = MathHelper.cos(animTime * flapSpeed.getValue().floatValue()) * flapAmplitude.getValue().floatValue();
        
        // Мелкие колебания с разными частотами для естественности
        float flutter1 = MathHelper.sin(animTime * flutterSpeed.getValue().floatValue()) * flutterAmplitude.getValue().floatValue();
        float flutter2 = MathHelper.sin(animTime * flutterSpeed.getValue().floatValue() * 1.7f) * flutterAmplitude.getValue().floatValue() * 0.6f;
        float flutter3 = MathHelper.cos(animTime * flutterSpeed.getValue().floatValue() * 0.8f) * flutterAmplitude.getValue().floatValue() * 0.4f;
        currentFlutter = (flutter1 + flutter2 + flutter3) / 3f;
        
        // Интерполяция между предыдущим и текущим кадром
        float interpFlap = MathHelper.lerp(tickDelta, prevFlap, currentFlap);
        float interpFlutter = MathHelper.lerp(tickDelta, prevFlutter, currentFlutter);
        
        setupRenderState();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player && mc.options.getPerspective().isFirstPerson()) continue;
            if (onlyOwn.getValue() && player != mc.player) continue;

            Vec3d base = getBase(player, tickDelta);
            Vec3d attach = base.add(0, player.getHeight() * 0.9, 0);

            double yawRad = Math.toRadians(player.getYaw());
            Vec3d backDir = new Vec3d(-Math.sin(yawRad), 0, Math.cos(yawRad));

            double halfSpan = wingSpan.getValue().doubleValue() * 0.5;
            double leftYawRad = Math.toRadians(player.getYaw() - 90);
            double rightYawRad = Math.toRadians(player.getYaw() + 90);
            Vec3d leftRoot = attach.add(new Vec3d(-Math.sin(leftYawRad) * halfSpan * 0.25, 0, Math.cos(leftYawRad) * halfSpan * 0.25));
            Vec3d rightRoot = attach.add(new Vec3d(-Math.sin(rightYawRad) * halfSpan * 0.25, 0, Math.cos(rightYawRad) * halfSpan * 0.25));

            // Используем интерполированные значения анимации
            drawWingLayerAnimated(player, leftRoot, -1, backDir, wingSpan.getValue().doubleValue(), 
                wingHeight.getValue().doubleValue(), thickness.getValue().doubleValue(), 
                gradation.getValue().intValue(), wingColor.getColor(), event.getMatrixStack(), 
                tickDelta, LayerType.BASE, interpFlap, interpFlutter);
            drawWingLayerAnimated(player, leftRoot, -1, backDir, wingSpan.getValue().doubleValue(), 
                wingHeight.getValue().doubleValue(), thickness.getValue().doubleValue(), 
                gradation.getValue().intValue(), wingColor.getColor(), event.getMatrixStack(), 
                tickDelta, LayerType.PRIMARY, interpFlap, interpFlutter);
            drawWingLayerAnimated(player, leftRoot, -1, backDir, wingSpan.getValue().doubleValue(), 
                wingHeight.getValue().doubleValue(), thickness.getValue().doubleValue(), 
                gradation.getValue().intValue(), wingColor.getColor(), event.getMatrixStack(), 
                tickDelta, LayerType.SECONDARY, interpFlap, interpFlutter);

            drawWingLayerAnimated(player, rightRoot, 1, backDir, wingSpan.getValue().doubleValue(), 
                wingHeight.getValue().doubleValue(), thickness.getValue().doubleValue(), 
                gradation.getValue().intValue(), wingColor.getColor(), event.getMatrixStack(), 
                tickDelta, LayerType.BASE, interpFlap, interpFlutter);
            drawWingLayerAnimated(player, rightRoot, 1, backDir, wingSpan.getValue().doubleValue(), 
                wingHeight.getValue().doubleValue(), thickness.getValue().doubleValue(), 
                gradation.getValue().intValue(), wingColor.getColor(), event.getMatrixStack(), 
                tickDelta, LayerType.PRIMARY, interpFlap, interpFlutter);
            drawWingLayerAnimated(player, rightRoot, 1, backDir, wingSpan.getValue().doubleValue(), 
                wingHeight.getValue().doubleValue(), thickness.getValue().doubleValue(), 
                gradation.getValue().intValue(), wingColor.getColor(), event.getMatrixStack(), 
                tickDelta, LayerType.SECONDARY, interpFlap, interpFlutter);
        }

        restoreRenderState();
    }

    private enum LayerType { BASE, PRIMARY, SECONDARY }

    private void setupRenderState() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        if (renderBehind.getValue()) {
            if (disableDepthTest.getValue()) {
                RenderSystem.disableDepthTest();
            } else {
                RenderSystem.enableDepthTest();
            }
            RenderSystem.depthMask(false);
            RenderSystem.polygonOffset(1.0f, -1100000.0f);
            RenderSystem.enablePolygonOffset();
        } else {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        }
    }

    private void restoreRenderState() {
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();

        if (renderBehind.getValue()) {
            RenderSystem.disablePolygonOffset();
        }
    }

    private void drawWingLayerAnimated(PlayerEntity player, Vec3d root, int side, Vec3d backDir, 
                                      double span, double height, double thick, int segments, 
                                      Color color, MatrixStack matrices, float tickDelta, 
                                      LayerType layer, float flap, float flutter) {
        matrices.push();

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        matrices.translate(root.x - cameraPos.x, root.y - cameraPos.y, root.z - cameraPos.z);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Matrix4f mat = matrices.peek().getPositionMatrix();

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, 
            VertexFormats.POSITION_COLOR);

        float baseR = color.getRed() / 255.0f;
        float baseG = color.getGreen() / 255.0f;
        float baseB = color.getBlue() / 255.0f;
        float baseA = color.getAlpha() / 255.0f;

        double layerScale = 1.0;
        double layerOffsetBack = 0.0;
        float layerAlphaMul = 1.0f;
        int segs = segments;
        
        switch (layer) {
            case BASE:
                layerScale = 1.0;
                layerOffsetBack = 0.0;
                layerAlphaMul = 0.75f;
                segs = Math.max(segments, 28);
                break;
            case PRIMARY:
                layerScale = 0.75;
                layerOffsetBack = 0.08 * span;
                layerAlphaMul = 0.9f;
                segs = Math.max(segments - 6, 16);
                break;
            case SECONDARY:
                layerScale = 0.45;
                layerOffsetBack = 0.16 * span;
                layerAlphaMul = 0.95f;
                segs = Math.max(segments - 12, 12);
                break;
        }

        for (int i = 0; i <= segs; i++) {
            double t = (double) i / segs;
            
            // Более плавная функция взмаха с использованием косинуса
            double flapFactor = Math.cos(Math.PI * t) * flap * (0.7 + 0.3 * (1 - t));
            
            // Плавное затухание взмаха к концу крыла
            double flapDecay = Math.cos(t * Math.PI / 2); // Math.PI/2 = HALF_PI
            
            // Плавная кривая для формы крыла
            double wingCurve = Math.sin(t * Math.PI);
            double wingCurveSmoothed = wingCurve * (0.95 + 0.05 * Math.cos(t * Math.PI));
            
            // Используем Math.pow для плавной кривой
            double reach = span * (0.15 + 0.85 * Math.pow(t, 0.9)) * layerScale * 
                          (1.0 + 0.06 * flap * flapDecay);
            
            double y = height * (wingCurveSmoothed + 0.05 * (1 - t)) * layerScale + flapFactor;
            
            // Более плавные колебания
            double flutterOffset = flutter * Math.sin(t * Math.PI * 2.5) * 
                                  Math.cos(t * Math.PI * 0.5);
            
            Vec3d bend = backDir.multiply(-(0.2 + 0.4 * (1 - t)) * t * span - layerOffsetBack);
            
            double rootFold = Math.max(0.0, 0.25 - t * 0.5);
            y += rootFold * height * 0.6;

            double lateralYawRad = Math.toRadians(player.getYaw() + (side == -1 ? -90 : 90));
            Vec3d lateral = new Vec3d(-Math.sin(lateralYawRad), 0, Math.cos(lateralYawRad)).multiply(reach);

            // Более плавное скручивание крыла
            double twist = (0.12 * Math.sin(t * Math.PI)) + flutterOffset * 0.4;
            double twistSmooth = twist * Math.cos(t * Math.PI / 2); // Math.PI/2 = HALF_PI
            
            Vec3d twistOffset = new Vec3d(backDir.x * twistSmooth * side * 0.5, 
                                         twistSmooth * 0.2, 
                                         backDir.z * twistSmooth * side * 0.5);

            Vec3d top = new Vec3d(lateral.x + bend.x + twistOffset.x, 
                                 y + bend.y + twistOffset.y, 
                                 lateral.z + bend.z + twistOffset.z);
                                 
            Vec3d inner = new Vec3d(lateral.x * (1 - thick * (1 + 0.5 * t)) + bend.x + twistOffset.x, 
                                   (y - thick * (1 + 0.3 * t)) + bend.y + twistOffset.y, 
                                   lateral.z * (1 - thick * (1 + 0.5 * t)) + bend.z + twistOffset.z);

            // Плавное изменение прозрачности и цвета
            float alpha = baseA * layerAlphaMul * (float)(0.85 + 0.15 * (1 - t));
            float alphaSmoothed = alpha * (float)Math.cos(t * Math.PI / 2 * 0.3); // Math.PI/2 = HALF_PI
            
            float rr = Math.min(1.0f, baseR + 0.06f * (float)(1 - t));
            float gg = Math.min(1.0f, baseG + 0.06f * (float)(1 - t));
            float bb = Math.min(1.0f, baseB + 0.06f * (float)(1 - t));

            buffer.vertex(mat, (float) top.x, (float) top.y, (float) top.z)
                  .color(rr, gg, bb, alphaSmoothed);
            buffer.vertex(mat, (float) inner.x, (float) inner.y, (float) inner.z)
                  .color(rr, gg, bb, alphaSmoothed * 0.9f);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        drawWingOutlineAnimated(mat, player, segs, span * layerScale, height * layerScale, 
                               thick * (0.8 + 0.2 * layerScale), side, backDir, color, 
                               layer, matrices, flap, flutter);

        if (layer == LayerType.PRIMARY || layer == LayerType.SECONDARY) {
            drawFeatherTipsAnimated(mat, player, segs, span * layerScale, height * layerScale, 
                                   thick * 0.6, side, backDir, color, matrices, flap, flutter);
        }

        matrices.pop();
    }

    private void drawWingOutlineAnimated(Matrix4f mat, PlayerEntity player, int segments, 
                                        double span, double height, double thick, int side, 
                                        Vec3d backDir, Color color, LayerType layer, 
                                        MatrixStack matrices, float flap, float flutter) {
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINE_STRIP, 
            VertexFormats.POSITION_COLOR);

        float lr = Math.min(1.0f, color.getRed() / 255.0f + 0.12f);
        float lg = Math.min(1.0f, color.getGreen() / 255.0f + 0.12f);
        float lb = Math.min(1.0f, color.getBlue() / 255.0f + 0.12f);
        float la = Math.min(1.0f, color.getAlpha() / 255.0f);

        float lineAlpha = la * (layer == LayerType.BASE ? 0.6f : 0.9f);

        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            double flapFactor = Math.cos(Math.PI * t) * flap * (0.7 + 0.3 * (1 - t));
            double flapDecay = Math.cos(t * Math.PI / 2); // Math.PI/2 = HALF_PI
            
            double reach = span * (0.15 + 0.85 * Math.pow(t, 0.9));
            double wingCurve = Math.sin(t * Math.PI);
            double y = height * (wingCurve * 0.95 + 0.05 * (1 - t)) + flapFactor * flapDecay;
            
            Vec3d bend = backDir.multiply(-(0.2 + 0.4 * (1 - t)) * t * span);
            
            double lateralYawRad = Math.toRadians(player.getYaw() + (side == -1 ? -90 : 90));
            Vec3d lateral = new Vec3d(-Math.sin(lateralYawRad), 0, Math.cos(lateralYawRad)).multiply(reach);
            Vec3d top = new Vec3d(lateral.x + bend.x, y + bend.y, lateral.z + bend.z);

            buf.vertex(mat, (float) top.x, (float) top.y, (float) top.z)
               .color(lr, lg, lb, lineAlpha);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void drawFeatherTipsAnimated(Matrix4f mat, PlayerEntity player, int segments, 
                                        double span, double height, double thick, int side, 
                                        Vec3d backDir, Color color, MatrixStack matrices, 
                                        float flap, float flutter) {
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, 
            VertexFormats.POSITION_COLOR);

        float baseR = color.getRed() / 255.0f;
        float baseG = color.getGreen() / 255.0f;
        float baseB = color.getBlue() / 255.0f;
        float baseA = color.getAlpha() / 255.0f;

        int step = Math.max(2, segments / 10);
        for (int i = 0; i <= segments; i += step) {
            double t = (double) i / segments;
            double flapDecay = Math.cos(t * Math.PI / 2); // Math.PI/2 = HALF_PI
            double flapFactor = Math.cos(Math.PI * t) * flap * 0.5 * flapDecay;
            
            double reach = span * (0.15 + 0.85 * Math.pow(t, 0.9));
            double y = height * (Math.sin(t * Math.PI) * 0.95 + 0.05 * (1 - t)) + flapFactor;
            
            Vec3d bend = backDir.multiply(-(0.2 + 0.4 * (1 - t)) * t * span);
            double lateralYawRad = Math.toRadians(player.getYaw() + (side == -1 ? -90 : 90));
            Vec3d lateral = new Vec3d(-Math.sin(lateralYawRad), 0, Math.cos(lateralYawRad)).multiply(reach);

            Vec3d tip = new Vec3d(lateral.x + bend.x, y + bend.y, lateral.z + bend.z);
            Vec3d inner = new Vec3d(lateral.x * 0.85 + bend.x, (y - thick * 1.2) + bend.y, lateral.z * 0.85 + bend.z);
            Vec3d feather = new Vec3d(lateral.x * 1.08 + bend.x - backDir.x * 0.03, 
                                     (y + 0.06) + bend.y, 
                                     lateral.z * 1.08 + bend.z - backDir.z * 0.03);

            float alpha = baseA * 0.9f * (float)(0.95 - 0.25 * t);
            float alphaSmoothed = alpha * (float)Math.cos(t * Math.PI / 2 * 0.5); // Math.PI/2 = HALF_PI
            
            float rr = Math.min(1.0f, baseR + 0.08f * (float)(1 - t));
            float gg = Math.min(1.0f, baseG + 0.08f * (float)(1 - t));
            float bb = Math.min(1.0f, baseB + 0.08f * (float)(1 - t));

            buf.vertex(mat, (float) tip.x, (float) tip.y, (float) tip.z)
               .color(rr, gg, bb, alphaSmoothed);
            buf.vertex(mat, (float) inner.x, (float) inner.y, (float) inner.z)
               .color(rr, gg, bb, alphaSmoothed * 0.75f);
            buf.vertex(mat, (float) feather.x, (float) feather.y, (float) feather.z)
               .color(rr, gg, bb, alphaSmoothed * 0.9f);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    public Vec3d getBase(net.minecraft.entity.Entity entity, float tickDelta) {
        double x = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
        double y = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
        double z = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());

        return new Vec3d(x, y, z);
    }
}