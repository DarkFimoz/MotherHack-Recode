package fun.motherhack.api.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.motherhack.MotherHack;
import fun.motherhack.modules.impl.client.CustomBackground;
import fun.motherhack.screen.AccountSwitcherScreen;
import fun.motherhack.utils.Wrapper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen implements Wrapper {

    private static float randomOffsetX = 0;
    private static float randomOffsetY = 0;
    private static float randomZoom = 1.0f;
    private static boolean initialized = false;
    private static String lastBackground = "";

    protected MixinTitleScreen(Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderCustomBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        CustomBackground customBackground = MotherHack.getInstance().getModuleManager().getModule(CustomBackground.class);
        
        if (customBackground != null && customBackground.isToggled()) {
            String currentBg = customBackground.getCurrentBackground();
            
            // Инициализируем случайные значения при смене фона или первом запуске
            if (!initialized || !lastBackground.equals(currentBg)) {
                Random random = new Random();
                
                System.out.println("=== CustomBackground: Changing background ===");
                System.out.println("From: '" + lastBackground + "'");
                System.out.println("To: '" + currentBg + "'");
                
                // Только для первого фона - сильное приближение
                if (currentBg.equals("background.png")) {
                    // Меньшее смещение для первого фона (±3%)
                    randomOffsetX = (random.nextFloat() - 0.5f) * this.width * 0.06f;
                    randomOffsetY = (random.nextFloat() - 0.5f) * this.height * 0.06f;
                    // Очень сильное приближение от 1.5 до 1.8
                    randomZoom = 1.5f + random.nextFloat() * 0.3f;
                    System.out.println("Zoom: " + randomZoom + " (background.png)");
                } else {
                    // Для остальных фонов - БЕЗ приближения
                    randomOffsetX = 0;
                    randomOffsetY = 0;
                    randomZoom = 1.0f;
                    System.out.println("Zoom: 1.0 (no zoom for " + currentBg + ")");
                }
                
                initialized = true;
                lastBackground = currentBg;
            }
            
            // Создаём Identifier для текущего фона
            Identifier CUSTOM_BACKGROUND = Identifier.of("motherhack", currentBg);
            
            // Рендерим чёрный фон (нижний слой)
            context.fill(0, 0, this.width, this.height, 0xFF000000);
            
            // Вычисляем размеры и позицию с учётом зума и смещения
            int scaledWidth = (int) (this.width * randomZoom);
            int scaledHeight = (int) (this.height * randomZoom);
            int startX = (int) ((this.width - scaledWidth) / 2 + randomOffsetX);
            int startY = (int) ((this.height - scaledHeight) / 2 + randomOffsetY);
            
            // Рендерим фотографию (средний слой) с приближением
            try {
                RenderSystem.setShaderTexture(0, CUSTOM_BACKGROUND);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                
                context.drawTexture(
                    identifier -> RenderLayer.getGuiTextured(identifier),
                    CUSTOM_BACKGROUND, 
                    startX, startY, 
                    0.0F, 0.0F, 
                    scaledWidth, scaledHeight, 
                    scaledWidth, scaledHeight
                );
                
                RenderSystem.disableBlend();
            } catch (Exception e) {
                System.err.println("Failed to render background: " + currentBg);
                e.printStackTrace();
            }
            
            // Затемняем фон для лучшей видимости кнопок
            context.fill(0, 0, this.width, this.height, 0x40000000);
            
            // Рендерим время красивым шрифтом выше кнопок
            java.time.LocalTime currentTime = java.time.LocalTime.now();
            String timeString = String.format("%02d:%02d", currentTime.getHour(), currentTime.getMinute());
            
            // Цвет времени зависит от фона
            java.awt.Color timeColor = currentBg.equals("background3.png") 
                ? new java.awt.Color(0, 0, 0, 220)      // Чёрный для background3
                : new java.awt.Color(255, 255, 255, 220); // Белый для остальных
            
            // Используем кастомный шрифт если доступен
            try {
                float timeX = this.width / 2f - fun.motherhack.utils.render.fonts.Fonts.BOLD.getFont(32f).getWidth(timeString) / 2f;
                float timeY = this.height / 2f - 180; // Выше кнопок
                fun.motherhack.utils.render.Render2D.drawFont(context.getMatrices(), 
                    fun.motherhack.utils.render.fonts.Fonts.BOLD.getFont(32f),
                    timeString, timeX, timeY, timeColor);
            } catch (Exception e) {
                // Fallback на стандартный шрифт
                int timeX = this.width / 2 - this.textRenderer.getWidth(timeString) / 2;
                int timeY = this.height / 2 - 180;
                int color = currentBg.equals("background3.png") ? 0xFF000000 : 0xFFFFFFFF;
                context.drawText(this.textRenderer, timeString, timeX, timeY, color, true);
            }
            
            // Рендерим кнопки и другие элементы (верхний слой)
            super.render(context, mouseX, mouseY, delta);
            
            // Отменяем стандартный рендеринг
            ci.cancel();
        }
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void removeOldButtons(CallbackInfo ci) {
        // Удаляем все старые кнопки ClickGui ДО того, как Minecraft добавит свои кнопки
        var childrenCopy = new java.util.ArrayList<>(this.children());
        for (var child : childrenCopy) {
            if (child instanceof ButtonWidget button) {
                String buttonText = button.getMessage().getString();
                if (buttonText.equals("ClickGui")) {
                    this.remove(button);
                }
            }
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void modifyButtons(CallbackInfo ci) {
        // Удаляем ненужные кнопки и добавляем новые
        var childrenCopy = new java.util.ArrayList<>(this.children());
        boolean hasClickGuiButton = false;
        
        for (var child : childrenCopy) {
            if (child instanceof ButtonWidget button) {
                String buttonText = button.getMessage().getString();
                
                // Проверяем наличие кнопки ClickGui
                if (buttonText.equals("ClickGui")) {
                    hasClickGuiButton = true;
                    continue;
                }
                
                // Удаляем кнопки: Титры, Атрибуции, Язык, Специальные возможности, Mojang AB
                if (buttonText.contains("Credits") || buttonText.contains("Титры") ||
                    buttonText.contains("Attribution") || buttonText.contains("Атрибуции") ||
                    buttonText.contains("Language") || buttonText.contains("Язык") ||
                    buttonText.contains("Accessibility") || buttonText.contains("Специальные возможности") ||
                    buttonText.contains("Copyright Mojang AB") || buttonText.contains("© Mojang AB")) {
                    this.remove(button);
                }
                // Заменяем Realms на Accounts
                else if (buttonText.contains("Realms") || buttonText.contains("Мир")) {
                    ButtonWidget accountsButton = ButtonWidget.builder(
                        Text.of("Accounts"),
                        btn -> mc.setScreen(new AccountSwitcherScreen())
                    ).dimensions(button.getX(), button.getY(), button.getWidth(), button.getHeight()).build();

                    this.remove(button);
                    this.addDrawableChild(accountsButton);
                }
            }
        }
        
        // Добавляем кнопку ClickGui только если её нет
        if (!hasClickGuiButton) {
            int screenCenterX = this.width / 2;
            int buttonWidth = 200;
            int buttonHeight = 20;
            int buttonY = this.height - 30; // Внизу экрана
            
            ButtonWidget clickGuiButton = ButtonWidget.builder(
                Text.of("ClickGui"),
                btn -> {
                    // Открываем MHackGui вместо ClickGui
                    mc.setScreen(MotherHack.getInstance().getMHackGui());
                }
            ).dimensions(screenCenterX - buttonWidth / 2, buttonY, buttonWidth, buttonHeight).build();
            
            this.addDrawableChild(clickGuiButton);
        }
    }
}