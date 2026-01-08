package fun.motherhack.modules.impl.misc;

import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.api.mixins.accessors.IMinecraftClient;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import meteordevelopment.orbit.EventHandler;

public class FastHub extends Module {
    private int ticks = 0;
    
    public FastHub() {
        super("FastHub", Category.Misc);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        ticks = 0;
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (mc.player == null || mc.player.networkHandler == null) return;
        
        if (ticks == 1) {
            mc.player.networkHandler.sendChatCommand("hub");
            this.toggle(); // Disable the module after sending the command
        }
        
        if (ticks < 2) {
            ticks++;
        }
    }
}