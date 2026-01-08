package fun.motherhack.modules.impl.movement;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.NumberSetting;

public class ElytraForward extends Module {

    public final NumberSetting forward = new NumberSetting("settings.elytraforward.forward", 3f, 1f, 6f, 1f);

    public ElytraForward() {
        super("ElytraForward", Category.Movement);
    }
}