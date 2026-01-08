package fun.motherhack.modules.impl.combat;

import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.api.mixins.accessors.IMinecraftClient;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import meteordevelopment.orbit.EventHandler;

public class NoAttackCooldown extends Module {

    public NoAttackCooldown() {
        super("NoAttackCooldown", Category.Combat);
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (fullNullCheck()) return;

        ((IMinecraftClient) mc).setAttackCooldown(0);
    }
}