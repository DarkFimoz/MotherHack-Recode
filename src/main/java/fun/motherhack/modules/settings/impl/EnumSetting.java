package fun.motherhack.modules.settings.impl;

import fun.motherhack.modules.settings.Setting;
import fun.motherhack.modules.settings.api.EnumConverter;
import fun.motherhack.modules.settings.api.Nameable;

import java.util.function.Supplier;

public class EnumSetting<Value extends Enum<?>> extends Setting<Value> {

    public EnumSetting(String name, Value defaultValue) {
        super(name, defaultValue);
    }

    public EnumSetting(String name, Value defaultValue, Supplier<Boolean> visible) {
        super(name, defaultValue, visible);
    }

    public void increaseEnum() {
        setValue((Value) EnumConverter.increaseEnum(value));
    }

    public String currentEnumName() {
        if (value instanceof Nameable) {
            return ((Nameable) value).getName();
        }
        return value.name(); // Используем стандартный метод enum, если не реализует Nameable
    }

    public void setEnumValue(String value) {
        for (Value e : (Value[]) this.value.getClass().getEnumConstants()) {
            if (e instanceof Nameable) {
                if (((Nameable) e).getName().equalsIgnoreCase(value)) {
                    setValue(e);
                    break;
                }
            } else if (e.name().equalsIgnoreCase(value)) {
                setValue(e);
                break;
            }
        }
    }
}