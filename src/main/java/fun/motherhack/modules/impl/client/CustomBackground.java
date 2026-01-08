package fun.motherhack.modules.impl.client;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.EnumSetting;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class CustomBackground extends Module {

    @AllArgsConstructor @Getter
    public enum BackgroundType implements Nameable {
        Background1("Background 1", "background.png"),
        Background2("Background 2", "background2.png"),
        Background3("Background 3", "background3.png");

        private final String name;
        private final String fileName;
    }

    public final EnumSetting<BackgroundType> backgroundType = new EnumSetting<>("Background", BackgroundType.Background1);

    public CustomBackground() {
        super("CustomBackground", Category.Client);
        setToggled(true); // Включён по умолчанию
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
    
    public String getCurrentBackground() {
        return backgroundType.getValue().getFileName();
    }
}
