package fun.motherhack.modules.impl.render;

import fun.motherhack.api.events.impl.EventCamera;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.Perspective;

public class NoCameraClip extends Module {

    public BooleanSetting antiFront = new BooleanSetting("AntiFront", false);
    public NumberSetting distance = new NumberSetting("Distance", 3f, 1f, 20f, 0.1f);

    private float animation;

    public NoCameraClip() {
        super("NoCameraClip", Category.Render);
    }

    @EventHandler
    public void onCamera(EventCamera event) {
        if (fullNullCheck()) return;

        if (mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            animation = lerp(animation, 0f, 0.1f);
        } else {
            animation = lerp(animation, 1f, 0.1f);
        }

        if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT && antiFront.getValue()) {
            mc.options.setPerspective(Perspective.FIRST_PERSON);
        }

        float currentDistance = getDistance();
        event.setDistance(currentDistance);
        event.setCameraClip(true);
        event.cancel();
    }

    public float getDistance() {
        return 1f + ((distance.getValue() - 1f) * animation);
    }

    private float lerp(float start, float end, float delta) {
        return start + (end - start) * delta;
    }
}
