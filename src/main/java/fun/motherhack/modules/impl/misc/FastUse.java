package fun.motherhack.modules.impl.misc;

import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.api.mixins.accessors.IMinecraftClient;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import meteordevelopment.orbit.EventHandler;

public class FastUse extends Module {

    public FastUse() {
        super("FastUse", Category.Misc);
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (fullNullCheck()) return;

        ((IMinecraftClient) mc).setItemUseCooldown(0);
    }
}