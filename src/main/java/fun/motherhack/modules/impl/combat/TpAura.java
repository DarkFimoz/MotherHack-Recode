package fun.motherhack.modules.impl.combat;

import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import lombok.AllArgsConstructor;
import lombok.Getter;
import fun.motherhack.utils.network.Server;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class TpAura extends Module {

    private final EnumSetting<Mode> mode = new EnumSetting<>("settings.tpaura.mode", Mode.BlockTP);
    private final NumberSetting range = new NumberSetting("settings.tpaura.range", 15f, 10f, 200f, 1f);
    private final NumberSetting offsetDistance = new NumberSetting("settings.tpaura.offset", 2f, 0.5f, 5f, 0.1f);

    private PlayerEntity currentTarget = null;
    private Vec3d lastHandledVec = Vec3d.ZERO;

    public TpAura() {
        super("TpAura", Category.Combat);
    }

    @AllArgsConstructor
    @Getter
    public enum Mode implements Nameable {
        BlockTP("settings.tpaura.mode.blocktp"),
        StepVH("settings.tpaura.mode.stepvh");

        private final String name;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        currentTarget = null;
        lastHandledVec = Vec3d.ZERO;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        currentTarget = null;
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick e) {
        if (fullNullCheck()) return;

        PlayerEntity newTarget = findTarget();
        if (newTarget != null && newTarget != currentTarget) {
            currentTarget = newTarget;
            performTeleport();
        }
    }

    private void performTeleport() {
        if (currentTarget == null || mc.player == null) return;

        double distance = mc.player.distanceTo(currentTarget);
        if (distance > range.getValue()) return;

        Vec3d targetPos = calculateTargetPosition(currentTarget);
        executeTeleport(mc.player.getPos(), targetPos);
    }

    private void executeTeleport(Vec3d from, Vec3d to) {
        double dx = Math.abs(from.x - to.x);
        double dy = Math.abs(from.y - to.y);
        double dz = Math.abs(from.z - to.z);
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        float distanceDensity = 1.0f;
        int packetCount = (int) (distance / (9.64 * distanceDensity)) + 1;

        for (int i = 0; i < packetCount; i++) {
            sendGroundPacket(false);
        }

        sendPositionPacket(to.x, to.y, to.z, false);
        mc.player.setPosition(to);
        lastHandledVec = to;
    }

    private Vec3d calculateTargetPosition(PlayerEntity target) {
        Vec3d targetPos = target.getPos();
        Vec3d direction = targetPos.subtract(mc.player.getPos()).normalize();
        Vec3d teleportPos = targetPos.subtract(direction.multiply(offsetDistance.getValue()));

        BlockPos groundPos = findNearestSolidBlock(teleportPos, 5);
        if (groundPos != null) {
            teleportPos = new Vec3d(teleportPos.x, groundPos.getY() + 1.0, teleportPos.z);
        }

        return teleportPos;
    }

    private BlockPos findNearestSolidBlock(Vec3d pos, double radius) {
        BlockPos center = new BlockPos((int) pos.x, (int) pos.y, (int) pos.z);
        for (int y = 0; y < radius; y++) {
            BlockPos checkPos = center.down(y);
            if (mc.world.getBlockState(checkPos).isSolid()) {
                return checkPos;
            }
        }
        return null;
    }

    private PlayerEntity findTarget() {
        if (mc.world == null || mc.player == null) return null;

        PlayerEntity closest = null;
        float closestDist = Float.MAX_VALUE;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) continue;
            if (!Server.isValid(player)) continue;

            float dist = mc.player.distanceTo(player);
            if (dist <= range.getValue() && dist < closestDist) {
                closest = player;
                closestDist = dist;
            }
        }

        return closest;
    }

    private void sendPositionPacket(double x, double y, double z, boolean onGround) {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, mc.player.horizontalCollision));
    }

    private void sendGroundPacket(boolean onGround) {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(onGround, mc.player.horizontalCollision));
    }
}
