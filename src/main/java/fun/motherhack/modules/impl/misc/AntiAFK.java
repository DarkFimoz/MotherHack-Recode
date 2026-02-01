package fun.motherhack.modules.impl.misc;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.modules.settings.api.Nameable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import fun.motherhack.api.events.impl.EventPlayerTick;

public class AntiAFK extends Module {
    private final EnumSetting<Mode> mode = new EnumSetting<>("Режим", Mode.Simple);
    private final BooleanSetting onlyWhenAfk = new BooleanSetting("Только при AFK", false);
    private final BooleanSetting command = new BooleanSetting("Команда", false);
    private final BooleanSetting move = new BooleanSetting("Движение", false);
    private final BooleanSetting spin = new BooleanSetting("Вращение", false);
    private final NumberSetting rotateSpeed = new NumberSetting("Скорость вращения", 5f, 1f, 7f, 0.1f);
    private final BooleanSetting jump = new BooleanSetting("Прыжки", false);
    private final BooleanSetting swing = new BooleanSetting("Взмах рукой", false);
    private final BooleanSetting alwayssneak = new BooleanSetting("Всегда красться", false);
    private final NumberSetting radius = new NumberSetting("Радиус", 64f, 1f, 128f, 1f);

    private int step;
    private long inactiveTime = System.currentTimeMillis();

    @AllArgsConstructor
    @Getter
    public enum Mode implements Nameable {
        Simple("Простой"),
        Baritone("Baritone");

        private final String name;
    }

    public AntiAFK() {
        super("AntiAFK", Category.Misc);
        getSettings().add(mode);
        getSettings().add(onlyWhenAfk);
        getSettings().add(command);
        getSettings().add(move);
        getSettings().add(spin);
        getSettings().add(rotateSpeed);
        getSettings().add(jump);
        getSettings().add(swing);
        getSettings().add(alwayssneak);
        getSettings().add(radius);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (alwayssneak.getValue() && mc.options != null)
            mc.options.sneakKey.setPressed(true);
        step = 0;
        inactiveTime = System.currentTimeMillis();
    }

    @EventHandler
    public void onTick(EventPlayerTick event) {
        if (fullNullCheck()) return;

        if (mode.getValue() == Mode.Simple ? isActive() : getPlayerSpeed() > 0.07)
            inactiveTime = System.currentTimeMillis();

        if (mode.getValue() == Mode.Simple) {
            if (!isAfk()) return;
            if (move.getValue())
                mc.player.setSprinting(false);

            if (spin.getValue()) {
                double gcdFix = (Math.pow(mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0)) * 1.2;
                float newYaw = mc.player.getYaw() + rotateSpeed.getValue();
                mc.player.setYaw((float) (newYaw - (newYaw - mc.player.getYaw()) % gcdFix));
            }

            if (jump.getValue() && mc.player.isOnGround())
                mc.player.jump();

            if (swing.getValue() && Math.random() < 0.01)
                mc.player.swingHand(mc.player.getActiveHand());

            if (command.getValue() && Math.random() < 0.01 && mc.player.networkHandler != null)
                mc.player.networkHandler.sendChatCommand("qwerty");
        } else {
            if (System.currentTimeMillis() - inactiveTime > 5000) {
                if (step > 3)
                    step = 0;
                float r = radius.getValue();
                if (mc.player.networkHandler != null) {
                    switch (step) {
                        case 0 -> mc.player.networkHandler.sendChatMessage("#goto ~ ~" + (int) r);
                        case 1 -> mc.player.networkHandler.sendChatMessage("#goto ~" + (int) r + " ~");
                        case 2 -> mc.player.networkHandler.sendChatMessage("#goto ~ ~-" + (int) r);
                        case 3 -> mc.player.networkHandler.sendChatMessage("#goto ~-" + (int) r + " ~");
                    }
                }
                step++;
                inactiveTime = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (alwayssneak.getValue() && mc.options != null)
            mc.options.sneakKey.setPressed(false);
        if (mode.getValue() == Mode.Baritone && mc.player != null && mc.player.networkHandler != null)
            mc.player.networkHandler.sendChatMessage("#stop");
    }

    private boolean isAfk() {
        return !onlyWhenAfk.getValue() || (System.currentTimeMillis() - inactiveTime) > 10000;
    }

    private boolean isActive() {
        return mc.options.forwardKey.isPressed() || mc.options.leftKey.isPressed() || 
               mc.options.rightKey.isPressed() || mc.options.backKey.isPressed();
    }

    private double getPlayerSpeed() {
        if (mc.player == null) return 0;
        double deltaX = mc.player.getX() - mc.player.prevX;
        double deltaZ = mc.player.getZ() - mc.player.prevZ;
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }
}
