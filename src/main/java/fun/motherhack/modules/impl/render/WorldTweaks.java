package fun.motherhack.modules.impl.render;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.modules.settings.impl.ColorSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.api.events.impl.EventFog;
import java.awt.*;

public class WorldTweaks extends Module {
    public final BooleanSetting fogModify = new BooleanSetting("Изменить туман", true);
    public final NumberSetting fogStart = new NumberSetting("Начало тумана", 0f, 0f, 256f, 1f);
    public final NumberSetting fogEnd = new NumberSetting("Конец тумана", 64f, 10f, 256f, 1f);
    public final ColorSetting fogColor = new ColorSetting("Цвет тумана", new Color(0xA900FF));
    
    public final BooleanSetting skyModify = new BooleanSetting("Изменить небо", false);
    public final ColorSetting skyColor = new ColorSetting("Цвет неба", new Color(0xFF0000));
    
    public final BooleanSetting ctime = new BooleanSetting("Изменить время", false);
    public final NumberSetting ctimeVal = new NumberSetting("Время", 21f, 0f, 23f, 1f);

    private long oldTime;

    public WorldTweaks() {
        super("WorldTweaks", Category.Render);
        getSettings().add(fogModify);
        getSettings().add(fogStart);
        getSettings().add(fogEnd);
        getSettings().add(fogColor);
        getSettings().add(skyModify);
        getSettings().add(skyColor);
        getSettings().add(ctime);
        getSettings().add(ctimeVal);
        
        fogStart.setVisible(() -> fogModify.getValue());
        fogEnd.setVisible(() -> fogModify.getValue());
        fogColor.setVisible(() -> fogModify.getValue());
        skyColor.setVisible(() -> skyModify.getValue());
        ctimeVal.setVisible(() -> ctime.getValue());
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (!fullNullCheck())
            oldTime = mc.world.getTime();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (!fullNullCheck())
            mc.world.setTime(oldTime, oldTime, true);
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive event) {
        if (fullNullCheck()) return;

        if (event.getPacket() instanceof WorldTimeUpdateS2CPacket && ctime.getValue()) {
            event.cancel();
        }
    }

    @EventHandler
    public void onTick(EventPlayerTick event) {
        if (fullNullCheck()) return;

        if (ctime.getValue()) {
            long time = (long) (ctimeVal.getValue() * 1000);
            mc.world.setTime(time, time, false);
        }
    }

    @EventHandler
    public void onFog(EventFog event) {
        if (fullNullCheck()) return;
        
        if (fogModify.getValue()) {
            event.setStart(fogStart.getValue());
            event.setEnd(fogEnd.getValue());
            event.setColor(fogColor.getColor());
        }
    }
}
