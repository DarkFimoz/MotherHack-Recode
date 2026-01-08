package fun.motherhack.modules.settings.impl;

import fun.motherhack.modules.settings.Setting;
import lombok.Getter;

import java.util.function.Supplier;

public class StringSetting extends Setting<String> {

    @Getter private final boolean onlyDigit;

    public StringSetting(String name, String defaultValue, boolean onlyDigit) {
        super(name, defaultValue);
        this.onlyDigit = onlyDigit;
    }

    public StringSetting(String name, String defaultValue, Supplier<Boolean> visible, boolean onlyDigit) {
        super(name, defaultValue, visible);
        this.onlyDigit = onlyDigit;
    }
}