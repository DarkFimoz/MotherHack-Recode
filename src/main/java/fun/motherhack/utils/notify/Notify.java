package fun.motherhack.utils.notify;

import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.utils.Wrapper;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.math.TimerUtils;
import fun.motherhack.utils.render.fonts.Fonts;
import fun.motherhack.utils.render.Render2D;
import lombok.Getter;

import java.awt.*;

@Getter
public class Notify implements Wrapper {

    private final NotifyIcon icon;
    private final String notify;
    private final long delay;
    private float y;
    private final Animation animation = new Animation(300, 1f, true, Easing.BOTH_SINE);
    private final TimerUtils timer = new TimerUtils();

    public Notify(NotifyIcon icon, String notify, long delay) {
        this.icon = icon;
        this.notify = notify;
        this.delay = delay;
        y = mc.getWindow().getScaledHeight() + 20;
        timer.reset();
    }

    public void render(EventRender2D e, float picunY, fun.motherhack.modules.impl.client.Notifications.NotificationPosition position) {
        y = animate(y, picunY);
        float fontSize = 10f;
        float iconSize = 9f;
        float width = Fonts.MEDIUM.getWidth(notify, fontSize);
        float width2 = Fonts.ICONS.getWidth(icon.icon(), iconSize);
        float width3 = width + width2 + 8f;
        float height = 17f;
        
        float x;
        switch (position) {
            case BottomLeft:
                x = 10f;
                break;
            case BottomRight:
                x = mc.getWindow().getScaledWidth() - width3 - 10f;
                break;
            case BottomCenter:
            default:
                x = mc.getWindow().getScaledWidth() / 2f - (width3 / 2f);
                break;
        }
        
        if (timer.passed(delay)) animation.update(false);
        Render2D.drawStyledRect(e.getContext().getMatrices(), x - 2.5f, y - 2.5f, width3 + 5f, height, 1f, new Color(0, 0, 0, (int) (87 * animation.getValue())), (int) (127 * animation.getValue()));
        Render2D.drawFont(e.getContext().getMatrices(), Fonts.MEDIUM.getFont(fontSize), notify, x + width2 + 4.5f, y - 0.5f, new Color(255, 255, 255, (int) (127 * animation.getValue())));
        Render2D.drawFont(e.getContext().getMatrices(), Fonts.ICONS.getFont(iconSize), icon.icon(), x + 1f, y + 1f, new Color(255, 255, 255, (int) (127 * animation.getValue())));
    }

    public float animate(float value, float target) {
        return value + (target - value) / 8f;
    }

    public boolean expired() {
        return timer.passed(delay) && animation.getValue() < 0.01f;
    }
}