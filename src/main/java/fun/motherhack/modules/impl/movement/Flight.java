package fun.motherhack.modules.impl.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.utils.math.TimerUtils;
import net.minecraft.util.math.Vec3d;

public class Flight extends Module {
    public Flight() { super("Flight", Category.Movement); }

    public final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.Motion);
    public final NumberSetting xspeed = new NumberSetting("X Speed", 1f, 0.0f, 5f, 0.1f);
    public final NumberSetting yspeed = new NumberSetting("Y Speed", 1f, 0.0f, 5f, 0.1f);

    public enum Mode implements Nameable { Motion, ElytraRWOld, Creative, GrimGlide;
        @Override public String getName() { return name(); }
    }

    private final TimerUtils timerUtil = new TimerUtils();
    private static final int ELYTRA_SWAP_DELAY = 520;
    private static final double MAX_COMP = 20.0;

    @EventHandler
    public void onTick(EventPlayerTick event) {
        if (fullNullCheck()) return;
        if (!toggled) { // безопасный выход при отключении модуля
            if (mc.player.noClip) mc.player.noClip = false;
            return;
        }

        switch (mode.getValue()) {
            case Motion -> motionMode();
            case Creative -> creativeMode();
            case GrimGlide -> grimGlideMode();
            case ElytraRWOld -> elytraRWOldMode();
        }
    }

    // --- Core: правильное преобразование WASD в вектор направления ---
    // Возвращает нормализованный вектор движения в локальных координатах игрока
    private Vec3d getMovementVector() {
        float forward = mc.player.input.movementForward;    // W/S: + forward, - back
        float strafe  = mc.player.input.movementSideways;   // A/D: - left, + right
        float yawDeg = mc.player.getYaw();
        double yawRad = Math.toRadians(yawDeg);

        // Если нет входа — ноль
        if (forward == 0 && strafe == 0) return Vec3d.ZERO;

        // Нормализуем входы для консистентной диагонали
        double len = Math.hypot(forward, strafe);
        double f = forward, s = strafe;
        if (len > 1.0) { f /= len; s /= len; }

        // Forward unit vector (fx,fz) и Right unit vector (rx,rz)
        double fx = -Math.sin(yawRad);
        double fz =  Math.cos(yawRad);
        double rx =  Math.cos(yawRad); // right = rotate forward by -90deg -> (cos, sin)? в нашей системе -> cos, -sin
        double rz =  Math.sin(yawRad);

        // Проверка корректности (важно: сочетание даёт ожидаемые направления)
        // итоговый вектор = forward * forwardVec + strafe * rightVec
        double vx = fx * f + rx * s;
        double vz = fz * f + rz * s;
        return new Vec3d(vx, 0, vz).normalize();
    }

    // --- Motion mode: прямое назначение скорости по направлению ввода ---
    private void motionMode() {
        double vy = 0;
        if (mc.options.jumpKey.isPressed()) vy = yspeed.getValue().doubleValue();
        else if (mc.options.sneakKey.isPressed()) vy = -yspeed.getValue().doubleValue();

        Vec3d mov = getMovementVector();
        double speed = xspeed.getValue().doubleValue();

        double vx = mov.x * speed;
        double vz = mov.z * speed;

        // При отсутствии ввода оставить горизонтальную скорость = 0 (стабильность)
        if (mc.player.input.movementForward == 0 && mc.player.input.movementSideways == 0) {
            vx = 0; vz = 0;
        }

        mc.player.setVelocity(vx, vy, vz);
        mc.player.fallDistance = 0f;
    }

    // --- Creative: плавный, сглаженный полёт, noClip включается только тут ---
    private void creativeMode() {
        double targetVy = 0;
        if (mc.options.jumpKey.isPressed()) targetVy = yspeed.getValue().doubleValue();
        else if (mc.options.sneakKey.isPressed()) targetVy = -yspeed.getValue().doubleValue();

        Vec3d dir = getMovementVector();
        double targetVx = dir.x * xspeed.getValue().doubleValue();
        double targetVz = dir.z * xspeed.getValue().doubleValue();

        // Сглаживание движения к целевым значениям
        Vec3d cur = mc.player.getVelocity();
        double smooth = 0.85;
        double vx = cur.x * smooth + targetVx * (1 - smooth);
        double vy = cur.y * smooth + targetVy * (1 - smooth);
        double vz = cur.z * smooth + targetVz * (1 - smooth);

        vx = clamp(vx, -MAX_COMP, MAX_COMP);
        vy = clamp(vy, -MAX_COMP, MAX_COMP);
        vz = clamp(vz, -MAX_COMP, MAX_COMP);

        mc.player.setVelocity(vx, vy, vz);
        mc.player.fallDistance = 0f;
        mc.player.noClip = true;

        // безопасный лимит скорости
        if (mc.player.getVelocity().length() > 20) mc.player.setVelocity(mc.player.getVelocity().multiply(0.9));
    }

    // --- GrimGlide: надежный elytra обход с контролем направления и лёгким ранджем ---
    private void grimGlideMode() {
        ItemStack chest = mc.player.getInventory().getArmorStack(2);
        if (chest.isOf(Items.ELYTRA) && !mc.player.isGliding() && !mc.player.isOnGround() && mc.player.fallDistance > 1.0f) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.startGliding();
        }

        if (!mc.player.isGliding()) return;

        // Получаем базовую цель по вводу
        Vec3d dir = getMovementVector();
        double forwardFactor = 0.12; // интенсивность управления
        double strafeFactor  = 0.08;

        double yawRad = Math.toRadians(mc.player.getYaw());
        // forward unit as before
        double fx = -Math.sin(yawRad);
        double fz =  Math.cos(yawRad);
        double rx =  Math.cos(yawRad);
        double rz =  Math.sin(yawRad);

        double mvf = mc.player.input.movementForward;
        double mvs = mc.player.input.movementSideways;

        double vx = mc.player.getVelocity().x;
        double vy = mc.player.getVelocity().y;
        double vz = mc.player.getVelocity().z;

        // apply control nudges based on player input (WASD)
        vx = vx * 0.78 + (fx * mvf * forwardFactor + rx * mvs * strafeFactor);
        vz = vz * 0.78 + (fz * mvf * forwardFactor + rz * mvs * strafeFactor);

        if (mc.options.jumpKey.isPressed()) vy = Math.min(vy + 0.04, 0.25);
        else if (mc.options.sneakKey.isPressed()) vy = Math.max(vy - 0.04, -0.4);
        else vy = Math.max(vy - 0.008 - Math.random() * 0.005, -0.25);

        // небольшой jitter для "человеческого" поведения
        double jitter = 0.01 + Math.random() * 0.02;
        vx += (Math.random() * jitter - jitter / 2);
        vy += (Math.random() * jitter / 2 - jitter / 4);
        vz += (Math.random() * jitter - jitter / 2);

        vx = clamp(vx, -6, 6);
        vy = clamp(vy, -1, 1);
        vz = clamp(vz, -6, 6);

        mc.player.setVelocity(vx, vy, vz);
        mc.player.fallDistance = 0f;
    }

    // --- ElytraRWOld: лёгкий автоматический триггер START_FALL_FLYING из хотбара ---
    private void elytraRWOldMode() {
        if (mc.player.isGliding()) { mc.player.fallDistance = 0f; return; }

        // Поиск эльтры в хотбаре
        int slot = -1;
        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getStack(i).isOf(Items.ELYTRA)) { slot = i; break; }
        if (slot == -1) return;

        if (!timerUtil.passed(ELYTRA_SWAP_DELAY)) return;
        timerUtil.reset();

        // Попытка инициировать fall flying — если сервер позволяет, начнём
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        mc.player.startGliding();
    }

    // --- утилиты ---
    private static double clamp(double v, double min, double max) { return v < min ? min : Math.min(v, max); }

    public static boolean fullNullCheck() { return mc == null || mc.player == null || mc.world == null; }

    // Разрешаем явное отключение модуля (используется выше)
    @Override
    public void onDisable() {
        if (mc != null && mc.player != null) {
            mc.player.noClip = false;
            // сбросим скорость, чтобы не оставаться в воздухе неконтролируемо
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
        }
        super.onDisable();
    }
}
