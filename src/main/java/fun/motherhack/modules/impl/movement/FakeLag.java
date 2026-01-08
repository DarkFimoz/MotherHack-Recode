package fun.motherhack.modules.impl.movement;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.api.events.impl.EventRender3D;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.combat.Aura;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.math.TimerUtils;
import fun.motherhack.utils.network.NetworkUtils;
import fun.motherhack.utils.render.Render3D;
import fun.motherhack.utils.movement.MoveUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FakeLag extends Module {

    private final NumberSetting randomMaxDelay = new NumberSetting("settings.fakelag.randommaxdelay", 500f, 0f, 1500f, 1f);
    private final NumberSetting randomMinDelay = new NumberSetting("settings.fakelag.randommindelay", 200f, 0f, 1500f, 1f);

    private final BooleanSetting onlyTarget = new BooleanSetting("settings.fakelag.onlytarget", false);
    private final BooleanSetting inRange = new BooleanSetting("settings.fakelag.inrange", false);
    private final NumberSetting randomMaxRange = new NumberSetting("settings.fakelag.randommaxrange", 5f, 1f, 6f, 0.05f, inRange::getValue);
    private final NumberSetting randomMinRange = new NumberSetting("settings.fakelag.randomminrange", 2f, 1f, 6f, 0.05f, inRange::getValue);
    private final BooleanSetting onlyMoving = new BooleanSetting("settings.fakelag.onlymoving", false);
    private final BooleanSetting onlyGround = new BooleanSetting("settings.fakelag.onlyground", false);

    private final BooleanSetting playerActionPacket = new BooleanSetting("Player Action Packet", true);
    private final BooleanSetting updateVelocityPacket = new BooleanSetting("Update Velocity Packet", true);
    private final BooleanSetting playerInteractPacket = new BooleanSetting("Player Interact Packet", true);
    private final BooleanSetting playerPositionPacket = new BooleanSetting("Player Position Packet", true);
    private final BooleanSetting explosionPacket = new BooleanSetting("Explosion Packet", true);
    private final BooleanSetting healthUpdatePacket = new BooleanSetting("Health Update Packet", true);

    private final BooleanSetting renderBox = new BooleanSetting("Render Box", true);

    public FakeLag() {
        super("FakeLag", Category.Movement);
    }

    private final List<Packet<?>> packets = new CopyOnWriteArrayList<>();
    private final TimerUtils packetTimer = new TimerUtils();
    private PlayerEntity inRangeTarget = null;
    private Box box;

    @EventHandler
    public void onPacketSend(EventPacket.Send e) {
        if (fullNullCheck()) return;
        if (!shouldActivate()) {
            resumePackets();
            box = null;
            return;
        }

        if (e.getPacket() instanceof PlayerMoveC2SPacket) {
            packets.add(e.getPacket());
            e.cancel();
        } else if (playerActionPacket.getValue() && e.getPacket() instanceof PlayerActionC2SPacket) resumePackets();
        else if (playerInteractPacket.getValue() && e.getPacket() instanceof PlayerInteractEntityC2SPacket) resumePackets();
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive e) {
        if (fullNullCheck()) return;
        if (!shouldActivate()) {
            resumePackets();
            box = null;
            return;
        }

        boolean shouldResume = false;

        if (updateVelocityPacket.getValue() && e.getPacket() instanceof EntityVelocityUpdateS2CPacket packet && isVelocity(packet)) shouldResume = true;
        else if (explosionPacket.getValue() && e.getPacket() instanceof ExplosionS2CPacket packet && isKnockback(packet)) shouldResume = true;
        else if (healthUpdatePacket.getValue() && e.getPacket() instanceof HealthUpdateS2CPacket) shouldResume = true;
        else if (playerPositionPacket.getValue() && e.getPacket() instanceof PlayerPositionLookS2CPacket) shouldResume = true;

        if (shouldResume) resumePackets();
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick e) {
        if (fullNullCheck()) return;
        if (!shouldActivate()) {
            resumePackets();
            box = null;
            return;
        }

        if (packetTimer.passed(getCurrentDelay())) resumePackets();
    }

    @EventHandler
    public void onGameRender3D(EventRender3D.Game e) {
        if (fullNullCheck() || !renderBox.getValue() || box == null) return;
        Render3D.renderCube(e.getMatrixStack(), box, true, new Color(255, 255, 255, 50), true, Color.WHITE);
    }

    private boolean shouldActivate() {
        if (onlyTarget.getValue() && MotherHack.getInstance().getModuleManager().getModule(Aura.class).getTarget() == null) return false;
        if (onlyMoving.getValue() && !MoveUtils.isMoving()) return false;
        if (onlyGround.getValue() && !mc.player.isOnGround()) return false;
        if (mc.player.isUsingItem()) return false;
        if (mc.player.horizontalCollision) return false;

        if (inRange.getValue()) {
            inRangeTarget = null;
            float checkRange = MathUtils.randomFloat(randomMinRange.getValue(), randomMaxRange.getValue());

            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player) continue;
                if (player.getPos().squaredDistanceTo(mc.player.getEyePos()) <= MathHelper.square(checkRange)) {
                    inRangeTarget = player;
                    break;
                }
            }

            return inRangeTarget != null;
        }

        return true;
    }

    private long getCurrentDelay() {
        return (long) (MathUtils.randomFloat(randomMinDelay.getValue(), randomMaxDelay.getValue()));
    }

    private boolean isVelocity(EntityVelocityUpdateS2CPacket packet) {
        if (packet.getEntityId() !=  mc.player.getId()) return false;
        return Math.abs(packet.getVelocityX() / 8000.0) > 0.1D || Math.abs(packet.getVelocityY() / 8000.0) > 0.1D || Math.abs(packet.getVelocityZ() / 8000.0) > 0.1D;
    }

    private boolean isKnockback(ExplosionS2CPacket packet) {
        if (packet.playerKnockback().isEmpty()) return false;
        return Math.abs(packet.playerKnockback().get().getX()) > 0.1D || Math.abs(packet.playerKnockback().get().getY()) > 0.1D || Math.abs(packet.playerKnockback().get().getZ()) > 0.1D;
    }

    private void resumePackets() {
        if (!packets.isEmpty()) {
            for (Packet<?> packet : packets) {
                NetworkUtils.sendSilentPacket(packet);
                packets.remove(packet);
            }
            box = mc.player.getBoundingBox();
            packetTimer.reset();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resumePackets();
        box = null;
        inRangeTarget = null;
    }
}