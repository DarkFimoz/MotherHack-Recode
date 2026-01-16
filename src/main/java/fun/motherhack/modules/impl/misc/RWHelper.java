package fun.motherhack.modules.impl.misc;

import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.utils.movement.MoveUtils;
import fun.motherhack.utils.network.NetworkUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;

public class RWHelper extends Module {

    private final BooleanSetting fly = new BooleanSetting("settings.rwhelper.fly", true);
    private final BooleanSetting closeMenu = new BooleanSetting("settings.rwhelper.closemenu", true);

    public RWHelper() {
        super("RWHelper", Category.Misc);
    }

    @EventHandler
    public void onTick(EventPlayerTick e) {
        if (fullNullCheck()) return;
        dragonFly();
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive e) {
        if (fullNullCheck()) return;
        checkWindow(e);
    }

    private void dragonFly() {
        if (fly.getValue() && mc.player.getAbilities().flying) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.0, mc.player.getVelocity().z);
            
            if (mc.options.jumpKey.isPressed()) {
                mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y + 0.185, mc.player.getVelocity().z);
            }
            if (mc.options.sneakKey.isPressed()) {
                mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y - 0.185, mc.player.getVelocity().z);
            }
            
            setMotion(1.050);
        }
    }

    private void checkWindow(EventPacket.Receive eventPacket) {
        if (closeMenu.getValue() && eventPacket.getPacket() instanceof OpenScreenS2CPacket packet) {
            if (!mc.world.getScoreboard().getTeams().isEmpty() && packet.getName().getString().contains("Меню")) {
                eventPacket.cancel();
                NetworkUtils.sendPacket(new CloseHandledScreenC2SPacket(0));
            }
        }
    }

    private void setMotion(double speed) {
        if (!MoveUtils.isMoving()) return;
        
        float yaw = mc.player.getYaw();
        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;
        
        if (forward == 0 && strafe == 0) {
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
            return;
        }
        
        if (forward != 0) {
            if (strafe > 0) {
                yaw += (forward > 0 ? -45 : 45);
            } else if (strafe < 0) {
                yaw += (forward > 0 ? 45 : -45);
            }
            strafe = 0;
            if (forward > 0) {
                forward = 1;
            } else if (forward < 0) {
                forward = -1;
            }
        }
        
        double rad = Math.toRadians(yaw);
        mc.player.setVelocity(
            forward * speed * -Math.sin(rad) + strafe * speed * Math.cos(rad),
            mc.player.getVelocity().y,
            forward * speed * Math.cos(rad) + strafe * speed * Math.sin(rad)
        );
    }
}
