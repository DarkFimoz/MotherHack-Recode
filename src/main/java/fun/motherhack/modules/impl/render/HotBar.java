package fun.motherhack.modules.impl.render;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.api.render.builders.Builder;
import fun.motherhack.api.render.renderers.impl.BuiltText;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Fonts;
import fun.motherhack.utils.render.fonts.Instance;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

import java.awt.*;

public class HotBar extends Module {

    public enum OffhandMode implements Nameable {
        Merged("Merged"),
        Separate("Separate");
        
        private final String name;
        
        OffhandMode(String name) {
            this.name = name;
        }
        
        @Override
        public String getName() {
            return name;
        }
    }
    
    private final EnumSetting<OffhandMode> offhandMode = new EnumSetting<>("Offhand Mode", OffhandMode.Merged);
    private final NumberSetting roundness = new NumberSetting("Roundness", 6f, 0f, 12f, 1f);
    private final BooleanSetting showLevel = new BooleanSetting("Show Level", true);
    private final BooleanSetting showHandCount = new BooleanSetting("Show Hand Count", true);
    private final BooleanSetting showHealth = new BooleanSetting("Show Health", true);
    private final BooleanSetting showHunger = new BooleanSetting("Show Hunger", true);
    private final BooleanSetting showArmor = new BooleanSetting("Show Armor", true);

    public HotBar() {
        super("HotBar", Category.Render);
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        if (fullNullCheck()) return;
        if (mc.options.hudHidden) return;

        DrawContext context = event.getContext();
        MatrixStack matrices = context.getMatrices();
        
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        int centerX = screenWidth / 2;
        
        float radius = roundness.getValue();
        Color bgColor = new Color(20, 20, 20, 180);
        Color selectedColor = new Color(47, 47, 47, 200);
        Color accentColor = MotherHack.getInstance().getModuleManager().getModule(UI.class).getTheme().getAccentColor();
        
        boolean hasOffhand = !mc.player.getOffHandStack().isEmpty();
        
        // Основной фон хотбара
        int hotbarWidth = 182;
        int hotbarHeight = 22;
        int hotbarY = screenHeight - 25;
        
        if (!hasOffhand) {
            Render2D.drawRoundedRect(matrices, centerX - 91, hotbarY, hotbarWidth, hotbarHeight, radius, bgColor);
        } else if (offhandMode.getValue() == OffhandMode.Merged) {
            Render2D.drawRoundedRect(matrices, centerX - 113, hotbarY, 226, hotbarHeight, radius, bgColor);
            // Разделитель для offhand
            Render2D.drawRoundedRect(matrices, centerX - 91, hotbarY + 2, 1f, hotbarHeight - 4, 0f, new Color(255, 255, 255, 40));
        } else {
            Render2D.drawRoundedRect(matrices, centerX - 91, hotbarY, hotbarWidth, hotbarHeight, radius, bgColor);
            Render2D.drawRoundedRect(matrices, centerX - 115, hotbarY, 22, hotbarHeight, radius, bgColor);
        }
        
        // Выделение выбранного слота
        int selectedSlot = mc.player.getInventory().selectedSlot;
        float selectedX = centerX - 89 + selectedSlot * 20;
        Render2D.drawRoundedRect(matrices, selectedX, hotbarY + 1, 18, hotbarHeight - 2, radius - 2, selectedColor);
        
        // Акцент на выбранном слоте
        Render2D.drawRoundedRect(matrices, selectedX, hotbarY + 1, 18, 1.5f, 0f, accentColor);
        
        // Рисуем предметы
        renderHotbarItems(context, centerX, screenHeight, hasOffhand);
        
        // Уровень опыта (по центру над хотбаром)
        if (showLevel.getValue() && mc.player.experienceLevel > 0) {
            String levelStr = String.valueOf(mc.player.experienceLevel);
            Instance font = Fonts.SEMIBOLD.getFont(10f);
            float textWidth = font.getWidth(levelStr);
            float textX = centerX - textWidth / 2;
            float textY = hotbarY - 12;
            
            // Тень
            drawCustomText(matrices, font, levelStr, textX + 0.5f, textY + 0.5f, new Color(0, 0, 0, 150));
            // Текст
            drawCustomText(matrices, font, levelStr, textX, textY, new Color(128, 255, 32));
        }
        
        // Здоровье (слева от хотбара)
        if (showHealth.getValue()) {
            float health = mc.player.getHealth();
            float maxHealth = mc.player.getMaxHealth();
            
            String healthText = String.format("%.1f", health);
            String fullText = healthText + " HP";
            Instance font = Fonts.MEDIUM.getFont(9f);
            float fullWidth = font.getWidth(fullText);
            float healthX = centerX - 91 - fullWidth - 8;
            float healthY = hotbarY + (hotbarHeight - font.getHeight()) / 2;
            
            // Фон для здоровья
            Render2D.drawRoundedRect(matrices, healthX - 4, healthY - 2, fullWidth + 8, font.getHeight() + 4, 3f, bgColor);
            
            // Цвет в зависимости от здоровья
            Color healthColor = getHealthColor(health, maxHealth);
            drawCustomText(matrices, font, fullText, healthX, healthY, healthColor);
        }
        
        // Голод (справа от хотбара)
        if (showHunger.getValue()) {
            int hunger = mc.player.getHungerManager().getFoodLevel();
            
            String hungerText = hunger + " Food";
            Instance font = Fonts.MEDIUM.getFont(9f);
            float hungerWidth = font.getWidth(hungerText);
            float hungerX = centerX + 91 + 8;
            float hungerY = hotbarY + (hotbarHeight - font.getHeight()) / 2;
            
            // Фон для голода
            Render2D.drawRoundedRect(matrices, hungerX - 4, hungerY - 2, hungerWidth + 8, font.getHeight() + 4, 3f, bgColor);
            
            // Цвет в зависимости от голода
            Color hungerColor = getHungerColor(hunger);
            drawCustomText(matrices, font, hungerText, hungerX, hungerY, hungerColor);
        }
        
        // Броня (над хотбаром слева)
        if (showArmor.getValue()) {
            int armor = mc.player.getArmor();
            if (armor > 0) {
                String armorText = armor + " Armor";
                Instance font = Fonts.MEDIUM.getFont(9f);
                float armorWidth = font.getWidth(armorText);
                float armorX = centerX - armorWidth / 2;
                float armorY = hotbarY - 26;
                
                // Фон для брони
                Render2D.drawRoundedRect(matrices, armorX - 4, armorY - 2, armorWidth + 8, font.getHeight() + 4, 3f, bgColor);
                
                drawCustomText(matrices, font, armorText, armorX, armorY, new Color(170, 170, 255));
            }
        }
        
        // Количество предметов в руке (над выбранным слотом)
        if (showHandCount.getValue()) {
            ItemStack mainHand = mc.player.getMainHandStack();
            if (!mainHand.isEmpty() && mainHand.getCount() > 1) {
                String countText = "x" + mainHand.getCount();
                Instance font = Fonts.BOLD.getFont(9f);
                float textWidth = font.getWidth(countText);
                float textX = selectedX + 9 - textWidth / 2;
                float textY = hotbarY - 12;
                
                Render2D.drawRoundedRect(matrices, textX - 2, textY - 2, textWidth + 4, font.getHeight() + 4, 3f, new Color(20, 20, 20, 200));
                drawCustomText(matrices, font, countText, textX, textY, accentColor);
            }
        }
    }
    
    private Color getHealthColor(float health, float maxHealth) {
        float percentage = health / maxHealth;
        
        if (percentage > 0.75f) {
            return new Color(85, 255, 85); // Зеленый
        } else if (percentage > 0.5f) {
            return new Color(255, 255, 85); // Желтый
        } else if (percentage > 0.25f) {
            return new Color(255, 170, 0); // Оранжевый
        } else {
            return new Color(255, 85, 85); // Красный
        }
    }
    
    private Color getHungerColor(int hunger) {
        if (hunger > 15) {
            return new Color(255, 200, 100); // Золотой
        } else if (hunger > 10) {
            return new Color(255, 170, 85); // Оранжевый
        } else if (hunger > 5) {
            return new Color(255, 140, 70); // Темно-оранжевый
        } else {
            return new Color(255, 85, 85); // Красный
        }
    }
    
    private void renderHotbarItems(DrawContext context, int centerX, int screenHeight, boolean hasOffhand) {
        int itemY = screenHeight - 20;
        
        // Offhand
        if (hasOffhand) {
            int offhandX = offhandMode.getValue() == OffhandMode.Merged ? centerX - 110 : centerX - 112;
            renderItem(context, offhandX, itemY, mc.player.getOffHandStack());
        }
        
        // Main hotbar
        for (int i = 0; i < 9; i++) {
            int itemX = centerX - 88 + i * 20;
            ItemStack stack = mc.player.getInventory().getStack(i);
            
            int yOffset = (i == mc.player.getInventory().selectedSlot) ? -1 : 0;
            renderItem(context, itemX, itemY + yOffset, stack);
        }
    }
    
    private void renderItem(DrawContext context, int x, int y, ItemStack stack) {
        if (stack.isEmpty()) return;
        
        context.getMatrices().push();
        context.getMatrices().translate(x + 8, y + 8, 0);
        context.getMatrices().scale(1.0f, 1.0f, 1f);
        context.getMatrices().translate(-(x + 8), -(y + 8), 0);
        
        context.drawItem(stack, x, y);
        context.drawStackOverlay(mc.textRenderer, stack, x, y);
        
        context.getMatrices().pop();
    }
    
    private void drawCustomText(MatrixStack matrices, Instance font, String text, float x, float y, Color color) {
        BuiltText built = Builder.text()
                .font(font.font())
                .text(text)
                .size(font.size())
                .thickness(0.05f)
                .smoothness(0.5f)
                .color(color)
                .build();
        built.render(matrices.peek().getPositionMatrix(), x, y);
    }
}
