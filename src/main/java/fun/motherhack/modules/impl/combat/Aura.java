package fun.motherhack.modules.impl.combat;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.movement.ElytraForward;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.*;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.animations.infinity.InfinityAnimation;
import fun.motherhack.utils.animations.infinity.RotationAnimation;
import fun.motherhack.utils.combat.IdealHitUtils;
import fun.motherhack.utils.combat.PredictUtils;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.math.TimerUtils;
import fun.motherhack.utils.network.NetworkUtils;
import fun.motherhack.utils.network.Server;
import fun.motherhack.utils.rotations.RotationChanger;
import fun.motherhack.utils.rotations.RotationUtils;
import fun.motherhack.utils.world.InventoryUtils;
import fun.motherhack.utils.world.MultipointUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class Aura extends Module {

    // === TARGETING ===
    private final EnumSetting<Priority> priority = new EnumSetting<>("settings.aura.priority", Priority.Distance);
    private final NumberSetting range = new NumberSetting("settings.aura.range", 4.2f, 1f, 8f, 0.05f);
    private final NumberSetting wallRange = new NumberSetting("settings.aura.wallrange", 3f, 0f, 8f, 0.05f);
    private final NumberSetting scanExtraRange = new NumberSetting("settings.aura.scanextrarange", 3f, 0f, 7f, 0.1f);
    private final BooleanSetting throughWalls = new BooleanSetting("settings.aura.throughwalls", true);
    private final EnumSetting<RaycastMode> raycast = new EnumSetting<>("settings.aura.raycast", RaycastMode.TRACE_ALL);
    private final NumberSetting maxTargets = new NumberSetting("settings.aura.maxtargets", 1f, 1f, 10f, 1f);
    private final NumberSetting ticksExisted = new NumberSetting("settings.aura.ticksexisted", 0f, 0f, 100f, 1f);
    
    // === TIMING ===
    private final EnumSetting<PvPMode> pvpMode = new EnumSetting<>("settings.aura.pvpmode", PvPMode.V1_12);
    private final BooleanSetting autoSwitchPvpMode = new BooleanSetting("settings.aura.autoswitchpvpmode", false);
    private final NumberSetting minCps = new NumberSetting("settings.aura.mincps", 8f, 1f, 20f, 1f, () -> pvpMode.getValue() == PvPMode.V1_8 || autoSwitchPvpMode.getValue());
    private final NumberSetting maxCps = new NumberSetting("settings.aura.maxcps", 12f, 1f, 20f, 1f, () -> pvpMode.getValue() == PvPMode.V1_8 || autoSwitchPvpMode.getValue());
    private final NumberSetting hurtTime = new NumberSetting("settings.aura.hurttime", 10f, 0f, 10f, 1f);
    private final NumberSetting failRate = new NumberSetting("settings.aura.failrate", 0f, 0f, 100f, 1f);
    private final BooleanSetting ignoreShieldBreakCooldown = new BooleanSetting("settings.aura.ignoreshieldbreakcooldown", true);
    
    // === ROTATION ===
    private final EnumSetting<Rotate> rotate = new EnumSetting<>("settings.rotate", Rotate.Normal);
    private final EnumSetting<RotationTiming> rotationTiming = new EnumSetting<>("settings.aura.rotationtiming", RotationTiming.NORMAL);
    private final NumberSetting turnSpeed = new NumberSetting("settings.aura.turnspeed", 180f, 1f, 180f, 1f);
    private final NumberSetting predict = new NumberSetting("settings.aura.predict", 1f, 0f, 5f, 0.1f);
    private final BooleanSetting randomize = new BooleanSetting("settings.aura.randomize", true);
    
    // === COMBAT ===
    private final EnumSetting<InventoryUtils.Swing> swing = new EnumSetting<>("settings.swing", InventoryUtils.Swing.MainHand);
    public final EnumSetting<Sprint> sprint = new EnumSetting<>("settings.aura.sprintreset", Sprint.Legit);
    private final BooleanSetting keepSprint = new BooleanSetting("settings.aura.keepsprint", true);
    private final BooleanSetting unpressShield = new BooleanSetting("settings.aura.unpressshield", false);
    private final BooleanSetting breakShield = new BooleanSetting("settings.aura.breakshield", true);
    private final BooleanSetting autoJump = new BooleanSetting("settings.aura.autojump", false);
    private final BooleanSetting smartCrit = new BooleanSetting("settings.aura.smartcrit", true);
    private final BooleanSetting onlyWeapon = new BooleanSetting("settings.aura.onlyweapon", false);
    
    // === BYPASS (LiquidBounce) ===
    private final BooleanSetting ignoreOpenInventory = new BooleanSetting("settings.aura.ignoreopeninventory", true);
    private final BooleanSetting simulateInventoryClosing = new BooleanSetting("settings.aura.simulateinventoryclosing", true);
    private final BooleanSetting failSwing = new BooleanSetting("settings.aura.failswing", false);
    
    // === AUTOBLOCK ===
    private final BooleanSetting autoBlock = new BooleanSetting("settings.aura.autoblock", false);
    private final EnumSetting<AutoBlockMode> autoBlockMode = new EnumSetting<>("settings.aura.autoblockmode", AutoBlockMode.INTERACT, () -> autoBlock.getValue());
    private final NumberSetting autoBlockTickOn = new NumberSetting("settings.aura.autoblocktick", 1f, 0f, 5f, 1f, () -> autoBlock.getValue());
    
    // === RESTORE HITS ===
    private final BooleanSetting restoreHits = new BooleanSetting("settings.aura.restorehits", true);
    private final NumberSetting restoreAttempts = new NumberSetting("settings.aura.restoreattempts", 3f, 1f, 10f, 1f, () -> restoreHits.getValue());

    public Aura() {
        super("Aura", Category.Combat);
    }

    @Getter private LivingEntity target;
    @Getter private List<LivingEntity> targets = new ArrayList<>();
    @Getter private float[] currentRotations = new float[2], targetRotations = new float[2];
    private final TimerUtils backTimer = new TimerUtils(), attackTimer = new TimerUtils();
    private final Queue<AttackData> pendingAttacks = new ConcurrentLinkedQueue<>();
    private final RotationAnimation shakingAnimation = new RotationAnimation(Easing.LINEAR, Easing.LINEAR);
    private final InfinityAnimation pitchAnimationInf = new InfinityAnimation(Easing.LINEAR);
    private final Animation yawAnimation = new Animation(300, 1, false, Easing.BOTH_CIRC);
    private final Animation pitchAnimation = new Animation(300, 1, false, Easing.BOTH_CIRC);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final RotationChanger changer = new RotationChanger(
            10000,
            () -> new Float[]{currentRotations[0], currentRotations[1]},
            () -> fullNullCheck() || target == null
    );
    
    // Timing
    private long lastAttackTime = 0;
    private long nextAttackDelay = 0;
    private int clicksThisTick = 0;
    private float currentScanExtraRange = 0;
    
    // AutoBlock state
    private boolean isBlocking = false;

    @EventHandler
    public void onPlayerTick(EventPlayerTick e) {
        if (fullNullCheck()) return;
        
        // Автосвитч PvP режима по боссбару
        if (autoSwitchPvpMode.getValue()) {
            String bossBarText = Server.getBossBarText().toLowerCase();
            if (bossBarText.contains("1.8")) {
                pvpMode.setValue(PvPMode.V1_8);
            } else if (bossBarText.contains("1.12")) {
                pvpMode.setValue(PvPMode.V1_12);
            }
        }
        
        // Inventory check (LiquidBounce bypass)
        boolean isInInventory = mc.currentScreen instanceof HandledScreen;
        if (isInInventory && !ignoreOpenInventory.getValue()) {
            target = null;
            targets.clear();
            stopBlocking();
            return;
        }
        
        // Проверка на оружие
        if (onlyWeapon.getValue() && !isWeapon()) {
            target = null;
            targets.clear();
            stopBlocking();
            return;
        }

        // Update scan extra range randomly
        if (currentScanExtraRange == 0) {
            currentScanExtraRange = MathUtils.randomFloat(2f, scanExtraRange.getValue());
        }

        executor.submit(() -> {
            targets = findTargets();
            target = targets.isEmpty() ? null : targets.get(0);
        });

        if (target != null || !backTimer.passed(250)) {
            if (target != null) {
                // Предсказание позиции цели
                Vec3d predictedPos = getPredictedPosition(target);
                targetRotations = RotationUtils.getRotations(predictedPos);
                
                // Рандомизация ротаций
                if (randomize.getValue()) {
                    targetRotations[0] += MathUtils.randomFloat(-2f, 2f);
                    targetRotations[1] += MathUtils.randomFloat(-2f, 2f);
                }
            }
            
            handleRotations();
            
            if (target != null) {
                // Rotation timing - SNAP or ON_TICK
                if (rotationTiming.getValue() == RotationTiming.SNAP) {
                    if (!willClickSoon()) {
                        // Don't rotate yet if not clicking soon
                    } else {
                        applyRotations();
                    }
                } else {
                    applyRotations();
                }
                
                // Check facing
                boolean isFacingEnemy = isFacingEnemy(target, currentRotations, range.getValue(), wallRange.getValue());
                
                if (!isFacingEnemy) {
                    // AutoBlock on scan range
                    if (autoBlock.getValue() && getDistanceTo(target) <= range.getValue() + currentScanExtraRange) {
                        startBlocking();
                    } else {
                        stopBlocking();
                    }
                    
                    // FailSwing when not facing
                    if (failSwing.getValue()) {
                        doFailSwing();
                    }
                    return;
                }
                
                // Attack
                clicksThisTick = 0;
                for (LivingEntity t : targets) {
                    if (clicksThisTick >= maxTargets.getValue().intValue()) break;
                    
                    // Raycast check
                    LivingEntity actualTarget = getRaycastTarget(t);
                    if (actualTarget == null) continue;
                    
                    if (shouldAttack(actualTarget)) {
                        attack(actualTarget, isInInventory);
                        clicksThisTick++;
                    }
                }
                
                // AutoJump с SmartCrit
                if (autoJump.getValue() && mc.player.isOnGround()) {
                    if (!smartCrit.getValue() || (target.isOnGround() && mc.player.getAttackCooldownProgress(0f) > 0.9f)) {
                        mc.player.jump();
                    }
                }
            }
            
            // Восстановление отмененных ударов
            if (restoreHits.getValue()) {
                processRestoreQueue();
            }
        } else {
            stopBlocking();
            if (target == null) backTimer.reset();
        }
    }

    private void applyRotations() {
        if (rotate.getValue() == Rotate.Packet) {
            MotherHack.getInstance().getRotationManager().addPacketRotation(currentRotations);
        } else {
            MotherHack.getInstance().getRotationManager().addRotation(changer);
        }
    }
    
    private void handleRotations() {
        if (target == null) return;
        
        if (mc.player.isGliding()) {
            Vec3d predictedPos = getPredictedPosition(target);
            if (MotherHack.getInstance().getModuleManager().getModule(ElytraForward.class).isToggled()) {
                int predictTicks = MotherHack.getInstance().getModuleManager().getModule(ElytraForward.class).forward.getValue().intValue();
                currentRotations = RotationUtils.getRotations(PredictUtils.predict(target, predictedPos, predictTicks));
            } else {
                currentRotations = RotationUtils.getRotations(predictedPos);
            }
            return;
        }
        
        if (rotate.getValue() == Rotate.FuntimeSnap) {
            handleFuntimeSnapRotation();
        } else if (rotate.getValue() == Rotate.Smoothness) {
            currentRotations = smoothRotation(currentRotations, targetRotations, turnSpeed.getValue());
            currentRotations[0] += shakingAnimation.animateYaw(MathUtils.randomFloat(-8, 8), 100);
            currentRotations[1] += shakingAnimation.animatePitch(MathUtils.randomFloat(-15, -15), 100);
        } else if (rotate.getValue() == Rotate.Normal) {
            currentRotations = smoothRotation(currentRotations, targetRotations, turnSpeed.getValue());
        } else {
            currentRotations = targetRotations;
        }
    }
    
    private void handleFuntimeSnapRotation() {
        yawAnimation.setDuration(700);
        pitchAnimation.setDuration(500);
        yawAnimation.update();
        pitchAnimation.update();
        float[] rotations = RotationUtils.getRotations(target.getPos().add(0, target.getHeight() / 2, 0));
        rotations[0] = (((((rotations[0] - mc.player.getYaw()) % 360) + 540) % 360) - 180);

        if ((!(IdealHitUtils.findFall(0) && !mc.player.isOnGround() || IdealHitUtils.canCritical(target))
                || (isWeapon() && !attackTimer.passed(300)))) {
            rotations[0] = yawAnimation.getValue() * 40 - 20;
            rotations[1] += pitchAnimation.getValue() * 15 - 7;
        } else {
            rotations[0] += MathUtils.randomInt(5, 10);
            rotations[1] += MathUtils.randomInt(5, 30);
        }

        currentRotations[0] = mc.player.getYaw() + MathUtils.getStep(currentRotations[0] - mc.player.getYaw(), rotations[0], MathUtils.randomInt(80, 85));
        currentRotations[1] = pitchAnimationInf.animate(rotations[1], MathUtils.randomInt(100, 150));
        currentRotations[1] = MathHelper.clamp(currentRotations[1], -90, 90);
        currentRotations = RotationUtils.correctRotation(currentRotations);
    }

    private Vec3d getPredictedPosition(LivingEntity entity) {
        Vec3d basePos = entity.getPos().add(0, entity.getHeight() / 2, 0);
        if (predict.getValue() <= 0) return basePos;
        
        Vec3d velocity = new Vec3d(
            entity.getX() - entity.prevX,
            entity.getY() - entity.prevY,
            entity.getZ() - entity.prevZ
        );
        
        return basePos.add(velocity.multiply(predict.getValue()));
    }
    
    private float[] smoothRotation(float[] current, float[] target, float speed) {
        float[] result = new float[2];
        float yawDiff = MathHelper.wrapDegrees(target[0] - current[0]);
        float pitchDiff = MathHelper.wrapDegrees(target[1] - current[1]);
        
        result[0] = current[0] + Math.signum(yawDiff) * Math.min(Math.abs(yawDiff), speed);
        result[1] = MathHelper.clamp(current[1] + Math.signum(pitchDiff) * Math.min(Math.abs(pitchDiff), speed), -90, 90);
        
        return result;
    }
    
    private boolean willClickSoon() {
        if (pvpMode.getValue() == PvPMode.V1_8) {
            return System.currentTimeMillis() - lastAttackTime >= nextAttackDelay - 50;
        }
        return mc.player.getAttackCooldownProgress(0f) > 0.9f;
    }
    
    // === RAYCAST (LiquidBounce) ===
    private LivingEntity getRaycastTarget(LivingEntity originalTarget) {
        if (raycast.getValue() == RaycastMode.TRACE_NONE) {
            return originalTarget;
        }
        
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = getVectorForRotation(currentRotations[1], currentRotations[0]);
        Vec3d endPos = eyePos.add(lookVec.multiply(range.getValue()));
        
        Entity hitEntity = null;
        double closestDistance = range.getValue();
        
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity == mc.player) continue;
            
            Box box = entity.getBoundingBox().expand(0.1);
            Optional<Vec3d> hitResult = box.raycast(eyePos, endPos);
            
            if (hitResult.isPresent()) {
                double distance = eyePos.distanceTo(hitResult.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    hitEntity = entity;
                }
            }
        }
        
        if (hitEntity instanceof LivingEntity living) {
            if (raycast.getValue() == RaycastMode.TRACE_ONLYENEMY) {
                return Server.isValid(living) ? living : null;
            }
            return living;
        }
        
        return originalTarget;
    }
    
    private Vec3d getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3d(f1 * f2, f3, f * f2);
    }

    // === FACING CHECK (LiquidBounce) ===
    private boolean isFacingEnemy(LivingEntity entity, float[] rotation, float attackRange, float wallsRange) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = getVectorForRotation(rotation[1], rotation[0]);
        Vec3d endPos = eyePos.add(lookVec.multiply(attackRange));
        
        Box box = entity.getBoundingBox().expand(0.1);
        Optional<Vec3d> hitResult = box.raycast(eyePos, endPos);
        
        if (hitResult.isPresent()) {
            double distance = eyePos.distanceTo(hitResult.get());
            boolean canSee = mc.player.canSee(entity);
            return canSee ? distance <= attackRange : distance <= wallsRange;
        }
        
        return false;
    }
    
    private double getDistanceTo(LivingEntity entity) {
        return mc.player.getEyePos().distanceTo(entity.getPos().add(0, entity.getHeight() / 2, 0));
    }

    private List<LivingEntity> findTargets() {
        List<LivingEntity> foundTargets = new ArrayList<>();
        float maxRange = range.getValue() + currentScanExtraRange;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!isValidTarget(living, maxRange)) continue;
            foundTargets.add(living);
        }

        if (foundTargets.isEmpty() || !isToggled()) return Collections.emptyList();

        switch (priority.getValue()) {
            case Distance -> foundTargets.sort(Comparator.comparingDouble(this::getDistanceTo));
            case Health -> foundTargets.sort(Comparator.comparingDouble(LivingEntity::getHealth));
            case Angle -> foundTargets.sort(Comparator.comparingDouble(entity -> 
                Math.abs(MathHelper.wrapDegrees(RotationUtils.getRotations(entity.getPos().add(0, entity.getHeight() / 2, 0))[0] - mc.player.getYaw())) +
                Math.abs(MathHelper.wrapDegrees(RotationUtils.getRotations(entity.getPos().add(0, entity.getHeight() / 2, 0))[1] - mc.player.getPitch()))));
            case HurtTime -> foundTargets.sort(Comparator.comparingInt(entity -> entity.hurtTime));
            case None -> {}
        }

        return foundTargets;
    }
    
    // === ATTACK ===
    private void attack(LivingEntity attackTarget, boolean wasInInventory) {
        // FailRate
        if (failRate.getValue() > 0 && ThreadLocalRandom.current().nextFloat() * 100 < failRate.getValue()) {
            return;
        }
        
        // SimulateInventoryClosing (LiquidBounce bypass)
        if (simulateInventoryClosing.getValue() && wasInInventory) {
            NetworkUtils.sendPacket(new CloseHandledScreenC2SPacket(0));
        }
        
        // Stop blocking before attack
        if (autoBlock.getValue() && isBlocking) {
            stopBlocking();
        }
        
        if (unpressShield.getValue() && mc.player.isBlocking()) {
            mc.interactionManager.stopUsingItem(mc.player);
        }
        
        // KeepSprint bypass
        if (!keepSprint.getValue() && sprint.getValue() == Sprint.Hvh
                && MotherHack.getInstance().getServerManager().isServerSprinting()) {
            NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }
        
        // ON_TICK rotation timing - send rotation packet before attack
        if (rotationTiming.getValue() == RotationTiming.ON_TICK) {
            NetworkUtils.sendPacket(new PlayerMoveC2SPacket.Full(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                currentRotations[0], currentRotations[1], mc.player.isOnGround(), mc.player.horizontalCollision
            ));
        }
        
        // Restore hits data
        if (restoreHits.getValue()) {
            pendingAttacks.offer(new AttackData(attackTarget, 0, System.currentTimeMillis()));
        }
        
        // Attack
        if (attackTarget instanceof PlayerEntity player && player.isBlocking() && breakShield.getValue()) {
            shieldBreak(player);
        } else {
            mc.interactionManager.attackEntity(mc.player, attackTarget);
        }
        InventoryUtils.swing(swing.getValue());
        
        // ON_TICK - restore rotation after attack
        if (rotationTiming.getValue() == RotationTiming.ON_TICK) {
            NetworkUtils.sendPacket(new PlayerMoveC2SPacket.Full(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround(), mc.player.horizontalCollision
            ));
        }
        
        if (!keepSprint.getValue() && sprint.getValue() == Sprint.Hvh
                && !MotherHack.getInstance().getServerManager().isServerSprinting()) {
            NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        }
        
        // SimulateInventoryClosing - reopen (simplified - just close the screen packet)
        // Note: Full inventory reopen would require more complex handling
        
        // Start blocking after attack
        if (autoBlock.getValue() && autoBlockTickOn.getValue() == 0) {
            startBlocking();
        }
        
        attackTimer.reset();
        lastAttackTime = System.currentTimeMillis();
        nextAttackDelay = getRandomCpsDelay();
        currentScanExtraRange = MathUtils.randomFloat(2f, scanExtraRange.getValue());
    }

    // === AUTOBLOCK (LiquidBounce) ===
    private void startBlocking() {
        if (isBlocking) return;
        if (!hasShieldInOffhand()) return;
        
        switch (autoBlockMode.getValue()) {
            case INTERACT -> {
                mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            }
            case FAKE -> {
                // Fake block - only visual, no packet
            }
            case PACKET -> {
                NetworkUtils.sendPacket(new PlayerInteractItemC2SPacket(Hand.OFF_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
            }
        }
        isBlocking = true;
    }
    
    private void stopBlocking() {
        if (!isBlocking) return;
        
        if (autoBlockMode.getValue() != AutoBlockMode.FAKE) {
            mc.interactionManager.stopUsingItem(mc.player);
        }
        isBlocking = false;
    }
    
    private boolean hasShieldInOffhand() {
        return mc.player.getOffHandStack().getItem() == Items.SHIELD;
    }
    
    // === FAILSWING (LiquidBounce) ===
    private void doFailSwing() {
        if (ThreadLocalRandom.current().nextFloat() < 0.3f) {
            InventoryUtils.swing(swing.getValue());
        }
    }
    
    private long getRandomCpsDelay() {
        int min = minCps.getValue().intValue();
        int max = maxCps.getValue().intValue();
        if (min >= max) return 1000L / max;
        int randomCps = ThreadLocalRandom.current().nextInt(min, max + 1);
        return 1000L / randomCps;
    }

    private void processRestoreQueue() {
        if (pendingAttacks.isEmpty()) return;
        
        long currentTime = System.currentTimeMillis();
        List<AttackData> toRemove = new ArrayList<>();
        
        for (AttackData data : pendingAttacks) {
            if (currentTime - data.timestamp < 100) continue;
            
            if (data.entity != null && data.entity.isAlive() && isValidTarget(data.entity, range.getValue())) {
                if (data.entity.hurtTime <= 0 && data.attempts < restoreAttempts.getValue().intValue()) {
                    restoreAttack(data.entity);
                    data.attempts++;
                    data.timestamp = currentTime;
                } else {
                    toRemove.add(data);
                }
            } else {
                toRemove.add(data);
            }
        }
        
        pendingAttacks.removeAll(toRemove);
    }
    
    private void restoreAttack(LivingEntity entity) {
        if (unpressShield.getValue() && mc.player.isBlocking()) mc.interactionManager.stopUsingItem(mc.player);
        if (!keepSprint.getValue() && sprint.getValue() == Sprint.Hvh
                && MotherHack.getInstance().getServerManager().isServerSprinting()) {
            NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }
        
        if (entity instanceof PlayerEntity player && player.isBlocking() && breakShield.getValue()) {
            shieldBreak(player);
        } else {
            mc.interactionManager.attackEntity(mc.player, entity);
        }
        InventoryUtils.swing(swing.getValue());
        
        if (!keepSprint.getValue() && sprint.getValue() == Sprint.Hvh
                && !MotherHack.getInstance().getServerManager().isServerSprinting()) {
            NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        }
    }
    
    private static class AttackData {
        LivingEntity entity;
        int attempts;
        long timestamp;
        
        AttackData(LivingEntity entity, int attempts, long timestamp) {
            this.entity = entity;
            this.attempts = attempts;
            this.timestamp = timestamp;
        }
    }
    
    private void shieldBreak(PlayerEntity entity) {
        int slot = InventoryUtils.findBestAxe(0, 8);
        int previousSlot = mc.player.getInventory().selectedSlot;
        if (slot == -1) return;
        InventoryUtils.switchSlot(InventoryUtils.Switch.Silent, slot, previousSlot);
        mc.interactionManager.attackEntity(mc.player, entity);
        InventoryUtils.swing(swing.getValue());
        InventoryUtils.switchBack(InventoryUtils.Switch.Silent, slot, previousSlot);
    }
    
    private boolean isWeapon() {
        return mc.player.getMainHandStack().getItem() != Items.AIR
                && (mc.player.getMainHandStack().getItem() instanceof SwordItem
                || mc.player.getMainHandStack().getItem() instanceof PickaxeItem
                || mc.player.getMainHandStack().getItem() instanceof AxeItem
                || mc.player.getMainHandStack().getItem() instanceof HoeItem
                || mc.player.getMainHandStack().getItem() instanceof ShovelItem
                || mc.player.getMainHandStack().getItem() == Items.MACE);
    }

    private boolean isValidTarget(LivingEntity entity, float maxRange) {
        if (entity == null) return false;
        if (entity.age < ticksExisted.getValue().intValue()) return false;
        
        double distance = getDistanceTo(entity);
        boolean canSee = mc.player.canSee(entity);
        
        // WallRange check
        if (!canSee && distance > wallRange.getValue()) return false;
        if (distance > maxRange) return false;
        if (!throughWalls.getValue() && !canSee) return false;

        return Server.isValid(entity);
    }

    private boolean shouldAttack(LivingEntity attackTarget) {
        // HurtTime check
        if (attackTarget.hurtTime > hurtTime.getValue().intValue()) return false;
        
        PvPMode currentMode = pvpMode.getValue();
        
        // Режим PvP 1.8
        if (currentMode == PvPMode.V1_8) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAttackTime < nextAttackDelay) return false;
        } else {
            // Режим PvP 1.12
            float cooldown = mc.player.getAttackCooldownProgress(0f);
            
            // IgnoreShieldBreakCooldown (LiquidBounce bypass)
            if (ignoreShieldBreakCooldown.getValue() && attackTarget instanceof PlayerEntity player 
                    && player.isBlocking() && breakShield.getValue()) {
                // Ignore cooldown when breaking shield
            } else if (cooldown < IdealHitUtils.getAICooldown()) {
                return false;
            }
        }
        
        if ((rotate.getValue() == Rotate.Normal || rotate.getValue() == Rotate.Smoothness)
                && !mc.player.isGliding()
                && !MathUtils.inFov(attackTarget.getPos(), 12, currentRotations[0])) return false;
        
        if (getDistanceTo(attackTarget) > range.getValue()) return false;

        // SmartCrit
        if (smartCrit.getValue() && currentMode == PvPMode.V1_12) {
            return IdealHitUtils.canCritical(attackTarget);
        }
        
        return currentMode == PvPMode.V1_8 || IdealHitUtils.canCritical(attackTarget);
    }
    
    // === ENUMS ===
    @AllArgsConstructor
    public enum Priority implements Nameable {
        Distance("settings.aura.priority.distance"),
        Health("settings.aura.priority.health"),
        Angle("settings.aura.priority.angle"),
        HurtTime("settings.aura.priority.hurttime"),
        None("settings.none");
        private final String name;
        @Override public String getName() { return name; }
    }

    @AllArgsConstructor
    public enum Rotate implements Nameable {
        Normal("settings.normal"),
        Smoothness("settings.aura.rotate.smoothness"),
        FuntimeSnap("settings.aura.rotate.funtimesnap"),
        Packet("settings.packet"),
        None("settings.none");
        private final String name;
        @Override public String getName() { return name; }
    }

    @AllArgsConstructor
    public enum Sprint implements Nameable {
        Hvh("settings.aura.sprintreset.hvh"),
        Legit("settings.aura.sprintreset.legit");
        private final String name;
        @Override public String getName() { return name; }
    }

    @AllArgsConstructor
    public enum PvPMode implements Nameable {
        V1_8("settings.aura.pvpmode.1.8"),
        V1_12("settings.aura.pvpmode.1.12");
        private final String name;
        @Override public String getName() { return name; }
    }
    
    @AllArgsConstructor
    public enum RaycastMode implements Nameable {
        TRACE_NONE("settings.aura.raycast.none"),
        TRACE_ONLYENEMY("settings.aura.raycast.enemy"),
        TRACE_ALL("settings.aura.raycast.all");
        private final String name;
        @Override public String getName() { return name; }
    }
    
    @AllArgsConstructor
    public enum RotationTiming implements Nameable {
        NORMAL("settings.aura.rotationtiming.normal"),
        SNAP("settings.aura.rotationtiming.snap"),
        ON_TICK("settings.aura.rotationtiming.ontick");
        private final String name;
        @Override public String getName() { return name; }
    }
    
    @AllArgsConstructor
    public enum AutoBlockMode implements Nameable {
        INTERACT("settings.aura.autoblockmode.interact"),
        FAKE("settings.aura.autoblockmode.fake"),
        PACKET("settings.aura.autoblockmode.packet");
        private final String name;
        @Override public String getName() { return name; }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        stopBlocking();
        if (target != null) backTimer.reset();
        target = null;
        targets.clear();
        pendingAttacks.clear();
        lastAttackTime = 0;
        nextAttackDelay = 0;
        currentScanExtraRange = 0;
    }
}
