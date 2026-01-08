package fun.motherhack.modules.impl.render;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.api.events.impl.EventRender3D;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.ColorSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.Render3D;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.Screen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Snow extends Module {

    // Основные настройки
    private final BooleanSetting enabled = new BooleanSetting("Enabled", false); // Выключено по умолчанию
    private final BooleanSetting enableInWorld = new BooleanSetting("EnableInWorld", true);
    private final BooleanSetting enableInGUI = new BooleanSetting("EnableInGUI", false);
    
    // Настройки частиц
    private final NumberSetting particleCount = new NumberSetting("ParticleCount", 150f, 20f, 500f, 10f);
    private final NumberSetting particleSize = new NumberSetting("ParticleSize", 4f, 1f, 15f, 0.5f);
    private final NumberSetting snowRadius = new NumberSetting("SnowRadius", 25f, 10f, 80f, 5f);
    private final NumberSetting fallSpeed = new NumberSetting("FallSpeed", 0.08f, 0.01f, 0.3f, 0.01f);
    private final NumberSetting swayAmount = new NumberSetting("SwayAmount", 0.015f, 0.001f, 0.05f, 0.001f);
    private final NumberSetting rotationSpeed = new NumberSetting("RotationSpeed", 1f, 0f, 5f, 0.1f);
    private final NumberSetting windStrength = new NumberSetting("WindStrength", 0.5f, 0f, 2f, 0.1f);
    
    // Настройки внешнего вида
    private final ColorSetting particleColor = new ColorSetting(Color.WHITE);
    private final BooleanSetting useTexture = new BooleanSetting("UseTexture", true);
    private final BooleanSetting rotation = new BooleanSetting("Rotation", true);
    private final BooleanSetting shadows = new BooleanSetting("Shadows", true);
    private final BooleanSetting dynamicLighting = new BooleanSetting("DynamicLighting", false);
    
    // Система частиц
    private final List<SnowParticle> particles = new ArrayList<>();
    private final List<GUIParticle> guiParticles = new ArrayList<>();
    private final Random random = new Random();
    private long lastUpdateTime = 0;
    
    // Ветер (меняется со временем)
    private float windX = 0f;
    private float windZ = 0f;
    private long lastWindUpdate = 0;

    public Snow() {
        super("Snow", Category.Render);
        
        // Добавляем настройки в правильном порядке
        getSettings().add(enabled);
        getSettings().add(enableInWorld);
        getSettings().add(enableInGUI);
        getSettings().add(particleCount);
        getSettings().add(particleSize);
        getSettings().add(snowRadius);
        getSettings().add(fallSpeed);
        getSettings().add(swayAmount);
        getSettings().add(rotationSpeed);
        getSettings().add(windStrength);
        getSettings().add(particleColor);
        getSettings().add(useTexture);
        getSettings().add(rotation);
        getSettings().add(shadows);
        getSettings().add(dynamicLighting);
    }

    @EventHandler
    private void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck() || !enabled.getValue() || !enableInWorld.getValue()) return;
        
        // Инициализация частиц, если они пустые
        if (particles.isEmpty()) {
            createSnowParticles();
            lastUpdateTime = System.currentTimeMillis();
            lastWindUpdate = System.currentTimeMillis();
        }
        
        // Обновление ветра (медленно меняется со временем)
        updateWind();
        
        // Обновление и рендеринг 3D снежинок
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000f;
        lastUpdateTime = currentTime;
        
        for (SnowParticle particle : particles) {
            particle.update(deltaTime);
            particle.render(event.getMatrixStack());
        }
    }
    
    @EventHandler
    private void onRender2D(EventRender2D e) {
        if (fullNullCheck() || !enabled.getValue() || !enableInGUI.getValue()) return;
        
        Screen currentScreen = mc.currentScreen;
        
        // Проверяем, находимся ли мы в GUI и нужно ли рендерить снег
        boolean shouldRenderGUI = currentScreen != null && 
            (currentScreen instanceof fun.motherhack.screen.clickgui.ClickGui ||
             currentScreen instanceof fun.motherhack.screen.csgui.MHackGui);
        
        // Инициализация GUI частиц если нужно
        if (shouldRenderGUI && guiParticles.isEmpty()) {
            createGUIParticles();
            lastUpdateTime = System.currentTimeMillis();
        }
        
        // Очищаем GUI частицы если не нужно
        if (!shouldRenderGUI && !guiParticles.isEmpty()) {
            guiParticles.clear();
        }
        
        // Обновление и рендеринг GUI снежинок
        if (shouldRenderGUI) {
            long currentTime = System.currentTimeMillis();
            float deltaTime = (currentTime - lastUpdateTime) / 1000f;
            lastUpdateTime = currentTime;
            
            for (GUIParticle particle : guiParticles) {
                particle.update(deltaTime);
                particle.render(e.getContext().getMatrices());
            }
        }
    }

    private void createSnowParticles() {
        particles.clear();
        
        int count = particleCount.getValue().intValue();
        double radius = snowRadius.getValue();
        
        for (int i = 0; i < count; i++) {
            // Распределение в цилиндрической области вокруг игрока
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = random.nextDouble() * radius;
            
            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;
            
            // Высота от земли до двойного радиуса
            double offsetY = random.nextDouble() * radius * 2;
            
            Vec3d position = new Vec3d(
                mc.player.getX() + offsetX,
                mc.player.getY() + offsetY,
                mc.player.getZ() + offsetZ
            );
            
            particles.add(new SnowParticle(position));
        }
    }

    private void createGUIParticles() {
        guiParticles.clear();
        
        int count = (int) (particleCount.getValue() * 0.3); // Меньше частиц для GUI
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        
        for (int i = 0; i < count; i++) {
            float x = random.nextFloat() * screenWidth;
            float y = random.nextFloat() * screenHeight;
            
            // Разный размер для GUI частиц
            float size = particleSize.getValue().floatValue() * 
                        (0.5f + random.nextFloat() * 0.5f);
            
            guiParticles.add(new GUIParticle(x, y, size));
        }
    }
    
    private void updateWind() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastWindUpdate > 5000) { // Меняем ветер каждые 5 секунд
            windX = (random.nextFloat() - 0.5f) * windStrength.getValue().floatValue();
            windZ = (random.nextFloat() - 0.5f) * windStrength.getValue().floatValue();
            lastWindUpdate = currentTime;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        particles.clear();
        guiParticles.clear();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (enabled.getValue()) {
            createSnowParticles();
        }
    }

    // 3D Снежинка для мира
    private class SnowParticle {
        private Vec3d position;
        private Vec3d velocity;
        private final double spawnY;
        private float rotationAngle;
        private float rotationSpeed;
        private float particleSize;
        private Color color;
        
        public SnowParticle(Vec3d initialPosition) {
            this.position = initialPosition;
            this.spawnY = initialPosition.y;
            
            // Начальная скорость
            float speed = fallSpeed.getValue().floatValue() * (0.8f + random.nextFloat() * 0.4f);
            float sway = swayAmount.getValue().floatValue() * (0.5f + random.nextFloat());
            
            this.velocity = new Vec3d(
                (random.nextFloat() - 0.5f) * sway,
                -speed,
                (random.nextFloat() - 0.5f) * sway
            );
            
            // Вращение
            this.rotationAngle = random.nextFloat() * 360;
            this.rotationSpeed = Snow.this.rotationSpeed.getValue().floatValue() * 
                                (0.5f + random.nextFloat());
            
            // Разный размер для разнообразия
            this.particleSize = Snow.this.particleSize.getValue().floatValue() * 
                              (0.7f + random.nextFloat() * 0.6f);
            
            // Цвет с небольшими вариациями
            this.color = applyColorVariation(particleColor.getColor());
        }
        
        public void update(float deltaTime) {
            // Обновление позиции
            position = position.add(
                velocity.x * deltaTime + windX * deltaTime,
                velocity.y * deltaTime,
                velocity.z * deltaTime + windZ * deltaTime
            );
            
            // Легкое качание
            if (random.nextFloat() < 0.1f) {
                velocity = velocity.add(
                    (random.nextFloat() - 0.5f) * swayAmount.getValue().floatValue() * 0.1f,
                    0,
                    (random.nextFloat() - 0.5f) * swayAmount.getValue().floatValue() * 0.1f
                );
            }
            
            // Обновление вращения
            if (rotation.getValue()) {
                rotationAngle += rotationSpeed * deltaTime * 50;
                if (rotationAngle > 360) rotationAngle -= 360;
            }
            
            // Сброс если слишком низко
            if (position.y < spawnY - snowRadius.getValue()) {
                resetParticle();
            }
            
            // Держим частицы в радиусе
            double distance = position.distanceTo(mc.player.getPos());
            if (distance > snowRadius.getValue() * 1.2) {
                Vec3d toPlayer = mc.player.getPos().subtract(position).normalize();
                position = position.add(toPlayer.multiply(0.5));
            }
        }
        
        public void render(MatrixStack matrixStack) {
            // Пропорциональный размер в зависимости от расстояния до игрока
            double distance = position.distanceTo(mc.player.getPos());
            float renderSize = (float) (particleSize * (1.0 - Math.min(distance / 50.0, 0.5)));
             
            // Рендер текстуры снежинки как биллборда (всегда смотрит на игрока)
            renderTexturedBillboard(matrixStack, position, renderSize * 0.1f, color);
        }
        
        private void resetParticle() {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = random.nextDouble() * snowRadius.getValue();
            
            position = new Vec3d(
                mc.player.getX() + Math.cos(angle) * distance,
                spawnY + snowRadius.getValue(),
                mc.player.getZ() + Math.sin(angle) * distance
            );
            
            // Новая скорость при респавне
            float speed = fallSpeed.getValue().floatValue() * (0.8f + random.nextFloat() * 0.4f);
            velocity = new Vec3d(
                velocity.x * 0.5f + (random.nextFloat() - 0.5f) * swayAmount.getValue().floatValue(),
                -speed,
                velocity.z * 0.5f + (random.nextFloat() - 0.5f) * swayAmount.getValue().floatValue()
            );
            
            // Новый цвет
            color = applyColorVariation(particleColor.getColor());
        }
    }

    // 2D Снежинка для GUI
    private class GUIParticle {
        private float x, y;
        private float velocityY;
        private float swayX;
        private float rotationAngle;
        private float rotationSpeed;
        private float size;
        private float alpha;
        private Color color;
        
        public GUIParticle(float initialX, float initialY, float size) {
            this.x = initialX;
            this.y = initialY;
            this.size = size;
            this.alpha = 0.7f + random.nextFloat() * 0.3f;
            
            // Начальная скорость
            this.velocityY = fallSpeed.getValue().floatValue() * 80f * (0.8f + random.nextFloat() * 0.4f);
            this.swayX = (random.nextFloat() - 0.5f) * swayAmount.getValue().floatValue() * 200f;
            
            // Вращение
            this.rotationAngle = random.nextFloat() * 360;
            this.rotationSpeed = Snow.this.rotationSpeed.getValue().floatValue() * 
                               (0.5f + random.nextFloat()) * 2f;
            
            // Цвет
            this.color = new Color(
                particleColor.getColor().getRed(),
                particleColor.getColor().getGreen(),
                particleColor.getColor().getBlue(),
                (int)(alpha * 255)
            );
        }
        
        public void update(float deltaTime) {
            // Обновление позиции
            y += velocityY * deltaTime;
            x += swayX * deltaTime;
            
            // Легкое изменение качания
            if (random.nextFloat() < 0.02f) {
                swayX += (random.nextFloat() - 0.5f) * swayAmount.getValue().floatValue() * 50f;
                // Ограничение скорости качания
                swayX = Math.max(Math.min(swayX, swayAmount.getValue().floatValue() * 200f), 
                                -swayAmount.getValue().floatValue() * 200f);
            }
            
            // Эффект ветра для GUI
            swayX += Math.sin(System.currentTimeMillis() * 0.001f) * 0.5f * windStrength.getValue().floatValue();
            
            // Обновление вращения
            if (rotation.getValue()) {
                rotationAngle += rotationSpeed * deltaTime * 50;
                if (rotationAngle > 360) rotationAngle -= 360;
            }
            
            // Сброс если вышла за пределы
            int screenWidth = mc.getWindow().getScaledWidth();
            int screenHeight = mc.getWindow().getScaledHeight();
            
            if (y > screenHeight + 50) {
                resetParticle(screenWidth, screenHeight);
            }
            if (x < -100 || x > screenWidth + 100) {
                x = random.nextFloat() * screenWidth;
                y = -50;
            }
        }
        
        public void render(MatrixStack matrixStack) {
            // Рендер текстуры снежинки для GUI
            if (useTexture.getValue()) {
                Render2D.drawTexture(matrixStack, x - size/2, y - size/2, size, size, 0f, MotherHack.id("particles/snow.png"), color);
            } else {
                // Простой прямоугольник если текстуры отключены
                Render2D.drawRoundedRect(matrixStack, x - size/2, y - size/2, size, size, 2, color);
            }
        }
        
        private void resetParticle(int screenWidth, int screenHeight) {
            y = -size - random.nextFloat() * 50;
            x = random.nextFloat() * screenWidth;
            
            // Новая скорость при респавне
            velocityY = fallSpeed.getValue().floatValue() * 80f * (0.8f + random.nextFloat() * 0.4f);
            swayX = (random.nextFloat() - 0.5f) * swayAmount.getValue().floatValue() * 200f;
            
            // Новый цвет
            alpha = 0.7f + random.nextFloat() * 0.3f;
            color = new Color(
                particleColor.getColor().getRed(),
                particleColor.getColor().getGreen(),
                particleColor.getColor().getBlue(),
                (int)(alpha * 255)
            );
        }
    }
    
    private Color applyColorVariation(Color baseColor) {
        if (!dynamicLighting.getValue()) return baseColor;
        
        // Добавляем небольшие вариации цвета
        int variation = 20;
        int r = Math.clamp(baseColor.getRed() + random.nextInt(variation*2) - variation, 0, 255);
        int g = Math.clamp(baseColor.getGreen() + random.nextInt(variation*2) - variation, 0, 255);
        int b = Math.clamp(baseColor.getBlue() + random.nextInt(variation*2) - variation, 0, 255);
        
        return new Color(r, g, b);
    }
    
    // Метод для рендера текстурированного биллборда (всегда смотрит на игрока)
    private void renderTexturedBillboard(MatrixStack matrixStack, Vec3d position, float size, Color color) {
        if (useTexture.getValue()) {
            // Рендер текстуры снежинки
            matrixStack.push();
            
            // Получаем позицию камеры
            Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
            
            // Перемещаемся к позиции снежинки
            matrixStack.translate(position.x - cameraPos.x, position.y - cameraPos.y, position.z - cameraPos.z);
            
            // Поворачиваем биллборд чтобы он всегда смотрел на игрока
            matrixStack.multiply(mc.getEntityRenderDispatcher().getRotation());
            
            // Рендерим текстуру
            RenderSystem.setShaderTexture(0, MotherHack.id("particles/snow.png"));
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            
            // Рисуем квадрат с текстурой
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            Matrix4f matrix = matrixStack.peek().getPositionMatrix();
            
            float halfSize = size / 2;
            float r = color.getRed() / 255f;
            float g = color.getGreen() / 255f;
            float b = color.getBlue() / 255f;
            float a = color.getAlpha() / 255f;
            
            // Вершины с текстурными координатами
            bufferBuilder.vertex(matrix, -halfSize, -halfSize, 0).texture(0, 0).color(r, g, b, a);
            bufferBuilder.vertex(matrix, halfSize, -halfSize, 0).texture(1, 0).color(r, g, b, a);
            bufferBuilder.vertex(matrix, halfSize, halfSize, 0).texture(1, 1).color(r, g, b, a);
            bufferBuilder.vertex(matrix, -halfSize, halfSize, 0).texture(0, 1).color(r, g, b, a);
            
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
            
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            
            matrixStack.pop();
        } else {
            // Рендер простого квадрата если текстуры отключены
            Render3D.renderCube(matrixStack, position, size, true, color, false, color.darker());
        }
    }
}