package fun.motherhack.modules.impl.render;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.NumberSetting;

public class SeeInvisibles extends Module {

    private final NumberSetting alpha = new NumberSetting("settings.seeinvisibles.alpha", 0.3f, 0.0f, 1.0f, 0.1f);

    public SeeInvisibles() {
        super("SeeInvisibles", Category.Render);
    }

    public float getAlpha() {
        return alpha.getValue();
    }
}
