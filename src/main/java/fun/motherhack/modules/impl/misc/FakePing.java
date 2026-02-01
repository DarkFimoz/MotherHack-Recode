package fun.motherhack.modules.impl.misc;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import fun.motherhack.api.events.impl.EventPacket;
import java.util.Random;

public class FakePing extends Module {
    private final NumberSetting minPing = new NumberSetting("Минимальный пинг", 500f, 0f, 10000f, 100f);
    private final NumberSetting maxPing = new NumberSetting("Максимальный пинг", 10000f, 0f, 10000f, 100f);
    private int fakePing = 500;
    private final Random random = new Random();

    public FakePing() {
        super("FakePing", Category.Misc);
        getSettings().add(minPing);
        getSettings().add(maxPing);
    }

    @EventHandler
    public void onPacket(EventPacket.Receive event) {
        if (event.getPacket() instanceof CommonPingS2CPacket) {
            fakePing = (int) (minPing.getValue() + random.nextInt((int) (maxPing.getValue() - minPing.getValue() + 1)));
            event.cancel();
        }
    }

    public int getFakePing() {
        return fakePing;
    }
}
