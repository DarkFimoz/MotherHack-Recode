package fun.motherhack.modules.impl.misc;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.managers.BotManager;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.ButtonSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.modules.settings.impl.StringSetting;
import fun.motherhack.utils.network.ChatUtils;
import meteordevelopment.orbit.EventHandler;

public class Bots extends Module {

    private final StringSetting nickname = new StringSetting("settings.bots.nickname", "Bot", false);
    private final NumberSetting botCount = new NumberSetting("settings.bots.count", 1, 1, 50, 1);
    private final StringSetting password = new StringSetting("settings.bots.password", "", false);
    private final BooleanSetting enableSpam = new BooleanSetting("settings.bots.enablespam", false);
    private final StringSetting spamMessage = new StringSetting("settings.bots.spammessage", "Hello from MotherHack!", false);
    private final NumberSetting spamDelay = new NumberSetting("settings.bots.spamdelay", 5.0f, 0.5f, 60.0f, 0.5f);
    private final NumberSetting joinDelay = new NumberSetting("settings.bots.joindelay", 1.0f, 0.1f, 10.0f, 0.1f);
    
    private final ButtonSetting startBots = new ButtonSetting("settings.bots.start", this::startBotsAction);
    private final ButtonSetting stopBots = new ButtonSetting("settings.bots.stop", this::stopBotsAction);

    private BotManager botManager;

    public Bots() {
        super("Bots", Category.Misc);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (fullNullCheck()) {
            setToggled(false);
            return;
        }
        
        botManager = MotherHack.getInstance().getBotManager();
        if (botManager == null) {
            ChatUtils.sendMessage("§c[Bots] BotManager not initialized!");
            setToggled(false);
            return;
        }
        
        ChatUtils.sendMessage("§a[Bots] Module enabled. Use buttons to start/stop bots.");
    }

    @Override
    public void onDisable() {
        stopBotsAction();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
    }

    private void startBotsAction() {
        if (fullNullCheck()) return;
        
        if (botManager == null) {
            botManager = MotherHack.getInstance().getBotManager();
            if (botManager == null) {
                ChatUtils.sendMessage("§c[Bots] BotManager not initialized!");
                return;
            }
        }

        String serverIp = getServerIp();
        if (serverIp == null || serverIp.isEmpty()) {
            ChatUtils.sendMessage("§c[Bots] Cannot get server IP! Join a server first.");
            return;
        }

        int port = getServerPort();
        String nick = nickname.getValue();
        int count = (int) botCount.getValue().floatValue();
        String pass = password.getValue();
        boolean spam = enableSpam.getValue();
        String spamMsg = spamMessage.getValue();
        long spamDel = (long) (spamDelay.getValue() * 1000);
        long joinDel = (long) (joinDelay.getValue() * 1000);

        ChatUtils.sendMessage("§a[Bots] Starting " + count + " bots to " + serverIp + ":" + port);
        
        botManager.startBots(serverIp, port, nick, count, pass, spam, spamMsg, spamDel, joinDel);
    }

    private void stopBotsAction() {
        if (botManager != null) {
            botManager.stopAllBots();
        }
    }

    private String getServerIp() {
        if (mc.getCurrentServerEntry() != null) {
            String address = mc.getCurrentServerEntry().address;
            if (address.contains(":")) {
                return address.split(":")[0];
            }
            return address;
        }
        return null;
    }

    private int getServerPort() {
        if (mc.getCurrentServerEntry() != null) {
            String address = mc.getCurrentServerEntry().address;
            if (address.contains(":")) {
                try {
                    return Integer.parseInt(address.split(":")[1]);
                } catch (NumberFormatException e) {
                    return 25565;
                }
            }
        }
        return 25565;
    }
}
