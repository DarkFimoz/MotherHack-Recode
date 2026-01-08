package fun.motherhack.modules.impl.movement;

import meteordevelopment.orbit.EventHandler;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.utils.math.TimerUtils;

public class AutoWalk extends Module {
    public AutoWalk() {
        super("AutoWalk", Category.Movement);
    }

    public final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.Normal);
    public final NumberSetting delay = new NumberSetting("Delay", 2.0f, 0.1f, 10.0f, 0.1f);
    public final NumberSetting jumps = new NumberSetting("Jumps", 2.0f, 1.0f, 10.0f, 1.0f);

    public enum Mode implements Nameable {
        Helper, Normal;

        @Override
        public String getName() {
            return name();
        }
    }

    private final TimerUtils jumpTimer = new TimerUtils();
    private int jumpCount = 0;

    @EventHandler
    public void onTick(EventPlayerTick event) {
        if (fullNullCheck()) return;

        if (mode.getValue() == Mode.Normal) {
            // Normal mode: just walk forward
            mc.options.forwardKey.setPressed(true);
        } else if (mode.getValue() == Mode.Helper) {
            // Helper mode: jump every delay seconds
            if (jumpTimer.passed((long) (delay.getValue().floatValue() * 1000))) {
                if (jumpCount < jumps.getValue().floatValue()) {
                    mc.options.jumpKey.setPressed(true);
                    jumpCount++;
                } else {
                    mc.options.jumpKey.setPressed(false);
                    jumpCount = 0;
                    jumpTimer.reset();
                }
            } else {
                mc.options.jumpKey.setPressed(false);
            }
            
            // Always walk forward in helper mode too
            mc.options.forwardKey.setPressed(true);
        }
    }

    @Override
    public void onDisable() {
        // Reset keys when module is disabled
        mc.options.forwardKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        super.onDisable();
    }
}
