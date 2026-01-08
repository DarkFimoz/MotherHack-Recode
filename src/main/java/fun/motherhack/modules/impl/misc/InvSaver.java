package fun.motherhack.modules.impl.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.api.Bind;
import fun.motherhack.MotherHack;
import fun.motherhack.utils.notify.Notify;
import fun.motherhack.utils.notify.NotifyIcons;

public class InvSaver extends Module {
    private Screen bufferedScreen;

    private final BooleanSetting notifyOnClose;

    public InvSaver() {
        super("InvSaver", Category.Misc);
        setBind(new Bind(66, false)); // B key

        notifyOnClose = new BooleanSetting("Notify on Close", false);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.bufferedScreen = null;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (this.bufferedScreen != null) {
            mc.setScreen(this.bufferedScreen);
            this.bufferedScreen = null;
        }
    }

    @EventHandler
    public void onUpdate(EventPlayerTick e) {
        if (mc.player != null && mc.currentScreen instanceof HandledScreen && this.bufferedScreen == null) {
            this.bufferedScreen = mc.currentScreen;
            mc.setScreen(null);
            if (notifyOnClose.getValue()) {
                MotherHack.getInstance().getNotifyManager().add(new Notify(NotifyIcons.successIcon, "Shop GUI closed", 2000));
            }
        }
    }
}
