package fun.motherhack.modules.impl.misc;

import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.api.Category;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

public class NoServerRotate extends Module {
    public NoServerRotate() {
        super("NoServerRotate", Category.Misc);
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive e) {
        if (fullNullCheck()) return;
        if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
            // Отменяем пакет, который пытается изменить вращение игрока
            e.cancel();
        }
    }
}