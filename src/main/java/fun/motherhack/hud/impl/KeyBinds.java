package fun.motherhack.hud.impl;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.hud.HudElement;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.client.MHACKGUI;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Font;
import fun.motherhack.utils.render.fonts.Fonts;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * KeyBinds HUD element - красивый стиль с blur и анимациями
 */
public class KeyBinds extends HudElement {

    private final NumberSetting fontSize = new NumberSetting("Font Size", 8f, 4f, 16f, 0.5f);
    private final NumberSetting backgroundAlpha = new NumberSetting("Background Alpha", 80f, 50f, 255f, 5f);
    private final BooleanSetting onlyEnabled = new BooleanSetting("Only Enabled", true);

    // Анимация появления
    private final Animation alphaAnimation = new Animation(300, 1f, false, Easing.SMOOTH_STEP);
    private boolean isVisible = false;

    // Карта для названий клавиш
    private static final Map<Integer, String> KEY_NAME_MAP = new HashMap<>();

    static {
        // Буквы A-Z
        for (int i = 0; i < 26; i++) {
            int key = GLFW.GLFW_KEY_A + i;
            char letter = (char) ('A' + i);
            KEY_NAME_MAP.put(key, String.valueOf(letter));
        }
        // Цифры 0-9
        for (int i = 0; i <= 9; i++) {
            KEY_NAME_MAP.put(GLFW.GLFW_KEY_0 + i, String.valueOf(i));
        }
        // Функциональные клавиши
        for (int i = 1; i <= 12; i++) {
            KEY_NAME_MAP.put(GLFW.GLFW_KEY_F1 + i - 1, "F" + i);
        }
        // Специальные клавиши
        KEY_NAME_MAP.put(GLFW.GLFW_KEY_LEFT_CONTROL, "LCTRL");
        KEY_NAME_MAP.put(GLFW.GLFW_KEY_RIGHT_CONTROL, "RCTRL");
        KEY_NAME_MAP.put(GLFW.GLFW_KEY_LEFT_SHIFT, "LSHIFT");
        KEY_NAME_MAP.put(GLFW.GLFW_KEY_RIGHT_SHIFT, "RSHIFT");
        KEY_NAME_MAP.put(GLFW.GLFW_KEY_LEFT_ALT, "LALT");
        KEY_NAME_MAP.put(GLFW.GLFW_KEY_RIGHT_ALT, "RALT");
        KEY_NAME_MAP.put(GLFW.GLFW_KEY_SPACE, "SPACE");
        KEY_NAME_MAP.put(GLFW.GLFW_KEY_TAB, "TAB");
        KEY_NAME_MAP.put(GLFW.GLFW_KEY_CAPS_LOCK, "CAPS");
    }

    public KeyBinds() {
        super("KeyBinds");
        getSettings().add(fontSize);
        getSettings().add(backgroundAlpha);
        getSettings().add(onlyEnabled);
        getPosition().getValue().setX(0.01f);
        getPosition().getValue().setY(0.3f);
    }

    private List<Module> getToggledModules() {
        return MotherHack.getInstance().getModuleManager().getModules().stream()
                .filter(m -> !m.getBind().isEmpty())
                .filter(m -> !onlyEnabled.getValue() || m.isToggled())
                .filter(m -> !(m instanceof UI)) // Исключаем UI модуль из списка
                .filter(m -> !(m instanceof MHACKGUI)) // Исключаем MHACKGUI модуль из списка
                .sorted(Comparator.comparing(Module::getName))
                .collect(Collectors.toList());
    }

    private String getKeyName(String bind) {
        if (bind == null || bind.isEmpty()) return "NONE";
        return bind.replace("key.keyboard.", "").replace(".", " ").toUpperCase();
    }

    @EventHandler
    public void onRender2DX2(EventRender2D e) {
        if (fullNullCheck()) return;
        BooleanSetting setting = MotherHack.getInstance().getHudManager().getElements().getName("KeyBinds");
        if (setting != null) {
            toggledAnimation.update(setting.getValue());
        } else {
            toggledAnimation.update(true);
        }
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;
        if (Fonts.SEMIBOLD == null || Fonts.REGULAR == null || Fonts.ICONS == null) return;

        UI.ClickGuiTheme theme = MotherHack.getInstance().getModuleManager().getModule(UI.class).getTheme();

        List<Module> toggledModules = getToggledModules();
        boolean isInChatScreen = mc.currentScreen instanceof ChatScreen;
        boolean shouldBeVisible = !toggledModules.isEmpty() || isInChatScreen;

        // Анимация появления/исчезновения
        if (shouldBeVisible != isVisible) {
            isVisible = shouldBeVisible;
            alphaAnimation.update(isVisible);
        }
        alphaAnimation.update(isVisible);

        float alpha = alphaAnimation.getValue();
        if (alpha < 0.01f && !isVisible) return;

        float x = getX();
        float y = getY();
        float padding = 5f;
        float currentFontSize = fontSize.getValue();
        Font headerFont = Fonts.SEMIBOLD;
        Font bodyFont = Fonts.REGULAR;

        float headerHeight = currentFontSize + 4f + padding * 2;
        int bgAlpha = (int) (backgroundAlpha.getValue() * alpha);
        int textAlpha = (int) (255 * alpha);
        Color textColor = new Color(255, 255, 255, textAlpha);
        Color bindColor = new Color(255, 100, 120, textAlpha);
        Color bgColor = new Color(theme.getBackgroundColor().getRed(), theme.getBackgroundColor().getGreen(), theme.getBackgroundColor().getBlue(), bgAlpha);
        Color blurColor = new Color(theme.getBackgroundColor().getRed(), theme.getBackgroundColor().getGreen(), theme.getBackgroundColor().getBlue(), (int)(bgAlpha * 0.3f));

        // Расчёт ширины
        String headerText = "KeyBinds";
        float headerTextWidth = headerFont.getWidth(headerText, currentFontSize + 1f);
        float maxWidth = headerTextWidth + 40f;

        if (toggledModules.isEmpty() && isInChatScreen) {
            String moduleName = "Aura";
            String bind = "R";
            float lineWidth = bodyFont.getWidth(moduleName, currentFontSize) + bodyFont.getWidth(bind, currentFontSize) + padding * 8;
            maxWidth = Math.max(maxWidth, lineWidth);
        } else {
            for (Module module : toggledModules) {
                String moduleName = module.getName();
                String bind = getKeyName(module.getBind().toString());
                float lineWidth = bodyFont.getWidth(moduleName, currentFontSize) + bodyFont.getWidth(bind, currentFontSize) + padding * 8;
                maxWidth = Math.max(maxWidth, lineWidth);
            }
        }

        float width = maxWidth;

        e.getContext().getMatrices().push();
        e.getContext().getMatrices().translate(x + width / 2, y + headerHeight / 2, 0f);
        e.getContext().getMatrices().scale(toggledAnimation.getValue(), toggledAnimation.getValue(), 0);
        e.getContext().getMatrices().translate(-(x + width / 2), -(y + headerHeight / 2), 0f);

        // Заголовок с blur
        Render2D.drawBlurredRect(e.getContext().getMatrices(), x, y, width, headerHeight, 6f, 12f, blurColor);
        Render2D.drawRoundedRect(e.getContext().getMatrices(), x, y, width, headerHeight, 6f, bgColor);

        // Иконка и текст заголовка
        Render2D.drawFont(e.getContext().getMatrices(), Fonts.ICONS.getFont(currentFontSize + 3f), "C", x + padding, y + padding + 1f, bindColor);
        Render2D.drawFont(e.getContext().getMatrices(), headerFont.getFont(currentFontSize + 1f), headerText, x + padding + 18, y + padding + 1f, textColor);

        float currentY = y + headerHeight + 3f;
        float rowHeight = currentFontSize + padding * 2;

        if (toggledModules.isEmpty() && isInChatScreen) {
            String moduleName = "Aura";
            String bind = "R";

            Render2D.drawBlurredRect(e.getContext().getMatrices(), x, currentY, width, rowHeight, 6f, 12f, blurColor);
            Render2D.drawRoundedRect(e.getContext().getMatrices(), x, currentY, width, rowHeight, 6f, bgColor);

            Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), moduleName, x + padding, currentY + padding, textColor);

            float bindWidth = bodyFont.getWidth(bind, currentFontSize);
            float bindX = x + width - bindWidth - padding;
            Render2D.drawFont(e.getContext().getMatrices(), headerFont.getFont(currentFontSize), bind, bindX, currentY + padding, bindColor);

            currentY += rowHeight + 3f;
        } else {
            for (Module module : toggledModules) {
                String moduleName = module.getName();
                String bind = getKeyName(module.getBind().toString());

                Render2D.drawBlurredRect(e.getContext().getMatrices(), x, currentY, width, rowHeight, 6f, 12f, blurColor);
                Render2D.drawRoundedRect(e.getContext().getMatrices(), x, currentY, width, rowHeight, 6f, bgColor);

                Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), moduleName, x + padding, currentY + padding, textColor);

                float bindWidth = bodyFont.getWidth(bind, currentFontSize);
                float bindX = x + width - bindWidth - padding;
                Render2D.drawFont(e.getContext().getMatrices(), headerFont.getFont(currentFontSize), bind, bindX, currentY + padding, bindColor);

                currentY += rowHeight + 3f;
            }
        }

        e.getContext().getMatrices().pop();

        float totalHeight = currentY - y;
        setBounds(getX(), getY(), width, totalHeight);
        super.onRender2D(e);
    }
}
