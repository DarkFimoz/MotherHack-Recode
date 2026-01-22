package fun.motherhack.modules.impl.combat;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.*;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.client.Targets;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.combat.IdealHitUtils;
import fun.motherhack.utils.combat.PredictUtils;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.math.TimerUtils;
import fun.motherhack.utils.render.Render3D;
import fun.motherhack.utils.rotations.RotationUtils;
import fun.motherhack.utils.world.InventoryUtils;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.awt.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static net.minecraft.util.math.MathHelper.clamp;
import static net.minecraft.util.math.MathHelper.wrapDegrees;

public class LegitAura extends Module {
    // --- Настройки ---
    private final NumberSetting range = new NumberSetting("Range", 4.0f, 1.0f, 6.0f, 0.1f);
    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls", false);
    private final EnumSetting<Priority> priority = new EnumSetting<>("Priority", Priority.Distance);
    private final BooleanSetting randomize = new BooleanSetting("Randomize", true);
    private final NumberSetting flightSmoothFactor = new NumberSetting("Flight Smooth Factor", 1.5f, 1.0f, 3.0f, 0.1f);
    private final NumberSetting elytraPredictTicks = new NumberSetting("Elytra Predict", 2.0f, 0.0f, 5.0f, 1.0f);
    private final BooleanSetting renderPredictionBox = new BooleanSetting("Render Prediction", true);
    private final NumberSetting predictionBoxAlpha = new NumberSetting("Prediction Alpha", 100f, 0f, 255f, 5f, () -> renderPredictionBox.getValue());

    // --- Состояние ---
    private Vec2f rotate;
    @Getter
    private LivingEntity target;
    private final TimerUtils attackTimer = new TimerUtils();
    private final TimerUtils randomizeTimer = new TimerUtils();
    private final SecureRandom random = new SecureRandom();
    private Vec3d predictedPos = null;

    public LegitAura() {
        super("LegitAura", Category.Combat);
    }

    @EventHandler
    public void onTick(EventRender2D event) {
        if (fullNullCheck()) return;

        if (rotate == null) {
            rotate = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
        }

        findTarget();

        if (target != null) {
            faceTargetSmooth();
            if (shouldAttack()) {
                // attack();
            }
        } else {
            resetRotation();
        }
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        if (target != null && shouldAttack() && attackTimer.passed(500)) {
            if (shouldAttack()) {
                faceTargetSmooth();
                attack();
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck() || target == null || !renderPredictionBox.getValue()) return;

        // Рендерим бокс предикта если летим на элитрах
        if (mc.player.isGliding() && elytraPredictTicks.getValue() > 0 && predictedPos != null) {
            renderPredictionBox(event.getMatrixStack());
        }
    }

    private void renderPredictionBox(MatrixStack matrixStack) {
        if (predictedPos == null) return;

        // Создаем бокс на основе размера цели
        Box predictionBox = target.getBoundingBox().offset(predictedPos.subtract(target.getPos()));

        // Рендерим контур бокса
        int alpha = predictionBoxAlpha.getValue().intValue();
        Render3D.renderBox(matrixStack, predictionBox, new Color(255, 0, 0, alpha));
    }

    private void findTarget() {
        // Используем модуль Targets для валидации целей
        Targets targetsModule = MotherHack.getInstance().getModuleManager().getModule(Targets.class);
        if (targetsModule == null) {
            target = null;
            predictedPos = null;
            return;
        }

        List<LivingEntity> targets = new ArrayList<>();
        ClientWorld world = mc.world;
        if (world == null) return;

        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || living == mc.player) continue;
            if (!targetsModule.isValid(living)) continue;
            if (!isValidTarget(living)) continue;
            targets.add(living);
        }

        if (targets.isEmpty()) {
            target = null;
            predictedPos = null;
            return;
        }

        switch (priority.getValue()) {
            case Distance -> targets.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(mc.player)));
            case Health -> targets.sort(Comparator.comparingDouble(LivingEntity::getHealth));
            case Angle -> targets.sort(Comparator.comparingDouble(e -> {
                Vec3d center = e.getPos().add(0, e.getEyeHeight(e.getPose()) * 0.5, 0);
                float[] needed = RotationUtils.getRotations(center);
                float yawDiff = wrapDegrees(needed[0] - mc.player.getYaw());
                float pitchDiff = wrapDegrees(needed[1] - mc.player.getPitch());
                return Math.abs(yawDiff) + Math.abs(pitchDiff);
            }));
        }

        target = targets.get(0);
        // Обновляем предиктед позицию при смене цели
        predictedPos = getPredictedTargetPos();
    }

    private void shieldBreak(PlayerEntity entity) {
        int slot = InventoryUtils.findBestAxe(0, 8);
        int previousSlot = mc.player.getInventory().selectedSlot;
        if (slot == -1) return;
        InventoryUtils.switchSlot(InventoryUtils.Switch.Silent, slot, previousSlot);
        mc.interactionManager.attackEntity(mc.player, entity);
        InventoryUtils.swing(InventoryUtils.Swing.MainHand);
        InventoryUtils.switchBack(InventoryUtils.Switch.Silent, previousSlot, previousSlot);
    }

    private boolean isValidTarget(LivingEntity e) {
        if (e.isDead() || e.getHealth() <= 0) return false;

        float effectiveRange = getEffectiveRange();
        if (e.distanceTo(mc.player) > effectiveRange) return false;

        if (!throughWalls.getValue() && !canSeeTarget(e)) return false;
        return true;
    }

    private boolean canSeeTarget(LivingEntity target) {
        Vec3d start = mc.player.getEyePos();
        Vec3d end = target.getPos().add(0, target.getEyeHeight(target.getPose()) * MathUtils.randomFloat(0.3f, 0.6f), 0);

        // Используем effective range для проверки дистанции
        if (start.distanceTo(end) > getEffectiveRange()) return false;

        HitResult result = mc.world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        if (result.getType() == HitResult.Type.MISS) return true;
        if (result.getType() == HitResult.Type.ENTITY) {
            return ((EntityHitResult) result).getEntity() == target;
        }
        return false;
    }

    private void faceTargetSmooth() {
        if (mc.player == null || target == null || rotate == null) return;

        Vec3d eyes = mc.player.getEyePos();

        // Получаем позицию цели с учетом предикта для элитр
        Vec3d targetPos = getPredictedTargetPos();

        if (!throughWalls.getValue() && !canSeePoint(eyes, targetPos)) {
            return;
        }

        float[] rotations = RotationUtils.getRotations(targetPos);
        float targetYaw = rotations[0];
        float targetPitch = rotations[1];

        if (randomize.getValue() && randomizeTimer.passed(50)) {
            targetYaw += MathUtils.randomFloat(-5.0f, 5.0f);
            targetPitch += MathUtils.randomFloat(-3.0f, 3.0f);
            randomizeTimer.reset();
        }

        float smoothYaw = smoothRotation(mc.player.getYaw(), targetYaw);
        float smoothPitch = smoothRotation(mc.player.getPitch(), targetPitch);
        smoothPitch = clamp(smoothPitch, -89.0f, 89.0f);

        mc.player.setYaw(smoothYaw);
        mc.player.setPitch(smoothPitch);
        rotate = new Vec2f(smoothYaw, smoothPitch);
    }

    private Vec3d getPredictedTargetPos() {
        Vec3d basePos = target.getPos().add(0, target.getHeight() / 2.0, 0);

        // Если летим на элитрах и включен предикт
        if (mc.player.isGliding() && elytraPredictTicks.getValue() > 0) {
            int predictTicks =  elytraPredictTicks.getValue().intValue();
            return PredictUtils.predict(target, basePos, predictTicks);
        }

        return basePos;
    }

    private float smoothRotation(float current, float target) {
        float diff = wrapDegrees(target - current);

        // Увеличиваем скорость поворота при полете
        float smoothFactor = 0.0472f;
        if (mc.player.isGliding() || !mc.player.isOnGround()) {
            smoothFactor *= flightSmoothFactor.getValue();
        }

        float adjust = diff * smoothFactor;
        return current + adjust;
    }

    private float getEffectiveRange() {
        return mc.player.isGliding() ? range.getValue() * 10.0f : range.getValue();
    }

    private void attack() {
        if (mc.player == null || target == null) return;

        if (attackTimer.passed(500 + random.nextInt(60))) {
            // Проверяем что мы смотрим на цель
            Vec3d eyes = mc.player.getEyePos();
            Vec3d targetPos = getPredictedTargetPos();
            float[] rotations = RotationUtils.getRotations(targetPos);
            
            // Проверяем что ротация близка к цели (в пределах 10 градусов)
            float yawDiff = Math.abs(wrapDegrees(rotations[0] - mc.player.getYaw()));
            float pitchDiff = Math.abs(rotations[1] - mc.player.getPitch());
            
            if (yawDiff > 10f || pitchDiff > 10f) {
                return; // Не атакуем если не смотрим на цель
            }

            if (mc.player.getAttackCooldownProgress(0f) < IdealHitUtils.getAICooldown()) return;
            if (!IdealHitUtils.canCritical(target)) return;
            if (target instanceof PlayerEntity player && player.isBlocking()) shieldBreak(player);
            else mc.interactionManager.attackEntity(mc.player, target);

            mc.player.swingHand(Hand.MAIN_HAND);

            mc.options.sprintKey.setPressed(false);

            attackTimer.reset();
        }
    }

    private void resetRotation() {
        if (rotate == null || mc.player == null) return;

        if (target == null) {
            rotate = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
            predictedPos = null;
            return;
        }

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();
        float yawDiff = wrapDegrees(currentYaw - rotate.x);
        float pitchDiff = wrapDegrees(currentPitch - rotate.y);

        if (Math.abs(yawDiff) < 0.1f && Math.abs(pitchDiff) < 0.1f) {
            rotate = new Vec2f(currentYaw, currentPitch);
            return;
        }

        float returnYaw = currentYaw - yawDiff * 0.05f;
        float returnPitch = clamp(currentPitch - pitchDiff * 0.05f, -89.0f, 89.0f);

        mc.player.setYaw(returnYaw);
        mc.player.setPitch(returnPitch);
        rotate = new Vec2f(returnYaw, returnPitch);
    }

    private boolean canSeePoint(Vec3d start, Vec3d end) {
        HitResult result = mc.world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return result.getType() == HitResult.Type.MISS || result.getPos().squaredDistanceTo(end) < 0.1;
    }

    private boolean shouldAttack() {
        if (target == null || target.isDead() || target.getHealth() <= 0) return false;

        float effectiveRange = getEffectiveRange();
        Vec3d eyes = mc.player.getEyePos();

        // Используем предиктед позицию для проверки атаки
        Vec3d attackPoint = getPredictedTargetPos();

        if (eyes.squaredDistanceTo(attackPoint) > effectiveRange * effectiveRange) return false;

        if (!throughWalls.getValue() && !canSeePoint(eyes, attackPoint)) return false;

        if (mc.player.getAttackCooldownProgress(0f) < IdealHitUtils.getAICooldown()) return false;

        if (!IdealHitUtils.canCritical(target)) return false;

        return true;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        rotate = null;
        target = null;
        predictedPos = null;
        if (mc.player != null) {
            rotate = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        target = null;
        rotate = null;
        predictedPos = null;
    }

    public enum Priority implements Nameable {
        Distance("Distance"),
        Health("Health"),
        Angle("Angle");

        private final String name;

        Priority(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}