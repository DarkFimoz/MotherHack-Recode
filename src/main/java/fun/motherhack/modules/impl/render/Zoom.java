package fun.motherhack.modules.impl.render;

import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.api.Bind;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;

public class Zoom extends Module {

    public NumberSetting zoom = new NumberSetting("Zoom", 0.5f, 0.01f, 1f, 0.01f);
    public BooleanSetting scroll = new BooleanSetting("Scroll", true);
    public NumberSetting scrollFactor = new NumberSetting("Scroll Factor", 0.3f, 0.1f, 0.65f, 0.01f);

    public Zoom() {
        super("Zoom", Category.Render);
    }

    private final Animation animation = new Animation(750, 1.0, true, Easing.EASE_OUT_CUBIC);
    private float needZoomValue;
    private float currentZoomValue;

    @Override
    public void onEnable() {
        currentZoomValue = 1;
        animation.reset();
        needZoomValue = 1 / zoom.getValue();
        super.onEnable();
    }

    public void mouseScroll(float step) {
        if (scroll.getValue()) {
            needZoomValue = Math.max(1, needZoomValue + (step * scrollFactor.getValue() * needZoomValue));
            animation.reset();
        }
    }

    public float getFov(float original) {
        currentZoomValue = (float) MathHelper.lerp(animation.getValue(), currentZoomValue, needZoomValue);
        return original / currentZoomValue;
    }
}
