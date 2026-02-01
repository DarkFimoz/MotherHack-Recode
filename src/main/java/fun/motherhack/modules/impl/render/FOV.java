package fun.motherhack.modules.impl.render;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;

public class FOV extends Module {
    public final NumberSetting fovModifier = new NumberSetting("FOV модификатор", 120f, 0f, 358f, 1f);
    public final BooleanSetting itemFov = new BooleanSetting("FOV предметов", false);
    public final NumberSetting itemFovModifier = new NumberSetting("FOV модификатор предметов", 120f, 0f, 358f, 1f);

    public FOV() {
        super("FOV", Category.Render);
        getSettings().add(fovModifier);
        getSettings().add(itemFov);
        getSettings().add(itemFovModifier);
    }
}
