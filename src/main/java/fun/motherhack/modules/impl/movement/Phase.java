package fun.motherhack.modules.impl.movement;

import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class Phase extends Module {

    public Phase() {
        super("Phase", Category.Movement);
    }

    private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.Vanilla);
    private final NumberSetting speed = new NumberSetting("Speed", 0.05f, 0.01f, 0.5f, 0.01f);
    private final BooleanSetting autoDisable = new BooleanSetting("Auto Disable", true);
    private final BooleanSetting onlyInBlock = new BooleanSetting("Only In Block", true);

    private int phaseTimer = 0;
    private boolean wasInBlock = false;

    @Override
    public void onEnable() {
        super.onEnable();
        phaseTimer = 0;
        wasInBlock = false;
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick event) {
        if (fullNullCheck()) return;

        boolean inBlock = isInsideBlock();

        if (onlyInBlock.getValue() && !inBlock) {
            if (wasInBlock && autoDisable.getValue()) {
                toggle();
            }
            wasInBlock = inBlock;
            return;
        }

        wasInBlock = inBlock;

        switch (mode.getValue()) {
            case Vanilla -> handleVanillaPhase();
            case Packet -> handlePacketPhase();
            case Skip -> handleSkipPhase();
        }
    }

    @EventHandler
    public void onPacketSend(EventPacket.Send event) {
        if (fullNullCheck()) return;

        if (mode.getValue() == Mode.Packet && event.getPacket() instanceof PlayerMoveC2SPacket) {
            if (isInsideBlock()) {
                event.cancel();
            }
        }
    }

    private void handleVanillaPhase() {
        if (!isInsideBlock()) return;

        Vec3d motion = getPhaseMotion();
        mc.player.setVelocity(motion.x, motion.y, motion.z);
    }

    private void handlePacketPhase() {
        if (!isInsideBlock()) return;

        Vec3d motion = getPhaseMotion();
        Vec3d newPos = mc.player.getPos().add(motion);

        mc.player.setPosition(newPos);
        mc.player.setVelocity(0, 0, 0);
    }

    private void handleSkipPhase() {
        if (!isInsideBlock()) return;

        phaseTimer++;
        if (phaseTimer >= 3) {
            Vec3d motion = getPhaseMotion().multiply(2.0);
            mc.player.setPosition(mc.player.getPos().add(motion));
            phaseTimer = 0;
        }
    }

    private Vec3d getPhaseMotion() {
        double forward = mc.player.input.movementForward;
        double strafe = mc.player.input.movementSideways;

        double yaw = Math.toRadians(mc.player.getYaw());
        double x = (forward * Math.sin(yaw) + strafe * Math.cos(yaw)) * speed.getValue();
        double z = (forward * Math.cos(yaw) - strafe * Math.sin(yaw)) * speed.getValue();
        double y = 0;

        if (mc.player.input.playerInput.jump()) y = speed.getValue();
        if (mc.player.input.playerInput.sneak()) y = -speed.getValue();

        return new Vec3d(-x, y, z);
    }

    private boolean isInsideBlock() {
        if (mc.player == null || mc.world == null) return false;

        Box box = mc.player.getBoundingBox().contract(0.0625);
        return mc.world.getBlockCollisions(mc.player, box).iterator().hasNext();
    }

    public enum Mode implements Nameable {
        Vanilla("Vanilla"),
        Packet("Packet"),
        Skip("Skip");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
