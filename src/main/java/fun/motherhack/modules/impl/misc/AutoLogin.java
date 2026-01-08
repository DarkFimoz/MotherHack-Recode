package fun.motherhack.modules.impl.misc;

import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.StringSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

public class AutoLogin extends Module {

    public final StringSetting password = new StringSetting("settings.autologin.password", "", false);

    public AutoLogin() {
        super("AutoLogin", Category.Misc);
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive e) {
        if (fullNullCheck()) return;

        if (e.getPacket() instanceof GameMessageS2CPacket packet) {
            String message = packet.content().getString().toLowerCase();
            
            if (message.contains("/login") || message.contains("/l") || 
                message.contains("/reg") || message.contains("/register")) {
                
                if (!password.getValue().isEmpty()) {
                    String command = null;
                    
                    if (message.contains("/login")) {
                        command = "login";
                    } else if (message.contains("/l ") || message.contains("/l\n") || message.endsWith("/l")) {
                        command = "l";
                    } else if (message.contains("/register")) {
                        command = "register";
                    } else if (message.contains("/reg")) {
                        command = "reg";
                    }
                    
                    if (command != null) {
                        mc.getNetworkHandler().sendChatCommand(command + " " + password.getValue());
                    }
                }
            }
        }
    }
}
