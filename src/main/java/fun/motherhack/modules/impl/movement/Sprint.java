package fun.motherhack.modules.impl.movement;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.combat.Aura;
import meteordevelopment.orbit.EventHandler;

public class Sprint extends Module {
	
    public Sprint() {
        super("Sprint", Category.Movement);
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (fullNullCheck()) return;

        if (MotherHack.getInstance().getModuleManager().getModule(Aura.class).isToggled()
        		&& MotherHack.getInstance().getModuleManager().getModule(Aura.class).getTarget() != null
                && MotherHack.getInstance().getModuleManager().getModule(Aura.class).sprint.getValue() == Aura.Sprint.Legit) {
        	if (mc.player.getAbilities().flying
        			|| mc.player.isRiding()
        			|| MotherHack.getInstance().getServerManager().getFallDistance() <= 0f
        			&& mc.player.isOnGround()) {
        		mc.options.sprintKey.setPressed(true);
        	} else {
                mc.options.sprintKey.setPressed(false);
                mc.player.setSprinting(false);
        	}
        } else mc.options.sprintKey.setPressed(true);
    }
}