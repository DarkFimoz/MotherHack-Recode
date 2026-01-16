package fun.motherhack.modules.impl.combat;

import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.math.TimerUtils;
import fun.motherhack.utils.network.Server;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

public class AutoClicker extends Module {

    private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.Single);
    private final NumberSetting minCps = new NumberSetting("Min CPS", 15f, 1f, 500f, 1f,
            () -> mode.getValue() != Mode.NoDelay);
    private final NumberSetting maxCps = new NumberSetting("Max CPS", 20f, 1f, 500f, 1f,
            () -> mode.getValue() != Mode.NoDelay);
    private final NumberSetting attacksPerTick = new NumberSetting("Attacks/Tick", 1f, 1f, 100f, 1f,
            () -> mode.getValue() == Mode.Multi || mode.getValue() == Mode.Packet);
    private final NumberSetting noDelayMultiplier = new NumberSetting("Multiplier", 50f, 1f, 500f, 1f,
            () -> mode.getValue() == Mode.NoDelay);
    private final BooleanSetting onlyWeapon = new BooleanSetting("Only Weapon", false);
    private final BooleanSetting requireClick = new BooleanSetting("Require LMB", true);
    private final BooleanSetting swing = new BooleanSetting("Swing", true);
    
    private final TimerUtils timer = new TimerUtils();
    private long currentDelay = 50;

    public AutoClicker() {
        super("AutoClicker", Category.Combat);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        timer.reset();
        currentDelay = getRandomDelay();
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick e) {
        if (fullNullCheck()) return;
        
        // Check if LMB is required and held
        if (requireClick.getValue() && !mc.options.attackKey.isPressed()) return;
        
        // Check weapon
        if (onlyWeapon.getValue() && !isHoldingWeapon()) return;

        if (!(mc.crosshairTarget instanceof EntityHitResult result)) return;
        
        Entity entity = result.getEntity();
        if (!(entity instanceof LivingEntity living)) return;
        if (!Server.isValid(living)) return;

        switch (mode.getValue()) {
            case Single -> {
                if (timer.passed(currentDelay)) {
                    attack(living);
                    timer.reset();
                    currentDelay = getRandomDelay();
                }
            }
            case Multi -> {
                if (timer.passed(currentDelay)) {
                    int attacks = attacksPerTick.getValue().intValue();
                    for (int i = 0; i < attacks; i++) {
                        attack(living);
                    }
                    timer.reset();
                    currentDelay = getRandomDelay();
                }
            }
            case Packet -> {
                // Максимальная скорость - атака каждый тик без задержки
                int attacks = attacksPerTick.getValue().intValue();
                for (int i = 0; i < attacks; i++) {
                    attackPacket(living);
                }
            }
            case NoDelay -> {
                // ПЕРДЁЖ MODE - абсолютно без задержки, максимальный спам
                int multiplier = noDelayMultiplier.getValue().intValue();
                for (int i = 0; i < multiplier; i++) {
                    mc.interactionManager.attackEntity(mc.player, living);
                    if (swing.getValue()) {
                        mc.player.swingHand(Hand.MAIN_HAND);
                    }
                }
            }
        }
    }

    private void attack(LivingEntity entity) {
        mc.interactionManager.attackEntity(mc.player, entity);
        if (swing.getValue()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private void attackPacket(LivingEntity entity) {
        // Прямая атака через interactionManager (самый быстрый способ)
        mc.interactionManager.attackEntity(mc.player, entity);
        if (swing.getValue()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private long getRandomDelay() {
        float min = Math.min(minCps.getValue(), maxCps.getValue());
        float max = Math.max(minCps.getValue(), maxCps.getValue());
        float cps = min + (float) Math.random() * (max - min);
        return (long) (1000f / cps);
    }

    private boolean isHoldingWeapon() {
        var item = mc.player.getMainHandStack().getItem();
        String name = item.toString().toLowerCase();
        return name.contains("sword") || name.contains("axe");
    }

    @Getter
    public enum Mode implements Nameable {
        Single("Single"),      // Обычный режим с CPS
        Multi("Multi"),        // Несколько атак за тик
        Packet("Packet"),      // Максимальная скорость
        NoDelay("NoDelay");    // ПЕРДЁЖ - без задержки вообще

        private final String name;

        Mode(String name) {
            this.name = name;
        }
    }
}