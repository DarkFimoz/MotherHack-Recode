package fun.motherhack.modules.impl.movement;

import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;

public class Speed extends Module {
    private final NumberSetting speed = new NumberSetting("Speed", 1.0f, 0.1f, 5.0f, 0.1f);
    private final BooleanSetting autoJump = new BooleanSetting("Auto Jump", false);
    private final BooleanSetting strafe = new BooleanSetting("Strafe", false);
    private final NumberSetting jumpHeight = new NumberSetting("Jump Height", 0.42f, 0.1f, 1.0f, 0.05f);
    
    public Speed() {
        super("Speed", Category.Movement);
        getSettings().add(speed);
        getSettings().add(autoJump);
        getSettings().add(strafe);
        getSettings().add(jumpHeight);
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick event) {
        if (fullNullCheck()) return;

        boolean moving = mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;
        boolean onGround = mc.player.isOnGround();

        // Auto Jump
        if (autoJump.getValue() && onGround && moving) {
            mc.player.jump();
            if (jumpHeight.getValue() != 0.42f) {
                mc.player.setVelocity(
                    mc.player.getVelocity().x,
                    jumpHeight.getValue().floatValue(),
                    mc.player.getVelocity().z
                );
            }
        }

        // Movement handling
        if (onGround && moving) {
            double yaw = Math.toRadians(mc.player.getYaw());
            double forward = mc.player.input.movementForward;
            double strafeInput = mc.player.input.movementSideways;

            double speedValue = speed.getValue().floatValue();
            if (strafe.getValue()) {
                // Simple strafing - just increase speed when moving sideways
                speedValue *= 1.5f;
            }
            
            // Apply speed boost to player's movement
            mc.player.setVelocity(
                mc.player.getVelocity().x * speedValue,
                mc.player.getVelocity().y,
                mc.player.getVelocity().z * speedValue
            );
        }
    }
}