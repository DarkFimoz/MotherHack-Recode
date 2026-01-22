package fun.motherhack.modules.impl.client;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.screen.clickgui.ClickGui;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Bind;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import lombok.AllArgsConstructor;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class UI extends Module {

    @AllArgsConstructor @Getter
    public enum ClickGuiTheme implements Nameable {
        Default("Default", new Color(0, 0, 0, 220), Color.WHITE, new Color(255, 255, 255, 25)),
        Christmas("Новогодний", new Color(139, 0, 0, 220), Color.WHITE, new Color(255, 255, 255, 25)),
        Lime("Лаймовый", new Color(0, 100, 0, 220), Color.WHITE, new Color(255, 255, 255, 25)),
        Oceanic("Океанический", new Color(0, 0, 139, 220), Color.WHITE, new Color(0, 191, 255, 25)),
        Sunset("Закат", new Color(255, 140, 0, 220), Color.WHITE, new Color(255, 165, 0, 25)),
        Forest("Лесной", new Color(34, 139, 34, 220), Color.WHITE, new Color(0, 255, 0, 25)),
        Midnight("Полночь", new Color(25, 25, 112, 220), Color.WHITE, new Color(0, 0, 255, 25)),
        Rose("Розовый", new Color(255, 20, 147, 220), Color.WHITE, new Color(255, 182, 193, 25)),
        Electric("Электрик", new Color(0, 255, 255, 220), Color.BLACK, new Color(0, 255, 255, 25)),
        Galaxy("Галактика", new Color(75, 0, 130, 220), Color.WHITE, new Color(255, 0, 255, 25)),
        Amber("Янтарный", new Color(255, 191, 0, 220), Color.BLACK, new Color(255, 215, 0, 25)),
        Mint("Мятный", new Color(0, 255, 128, 220), Color.BLACK, new Color(0, 255, 128, 25)),
        Crimson("Багровый", new Color(220, 20, 60, 220), Color.WHITE, new Color(255, 0, 0, 25)),
        Azure("Лазурный", new Color(0, 127, 255, 220), Color.WHITE, new Color(0, 191, 255, 25)),
        Emerald("Изумрудный", new Color(0, 128, 0, 220), Color.WHITE, new Color(0, 255, 0, 25)),
        Ruby("Рубиновый", new Color(155, 17, 30, 220), Color.WHITE, new Color(255, 0, 0, 25));

        private final String name;
        private final Color backgroundColor;
        private final Color textColor;
        private final Color accentColor;
    }
    
    private final EnumSetting<ClickGuiTheme> theme = new EnumSetting<>("Theme", ClickGuiTheme.Default);
    private final BooleanSetting showBackground = new BooleanSetting("Show Background", false);
    private final BooleanSetting enableBlur = new BooleanSetting("Enable Blur", true);
    private final NumberSetting blurRadius = new NumberSetting("Blur Radius", 20f, 5f, 50f, 1f);
    private final NumberSetting blurAlpha = new NumberSetting("Blur Alpha", 150f, 50f, 255f, 5f);
    private final NumberSetting backgroundAlpha = new NumberSetting("Background Alpha", 180f, 100f, 255f, 5f);
    private final NumberSetting panelWidth = new NumberSetting("Panel Width", 110f, 80f, 150f, 5f);
    private final NumberSetting panelSpacing = new NumberSetting("Panel Spacing", 4f, 0f, 20f, 1f);
    private final BooleanSetting enableSounds = new BooleanSetting("Enable Sounds", true);

    public UI() {
        super("UI", Category.Client);
        setBind(new Bind(GLFW.GLFW_KEY_RIGHT_SHIFT, false));
        
        // Добавляем настройки
        getSettings().add(theme);
        getSettings().add(showBackground);
        getSettings().add(enableSounds);
        getSettings().add(enableBlur);
        getSettings().add(blurRadius);
        getSettings().add(blurAlpha);
        getSettings().add(backgroundAlpha);
        getSettings().add(panelWidth);
        getSettings().add(panelSpacing);
        
        // Показывать настройки blur только когда он включен
        blurRadius.setVisible(() -> enableBlur.getValue());
        blurAlpha.setVisible(() -> enableBlur.getValue());
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (!(mc.currentScreen instanceof ClickGui)) setToggled(false);
    }

    public ClickGuiTheme getTheme() {
        return theme.getValue();
    }

    public boolean isShowBackground() {
        return showBackground.getValue();
    }
    
    public boolean isEnableBlur() {
        return enableBlur.getValue();
    }
    
    public float getBlurRadius() {
        return blurRadius.getValue();
    }
    
    public float getBlurAlpha() {
        return blurAlpha.getValue();
    }
    
    public float getBackgroundAlpha() {
        return backgroundAlpha.getValue();
    }
    
    public float getPanelWidth() {
        return panelWidth.getValue();
    }
    
    public float getPanelSpacing() {
        return panelSpacing.getValue();
    }
    
    public boolean isSoundsEnabled() {
        return enableSounds.getValue();
    }

    @Override
    public void onEnable() {
        toggled = true;
        MotherHack.getInstance().getEventHandler().subscribe(this);
        // Не воспроизводим звуки и не показываем уведомления для UI модуля
        mc.setScreen(MotherHack.getInstance().getClickGui());
    }
    
    @Override
    public void onDisable() {
        toggled = false;
        MotherHack.getInstance().getEventHandler().unsubscribe(this);
        // Не воспроизводим звуки и не показываем уведомления для UI модуля
    }
}