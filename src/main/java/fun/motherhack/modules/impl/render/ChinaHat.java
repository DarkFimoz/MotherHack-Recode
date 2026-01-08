package fun.motherhack.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import fun.motherhack.MotherHack;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.ColorSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.utils.math.MathUtils;
import net.minecraft.client.gl.ShaderProgramKeys;
import fun.motherhack.api.events.impl.EventRender3D;
import meteordevelopment.orbit.EventHandler;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import fun.motherhack.modules.settings.api.Nameable;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class ChinaHat extends Module {
    @AllArgsConstructor @Getter
    public enum ColorMode implements Nameable {
        Custom("Custom"),
        UIColor("UIColor");

        private final String name;
    }

    private final NumberSetting size = new NumberSetting("Size", 0.5f, 0.1f, 3f, 0.1f);
    private final NumberSetting height = new NumberSetting("Height", 0.3f, 0.05f, 2f, 0.05f);
    private final NumberSetting brimThickness = new NumberSetting("BrimThickness", 0.01f, 0.01f, 1f, 0.01f);
    private final EnumSetting<ColorMode> colorMode = new EnumSetting<>("Color Mode", ColorMode.UIColor);
    private final BooleanSetting useGradient = new BooleanSetting("Use Gradient", false);
    private final ColorSetting color = new ColorSetting(new Color(0x64007CFF, true));
    private final ColorSetting gradientLeft = new ColorSetting(new Color(0xFF000000, true)); // Black
    private final ColorSetting gradientRight = new ColorSetting(new Color(0xFFFFFFFF, true)); // White
    private final NumberSetting gradientSpeed = new NumberSetting("GradientSpeed", 0.5f, 0f, 5f, 0.1f);
    private final BooleanSetting gradientLooped = new BooleanSetting("GradientLooped", false);
    private final NumberSetting gradientPeriod = new NumberSetting("GradientPeriod", 4.0f, 0.1f, 60f, 0.1f);
    private final NumberSetting outlineGlowStrength = new NumberSetting("OutlineGlow", 0.6f, 0f, 3f, 0.1f);
    private final BooleanSetting outlineEnabled = new BooleanSetting("Outline", true);
    private final ColorSetting outlineColor = new ColorSetting(new Color(0x64007CFF));
    private final EnumSetting<HatStyle> hatStyle = new EnumSetting<>("Style", HatStyle.CHINA_HAT);
    
    // New Year settings
    private final NumberSetting pomPomSize = new NumberSetting("PomPom Size", 0.15f, 0.05f, 0.5f, 0.01f);
    private final ColorSetting pomPomColor = new ColorSetting(new Color(0xFFFF0000, true)); // Red
    private final NumberSetting stripeWidth = new NumberSetting("Stripe Width", 0.2f, 0.05f, 1f, 0.01f);
    private final ColorSetting stripeColor = new ColorSetting(new Color(0xFFFFFFFF, true)); // White
    private final BooleanSetting showBells = new BooleanSetting("Bells", true);
    private final NumberSetting bellCount = new NumberSetting("Bell Count", 4, 1, 8, 1);
    private final NumberSetting bellSize = new NumberSetting("Bell Size", 0.08f, 0.03f, 0.2f, 0.01f);
    private final ColorSetting bellColor = new ColorSetting(new Color(0xFFFFFF00, true)); // Yellow/Gold
    private final NumberSetting sparkleSpeed = new NumberSetting("Sparkle Speed", 1f, 0.1f, 5f, 0.1f);
    private final BooleanSetting rotatingMode = new BooleanSetting("Rotating Mode", false);
    private final NumberSetting rotationSpeed = new NumberSetting("Rotation Speed", 1f, 0f, 5f, 0.1f);
    private final BooleanSetting snowflakesSetting = new BooleanSetting("Snowflakes", true);
    private final NumberSetting snowflakeCount = new NumberSetting("Snowflake Count", 20, 5, 50, 1);
    private final NumberSetting snowflakeSize = new NumberSetting("Snowflake Size", 0.05f, 0.01f, 0.2f, 0.01f);
    private final NumberSetting snowflakeSpeed = new NumberSetting("Snowflake Speed", 0.5f, 0.1f, 2f, 0.1f);
    
    // New settings
    private final BooleanSetting hideInFirstPerson = new BooleanSetting("Hide in First Person", true);
    private final BooleanSetting renderBehind = new BooleanSetting("Render Behind Player", true);
    private final BooleanSetting disableDepthTest = new BooleanSetting("Disable Depth Test", true);
    
    private float rotationAngle = 0f;
    private final List<Snowflake> snowflakes = new ArrayList<>();
    private final Random random = new Random();
    
    private static class Snowflake {
        float x, y, z;
        float speed;
        float size;
        float rotation;
        float rotationSpeed;
        
        Snowflake(float x, float y, float z, float speed, float size) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.speed = speed;
            this.size = size;
            this.rotation = (float) (Math.random() * 360);
            this.rotationSpeed = (float) (Math.random() * 2 - 1);
        }
        
        void update() {
            y -= speed;
            rotation += rotationSpeed;
            if (y < -2) {
                y = 2;
                x = (float) (Math.random() * 4 - 2);
                z = (float) (Math.random() * 4 - 2);
            }
        }
    }
    
    public enum HatStyle implements Nameable {
        CHINA_HAT("China Hat"),
        NEW_YEAR("New Year Hat");
        
        private final String name;
        
        HatStyle(String name) {
            this.name = name;
        }
        
        @Override
        public String getName() {
            return name;
        }
    }
    
    public ChinaHat() {
        super("ChinaHat", Category.Render);
        
        getSettings().add(size);
        getSettings().add(height);
        getSettings().add(brimThickness);
        getSettings().add(colorMode);
        getSettings().add(useGradient);
        getSettings().add(color);
        getSettings().add(gradientLeft);
        getSettings().add(gradientRight);
        getSettings().add(gradientSpeed);
        getSettings().add(gradientLooped);
        getSettings().add(gradientPeriod);
        getSettings().add(outlineGlowStrength);
        getSettings().add(outlineEnabled);
        getSettings().add(outlineColor);
        getSettings().add(hatStyle);
        getSettings().add(pomPomSize);
        getSettings().add(pomPomColor);
        getSettings().add(stripeWidth);
        getSettings().add(stripeColor);
        getSettings().add(showBells);
        getSettings().add(bellCount);
        getSettings().add(bellSize);
        getSettings().add(bellColor);
        getSettings().add(sparkleSpeed);
        getSettings().add(rotatingMode);
        getSettings().add(rotationSpeed);
        getSettings().add(snowflakesSetting);
        getSettings().add(snowflakeCount);
        getSettings().add(snowflakeSize);
        getSettings().add(snowflakeSpeed);
        getSettings().add(hideInFirstPerson);
        getSettings().add(renderBehind);
        getSettings().add(disableDepthTest);
        
        // Initialize snowflakes
        for (int i = 0; i < 20; i++) {
            snowflakes.add(new Snowflake(
                (float) (Math.random() * 4 - 2),
                (float) (Math.random() * 4),
                (float) (Math.random() * 4 - 2),
                (float) (Math.random() * 0.01 + 0.005) * snowflakeSpeed.getValue(),
                (float) (Math.random() * 0.03 + 0.02)
            ));
        }
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        rotationAngle = 0f;
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        // Очищаем снежинки при отключении модуля
        snowflakes.clear();
        rotationAngle = 0f;
    }
    
    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (!toggled) return; // Не рендерим если модуль выключен
        if (mc.options.hudHidden) return;
        if (mc.player == null) return;
        
        // Hide in first person if enabled
        if (hideInFirstPerson.getValue() && !mc.gameRenderer.getCamera().isThirdPerson()) {
            return;
        }
        
        // Render hat AFTER all entities (post-render)
        renderHat(e.getMatrixStack(), mc.player, e.getTickCounter().getTickDelta(false));
    }
    
    private void renderHat(MatrixStack stack, Entity entity, float partialTicks) {
        Vec3d interpolated = new Vec3d(
            MathUtils.interpolate(entity.prevX, entity.getX(), partialTicks),
            MathUtils.interpolate(entity.prevY, entity.getY(), partialTicks),
            MathUtils.interpolate(entity.prevZ, entity.getZ(), partialTicks)
        ).add(0, entity.getHeight(), 0);
        
        // Check if hat is visible
        Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();
        RaycastContext context = new RaycastContext(cameraPos, interpolated, 
            RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity);
        BlockHitResult result = mc.world.raycast(context);
        if (result.getType() == BlockHitResult.Type.BLOCK) return;
        
        rotationAngle += rotationSpeed.getValue() * 0.1f;
        if (rotationAngle > 360) rotationAngle -= 360;
        
        // Update snowflakes
        if (snowflakesSetting.getValue() && hatStyle.getValue() == HatStyle.NEW_YEAR) {
            updateSnowflakes();
        }
        
        stack.push();
        stack.translate(
            interpolated.x - cameraPos.x,
            interpolated.y - cameraPos.y,
            interpolated.z - cameraPos.z
        );
        
        if (rotatingMode.getValue()) {
            stack.multiply(new Quaternionf().rotationY((float)Math.toRadians(rotationAngle)));
        }
        
        // Setup render state - RENDER BEHIND PLAYER
        setupRenderState();
        
        if (hatStyle.getValue() == HatStyle.CHINA_HAT) {
            renderChinaHat(stack, entity, partialTicks);
        } else {
            renderNewYearHat(stack, entity, partialTicks);
        }
        
        // Render snowflakes around player
        if (snowflakesSetting.getValue() && hatStyle.getValue() == HatStyle.NEW_YEAR) {
            renderSnowflakes(stack);
        }
        
        // Restore render state
        restoreRenderState();
        stack.pop();
    }
    
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
    
    private void updateSnowflakes() {
        int targetCount = snowflakeCount.getValue().intValue();
        while (snowflakes.size() < targetCount) {
            snowflakes.add(new Snowflake(
                (float) (Math.random() * 4 - 2),
                (float) (Math.random() * 4),
                (float) (Math.random() * 4 - 2),
                (float) (Math.random() * 0.01 + 0.005) * snowflakeSpeed.getValue(),
                snowflakeSize.getValue() * (0.5f + (float)Math.random() * 0.5f)
            ));
        }
        while (snowflakes.size() > targetCount && !snowflakes.isEmpty()) {
            snowflakes.remove(0);
        }
        
        for (Snowflake flake : snowflakes) {
            flake.update();
        }
    }
    
    private void renderChinaHat(MatrixStack stack, Entity entity, float partialTicks) {
        double brimRadius = size.getValue();
        double innerRadius = Math.max(brimRadius - brimThickness.getValue(), 0.0);
        int segments = 36;
        
        long time = System.currentTimeMillis();
        double phase = gradientLooped.getValue() ?
            ((time / 1000.0) % gradientPeriod.getValue()) / gradientPeriod.getValue() :
            (time / 1000.0 * gradientSpeed.getValue()) % 1.0;
        
        // Всегда используем цвет из UI модуля
        Color uiAccent = MotherHack.getInstance().getModuleManager().getModule(UI.class).getTheme().getAccentColor();
        
        int leftInt, rightInt;
        if (useGradient.getValue() && colorMode.getValue() == ColorMode.Custom) {
            // Используем пользовательский градиент только в режиме Custom
            leftInt = gradientLeft.getColor().getRGB();
            rightInt = gradientRight.getColor().getRGB();
        } else {
            // Во всех остальных случаях используем цвет UI
            leftInt = uiAccent.getRGB();
            rightInt = uiAccent.getRGB();
        }
        
        // Рендерим немного выше, чтобы избежать конфликтов с головой
        stack.push();
        stack.translate(0, 0.02f, 0); // Смещение на 2 см вверх
        
        drawRingAnimated(stack, brimRadius, innerRadius, segments, leftInt, rightInt, phase);
        
        double coneHeight = height.getValue();
        int coneSegments = 8;
        for (int i = 0; i < coneSegments; i++) {
            double t1 = i / (double) coneSegments;
            double t2 = (i + 1) / (double) coneSegments;
            double y1 = coneHeight * (1.0 - t1);
            double y2 = coneHeight * (1.0 - t2);
            double r1 = innerRadius * t1;
            double r2 = innerRadius * t2;
            drawCylinderSlice(stack, r1, r2, (float) y1, (float) y2, segments, leftInt, rightInt, phase);
        }
        
        if (outlineEnabled.getValue()) {
            // Используем цвет UI для обводки
            Color uiColor = MotherHack.getInstance().getModuleManager().getModule(UI.class).getTheme().getAccentColor();
            int outlineColorInt = uiColor.getRGB();
            
            drawCircleOutlineAnimated(stack, (float) brimRadius, outlineColorInt, outlineColorInt, phase, segments);
            
            double glow = outlineGlowStrength.getValue();
            int layers = (int) (1 + (glow * 2));
            for (int i = 1; i <= layers; i++) {
                double t = i / (double) (layers + 1);
                int a = (int) (uiColor.getAlpha() * (1.0 - t) * 0.6);
                int leftGlow = (a << 24) | (uiColor.getRGB() & 0x00FFFFFF);
                int rightGlow = (a << 24) | (uiColor.getRGB() & 0x00FFFFFF);
                drawCircleOutlineAnimated(stack, (float) brimRadius, leftGlow, rightGlow, phase, segments);
            }
        }
        
        stack.pop();
    }
    
    private void renderNewYearHat(MatrixStack stack, Entity entity, float partialTicks) {
        double brimRadius = size.getValue();
        double innerRadius = Math.max(brimRadius - brimThickness.getValue(), 0.0);
        int segments = 36;
        double coneHeight = height.getValue();
        
        long time = System.currentTimeMillis();
        double sparklePhase = (time / 1000.0 * sparkleSpeed.getValue()) % 1.0;
        
        // Рендерим немного выше
        stack.push();
        stack.translate(0, 0.02f, 0);
        
        int pomPomColorInt = pomPomColor.getColor().getRGB();
        int stripeColorInt = stripeColor.getColor().getRGB();
        int bellColorInt = bellColor.getColor().getRGB();
        int outlineColorInt = outlineColor.getColor().getRGB();
        
        // Draw brim (white fur)
        drawRing(stack, brimRadius, innerRadius, segments, stripeColorInt);
        
        // Add fluffy effect to brim
        drawFluffyBrim(stack, brimRadius, innerRadius, 24, sparklePhase);
        
        // Draw cone with stripes
        int stripeSegments = (int) (coneHeight / stripeWidth.getValue());
        boolean isRedStripe = true;
        
        for (int i = 0; i < stripeSegments; i++) {
            double y1 = coneHeight * (i / (double) stripeSegments);
            double y2 = coneHeight * ((i + 1) / (double) stripeSegments);
            double r1 = innerRadius * (1.0 - y1 / coneHeight);
            double r2 = innerRadius * (1.0 - y2 / coneHeight);
            
            int stripeColorIntCurrent = isRedStripe ? pomPomColorInt : stripeColorInt;
            drawCylinderSlice(stack, r1, r2, (float) y1, (float) y2, segments, 
                stripeColorIntCurrent, stripeColorIntCurrent, 0);
            
            // Add texture to red stripes
            if (isRedStripe) {
                drawStripeTexture(stack, r1, r2, (float) y1, (float) y2, segments, sparklePhase);
            }
            
            isRedStripe = !isRedStripe;
        }
        
        // Draw pom-pom at the top with fluffy effect
        drawPomPom(stack, coneHeight, sparklePhase);
        
        // Draw bells on the brim if enabled
        if (showBells.getValue()) {
            drawBells(stack, brimRadius, sparklePhase);
        }
        
        // Draw outline if enabled
        if (outlineEnabled.getValue()) {
            drawCircleOutline(stack, (float) brimRadius, outlineColorInt, segments);
        }
        
        stack.pop();
    }
    
    // Все остальные методы (drawFluffyBrim, drawStripeTexture, drawRing, и т.д.)
    // остаются БЕЗ ИЗМЕНЕНИЙ, как в оригинальном коде, но без параметра alpha
    
    // ВАЖНО: Все методы рисования должны использовать цвета как есть, без применения прозрачности
    
    // Методы для рисования остаются точно такими же, как в оригинальном коде:
    // drawFluffyBrim, drawStripeTexture, drawRing, drawPomPom, drawBells,
    // drawBell, drawSphere, renderSnowflakes, drawSnowflake,
    // drawCircleOutline, drawRingAnimated, drawCylinderSlice,
    // drawCircleOutlineAnimated, interpolateColor
    // (просто скопируйте их из оригинального кода без изменений)
    
    private void drawFluffyBrim(MatrixStack stack, double outer, double inner, int segments, double phase) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Matrix4f mat = stack.peek().getPositionMatrix();
        
        int color = stripeColor.getColor().getRGB();
        double step = Math.PI * 2 / segments;
        double fluffHeight = 0.05;
        
        for (int i = 0; i < segments; i++) {
            double a1 = i * step;
            double a2 = (i + 1) * step;
            double a3 = (i + 0.5) * step;
            
            double x1 = Math.cos(a1) * outer;
            double z1 = Math.sin(a1) * outer;
            double x2 = Math.cos(a2) * outer;
            double z2 = Math.sin(a2) * outer;
            double x3 = Math.cos(a3) * (outer + fluffHeight * Math.sin(phase * Math.PI * 2 + a3 * 5));
            double z3 = Math.sin(a3) * (outer + fluffHeight * Math.sin(phase * Math.PI * 2 + a3 * 5));
            
            bufferBuilder.vertex(mat, (float) x1, 0f, (float) z1).color(color);
            bufferBuilder.vertex(mat, (float) x2, 0f, (float) z2).color(color);
            bufferBuilder.vertex(mat, (float) x3, (float) fluffHeight, (float) z3).color(color);
        }
        
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }
    
    private void drawStripeTexture(MatrixStack stack, double r1, double r2, float y1, float y2, int segments, double phase) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f mat = stack.peek().getPositionMatrix();
        
        int darkRed = new Color(0xCC0000).getRGB();
        int lightRed = new Color(0xFF3333).getRGB();
        double step = Math.PI * 2 / segments;
        
        for (int i = 0; i < segments; i += 2) {
            double a1 = i * step;
            double a2 = (i + 1) * step;
            
            double x1a = Math.cos(a1) * r1;
            double z1a = Math.sin(a1) * r1;
            double x2a = Math.cos(a2) * r1;
            double z2a = Math.sin(a2) * r1;
            double x1b = Math.cos(a1) * r2;
            double z1b = Math.sin(a1) * r2;
            double x2b = Math.cos(a2) * r2;
            double z2b = Math.sin(a2) * r2;
            
            int color = ((i / 2) % 2 == 0) ? darkRed : lightRed;
            
            bufferBuilder.vertex(mat, (float) x1a, y1, (float) z1a).color(color);
            bufferBuilder.vertex(mat, (float) x2a, y1, (float) z2a).color(color);
            bufferBuilder.vertex(mat, (float) x2b, y2, (float) z2b).color(color);
            bufferBuilder.vertex(mat, (float) x1b, y2, (float) z1b).color(color);
        }
        
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }
    
    private void drawRing(MatrixStack stack, double outer, double inner, int segments, int color) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f mat = stack.peek().getPositionMatrix();
        double step = Math.PI * 2 / segments;
        
        for (int i = 0; i < segments; i++) {
            double a1 = i * step;
            double a2 = (i + 1) * step;
            
            double x1o = Math.cos(a1) * outer;
            double z1o = Math.sin(a1) * outer;
            double x2o = Math.cos(a2) * outer;
            double z2o = Math.sin(a2) * outer;
            
            double x1i = Math.cos(a1) * inner;
            double z1i = Math.sin(a1) * inner;
            double x2i = Math.cos(a2) * inner;
            double z2i = Math.sin(a2) * inner;
            
            bufferBuilder.vertex(mat, (float) x1o, 0f, (float) z1o).color(color);
            bufferBuilder.vertex(mat, (float) x2o, 0f, (float) z2o).color(color);
            bufferBuilder.vertex(mat, (float) x2i, 0f, (float) z2i).color(color);
            bufferBuilder.vertex(mat, (float) x1i, 0f, (float) z1i).color(color);
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }
    
    private void drawPomPom(MatrixStack stack, double coneHeight, double phase) {
        double pomPomRadius = pomPomSize.getValue();
        int segments = 16;
        int color = pomPomColor.getColor().getRGB();
        int lightColor = new Color(0xFF6666).getRGB();
        
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        Matrix4f mat = stack.peek().getPositionMatrix();
        
        bufferBuilder.vertex(mat, 0f, (float) coneHeight + (float) pomPomRadius, 0f).color(lightColor);
        
        double sparkleOffset = Math.sin(phase * Math.PI * 2) * 0.1;
        
        for (int i = 0; i <= segments; i++) {
            double angle = i * Math.PI * 2 / segments;
            double x = Math.cos(angle) * pomPomRadius;
            double z = Math.sin(angle) * pomPomRadius;
            double yOffset = Math.cos(angle * 2 + phase * Math.PI * 4) * sparkleOffset;
            
            int col = (i % 2 == 0) ? color : lightColor;
            bufferBuilder.vertex(mat, (float) x, (float) (coneHeight + pomPomRadius + yOffset), (float) z).color(col);
        }
        
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }
    
    private void drawBells(MatrixStack stack, double brimRadius, double phase) {
        int bellCountVal = bellCount.getValue().intValue();
        double bellSizeVal = bellSize.getValue();
        int bellColorInt = bellColor.getColor().getRGB();
        int highlightColor = new Color(0xFFFFAA00).getRGB();
        
        for (int i = 0; i < bellCountVal; i++) {
            double angle = i * Math.PI * 2 / bellCountVal;
            double x = Math.cos(angle) * (brimRadius + bellSizeVal * 0.5);
            double z = Math.sin(angle) * (brimRadius + bellSizeVal * 0.5);
            
            double swing = Math.sin(phase * Math.PI * 2 + angle * 2) * 0.1;
            double swingX = Math.cos(angle + swing) * bellSizeVal * 0.3;
            double swingZ = Math.sin(angle + swing) * bellSizeVal * 0.3;
            
            stack.push();
            stack.translate(x + swingX, -bellSizeVal * 0.5, z + swingZ);
            
            drawBell(stack, bellSizeVal, bellColorInt, highlightColor, phase + angle);
            
            stack.pop();
        }
    }
    
    private void drawBell(MatrixStack stack, double radius, int color, int highlightColor, double phase) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        drawSphere(stack, radius, color, 8);
        
        stack.push();
        stack.translate(radius * 0.3, radius * 0.3, radius * 0.3);
        drawSphere(stack, radius * 0.3, highlightColor, 6);
        stack.pop();
    }
    
    private void drawSphere(MatrixStack stack, double radius, int color, int segments) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f mat = stack.peek().getPositionMatrix();
        
        for (int i = 0; i <= segments; i++) {
            double lat0 = Math.PI * (-0.5 + (double) (i - 1) / segments);
            double z0 = Math.sin(lat0) * radius;
            double zr0 = Math.cos(lat0) * radius;
            
            double lat1 = Math.PI * (-0.5 + (double) i / segments);
            double z1 = Math.sin(lat1) * radius;
            double zr1 = Math.cos(lat1) * radius;
            
            for (int j = 0; j <= segments; j++) {
                double lng = 2 * Math.PI * (double) (j - 1) / segments;
                double x = Math.cos(lng);
                double y = Math.sin(lng);
                
                bufferBuilder.vertex(mat, (float) (x * zr0), (float) z0, (float) (y * zr0)).color(color);
                bufferBuilder.vertex(mat, (float) (x * zr1), (float) z1, (float) (y * zr1)).color(color);
            }
        }
        
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }
    
    private void renderSnowflakes(MatrixStack stack) {
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        
        for (Snowflake flake : snowflakes) {
            stack.push();
            stack.translate(flake.x, flake.y, flake.z);
            stack.multiply(new Quaternionf().rotateZ(flake.rotation));
            
            drawSnowflake(stack, flake.size);
            
            stack.pop();
        }
        
        RenderSystem.enableCull();
    }
    
    private void drawSnowflake(MatrixStack stack, float size) {
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
        Matrix4f mat = stack.peek().getPositionMatrix();
        
        int color = new Color(255, 255, 255, 200).getRGB();
        
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3;
            double x = Math.cos(angle) * size;
            double y = Math.sin(angle) * size;
            
            bufferBuilder.vertex(mat, 0f, 0f, 0f).color(color);
            bufferBuilder.vertex(mat, (float) x, (float) y, 0f).color(color);
        }
        
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }
    
    private void drawCircleOutline(MatrixStack stack, float radius, int color, int segments) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f mat = stack.peek().getPositionMatrix();
        
        double step = Math.PI * 2 / segments;
        for (int i = 0; i <= segments; i++) {
            double a = i * step;
            double x = Math.cos(a) * radius;
            double z = Math.sin(a) * radius;
            
            bufferBuilder.vertex(mat, (float) x, 0f, (float) z).color(color);
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }
    
    // Анимированные методы рисования (остаются без изменений)
    private void drawRingAnimated(MatrixStack stack, double outer, double inner, int segments, int leftColorInt, int rightColorInt, double phase) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f mat = stack.peek().getPositionMatrix();
        double step = Math.PI * 2 / segments;
        for (int i = 0; i < segments; i++) {
            double a1 = i * step;
            double a2 = (i + 1) * step;
            double x1o = Math.cos(a1) * outer;
            double z1o = Math.sin(a1) * outer;
            double x2o = Math.cos(a2) * outer;
            double z2o = Math.sin(a2) * outer;
            double x1i = Math.cos(a1) * inner;
            double z1i = Math.sin(a1) * inner;
            double x2i = Math.cos(a2) * inner;
            double z2i = Math.sin(a2) * inner;

            double fx1o = (((a1 / (Math.PI * 2.0)) + phase) % 1.0 + 1.0) % 1.0;
            double fx2o = (((a2 / (Math.PI * 2.0)) + phase) % 1.0 + 1.0) % 1.0;
            double fx2i = fx2o;
            double fx1i = fx1o;

            double t1o = 0.5 * (1.0 - Math.cos(2.0 * Math.PI * fx1o));
            double t2o = 0.5 * (1.0 - Math.cos(2.0 * Math.PI * fx2o));
            double t2i = t2o;
            double t1i = t1o;

            int c1o = interpolateColor(leftColorInt, rightColorInt, t1o);
            int c2o = interpolateColor(leftColorInt, rightColorInt, t2o);
            int c2i = c2o;
            int c1i = c1o;

            bufferBuilder.vertex(mat, (float) x1o, 0f, (float) z1o).color(c1o);
            bufferBuilder.vertex(mat, (float) x2o, 0f, (float) z2o).color(c2o);
            bufferBuilder.vertex(mat, (float) x2i, 0f, (float) z2i).color(c2i);
            bufferBuilder.vertex(mat, (float) x1i, 0f, (float) z1i).color(c1i);
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    private void drawCylinderSlice(MatrixStack stack, double r1, double r2, float y1, float y2, int segments, int leftColorInt, int rightColorInt, double phase) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f mat = stack.peek().getPositionMatrix();
        double step = Math.PI * 2 / segments;
        for (int i = 0; i < segments; i++) {
            double a1 = i * step;
            double a2 = (i + 1) * step;
            double x1a = Math.cos(a1) * r1;
            double z1a = Math.sin(a1) * r1;
            double x2a = Math.cos(a2) * r1;
            double z2a = Math.sin(a2) * r1;
            double x1b = Math.cos(a1) * r2;
            double z1b = Math.sin(a1) * r2;
            double x2b = Math.cos(a2) * r2;
            double z2b = Math.sin(a2) * r2;

            double fx1a = (((a1 / (Math.PI * 2.0)) + phase) % 1.0 + 1.0) % 1.0;
            double fx2a = (((a2 / (Math.PI * 2.0)) + phase) % 1.0 + 1.0) % 1.0;
            double fx2b = fx2a;
            double fx1b = fx1a;

            double t1 = 0.5 * (1.0 - Math.cos(2.0 * Math.PI * fx1a));
            double t2 = 0.5 * (1.0 - Math.cos(2.0 * Math.PI * fx2a));
            double t3 = t2;
            double t4 = t1;

            int c1 = interpolateColor(leftColorInt, rightColorInt, t1);
            int c2 = interpolateColor(leftColorInt, rightColorInt, t2);
            int c3 = c2;
            int c4 = c1;

            bufferBuilder.vertex(mat, (float) x1a, y1, (float) z1a).color(c1);
            bufferBuilder.vertex(mat, (float) x2a, y1, (float) z2a).color(c2);
            bufferBuilder.vertex(mat, (float) x2b, y2, (float) z2b).color(c3);
            bufferBuilder.vertex(mat, (float) x1b, y2, (float) z1b).color(c4);
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    private void drawCircleOutlineAnimated(MatrixStack stack, float radius, int leftColorInt, int rightColorInt, double phase, int segments) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f mat = stack.peek().getPositionMatrix();

        double step = Math.PI * 2 / segments;
        for (int i = 0; i <= segments; i++) {
            double a = i * step;
            double x = Math.cos(a) * radius;
            double z = Math.sin(a) * radius;

            double fx = (((a / (Math.PI * 2.0)) + phase) % 1.0 + 1.0) % 1.0;
            double t = 0.5 * (1.0 - Math.cos(2.0 * Math.PI * fx));
            int col = interpolateColor(leftColorInt, rightColorInt, t);

            bufferBuilder.vertex(mat, (float) x, 0f, (float) z).color(col);
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    private int interpolateColor(int a, int b, double t) {
        int ta = (a >> 24) & 0xFF;
        int ra = (a >> 16) & 0xFF;
        int ga = (a >> 8) & 0xFF;
        int ba = a & 0xFF;

        int tb = (b >> 24) & 0xFF;
        int rb = (b >> 16) & 0xFF;
        int gb = (b >> 8) & 0xFF;
        int bb = b & 0xFF;

        int na = (int) (ta + (tb - ta) * t);
        int nr = (int) (ra + (rb - ra) * t);
        int ng = (int) (ga + (gb - ga) * t);
        int nb = (int) (ba + (bb - ba) * t);

        return (na << 24) | (nr << 16) | (ng << 8) | nb;
    }
}