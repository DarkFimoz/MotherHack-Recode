package fun.motherhack.managers;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.api.events.impl.EventPopTotem;
import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.modules.api.Module;
import fun.motherhack.utils.Wrapper;
import fun.motherhack.utils.network.ChatUtils;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import fun.motherhack.api.mixins.accessors.IBossBarHud;

@Getter
public class ServerManager implements Wrapper {

    private int serverSlot;
    private float serverYaw, serverPitch, fallDistance;
    private double serverX, serverY, serverZ;
    private boolean serverOnGround, serverSprinting, serverSneaking, serverHorizontalCollision;

    public ServerManager() {
        MotherHack.getInstance().getEventHandler().subscribe(this);
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (Module.fullNullCheck()) return;

        double y = mc.player.prevY - mc.player.getY();
        if (mc.player.isOnGround()) fallDistance = 0;
        else if (y > 0) fallDistance += (float) y;
    }

    @EventHandler
    public void onPacketSend(EventPacket.Send e) {
        if (Module.fullNullCheck()) return;

        if (e.getPacket() instanceof PlayerMoveC2SPacket packet) {
            if (packet.changesPosition()) {
                serverX = packet.getX(mc.player.getX());
                serverY = packet.getY(mc.player.getY());
                serverZ = packet.getZ(mc.player.getZ());
            }

            if (packet.changesLook()) {
                serverYaw = packet.getYaw(mc.player.getYaw());
                serverPitch = packet.getPitch(mc.player.getPitch());
            }

            serverOnGround = packet.isOnGround();
            serverHorizontalCollision = packet.horizontalCollision();
        }

        if (e.getPacket() instanceof UpdateSelectedSlotC2SPacket packet) serverSlot = packet.getSelectedSlot();

        if (e.getPacket() instanceof ClientCommandC2SPacket packet) {
            switch (packet.getMode()) {
                case START_SPRINTING -> serverSprinting = true;
                case STOP_SPRINTING -> serverSprinting = false;
                case PRESS_SHIFT_KEY -> serverSneaking = true;
                case RELEASE_SHIFT_KEY -> serverSneaking = false;
            }
        }
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive e) {
        if (Module.fullNullCheck()) return;

        if (e.getPacket() instanceof EntityStatusS2CPacket packet && packet.getStatus() == 35) {
            if (!(packet.getEntity(mc.world) instanceof PlayerEntity player)) return;
            MotherHack.getInstance().getEventHandler().post(new EventPopTotem(player));
        }
    }
}