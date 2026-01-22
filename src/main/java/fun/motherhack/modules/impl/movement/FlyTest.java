package fun.motherhack.modules.impl.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;

public class FlyTest extends Module {

    public FlyTest() {
        super("Fly", Category.Movement);
        speed = new NumberSetting("Speed", 1.0f, 0.1f, 5.0f, 0.1f);
        lockY = new BooleanSetting("Lock Y", true);
        antiKick = new BooleanSetting("AntiKick", true);
    }

    private final NumberSetting speed;
    private final BooleanSetting lockY;
    private final BooleanSetting antiKick;
    private double lockedY;
    private boolean positionLocked;
    private int tickCounter;

    @Override
    public void onEnable() {
        super.onEnable();
        positionLocked = false;
        tickCounter = 0;
        if (mc.player != null) {
            mc.player.getAbilities().flying = false;
        }
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            mc.player.getAbilities().flying = false;
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
        }
        positionLocked = false;
        super.onDisable();
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick event) {
        if (fullNullCheck()) return;

        if (!positionLocked && lockY.getValue()) {
            lockedY = mc.player.getY();
            positionLocked = true;
        }

        tickCounter++;
        
        mc.player.setVelocity(0, 0, 0);
        mc.player.fallDistance = 0;
        mc.player.setOnGround(true);

        float forward = mc.player.input.movementForward;
        float sideways = mc.player.input.movementSideways;

        if (forward == 0 && sideways == 0) {
            if (lockY.getValue()) {
                mc.player.setPosition(mc.player.getX(), lockedY, mc.player.getZ());
            }
            return;
        }

        float yaw = mc.player.getYaw();
        float radianYaw = (float) Math.toRadians(yaw);

        double motionX = (forward * Math.sin(radianYaw) + sideways * Math.cos(radianYaw)) * -1;
        double motionZ = (forward * Math.cos(radianYaw) - sideways * Math.sin(radianYaw));

        double horizontalMagnitude = Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (horizontalMagnitude > 0) {
            motionX = (motionX / horizontalMagnitude) * speed.getValue();
            motionZ = (motionZ / horizontalMagnitude) * speed.getValue();
        } else {
            motionX = 0;
            motionZ = 0;
        }

        double motionY = 0;
        
        if (antiKick.getValue() && tickCounter % 80 == 0) {
            motionY = -0.04;
        }

        if (!lockY.getValue()) {
            if (mc.options.jumpKey.isPressed()) {
                motionY = speed.getValue();
            } else if (mc.options.sneakKey.isPressed()) {
                motionY = -speed.getValue();
            }
        }

        mc.player.setVelocity(motionX, motionY, motionZ);

        if (lockY.getValue()) {
            mc.player.setPosition(mc.player.getX(), lockedY, mc.player.getZ());
        }
    }

    @EventHandler
    public void onPacketSend(EventPacket.Send event) {
        if (event.getPacket() instanceof PlayerMoveC2SPacket) {
            if (lockY.getValue()) {
                PlayerMoveC2SPacket packet = (PlayerMoveC2SPacket) event.getPacket();
                if (packet.changesPosition()) {
                    event.cancel();
                }
            }
        }
    }
}
