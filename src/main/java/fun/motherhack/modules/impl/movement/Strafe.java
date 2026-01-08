package fun.motherhack.modules.impl.movement;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.api.events.impl.rotations.EventMotion;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.combat.Aura;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.movement.MoveUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.MathHelper;

public class Strafe extends Module {

private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.Matrix);
private final NumberSetting speed = new NumberSetting("Speed", 0.42f, 0f, 1f, 0.01f,
        () -> mode.getValue() == Mode.Matrix);

private float lastYaw = 0;

public Strafe() {
    super("Strafe", Category.Movement);
}

@Override
public void onEnable() {
    super.onEnable();
    if (!fullNullCheck()) {
        lastYaw = mc.player.getYaw();
    }
}

@Override
public void onDisable() {
    super.onDisable();
    if (!fullNullCheck()) {
        mc.player.setSprinting(false);
        mc.options.sprintKey.setPressed(false);
    }
}

@EventHandler
public void onMotion(EventMotion e) {
    if (fullNullCheck()) return;

    boolean moving = MoveUtils.isMoving();

    if (mode.getValue() == Mode.Matrix) {
        if (moving) {
            float yaw = getMoveYaw(mc.player.getYaw());
            double motion = speed.getValue().doubleValue() * 1.5;
            setVelocity(motion);
            lastYaw = yaw;
        } else {
            setVelocity(0);
        }
    } else if (mode.getValue() == Mode.Grim) {
        if (moving) {
            float yaw = getMoveYaw(mc.player.getYaw());
            
            Aura aura = MotherHack.getInstance().getModuleManager().getModule(Aura.class);
            if (aura == null || aura.getTarget() == null) {
                e.setYaw(yaw);
                mc.player.setBodyYaw(yaw);
                mc.player.setHeadYaw(yaw);
            }
            lastYaw = yaw;
        }
    }
}

@EventHandler
public void onTick(EventPlayerTick event) {
    if (fullNullCheck()) return;

    boolean moving = MoveUtils.isMoving();

    if (mode.getValue() == Mode.Grim && moving) {
        // Подменяем инпут чтобы игрок всегда шёл "вперёд" по направлению движения
        // Это позволяет спринтить в любую сторону
        mc.player.input.movementForward = 1.0f;
        mc.player.input.movementSideways = 0.0f;

        // Включаем спринт — клиентский флаг и системная клавиша
        mc.player.setSprinting(true);
        mc.options.sprintKey.setPressed(true);
    } else {
        // При выходе из Grim или остановке — сбрасываем спринт и клавишу
        mc.player.setSprinting(false);
        mc.options.sprintKey.setPressed(false);
    }
}

private float getMoveYaw(float yaw) {
    float forward = mc.player.input.movementForward;
    float strafe = mc.player.input.movementSideways;

    if (forward == 0 && strafe == 0) {
        return yaw;
    }

    float moveYaw = yaw;

    if (forward < 0) {
        if (strafe > 0) {
            moveYaw -= 135;
        } else if (strafe < 0) {
            moveYaw += 135;
        } else {
            moveYaw += 180;
        }
    } else if (forward > 0) {
        if (strafe > 0) {
            moveYaw -= 45;
        } else if (strafe < 0) {
            moveYaw += 45;
        }
    } else {
        if (strafe > 0) {
            moveYaw -= 90;
        } else if (strafe < 0) {
            moveYaw += 90;
        }
    }

    return MathHelper.wrapDegrees(moveYaw);
}

private void setVelocity(double speed) {
    if (!MoveUtils.isMoving()) {
        mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
        return;
    }

    float yaw = getMoveYaw(mc.player.getYaw());
    double rad = Math.toRadians(yaw);

    mc.player.setVelocity(
        -Math.sin(rad) * speed,
        mc.player.getVelocity().y,
        Math.cos(rad) * speed
    );
}

public enum Mode implements Nameable {
    Matrix("Matrix"),
    Grim("Grim");

    private final String name;

    Mode(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
}