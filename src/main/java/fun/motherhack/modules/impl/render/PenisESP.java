package fun.motherhack.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender3D;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.ColorSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PenisESP extends Module {
    public PenisESP() {
        super("PenisESP", Category.Render);
    }

    private final BooleanSetting onlyOwn = new BooleanSetting("OnlyOwn", false);
    private final NumberSetting ballSize = new NumberSetting("BallSize", 0.1f, 0.1f, 0.5f, 0.01f);
    private final NumberSetting penisSize = new NumberSetting("PenisSize", 1.5f, 0.1f, 3.0f, 0.1f);
    private final NumberSetting friendSize = new NumberSetting("FriendSize", 1.5f, 0.1f, 3.0f, 0.1f);
    private final NumberSetting enemySize = new NumberSetting("EnemySize", 0.5f, 0.1f, 3.0f, 0.1f);
    private final NumberSetting gradation = new NumberSetting("Gradation", 30f, 20f, 100f, 1f);
    private final ColorSetting penisColor = new ColorSetting(new Color(231, 180, 122, 255));
    private final ColorSetting headColor = new ColorSetting(new Color(240, 50, 180, 255));
    
    // New settings for better rendering control (inspired by ChinaHat)
    private final BooleanSetting renderBehind = new BooleanSetting("Render Behind Player", true);
    private final BooleanSetting disableDepthTest = new BooleanSetting("Disable Depth Test", true);

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;
        
        // Setup render state before rendering anything
        setupRenderState();
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (onlyOwn.getValue() && player != mc.player) continue;
            double size = (MotherHack.getInstance().getFriendManager().isFriend(player.getName().getString()) ? friendSize.getValue() : (player != mc.player ? enemySize.getValue() : penisSize.getValue()));

            Vec3d base = getBase(player, event.getTickCounter().getTickDelta(true));
            Vec3d forward = base.add(0, player.getHeight() / 2.4, 0).add(Vec3d.fromPolar(0, player.getYaw()).multiply(0.1));

            Vec3d left = forward.add(Vec3d.fromPolar(0, player.getYaw() - 90).multiply(ballSize.getValue()));
            Vec3d right = forward.add(Vec3d.fromPolar(0, player.getYaw() + 90).multiply(ballSize.getValue()));

            drawSphere(player, left, ballSize.getValue(), gradation.getValue(), penisColor.getColor(), event.getMatrixStack(), event.getTickCounter().getTickDelta(true));
            drawSphere(player, right, ballSize.getValue(), gradation.getValue(), penisColor.getColor(), event.getMatrixStack(), event.getTickCounter().getTickDelta(true));
            drawPenis(player, event.getMatrixStack(), size, forward, event.getTickCounter().getTickDelta(true));
        }
        
        // Restore render state after rendering
        restoreRenderState();
    }

    public Vec3d getBase(Entity entity, float tickDelta) {
        double x = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
        double y = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
        double z = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());

        return new Vec3d(x, y, z);
    }
    
    // Render state management methods (inspired by ChinaHat)
    private void setupRenderState() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        
        if (renderBehind.getValue()) {
            // Disable depth test and depth mask to render behind everything
            if (disableDepthTest.getValue()) {
                RenderSystem.disableDepthTest();
            }
            RenderSystem.depthMask(false); // Don't write to depth buffer
            
            // Set polygon offset to prevent z-fighting
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

    public void drawSphere(PlayerEntity player, Vec3d center, double radius, float gradation, Color color, MatrixStack matrices, float tickDelta) {
        matrices.push();
        
        // Перемещаемся в центр сферы
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        matrices.translate(center.x - cameraPos.x, center.y - cameraPos.y, center.z - cameraPos.z);
        
        // Настройки рендеринга
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        
        Matrix4f mat = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        
        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;
        
        // Рисуем сферу с помощью треугольников
        int stacks = (int) gradation;
        int slices = (int) gradation;
        
        for (int i = 0; i <= stacks; ++i) {
            double lat0 = Math.PI * (-0.5 + (double) (i - 1) / stacks);
            double z0 = Math.sin(lat0) * radius;
            double zr0 = Math.cos(lat0) * radius;
            
            double lat1 = Math.PI * (-0.5 + (double) i / stacks);
            double z1 = Math.sin(lat1) * radius;
            double zr1 = Math.cos(lat1) * radius;
            
            for (int j = 0; j <= slices; ++j) {
                double lng = 2 * Math.PI * (double) (j - 1) / slices;
                double x1 = Math.cos(lng) * zr1;
                double y1 = Math.sin(lng) * zr1;
                
                double x0 = Math.cos(lng) * zr0;
                double y0 = Math.sin(lng) * zr0;
                
                buffer.vertex(mat, (float)x0, (float)y0, (float)z0).color(r, g, b, a);
                buffer.vertex(mat, (float)x1, (float)y1, (float)z1).color(r, g, b, a);
            }
        }
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        matrices.pop();
    }

    public void drawPenis(PlayerEntity player, MatrixStack matrices, double length, Vec3d start, float tickDelta) {
        Vec3d base = getBase(player, tickDelta);
        Vec3d forwardDir = Vec3d.fromPolar(0, player.getYaw()).normalize();
        
        // Центральная точка начала
        Vec3d centerStart = base.add(0, player.getHeight() / 2.4, 0);
        // Конечная точка
        Vec3d centerEnd = centerStart.add(forwardDir.multiply(length));
        
        // Рисуем цилиндр (ствол пениса)
        drawCylinder(centerStart, centerEnd, 0.08, gradation.getValue(), penisColor.getColor(), matrices, tickDelta);
        
        // Рисуем головку пениса (сфера на конце)
        drawSphere(player, centerEnd, 0.1f, gradation.getValue(), headColor.getColor(), matrices, tickDelta);
    }
    
    public void drawCylinder(Vec3d start, Vec3d end, double radius, float gradation, Color color, MatrixStack matrices, float tickDelta) {
        matrices.push();
        
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        Vec3d direction = end.subtract(start).normalize();
        
        // Находим произвольный вектор, перпендикулярный направлению
        Vec3d arbitrary;
        if (Math.abs(direction.x) < 0.1 && Math.abs(direction.z) < 0.1) {
            arbitrary = new Vec3d(1, 0, 0);
        } else {
            arbitrary = new Vec3d(0, 1, 0);
        }
        
        // Находим два перпендикулярных вектора
        Vec3d u = direction.crossProduct(arbitrary).normalize();
        Vec3d v = direction.crossProduct(u).normalize();
        
        // Настройки рендеринга
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        
        Matrix4f mat = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        
        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;
        
        int segments = (int) gradation;
        
        for (int i = 0; i <= segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            double cos = Math.cos(angle) * radius;
            double sin = Math.sin(angle) * radius;
            
            // Вектор смещения для круга
            Vec3d offset = u.multiply(cos).add(v.multiply(sin));
            
            // Точки на начале и конце цилиндра
            Vec3d pointStart = start.add(offset).subtract(cameraPos);
            Vec3d pointEnd = end.add(offset).subtract(cameraPos);
            
            buffer.vertex(mat, (float)pointStart.x, (float)pointStart.y, (float)pointStart.z).color(r, g, b, a);
            buffer.vertex(mat, (float)pointEnd.x, (float)pointEnd.y, (float)pointEnd.z).color(r, g, b, a);
        }
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        // Рисуем крышки цилиндра
        drawCircle(start, direction.multiply(-1), radius, segments, color, matrices, tickDelta);
        drawCircle(end, direction, radius, segments, color, matrices, tickDelta);
        
        matrices.pop();
    }
    
    public void drawCircle(Vec3d center, Vec3d normal, double radius, int segments, Color color, MatrixStack matrices, float tickDelta) {
        // Находим два перпендикулярных вектора для плоскости круга
        Vec3d arbitrary;
        if (Math.abs(normal.x) < 0.1 && Math.abs(normal.z) < 0.1) {
            arbitrary = new Vec3d(1, 0, 0);
        } else {
            arbitrary = new Vec3d(0, 1, 0);
        }
        
        Vec3d u = normal.crossProduct(arbitrary).normalize();
        Vec3d v = normal.crossProduct(u).normalize();
        
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        
        Matrix4f mat = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        
        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;
        
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        Vec3d centerRel = center.subtract(cameraPos);
        
        // Центральная точка
        buffer.vertex(mat, (float)centerRel.x, (float)centerRel.y, (float)centerRel.z).color(r, g, b, a);
        
        // Точки по окружности
        for (int i = 0; i <= segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            double cos = Math.cos(angle) * radius;
            double sin = Math.sin(angle) * radius;
            
            Vec3d point = center.add(u.multiply(cos).add(v.multiply(sin))).subtract(cameraPos);
            buffer.vertex(mat, (float)point.x, (float)point.y, (float)point.z).color(r, g, b, a);
        }
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
}