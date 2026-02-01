package fun.motherhack.modules.impl.movement;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.api.Nameable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import fun.motherhack.api.events.impl.EventPlayerTick;

public class WaterSpeed extends Module {
    private final EnumSetting<Mode> mode = new EnumSetting<>("Режим", Mode.DolphinGrace);
    private float acceleration = 0f;

    @AllArgsConstructor
    @Getter
    public enum Mode implements Nameable {
        DolphinGrace("Благодать дельфина"),
        Intave("Intave"),
        CancelResurface("Отмена всплытия"),
        FunTimeNew("FunTime новое");

        private final String name;
    }

    public WaterSpeed() {
        super("WaterSpeed", Category.Movement);
        getSettings().add(mode);
    }

    @EventHandler
    public void onTick(EventPlayerTick event) {
        if (fullNullCheck()) return;

        if (mode.getValue() == Mode.DolphinGrace) {
            if (mc.player.isSwimming())
                mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 2, 2));
            else
                mc.player.removeStatusEffect(StatusEffects.DOLPHINS_GRACE);
        }
    }

    @Override
    public void onDisable() {
        if (fullNullCheck()) return;
        if (mode.getValue() == Mode.DolphinGrace && mc.player != null)
            mc.player.removeStatusEffect(StatusEffects.DOLPHINS_GRACE);
    }
}
