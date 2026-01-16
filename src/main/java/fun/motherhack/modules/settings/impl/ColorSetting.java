package fun.motherhack.modules.settings.impl;

import fun.motherhack.modules.settings.Setting;

import java.awt.*;
import java.util.function.Supplier;

public class ColorSetting extends Setting<Color> {
    public ColorSetting(Color defaultValue) {
        super("Color", defaultValue);
    }

    public ColorSetting(String name, Color defaultValue) {
        super(name, defaultValue);
    }

    public ColorSetting(String name, Color defaultValue, Supplier<Boolean> visible) {
        super(name, defaultValue, visible);
    }

    public Color getColor() {
        return getValue();
    }
}
