package fun.motherhack.modules.impl.misc;

import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.api.mixins.accessors.ILivingEntity;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import meteordevelopment.orbit.EventHandler;

public class NoJumpDelay extends Module {

    public NoJumpDelay() {
        super("NoJumpDelay", Category.Misc);
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (fullNullCheck()) return;

        ((ILivingEntity) mc.player).setJumpingCooldown(0);
    }
}