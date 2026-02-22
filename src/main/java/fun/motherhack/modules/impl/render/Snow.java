package fun.motherhack.modules.impl.render;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.utils.render.Render2D;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Snow extends Module {

    // Хардкод настройки (не настраиваемые)
    private static final boolean ENABLE_IN_GUI = true;
    private static final boolean USE_TEXTURE = true;
    private static final boolean ROTATION = true;
    
    // Дефолтные значения для частиц
    private static final float PARTICLE_COUNT = 150f;
    private static final float PARTICLE_SIZE = 4f;
    private static final float FALL_SPEED = 0.08f;
    private static final float SWAY_AMOUNT = 0.015f;
    private static final float ROTATION_SPEED = 1f;
    private static final float WIND_STRENGTH = 0.5f;
    private static final Color PARTICLE_COLOR = Color.WHITE;
    
    // Система частиц (только GUI)
    private final List<GUIParticle> guiParticles = new ArrayList<>();
    private final Random random = new Random();
    private long lastUpdateTime = 0;

    public Snow() {
        super("Snow", Category.Render);
    }


    
    @EventHandler
    private void onRender2D(EventRender2D e) {
        if (fullNullCheck() || !ENABLE_IN_GUI) return;
        
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



    private void createGUIParticles() {
        guiParticles.clear();
        
        int count = (int) (PARTICLE_COUNT * 0.3); // Меньше частиц для GUI
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        
        for (int i = 0; i < count; i++) {
            float x = random.nextFloat() * screenWidth;
            float y = random.nextFloat() * screenHeight;
            
            // Разный размер для GUI частиц
            float size = PARTICLE_SIZE * (0.5f + random.nextFloat() * 0.5f);
            
            guiParticles.add(new GUIParticle(x, y, size));
        }
    }
    


    @Override
    public void onDisable() {
        super.onDisable();
        guiParticles.clear();
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
            this.velocityY = FALL_SPEED * 80f * (0.8f + random.nextFloat() * 0.4f);
            this.swayX = (random.nextFloat() - 0.5f) * SWAY_AMOUNT * 200f;
            
            // Вращение
            this.rotationAngle = random.nextFloat() * 360;
            this.rotationSpeed = ROTATION_SPEED * (0.5f + random.nextFloat()) * 2f;
            
            // Цвет
            this.color = new Color(
                PARTICLE_COLOR.getRed(),
                PARTICLE_COLOR.getGreen(),
                PARTICLE_COLOR.getBlue(),
                (int)(alpha * 255)
            );
        }
        
        public void update(float deltaTime) {
            // Обновление позиции
            y += velocityY * deltaTime;
            x += swayX * deltaTime;
            
            // Легкое изменение качания
            if (random.nextFloat() < 0.02f) {
                swayX += (random.nextFloat() - 0.5f) * SWAY_AMOUNT * 50f;
                // Ограничение скорости качания
                swayX = Math.max(Math.min(swayX, SWAY_AMOUNT * 200f), -SWAY_AMOUNT * 200f);
            }
            
            // Эффект ветра для GUI
            swayX += Math.sin(System.currentTimeMillis() * 0.001f) * 0.5f * WIND_STRENGTH;
            
            // Обновление вращения
            if (ROTATION) {
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
            if (USE_TEXTURE) {
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
            velocityY = FALL_SPEED * 80f * (0.8f + random.nextFloat() * 0.4f);
            swayX = (random.nextFloat() - 0.5f) * SWAY_AMOUNT * 200f;
            
            // Новый цвет
            alpha = 0.7f + random.nextFloat() * 0.3f;
            color = new Color(
                PARTICLE_COLOR.getRed(),
                PARTICLE_COLOR.getGreen(),
                PARTICLE_COLOR.getBlue(),
                (int)(alpha * 255)
            );
        }
    }
    

}