package fun.motherhack.modules.impl.combat;

import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.math.TimerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;

public class AutoGapple extends Module {
    private final NumberSetting useDelay = new NumberSetting("settings.autogapple.usedelay", 0, 0, 2000, 10);
    private final NumberSetting health = new NumberSetting("settings.autogapple.health", 15f, 1f, 36f, 0.5f);
    private final BooleanSetting absorption = new BooleanSetting("settings.autogapple.absorption", false);

    private boolean isActive;
    private final TimerUtils timer = new TimerUtils();

    public AutoGapple() {
        super("AutoGapple", Category.Combat);
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick e) {
        if (fullNullCheck()) return;
        
        if (hasGappleInOffHand()) {
            float currentHealth = mc.player.getHealth() + (absorption.getValue() ? mc.player.getAbsorptionAmount() : 0);
            
            if (currentHealth <= health.getValue() && timer.passed(useDelay.getValue().longValue())) {
                isActive = true;
                mc.options.useKey.setPressed(true);
                timer.reset();
            } else if (isActive && currentHealth > health.getValue()) {
                isActive = false;
                mc.options.useKey.setPressed(false);
            }
        } else if (isActive) {
            isActive = false;
            mc.options.useKey.setPressed(false);
        }
    }

    private boolean hasGappleInOffHand() {
        return !mc.player.getOffHandStack().isEmpty() 
            && (mc.player.getOffHandStack().getItem() == Items.GOLDEN_APPLE 
                || mc.player.getOffHandStack().getItem() == Items.ENCHANTED_GOLDEN_APPLE);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (isActive) {
            isActive = false;
            mc.options.useKey.setPressed(false);
        }
    }
}
