package fun.motherhack.modules.impl.movement;

import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.utils.math.TimerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

public class Phase extends Module {
    public Phase() {
        super("Phase", Category.Movement);
    }

    public final BooleanSetting duplicate = new BooleanSetting("Duplicate on flag", false);

    private boolean isInCollisionCheck = false;
    private Vec3d start = Vec3d.ZERO;
    private final ArrayList<Packet<?>> blinkedPackets = new ArrayList<>();
    private final ArrayList<Packet<?>> buffered = new ArrayList<>();
    private final TimerUtils timeResend = new TimerUtils();

    @Override
    public void onEnable() {
        super.onEnable();
        if (fullNullCheck()) return;
        isInCollisionCheck = false;
        start = mc.player.getPos();
        blinkedPackets.clear();
        buffered.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (!fullNullCheck()) {
            mc.player.noClip = false;
        }
        flushPackets();
        isInCollisionCheck = false;
        timeResend.reset();
    }

    @EventHandler
    public void onPacketSend(EventPacket.Send event) {
        if (fullNullCheck()) return;
        
        if (event.getPacket() instanceof PlayerMoveC2SPacket) {
            event.cancel();
            blinkedPackets.add(event.getPacket());
            buffered.add(event.getPacket());
        }
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive event) {
        if (fullNullCheck()) return;
        
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket packet) {
            if (packet.change().position().distanceTo(start) < 0.3 && duplicate.getValue()) {
                timeResend.reset();
            }
        }
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick event) {
        if (fullNullCheck()) return;

        boolean hasBlock = mc.world.getStatesInBox(mc.player.getBoundingBox().shrink(0.01F, 0.01F, 0.01F))
                .anyMatch(BlockState::isSolid);

        if (!hasBlock && isInCollisionCheck) {
            flushPackets();
            isInCollisionCheck = false;
            mc.player.noClip = false;
            timeResend.reset();
            return;
        }

        if (hasBlock) {
            mc.player.setVelocity(mc.player.getVelocity().multiply(0.6F, 1, 0.6F));
            mc.player.noClip = true;
            isInCollisionCheck = true;
        }
    }

    private void flushPackets() {
        if (blinkedPackets.isEmpty()) return;
        
        // Создаем копию списка для безопасной итерации
        ArrayList<Packet<?>> packetsToSend = new ArrayList<>(blinkedPackets);
        blinkedPackets.clear();
        
        for (Packet<?> packet : packetsToSend) {
            mc.getNetworkHandler().sendPacket(packet);
        }
    }
}
