package fun.motherhack.modules.settings.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;

@AllArgsConstructor @Getter @Setter
public class Bind {
    private int key;
    private boolean mouse;

    @Override
    public String toString() {
        if (key == -1) return "None";
        if (mouse) return "M" + (key + 1);

        String str = "";
        try {
            for (Field field : GLFW.class.getDeclaredFields()) {
                if (field.getName().startsWith("GLFW_KEY_")) {
                    int a = (int) field.get(null);
                    if (a == key) {
                        String nb = field.getName().substring("GLFW_KEY_".length());
                        str = nb.substring(0, 1).toUpperCase() + nb.substring(1).toLowerCase();
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}

        return str.toUpperCase();
    }

    public boolean isEmpty() {
        return key < 0;
    }
}