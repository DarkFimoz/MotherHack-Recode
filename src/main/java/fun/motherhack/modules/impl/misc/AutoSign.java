package fun.motherhack.modules.impl.misc;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.StringSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import fun.motherhack.api.events.impl.EventPacket;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AutoSign extends Module {
    private final StringSetting line1 = new StringSetting("Строка 1", "<player>", false);
    private final StringSetting line2 = new StringSetting("Строка 2", "was here", false);
    private final StringSetting line3 = new StringSetting("Строка 3", "<--------------->", false);
    private final StringSetting line4 = new StringSetting("Строка 4", "<date>", false);
    private final StringSetting dateFormat = new StringSetting("Формат даты", "dd/MM/yyyy", false);
    private final BooleanSetting glow = new BooleanSetting("Светящийся", false);

    public AutoSign() {
        super("AutoSign", Category.Misc);
        getSettings().add(line1);
        getSettings().add(line2);
        getSettings().add(line3);
        getSettings().add(line4);
        getSettings().add(dateFormat);
        getSettings().add(glow);
    }

    @EventHandler
    public void onPacketSend(EventPacket.Send event) {
        if (fullNullCheck()) return;

        if (event.getPacket() instanceof UpdateSignC2SPacket packet) {
            if (mc.currentScreen instanceof SignEditScreen) {
                event.cancel();
                
                if (mc.player.networkHandler != null) {
                    mc.player.networkHandler.sendPacket(new UpdateSignC2SPacket(
                            packet.getPos(),
                            packet.isFront(),
                            format(line1.getValue()),
                            format(line2.getValue()),
                            format(line3.getValue()),
                            format(line4.getValue())
                    ));
                }
            }
        }
    }

    private String format(String s) {
        String dateStr = "dd/MM/yyyy";
        try {
            dateStr = new SimpleDateFormat(dateFormat.getValue()).format(new Date());
        } catch (Exception e) {
        }
        return s.replace("<player>", mc.getSession().getUsername()).replace("<date>", dateStr);
    }
}
