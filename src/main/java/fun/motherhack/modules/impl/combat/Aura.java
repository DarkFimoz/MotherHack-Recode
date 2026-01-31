package fun.motherhack.modules.impl.combat;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.api.mixins.accessors.IBossBarHud;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.client.Targets;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.math.TimerUtils;
import fun.motherhack.utils.rotations.RotationChanger;
import fun.motherhack.utils.rotations.RotationUtils;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class Aura extends Module {

    // PvP Mode
    public final EnumSetting<PvPMode> pvpMode = new EnumSetting<>("PvP Mode", PvPMode.Mode_1_8);
    
    // Auto Switch by BossBar
    private final BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", false);
    
    // Distance settings
    private final NumberSetting attackRange = new NumberSetting("Attack Range", 3.0f, 1.0f, 6.0f, 0.1f);
    private final NumberSetting rotationRange = new NumberSetting("Rotation Range", 4.5f, 1.0f, 8.0f, 0.1f);
    
    // Reach integration
    private final BooleanSetting useReachModule = new BooleanSetting("Use Reach Module", false);
    
    // 1.8 Mode settings (NoDelay style - very high CPS)
    private final NumberSetting attacksPerTick = new NumberSetting("Attacks/Tick", 50f, 1f, 500f, 1f, 
            () -> pvpMode.getValue() == PvPMode.Mode_1_8);
    private final BooleanSetting antiKick = new BooleanSetting("Anti Kick", true,
            () -> pvpMode.getValue() == PvPMode.Mode_1_8);
    private final NumberSetting antiKickDelay = new NumberSetting("Anti Kick Delay", 50f, 10f, 200f, 10f,
            () -> pvpMode.getValue() == PvPMode.Mode_1_8 && antiKick.getValue());
    
    // 1.12 Mode settings (with delays)
    private final BooleanSetting useCooldown = new BooleanSetting("Use Cooldown", true,
            () -> pvpMode.getValue() == PvPMode.Mode_1_12);
    private final NumberSetting cooldownProgress = new NumberSetting("Cooldown %", 0.9f, 0.5f, 1.0f, 0.05f,
            () -> pvpMode.getValue() == PvPMode.Mode_1_12 && useCooldown.getValue());
    private final NumberSetting minDelay = new NumberSetting("Min Delay", 400f, 50f, 1000f, 10f, 
            () -> pvpMode.getValue() == PvPMode.Mode_1_12 && !useCooldown.getValue());
    private final NumberSetting maxDelay = new NumberSetting("Max Delay", 600f, 50f, 1000f, 10f, 
            () -> pvpMode.getValue() == PvPMode.Mode_1_12 && !useCooldown.getValue());
    
    // Rotation settings
    private final EnumSetting<RotationMode> rotationMode = new EnumSetting<>("Rotation", RotationMode.Smooth);
    private final NumberSetting rotationSpeed = new NumberSetting("Rotation Speed", 80f, 10f, 180f, 5f,
            () -> rotationMode.getValue() == RotationMode.Smooth);
    
    // Sort mode
    private final EnumSetting<SortMode> sortMode = new EnumSetting<>("Sort", SortMode.Distance);
    
    // Sprint settings
    public final EnumSetting<Sprint> sprint = new EnumSetting<>("Sprint", Sprint.None);
    
    // Smart Crit settings
    private final BooleanSetting smartCrit = new BooleanSetting("settings.aura.smartcrit", false,
            () -> pvpMode.getValue() == PvPMode.Mode_1_12);
    private final BooleanSetting smartCritAutoJump = new BooleanSetting("settings.aura.smartcrit.autojump", true, 
            () -> smartCrit.getValue() && pvpMode.getValue() == PvPMode.Mode_1_12);
    private final BooleanSetting smartCritOnlyCrits = new BooleanSetting("settings.aura.smartcrit.onlycrits", false, 
            () -> smartCrit.getValue() && pvpMode.getValue() == PvPMode.Mode_1_12);
    
    // Other settings
    private final BooleanSetting onlyOnGround = new BooleanSetting("Only Ground Crit", false);
    private final BooleanSetting raycast = new BooleanSetting("Raycast", false);
    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls", true);
    
    // ElytraFreezer settings
    private final BooleanSetting elytraFreezer = new BooleanSetting("settings.aura.elytrafreezer", false);
    private final NumberSetting elytraFreezerRadius = new NumberSetting("settings.aura.elytrafreezer.radius", 3.0f, 1.0f, 6.0f, 0.5f,
            () -> elytraFreezer.getValue());

    @Getter
    private LivingEntity target;
    private final TimerUtils attackTimer = new TimerUtils();
    private final TimerUtils antiKickTimer = new TimerUtils();
    private long currentDelay = 0;
    private float lastYaw, lastPitch;
    private RotationChanger rotationChanger;
    private int attackCount = 0;
    private boolean isFrozen = false;

    public Aura() {
        super("Aura", Category.Combat);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        target = null;
        attackTimer.reset();
        antiKickTimer.reset();
        attackCount = 0;
        isFrozen = false;
        currentDelay = getRandomDelay();
        if (!fullNullCheck()) {
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        target = null;
        attackCount = 0;
        isFrozen = false;
        if (rotationChanger != null) {
            MotherHack.getInstance().getRotationManager().removeRotation(rotationChanger);
            rotationChanger = null;
        }
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick e) {
        if (fullNullCheck()) return;

        // Auto switch PvP mode based on bossbar
        if (autoSwitch.getValue()) {
            checkBossBarAndSwitch();
        }

        // Update target
        LivingEntity newTarget = findTarget();
        
        // Держим старую цель если она ещё жива и в пределах rotationRange
        if (newTarget == null && target != null && !target.isRemoved() && target.isAlive()) {
            double distance = mc.player.getEyePos().distanceTo(target.getPos().add(0, target.getHeight() / 2.0, 0));
            if (distance <= rotationRange.getValue()) {
                newTarget = target; // сохраняем старую цель
            }
        }
        
        target = newTarget;
        
        if (target == null) {
            if (rotationChanger != null) {
                MotherHack.getInstance().getRotationManager().removeRotation(rotationChanger);
                rotationChanger = null;
            }
            return;
        }

        // Handle ElytraFreezer
        handleElytraFreezer();
        
        // Handle rotations
        handleRotations();
        
        // Handle Smart Crit
        handleSmartCrit();

        // Check if can attack
        if (canAttack()) {
            attack();
        }
    }
    
    private void handleSmartCrit() {
        if (!smartCrit.getValue() || target == null || pvpMode.getValue() == PvPMode.Mode_1_8) return;
        
        double distance = mc.player.getEyePos().distanceTo(getTargetPosition());
        
        // Auto jump for crits - jump when on ground and target in range
        if (smartCritAutoJump.getValue() && mc.player.isOnGround() && distance <= attackRange.getValue()) {
            mc.player.jump();
        }
    }
    
    private void handleElytraFreezer() {
        if (!elytraFreezer.getValue() || target == null) {
            // Если функция выключена или нет цели, снимаем заморозку
            if (isFrozen) {
                isFrozen = false;
            }
            return;
        }
        
        // Проверяем, раскрыты ли элитры
        boolean isElytraFlying = mc.player.isGliding();
        
        if (!isElytraFlying) {
            // Если элитры не раскрыты, снимаем заморозку
            if (isFrozen) {
                isFrozen = false;
            }
            return;
        }
        
        // Проверяем расстояние до цели
        double distance = mc.player.squaredDistanceTo(target);
        double radiusSquared = elytraFreezerRadius.getValue() * elytraFreezerRadius.getValue();
        
        if (distance <= radiusSquared) {
            // Цель в радиусе - замораживаем
            if (!isFrozen) {
                isFrozen = true;
            }
            mc.player.setVelocity(0.0, 0.0, 0.0);
        } else {
            // Цель вне радиуса - снимаем заморозку
            if (isFrozen) {
                isFrozen = false;
            }
        }
    }
    
    private boolean canCrit() {
        // Крит только когда падаешь (отрицательная вертикальная скорость)
        return mc.player.getVelocity().y < 0 
                && !mc.player.isOnGround()
                && !mc.player.isClimbing()
                && !mc.player.isTouchingWater()
                && !mc.player.isInLava()
                && !mc.player.hasVehicle()
                && !mc.player.getAbilities().flying;
    }

    private void handleRotations() {
        if (target == null) return;

        Vec3d targetPos = getTargetPosition();
        float[] rotations = RotationUtils.getRotations(targetPos);
        
        if (rotationMode.getValue() == RotationMode.Smooth) {
            rotations = smoothRotation(rotations);
        }
        
        rotations = RotationUtils.correctRotation(rotations);
        lastYaw = rotations[0];
        lastPitch = rotations[1];

        final float[] finalRotations = rotations;
        
        if (rotationChanger != null) {
            MotherHack.getInstance().getRotationManager().removeRotation(rotationChanger);
        }
        
        rotationChanger = new RotationChanger(
                100,
                () -> new Float[]{finalRotations[0], finalRotations[1]},
                () -> target == null || !isToggled()
        );
        
        MotherHack.getInstance().getRotationManager().addRotation(rotationChanger);
    }

    private float[] smoothRotation(float[] targetRotations) {
        float speed = rotationSpeed.getValue() / 100f;
        
        float deltaYaw = MathHelper.wrapDegrees(targetRotations[0] - lastYaw);
        float deltaPitch = targetRotations[1] - lastPitch;
        
        float newYaw = lastYaw + deltaYaw * speed;
        float newPitch = lastPitch + deltaPitch * speed;
        
        newPitch = MathHelper.clamp(newPitch, -90f, 90f);
        
        return new float[]{newYaw, newPitch};
    }

    private Vec3d getTargetPosition() {
        // Get center of target's hitbox
        return target.getPos().add(0, target.getHeight() / 2.0, 0);
    }

    private boolean canAttack() {
        if (target == null) return false;
        
        double distance = mc.player.getEyePos().distanceTo(getTargetPosition());
        
        // Use Reach module if enabled
        float effectiveRange = attackRange.getValue();
        if (useReachModule.getValue()) {
            Reach reach = MotherHack.getInstance().getModuleManager().getModule(Reach.class);
            if (reach != null && reach.isToggled()) {
                effectiveRange = reach.getReachDistance();
            }
        }
        
        if (distance > effectiveRange) return false;
        
        // Check raycast if enabled
        if (raycast.getValue() && !throughWalls.getValue()) {
            if (!canSeeTarget()) return false;
        }
        
        // Check ground crit
        if (onlyOnGround.getValue() && !mc.player.isOnGround()) return false;
        
        // Smart Crit - only attack when can crit (only for 1.12 mode)
        if (smartCrit.getValue() && smartCritOnlyCrits.getValue() && pvpMode.getValue() == PvPMode.Mode_1_12) {
            if (!canCrit()) return false;
        }
        
        // Check attack timing based on PvP mode
        switch (pvpMode.getValue()) {
            case Mode_1_8 -> {
                return true; // NoDelay - always can attack
            }
            case Mode_1_12 -> {
                if (useCooldown.getValue()) {
                    // Используем ванильный кулдаун атаки
                    return mc.player.getAttackCooldownProgress(0.5f) >= cooldownProgress.getValue();
                } else {
                    return attackTimer.passed(currentDelay);
                }
            }
        }
        
        return false;
    }

    private void attack() {
        // Handle sprint
        handleSprint();
        
        // Attack based on mode
        if (pvpMode.getValue() == PvPMode.Mode_1_8) {
            // NoDelay style - multiple attacks per tick with anti-kick
            int attacks = attacksPerTick.getValue().intValue();
            
            // Anti-kick: добавляем паузу каждые N атак
            if (antiKick.getValue()) {
                if (antiKickTimer.passed(antiKickDelay.getValue().longValue())) {
                    // Пауза для предотвращения кика
                    antiKickTimer.reset();
                    attackCount = 0;
                    return;
                }
            }
            
            for (int i = 0; i < attacks; i++) {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
                attackCount++;
            }
        } else {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.player.resetLastAttackedTicks(); // Сбрасываем кулдаун после атаки
        }
        
        // Reset timer and get new delay
        attackTimer.reset();
        currentDelay = getRandomDelay();
    }

    private void handleSprint() {
        switch (sprint.getValue()) {
            case Legit -> {
                if (mc.player.isSprinting() && !mc.player.isOnGround()) {
                    mc.player.setSprinting(false);
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, 
                            ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                }
            }
            case Packet -> {
                if (mc.player.isSprinting()) {
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, 
                            ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, 
                            ClientCommandC2SPacket.Mode.START_SPRINTING));
                }
            }
            case None -> {}
        }
    }

    private long getRandomDelay() {
        switch (pvpMode.getValue()) {
            case Mode_1_8 -> {
                return 0; // NoDelay
            }
            case Mode_1_12 -> {
                return (long) (minDelay.getValue() + Math.random() * (maxDelay.getValue() - minDelay.getValue()));
            }
        }
        return 50;
    }

    private boolean canSeeTarget() {
        return mc.player.canSee(target);
    }

    private void checkBossBarAndSwitch() {
        Map<java.util.UUID, ClientBossBar> bossBars = ((IBossBarHud) mc.inGameHud.getBossBarHud()).getBossBars();
        
        for (ClientBossBar bossBar : bossBars.values()) {
            String name = bossBar.getName().getString().toLowerCase();
            
            if (name.contains("1.8")) {
                if (pvpMode.getValue() != PvPMode.Mode_1_8) {
                    pvpMode.setValue(PvPMode.Mode_1_8);
                    currentDelay = getRandomDelay();
                }
                return;
            }
            
            if (name.contains("1.12")) {
                if (pvpMode.getValue() != PvPMode.Mode_1_12) {
                    pvpMode.setValue(PvPMode.Mode_1_12);
                    currentDelay = getRandomDelay();
                }
                return;
            }
        }
    }

    private LivingEntity findTarget() {
        Targets targetsModule = MotherHack.getInstance().getModuleManager().getModule(Targets.class);
        if (targetsModule == null) return null;

        List<LivingEntity> targets = new ArrayList<>();
        
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!targetsModule.isValid(living)) continue;
            
            double distance = mc.player.getEyePos().distanceTo(living.getPos().add(0, living.getHeight() / 2.0, 0));
            if (distance > rotationRange.getValue()) continue;
            
            if (!throughWalls.getValue() && !mc.player.canSee(living)) continue;
            
            targets.add(living);
        }

        if (targets.isEmpty()) return null;

        // Sort targets
        switch (sortMode.getValue()) {
            case Distance -> targets.sort(Comparator.comparingDouble(e -> 
                    mc.player.squaredDistanceTo(e)));
            case Health -> targets.sort(Comparator.comparingDouble(LivingEntity::getHealth));
            case Angle -> targets.sort(Comparator.comparingDouble(e -> {
                float[] rotations = RotationUtils.getRotations(e.getPos().add(0, e.getHeight() / 2.0, 0));
                double yawDiff = Math.abs(MathHelper.wrapDegrees(rotations[0] - mc.player.getYaw()));
                double pitchDiff = Math.abs(rotations[1] - mc.player.getPitch());
                return yawDiff + pitchDiff;
            }));
        }

        return targets.get(0);
    }

    @Getter
    public enum PvPMode implements Nameable {
        Mode_1_8("1.8 (NoDelay)"),
        Mode_1_12("1.12 (Delay)");

        private final String name;

        PvPMode(String name) {
            this.name = name;
        }
    }

    @Getter
    public enum RotationMode implements Nameable {
        Instant("Instant"),
        Smooth("Smooth");

        private final String name;

        RotationMode(String name) {
            this.name = name;
        }
    }

    @Getter
    public enum SortMode implements Nameable {
        Distance("Distance"),
        Health("Health"),
        Angle("Angle");

        private final String name;

        SortMode(String name) {
            this.name = name;
        }
    }

    @Getter
    public enum Sprint implements Nameable {
        None("None"),
        Legit("Legit"),
        Packet("Packet");

        private final String name;

        Sprint(String name) {
            this.name = name;
        }
    }
}
