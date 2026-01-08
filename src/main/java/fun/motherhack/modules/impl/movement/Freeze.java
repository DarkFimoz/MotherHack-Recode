package fun.motherhack.modules.impl.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.api.Bind;

public class Freeze extends Module {

    public Freeze() {
        super("Freeze", Category.Movement);

        autoFreeze = new BooleanSetting("AutoFreeze", false);
    }

    private final BooleanSetting autoFreeze;

    @EventHandler
    public void onPlayerTick(EventPlayerTick event) {
        if (fullNullCheck()) return;

        if (autoFreeze.getValue() && mc.player.getY() <= -60) {
            mc.player.setVelocity(0.0, 0.0, 0.0);
        } else if (isToggled()) {
            mc.player.setVelocity(0.0, 0.0, 0.0);
        }
    }

    @EventHandler
    public void onPacketSend(EventPacket.Send event) {
        if (event.getPacket() instanceof PlayerMoveC2SPacket) {
            if (autoFreeze.getValue() && mc.player.getY() <= -40) {
                event.cancel();
            } else if (isToggled()) {
                event.cancel();
            }
        }
    }
}
