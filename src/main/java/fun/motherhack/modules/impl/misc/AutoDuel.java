package fun.motherhack.modules.impl.misc;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.modules.settings.impl.StringSetting;
import fun.motherhack.modules.settings.api.Nameable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import fun.motherhack.api.events.impl.EventPacket;

public class AutoDuel extends Module {
    private final EnumSetting<Mode> mode = new EnumSetting<>("Режим", Mode.Accept);
    private final StringSetting nickname = new StringSetting("Ник", "06ED", false);
    private final NumberSetting delay = new NumberSetting("Задержка", 2f, 0f, 30f, 0.1f);
    private long timer = 0;
    private boolean waiting = false;

    @AllArgsConstructor
    @Getter
    public enum Mode implements Nameable {
        Accept("Принять"),
        Send("Отправить");

        private final String name;
    }

    public AutoDuel() {
        super("AutoDuel", Category.Misc);
        getSettings().add(mode);
        getSettings().add(nickname);
        getSettings().add(delay);
    }

    @Override
    public void onEnable() {
        waiting = false;
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive event) {
        if (waiting && System.currentTimeMillis() - timer > (long) (1000 * delay.getValue())) {
            clickSlot(0);
            timer = System.currentTimeMillis();
            waiting = false;
        }

        if (event.getPacket() instanceof GameMessageS2CPacket pac) {
            String message = pac.content().getString().toLowerCase();
            switch (mode.getValue()) {
                case Accept -> {
                    if (!message.contains("duel request received from " + nickname.getValue().toLowerCase()))
                        return;
                    if (mc.player != null && mc.player.networkHandler != null)
                        mc.player.networkHandler.sendChatCommand("duel accept " + nickname.getValue());
                }
                case Send -> {
                    if (message.contains("[duels]") && message.contains(nickname.getValue().toLowerCase()) && 
                        message.contains(mc.getSession().getUsername().toLowerCase())) {
                        if (mc.player != null && mc.player.networkHandler != null)
                            mc.player.networkHandler.sendChatCommand("duel " + nickname.getValue());
                        waiting = true;
                        timer = System.currentTimeMillis();
                    }
                }
            }
        }
    }

    private void clickSlot(int slot) {
        if (mc.interactionManager != null && mc.player != null)
            mc.interactionManager.clickSlot(0, slot, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
    }
}
