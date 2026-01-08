package fun.motherhack.modules.settings.api;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EnumConverter {

    public int currentEnum(Enum<?> clazz) {
        for (int i = 0; i < clazz.getDeclaringClass().getEnumConstants().length; ++i) {
            Enum<?> e = clazz.getDeclaringClass().getEnumConstants()[i];
            if (!e.name().equalsIgnoreCase(clazz.name())) continue;
            return i;
        }
        return -1;
    }

    public Enum<?> increaseEnum(Enum<?> clazz) {
        int index = EnumConverter.currentEnum(clazz);
        for (int i = 0; i < clazz.getDeclaringClass().getEnumConstants().length; ++i) {
            Enum<?> e = clazz.getDeclaringClass().getEnumConstants()[i];
            if (i != index + 1) continue;
            return e;
        }

        return clazz.getDeclaringClass().getEnumConstants()[0];
    }
}