package fun.motherhack.modules.impl.movement;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventKeyboardInput;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.utils.rotations.RotationChanger;
import meteordevelopment.orbit.EventHandler;

public class LegitStrafe extends Module {
    
    private RotationChanger rotationChanger;

    public LegitStrafe() {
        super("legitStrafe", Category.Movement);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        rotationChanger = new RotationChanger(
            0,
            () -> new Float[]{mc.player.getYaw() + calculateYawOffset(), mc.player.getPitch()},
            () -> false
        );
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (!fullNullCheck() && rotationChanger != null) {
            MotherHack.getInstance().getRotationManager().removeRotation(rotationChanger);
        }
    }

    @EventHandler
    public void onRotationUpdate(EventKeyboardInput event) {
        if (fullNullCheck()) return;

        MotherHack.getInstance().getRotationManager().addRotation(rotationChanger);
    }

    @EventHandler
    public void onMovementInput(EventKeyboardInput event) {
        if (fullNullCheck()) return;

        boolean w = mc.options.forwardKey.isPressed();
        boolean s = mc.options.backKey.isPressed();
        boolean a = mc.options.leftKey.isPressed();
        boolean d = mc.options.rightKey.isPressed();

        if (w && s) {
            w = false;
            s = false;
        }
        if (a && d) {
            a = false;
            d = false;
        }

        event.setMovementSideways(0);
        event.setMovementForward(w || s || a || d ? 1.0f : 0);
    }

    private float calculateYawOffset() {
        boolean w = mc.options.forwardKey.isPressed();
        boolean s = mc.options.backKey.isPressed();
        boolean a = mc.options.leftKey.isPressed();
        boolean d = mc.options.rightKey.isPressed();

        if (w && s) {
            w = false;
            s = false;
        }
        if (a && d) {
            a = false;
            d = false;
        }

        if (w) {
            if (a) return -45f;
            if (d) return 45f;
            return 0f;
        }
        if (s) {
            if (a) return -135f;
            if (d) return 135f;
            return 180f;
        }
        if (a) return -90f;
        if (d) return 90f;
        return 0f;
    }
}