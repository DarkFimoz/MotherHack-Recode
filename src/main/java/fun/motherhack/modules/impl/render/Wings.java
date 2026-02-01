package fun.motherhack.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.motherhack.api.events.impl.EventRender3D;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
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

public class Wings extends Module {
    public Wings() {
        super("Wings", Category.Render);
    }

    private final BooleanSetting renderBehind = new BooleanSetting("render behind player", false);
    private final BooleanSetting disableDepthTest = new BooleanSetting("disable depth test", false);
    private final BooleanSetting onlySelf = new BooleanSetting("only self", false);

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;

        float tickDelta = event.getTickCounter().getTickDelta(true);
        setupRenderState();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player && mc.options.getPerspective().isFirstPerson()) continue;
            if (onlySelf.getValue() && player != mc.player) continue;

            Vec3d base = getBase(player, tickDelta);
            // Крылья крепятся к спине (между плечами), чуть выше
            Vec3d attach = base.add(0, player.getHeight() * 0.65, 0);

            drawParticleWing(player, attach, -1, event.getMatrixStack(), tickDelta);
            drawParticleWing(player, attach, 1, event.getMatrixStack(), tickDelta);
        }

        restoreRenderState();
    }

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

    private void drawParticleWing(PlayerEntity player, Vec3d attach, int side, MatrixStack matrices, float tickDelta) {
        matrices.push();

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        matrices.translate(attach.x - cameraPos.x, attach.y - cameraPos.y, attach.z - cameraPos.z);

        double yawRad = Math.toRadians(player.getYaw());
        Vec3d backDir = new Vec3d(-Math.sin(yawRad), 0, Math.cos(yawRad));

        float time = (player.age + tickDelta) * 0.1f;
        float flap = (float) Math.sin(time) * 0.2f;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Matrix4f mat = matrices.peek().getPositionMatrix();

        // Параметры крыла как на картинке
        double wingSpan = 2.0;      // Размах крыла (горизонтально)
        int featherCount = 12;      // Количество перьев
        
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, 
            VertexFormats.POSITION_COLOR);

        // Массив для хранения контурных точек
        Vec3d[][] featherOutlines = new Vec3d[featherCount][2]; // [feather][left/right edge]

        // Рисуем крыло из перьев
        for (int feather = 0; feather < featherCount; feather++) {
            double featherProgress = (double) feather / (featherCount - 1);
            
            // Угол наклона пера (веерообразное раскрытие)
            double angle = featherProgress * 60 - 15; // От -15 до 45 градусов
            double angleRad = Math.toRadians(angle);
            
            // Длина пера (внешние перья длиннее)
            double featherLength = wingSpan * (0.6 + 0.4 * Math.sin(featherProgress * Math.PI));
            
            // Ширина пера
            double featherWidth = 0.15;
            
            // Количество сегментов в пере
            int segments = 20;
            
            Vec3d prevLeftEdge = null;
            Vec3d prevRightEdge = null;
            Vec3d prevCenter = null;
            
            for (int seg = 0; seg < segments; seg++) {
                double segProgress = (double) seg / (segments - 1);
                
                // Позиция вдоль пера
                double dist = featherLength * segProgress;
                
                // Ширина пера сужается к концу
                double width = featherWidth * (1.0 - segProgress * 0.7);
                
                // Эффект взмаха
                double flapOffset = flap * segProgress * 0.3;
                
                // Вычисляем позицию точки пера
                double lateralYawRad = Math.toRadians(player.getYaw() + (side == -1 ? -90 : 90));
                
                // Основное направление пера
                double x = Math.cos(angleRad) * dist;
                double y = Math.sin(angleRad) * dist + flapOffset;
                
                // Крыло немного уходит назад
                double backOffset = -0.2 * segProgress;
                
                // Преобразуем в мировые координаты
                Vec3d lateral = new Vec3d(-Math.sin(lateralYawRad), 0, Math.cos(lateralYawRad)).multiply(x);
                Vec3d back = backDir.multiply(backOffset);
                Vec3d pos = new Vec3d(lateral.x + back.x, y + back.y, lateral.z + back.z);
                
                // Цвет - черный с красными кончиками
                float r, g, b;
                if (segProgress > 0.7) {
                    // Красные кончики
                    float tipProgress = (float)((segProgress - 0.7) / 0.3);
                    r = 0.1f + tipProgress * 0.8f;
                    g = 0.1f;
                    b = 0.1f;
                } else {
                    // Черное основание
                    r = 0.05f;
                    g = 0.05f;
                    b = 0.05f;
                }
                
                float alpha = 0.9f * (1.0f - (float)segProgress * 0.2f);
                
                // Вычисляем края пера для контура
                Vec3d lineDir = new Vec3d(Math.cos(lateralYawRad), 0, Math.sin(lateralYawRad));
                Vec3d leftEdge = pos.add(lineDir.multiply(-width / 2));
                Vec3d rightEdge = pos.add(lineDir.multiply(width / 2));
                
                // Сохраняем крайние точки для контура между перьями
                if (seg == segments - 1) {
                    featherOutlines[feather][0] = leftEdge;
                    featherOutlines[feather][1] = rightEdge;
                }
                
                // Рисуем контур пера
                if (prevLeftEdge != null) {
                    // Левый край пера
                    buffer.vertex(mat, (float)prevLeftEdge.x, (float)prevLeftEdge.y, (float)prevLeftEdge.z)
                        .color(r * 1.2f, g * 1.2f, b * 1.2f, alpha);
                    buffer.vertex(mat, (float)leftEdge.x, (float)leftEdge.y, (float)leftEdge.z)
                        .color(r * 1.2f, g * 1.2f, b * 1.2f, alpha);
                    
                    // Правый край пера
                    buffer.vertex(mat, (float)prevRightEdge.x, (float)prevRightEdge.y, (float)prevRightEdge.z)
                        .color(r * 1.2f, g * 1.2f, b * 1.2f, alpha);
                    buffer.vertex(mat, (float)rightEdge.x, (float)rightEdge.y, (float)rightEdge.z)
                        .color(r * 1.2f, g * 1.2f, b * 1.2f, alpha);
                    
                    // Центральная линия пера (стержень)
                    buffer.vertex(mat, (float)prevCenter.x, (float)prevCenter.y, (float)prevCenter.z)
                        .color(r * 1.5f, g * 1.5f, b * 1.5f, alpha);
                    buffer.vertex(mat, (float)pos.x, (float)pos.y, (float)pos.z)
                        .color(r * 1.5f, g * 1.5f, b * 1.5f, alpha);
                }
                
                prevLeftEdge = leftEdge;
                prevRightEdge = rightEdge;
                prevCenter = pos;
                
                // Рисуем несколько линий для создания объема пера
                for (int line = 0; line < 3; line++) {
                    double lineOffset = (line - 1) * width / 2;
                    
                    Vec3d finalPos = pos.add(lineDir.multiply(lineOffset));
                    
                    float px = (float) finalPos.x;
                    float py = (float) finalPos.y;
                    float pz = (float) finalPos.z;
                    
                    // Рисуем крестик для каждой точки
                    float size = 0.015f;
                    
                    buffer.vertex(mat, px - size, py, pz).color(r, g, b, alpha);
                    buffer.vertex(mat, px + size, py, pz).color(r, g, b, alpha);
                    
                    buffer.vertex(mat, px, py - size, pz).color(r, g, b, alpha);
                    buffer.vertex(mat, px, py + size, pz).color(r, g, b, alpha);
                }
            }
        }
        
        // Рисуем контур между перьями (соединяем кончики)
        for (int feather = 0; feather < featherCount - 1; feather++) {
            Vec3d leftTip1 = featherOutlines[feather][0];
            Vec3d rightTip1 = featherOutlines[feather][1];
            Vec3d leftTip2 = featherOutlines[feather + 1][0];
            Vec3d rightTip2 = featherOutlines[feather + 1][1];
            
            if (leftTip1 != null && leftTip2 != null) {
                // Соединяем левые края соседних перьев
                buffer.vertex(mat, (float)leftTip1.x, (float)leftTip1.y, (float)leftTip1.z)
                    .color(0.9f, 0.2f, 0.2f, 0.8f);
                buffer.vertex(mat, (float)leftTip2.x, (float)leftTip2.y, (float)leftTip2.z)
                    .color(0.9f, 0.2f, 0.2f, 0.8f);
            }
            
            if (rightTip1 != null && rightTip2 != null) {
                // Соединяем правые края соседних перьев
                buffer.vertex(mat, (float)rightTip1.x, (float)rightTip1.y, (float)rightTip1.z)
                    .color(0.9f, 0.2f, 0.2f, 0.8f);
                buffer.vertex(mat, (float)rightTip2.x, (float)rightTip2.y, (float)rightTip2.z)
                    .color(0.9f, 0.2f, 0.2f, 0.8f);
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();
    }

    public Vec3d getBase(net.minecraft.entity.Entity entity, float tickDelta) {
        double x = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
        double y = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
        double z = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());

        return new Vec3d(x, y, z);
    }
}