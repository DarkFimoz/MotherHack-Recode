package fun.motherhack.modules.impl.render;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.render.ColorUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

import java.awt.*;

public class AmbienceModule extends Module {

    NumberSetting timeSetting = new NumberSetting("Время", 10, 1f, 24000f, 1f);
    public final NumberSetting distanceFog = new NumberSetting("Дистанция Тумана", 50f, 10f, 100f, 0.01f);
    public final BooleanSetting customFog = new BooleanSetting("Кастом туман", true);

    public Color getFogColor() {
        UI uiModule = MotherHack.getInstance().getModuleManager().getModule(UI.class);
        if (uiModule != null) {
            return uiModule.getTheme().getBackgroundColor();
        }
        return ColorUtils.getGlobalColor();
    }



    public AmbienceModule() {
        super("AmbienceModule", Category.Render);
        getSettings().add(timeSetting);
        getSettings().add(customFog);
        getSettings().add(distanceFog);
    }


    @EventHandler
    public void onPacket(EventPacket.Receive event) {
        if (mc == null || mc.world == null) return;

        if (event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
            event.cancel();
            long time = timeSetting.getValue().longValue();
            mc.world.setTime(time, time, true);
        }
    }
}

