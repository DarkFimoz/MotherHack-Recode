package fun.motherhack.modules.settings.impl;

import fun.motherhack.modules.settings.Setting;

import java.awt.*;

public class ColorSetting extends Setting<Color> {
    public ColorSetting(Color defaultValue) {
        super("Color", defaultValue);
    }

    public ColorSetting(String name, Color defaultValue) {
        super(name, defaultValue);
    }

    public Color getColor() {
        return getValue();
    }
}
