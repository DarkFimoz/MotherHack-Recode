package fun.motherhack.modules.impl.render;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.NumberSetting;

public class AspectRatio extends Module {

    public NumberSetting ratio = new NumberSetting("Ratio", 1.78f, 0.5f, 3.0f, 0.01f);

    public AspectRatio() {
        super("AspectRatio", Category.Render);
    }

    public float getAspectRatio(float original) {
        return ratio.getValue();
    }
}
