package fun.motherhack.modules.settings.impl;

import fun.motherhack.modules.settings.Setting;
import lombok.Getter;

import java.util.function.Supplier;

public class NumberSetting extends Setting<Float> {
    @Getter private final float min, max, increment;

    public NumberSetting(String name, float defaultValue, float min, float max, float increment) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public NumberSetting(String name, float defaultValue, float min, float max, float increment, Supplier<Boolean> visible) {
        super(name, defaultValue, visible);
        this.min = min;
        this.max = max;
        this.increment = increment;
    }
}