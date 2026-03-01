package fun.motherhack.modules.impl.render;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.ColorSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.render.ColorUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

import java.awt.*;

public class AmbienceModule extends Module {

    public enum TimeMode implements Nameable {
        CUSTOM("Кастом"),
        DAWN("Рассвет"),
        DAY("День"),
        NOON("Полдень"),
        DUSK("Закат"),
        NIGHT("Ночь"),
        MIDNIGHT("Полночь");

        private final String name;

        TimeMode(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum WeatherMode implements Nameable {
        NO_CHANGE("Без изменений"),
        SUNNY("Солнечно"),
        RAIN("Дождь"),
        THUNDER("Гроза");

        private final String name;

        WeatherMode(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    // Time settings
    private final EnumSetting<TimeMode> timeMode = new EnumSetting<>("Режим времени", TimeMode.DAY);
    private final NumberSetting customTime = new NumberSetting("Время", 6000, 0f, 24000f, 100f, () -> timeMode.getValue() == TimeMode.CUSTOM);
    
    // Weather settings
    private final EnumSetting<WeatherMode> weatherMode = new EnumSetting<>("Режим погоды", WeatherMode.NO_CHANGE);
    private final BooleanSetting removeRain = new BooleanSetting("Убрать дождь", false, () -> weatherMode.getValue() != WeatherMode.NO_CHANGE);
    
    // Fog settings
    private final BooleanSetting customFog = new BooleanSetting("Кастомный туман", false);
    private final ColorSetting fogColor = new ColorSetting("Цвет тумана", new Color(200, 200, 200, 255), customFog::getValue);
    private final BooleanSetting syncFogWithUI = new BooleanSetting("Синхронизировать с UI", false, customFog::getValue);
    private final NumberSetting fogStart = new NumberSetting("Начало тумана", 20f, 0f, 100f, 1f, customFog::getValue);
    private final NumberSetting fogEnd = new NumberSetting("Конец тумана", 80f, 0f, 200f, 1f, customFog::getValue);
    private final NumberSetting fogDensity = new NumberSetting("Плотность тумана", 1f, 0f, 2f, 0.1f, customFog::getValue);
    
    // Sky settings
    private final BooleanSetting customSky = new BooleanSetting("Кастомное небо", false);
    private final ColorSetting skyColor = new ColorSetting("Цвет неба", new Color(135, 206, 235, 255), customSky::getValue);
    
    // Additional settings
    private final BooleanSetting removeWeatherParticles = new BooleanSetting("Убрать частицы погоды", false);
    private final BooleanSetting smoothTransition = new BooleanSetting("Плавный переход", true);
    private final NumberSetting transitionSpeed = new NumberSetting("Скорость перехода", 0.05f, 0.01f, 0.5f, 0.01f, smoothTransition::getValue);

    private long targetTime = 6000;

    public AmbienceModule() {
        super("AmbienceModule", Category.Render);
        getSettings().add(timeMode);
        getSettings().add(customTime);
        getSettings().add(weatherMode);
        getSettings().add(removeRain);
        getSettings().add(customFog);
        getSettings().add(fogColor);
        getSettings().add(syncFogWithUI);
        getSettings().add(fogStart);
        getSettings().add(fogEnd);
        getSettings().add(fogDensity);
        getSettings().add(customSky);
        getSettings().add(skyColor);
        getSettings().add(removeWeatherParticles);
        getSettings().add(smoothTransition);
        getSettings().add(transitionSpeed);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        updateTargetTime();
    }

    @EventHandler
    public void onPacket(EventPacket.Receive event) {
        if (mc == null || mc.world == null) return;

        // Handle time packets
        if (event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
            event.cancel();
            updateTargetTime();
            mc.world.setTime(targetTime, targetTime, true);
        }

        // Handle weather packets
        if (weatherMode.getValue() != WeatherMode.NO_CHANGE && event.getPacket() instanceof GameStateChangeS2CPacket) {
            if (removeRain.getValue()) {
                event.cancel();
            }
        }
    }

    private void updateTargetTime() {
        targetTime = switch (timeMode.getValue()) {
            case DAWN -> 23000L;
            case DAY -> 1000L;
            case NOON -> 6000L;
            case DUSK -> 12000L;
            case NIGHT -> 13000L;
            case MIDNIGHT -> 18000L;
            case CUSTOM -> customTime.getValue().longValue();
        };
    }

    public Color getFogColor() {
        if (!customFog.getValue()) {
            return null;
        }
        
        if (syncFogWithUI.getValue()) {
            UI uiModule = MotherHack.getInstance().getModuleManager().getModule(UI.class);
            if (uiModule != null) {
                return uiModule.getTheme().getBackgroundColor();
            }
            return ColorUtils.getGlobalColor();
        }
        
        return fogColor.getValue();
    }

    public float getFogStart() {
        return customFog.getValue() ? fogStart.getValue() : -1f;
    }

    public float getFogEnd() {
        return customFog.getValue() ? fogEnd.getValue() : -1f;
    }

    public float getFogDensity() {
        return customFog.getValue() ? fogDensity.getValue() : 1f;
    }

    public Color getSkyColor() {
        return customSky.getValue() ? skyColor.getValue() : null;
    }

    public boolean shouldRemoveWeatherParticles() {
        return removeWeatherParticles.getValue();
    }

    public long getTargetTime() {
        return targetTime;
    }

    public boolean isCustomFogEnabled() {
        return customFog.getValue();
    }
}

