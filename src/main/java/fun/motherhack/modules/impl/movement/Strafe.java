package fun.motherhack.modules.impl.movement;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.utils.rotations.RotationChanger;
import lombok.AllArgsConstructor;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.api.events.impl.EventKeyboardInput;

public class Strafe extends Module {
    private final EnumSetting<Mode> mode = new EnumSetting<>("mode", Mode.Legit);
    private final EnumSetting<Boost> boost = new EnumSetting<>("Буст", Boost.None, () -> mode.getValue() == Mode.Rage);
    private final NumberSetting setSpeed = new NumberSetting("Скорость", 1.3f, 0.0f, 2f, 0.1f, () -> mode.getValue() == Mode.Rage);
    private final NumberSetting velReduction = new NumberSetting("Редукция", 6.0f, 0.1f, 10f, 0.1f, () -> mode.getValue() == Mode.Rage);
    private final NumberSetting maxVelocitySpeed = new NumberSetting("Макс скорость", 0.8f, 0.1f, 2f, 0.1f, () -> mode.getValue() == Mode.Rage);

    public static double oldSpeed = 0;
    public static double contextFriction = 0.91;
    public static boolean needSwap = false;
    public static boolean needSprintState = false;
    public static boolean disabled = false;
    static long disableTime = 0;
    public static int noSlowTicks = 0;

    private RotationChanger rotationChanger;

    @AllArgsConstructor
    @Getter
    public enum Mode implements Nameable {
        Legit("Легит"),
        Rage("Рейдж");

        private final String name;
    }

    @AllArgsConstructor
    @Getter
    public enum Boost implements Nameable {
        None("Нет"),
        Elytra("Элитра"),
        Damage("Урон");

        private final String name;
    }

    public Strafe() {
        super("Strafe", Category.Movement);
        getSettings().add(mode);
        getSettings().add(boost);
        getSettings().add(setSpeed);
        getSettings().add(velReduction);
        getSettings().add(maxVelocitySpeed);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        oldSpeed = 0.0;
        
        if (mode.getValue() == Mode.Legit) {
            rotationChanger = new RotationChanger(
                0,
                () -> new Float[]{mc.player.getYaw() + calculateYawOffset(), mc.player.getPitch()},
                () -> false
            );
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        
        if (mode.getValue() == Mode.Legit && !fullNullCheck() && rotationChanger != null) {
            MotherHack.getInstance().getRotationManager().removeRotation(rotationChanger);
        }
    }

    public boolean canStrafe() {
        if (mc.player.isSneaking()) return false;
        if (mc.player.isInLava()) return false;
        if (mc.player.isSubmergedInWater()) return false;
        return !mc.player.getAbilities().flying;
    }

    public Box getBoundingBox() {
        return new Box(mc.player.getX() - 0.1, mc.player.getY(), mc.player.getZ() - 0.1,
                mc.player.getX() + 0.1, mc.player.getY() + 1, mc.player.getZ() + 0.1);
    }

    public double calculateSpeed() {
        float speedAttributes = getAIMoveSpeed();
        final float frictionFactor = mc.world.getBlockState(new BlockPos.Mutable().set(mc.player.getX(),
                getBoundingBox().getMin(Direction.Axis.Y), mc.player.getZ())).getBlock().getSlipperiness() * 0.91F;
        float n6 = mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST) && mc.player.isUsingItem() ? 0.88f : 0.91F;
        if (mc.player.isOnGround()) n6 = frictionFactor;
        float n7 = (float) (0.1631f / Math.pow(n6, 3.0f));
        float n8;
        if (mc.player.isOnGround()) {
            n8 = speedAttributes * n7;
            disabled = false;
        } else {
            n8 = 0.0255f;
        }
        double max2 = oldSpeed + n8;
        contextFriction = n6;
        if (!mc.player.isOnGround()) {
            needSprintState = !mc.player.isSprinting();
            needSwap = true;
        } else {
            needSprintState = false;
        }
        return max2;
    }

    public float getAIMoveSpeed() {
        boolean prevSprinting = mc.player.isSprinting();
        mc.player.setSprinting(false);
        float speed = mc.player.getMovementSpeed() * 1.3f;
        mc.player.setSprinting(prevSprinting);
        return speed;
    }

    @EventHandler
    public void onTick(EventPlayerTick event) {
        if (fullNullCheck()) return;
        
        if (mode.getValue() == Mode.Rage) {
            handleRageMode();
        }
    }

    @EventHandler
    public void onKeyboardInput(EventKeyboardInput event) {
        if (fullNullCheck()) return;
        
        if (mode.getValue() == Mode.Legit) {
            handleLegitMode(event);
        }
    }

    private void handleRageMode() {
        if (!canStrafe()) {
            oldSpeed = 0;
            return;
        }

        if (isMoving()) {
            double speed = calculateSpeed();
            double[] motion = forward(speed);
            mc.player.setVelocity(motion[0], mc.player.getVelocity().y, motion[1]);
            oldSpeed = speed * contextFriction;
        } else {
            oldSpeed = 0;
        }
    }

    private void handleLegitMode(EventKeyboardInput event) {
        MotherHack.getInstance().getRotationManager().addRotation(rotationChanger);

        boolean w = mc.options.forwardKey.isPressed();
        boolean s = mc.options.backKey.isPressed();
        boolean a = mc.options.leftKey.isPressed();
        boolean d = mc.options.rightKey.isPressed();

        if (w && s) {
            w = false;
            s = false;
        }
        if (a && d) {
            a = false;
            d = false;
        }

        event.setMovementSideways(0);
        event.setMovementForward(w || s || a || d ? 1.0f : 0);
    }

    private float calculateYawOffset() {
        boolean w = mc.options.forwardKey.isPressed();
        boolean s = mc.options.backKey.isPressed();
        boolean a = mc.options.leftKey.isPressed();
        boolean d = mc.options.rightKey.isPressed();

        if (w && s) {
            w = false;
            s = false;
        }
        if (a && d) {
            a = false;
            d = false;
        }

        if (w) {
            if (a) return -45f;
            if (d) return 45f;
            return 0f;
        }
        if (s) {
            if (a) return -135f;
            if (d) return 135f;
            return 180f;
        }
        if (a) return -90f;
        if (d) return 90f;
        return 0f;
    }

    private boolean isMoving() {
        return mc.options.forwardKey.isPressed() || mc.options.leftKey.isPressed() ||
                mc.options.rightKey.isPressed() || mc.options.backKey.isPressed();
    }

    private double[] forward(double speed) {
        float forward = 0;
        float strafe = 0;
        float yaw = mc.player.getYaw();

        if (mc.options.forwardKey.isPressed()) forward++;
        if (mc.options.backKey.isPressed()) forward--;
        if (mc.options.leftKey.isPressed()) strafe++;
        if (mc.options.rightKey.isPressed()) strafe--;

        if (forward == 0 && strafe == 0) {
            return new double[]{0, 0};
        }

        if (forward != 0 && strafe != 0) {
            forward *= Math.sin(Math.toRadians(45));
            strafe *= Math.cos(Math.toRadians(45));
        }

        double x = forward * speed * -Math.sin(Math.toRadians(yaw)) + strafe * speed * Math.cos(Math.toRadians(yaw));
        double z = forward * speed * Math.cos(Math.toRadians(yaw)) - strafe * speed * -Math.sin(Math.toRadians(yaw));

        return new double[]{x, z};
    }
}
