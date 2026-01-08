package fun.motherhack.modules.impl.client;

import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.screen.csgui.MHackGui;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Bind;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import meteordevelopment.orbit.EventHandler;
import org.lwjgl.glfw.GLFW;

public class MHACKGUI extends Module {

    private final BooleanSetting showBackground = new BooleanSetting("Show Background", false);

    public MHACKGUI() {
        super("MHACKGUI", Category.Client);
        setBind(new Bind(GLFW.GLFW_KEY_RIGHT_CONTROL, false));
    }

    public boolean isShowBackground() {
        return showBackground.getValue();
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (!(mc.currentScreen instanceof MHackGui)) setToggled(false);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        mc.execute(() -> mc.setScreen(MHackGui.getInstance()));
    }
}