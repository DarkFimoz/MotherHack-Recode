package fun.motherhack.modules.impl.misc;

import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.network.NetworkUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;

public class NoOpen extends Module {
    private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.NoOpen);
    private final NumberSetting delay = new NumberSetting("Delay", 50f, 0f, 500f, 10f, 
            () -> mode.getValue() == Mode.Closer);

    private int pendingSyncId = -1;
    private int ticksWaited = 0;

    public NoOpen() {
        super("NoOpen", Category.Misc);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        pendingSyncId = -1;
        ticksWaited = 0;
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (fullNullCheck()) return;
        
        // Обработка отложенного закрытия для режима Closer
        if (mode.getValue() == Mode.Closer && pendingSyncId != -1) {
            ticksWaited++;
            
            // Конвертируем миллисекунды в тики (50ms = 1 tick при 20 TPS)
            int ticksToWait = (int) (delay.getValue() / 50f);
            
            if (ticksWaited >= ticksToWait) {
                NetworkUtils.sendPacket(new CloseHandledScreenC2SPacket(pendingSyncId));
                pendingSyncId = -1;
                ticksWaited = 0;
            }
        }
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive e) {
        if (fullNullCheck()) return;
        
        if (e.getPacket() instanceof OpenScreenS2CPacket packet) {
            Mode currentMode = mode.getValue();
            
            switch (currentMode) {
                case NoPKM -> {
                    // В режиме NoPKM не блокируем пакеты открытия экрана
                }
                case NoOpen -> {
                    // Блокируем пакет открытия любого экрана (сундук, инвентарь, магазин и т.п.)
                    e.cancel();
                }
                case Closer -> {
                    // Автоматически закрываем экран с задержкой для легитимности
                    pendingSyncId = packet.getSyncId();
                    ticksWaited = 0;
                }
            }
        }
    }

    @EventHandler
    public void onPacketSend(EventPacket.Send e) {
        if (fullNullCheck()) return;
        
        // Режим NoPKM - блокируем пакеты взаимодействия с блоками (ПКМ)
        if (mode.getValue() == Mode.NoPKM) {
            if (e.getPacket() instanceof PlayerInteractBlockC2SPacket) {
                e.cancel();
            }
        }
    }

    public enum Mode implements Nameable {
        NoPKM("settings.noopen.mode.nopkm"),
        NoOpen("settings.noopen.mode.noopen"),
        Closer("settings.noopen.mode.closer");

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
