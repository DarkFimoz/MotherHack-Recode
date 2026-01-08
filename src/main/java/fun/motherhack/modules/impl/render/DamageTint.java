package fun.motherhack.modules.impl.render;

import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.render.Render2D;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class DamageTint extends Module {
    private final NumberSetting intensity = new NumberSetting("Intensity", 50f, 0f, 255f, 1f);
    
    public DamageTint() {
        super("DamageTint", Category.Render);
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck()) return;
        
        float factor = 1f - MathHelper.clamp(mc.player.getHealth(), 0f, 12f) / 12f;
        if (factor > 0.01f) {
            Color red = new Color(255, 0, 0, (int) (factor * intensity.getValue()));
            Render2D.drawRoundedRect(e.getContext().getMatrices(), 0, 0, 
                mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), 
                0, red);
        }
    }
}