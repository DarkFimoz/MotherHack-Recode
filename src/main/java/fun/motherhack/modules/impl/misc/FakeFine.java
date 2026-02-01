package fun.motherhack.modules.impl.misc;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.EnumSetting;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class FakeFine extends Module {

    public final EnumSetting<OptiFineVersion> version = new EnumSetting<>("Version", OptiFineVersion.V1_16_5);

    public FakeFine() {
        super("FakeFine", Category.Misc);
    }

    public String getBrand() {
        return version.getValue().getBrand();
    }

    @AllArgsConstructor
    @Getter
    public enum OptiFineVersion implements Nameable {
        V1_16_5("1.16.5", "optifine:1.16.5"),
        V1_17_1("1.17.1", "optifine:1.17.1");

        private final String name;
        private final String brand;
    }
}
