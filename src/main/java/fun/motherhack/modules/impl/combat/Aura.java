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
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Aura extends Module {

    private final EnumSetting<Rotate> rotate = new EnumSetting<>("settings.rotate", Rotate.Normal);
    private final NumberSetting range = new NumberSetting("settings.aura.range", 3f, 0f, 6f, 0.05f);
    private final NumberSetting aimRange = new NumberSetting("settings.aura.aimrange", 3f, 0f, 6f, 0.05f, () -> rotate.getValue() != Rotate.FuntimeSnap);
    private final NumberSetting elytraAimRange = new NumberSetting("settings.aura.elytraaimrange", 50f, 10f, 100f, 0.05f);
    private final EnumSetting<InventoryUtils.Swing> swing = new EnumSetting<>("settings.swing", InventoryUtils.Swing.MainHand);
    public final EnumSetting<Sprint> sprint = new EnumSetting<>("settings.aura.sprintreset", Sprint.Legit);
    private final BooleanSetting throughWalls = new BooleanSetting("settings.aura.throughwalls", true);
    private final BooleanSetting unpressShield = new BooleanSetting("settings.aura.unpressshield", false);
    private final BooleanSetting breakShield = new BooleanSetting("settings.aura.breakshield", true);
    private final EnumSetting<Priority> priority = new EnumSetting<>("settings.aura.priority", Priority.Distance);
    private final BooleanSetting autoJump = new BooleanSetting("settings.aura.autojump", false);
    private final EnumSetting<PvPMode> pvpMode = new EnumSetting<>("settings.aura.pvpmode", PvPMode.V1_12);
    private final NumberSetting cps18 = new NumberSetting("settings.aura.cps18", 100f, 1f, 100f, 1f, () -> pvpMode.getValue() == PvPMode.V1_8);
    private final BooleanSetting restoreHits = new BooleanSetting("settings.aura.restorehits", true);
    private final NumberSetting restoreAttempts = new NumberSetting("settings.aura.restoreattempts", 3f, 1f, 10f, 1f, () -> restoreHits.getValue());

    public Aura() {
        super("Aura", Category.Combat);
    }

    @Getter private LivingEntity target;
    private Vec3d multipoint = Vec3d.ZERO;
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

    @EventHandler
    public void onPlayerTick(EventPlayerTick e) {
        if (fullNullCheck()) return;

        executor.submit(() -> {
            target = findTarget();
            multipoint = target == null ? Vec3d.ZERO : getMultipoint(target);
        });

        if (target != null || !backTimer.passed(250)) {
        	float[] elytraRotations = new float[2];
        	if (target != null) {
        		elytraRotations = RotationUtils.getRotations(PredictUtils.predict(target, target.getPos().add(0, target.getHeight() / 2, 0), 0));
                if (MotherHack.getInstance().getModuleManager().getModule(ElytraForward.class).isToggled()) {
                    int predict = MotherHack.getInstance().getModuleManager().getModule(ElytraForward.class).forward.getValue().intValue();
                    elytraRotations = RotationUtils.getRotations(PredictUtils.predict(target, target.getPos().add(0, target.getHeight() / 2, 0), predict));
                }

                targetRotations = RotationUtils.getRotations(target.getPos().add(0, target.getHeight() / 2, 0));
        	}
        	
            if (mc.player.isGliding()) currentRotations = elytraRotations;
            else {
                if (rotate.getValue() == Rotate.FuntimeSnap) {
                	yawAnimation.setDuration(700);
                    pitchAnimation.setDuration(500);
                    yawAnimation.update();
                    pitchAnimation.update();
                    float[] rotations = target == null ? new float[2] : RotationUtils.getRotations(target.getPos().add(0, target.getHeight() / 2, 0));
                    rotations[0] = (((((rotations[0] - mc.player.getYaw()) % 360) + 540) % 360) - 180);

                    if (target != null) {
                    	if ((!(IdealHitUtils.findFall(0)
                    			&& !mc.player.isOnGround()
                    			|| IdealHitUtils.canCritical(target))
                    			|| (isWeapon() && !attackTimer.passed(300)))
                    	) {
                            rotations[0] = yawAnimation.getValue() * 40 - 20;
                            rotations[1] += pitchAnimation.getValue() * 15 - 7;
                        } else {
                            rotations[0] += MathUtils.randomInt(5, 10);
                            rotations[1] += MathUtils.randomInt(5, 30);
                        }
                    }

                    currentRotations[0] = mc.player.getYaw() + MathUtils.getStep(currentRotations[0] - mc.player.getYaw(), rotations[0], MathUtils.randomInt(80, 85));
                    currentRotations[1] = pitchAnimationInf.animate(rotations[1], MathUtils.randomInt(100, 150));
                    currentRotations[1] = MathHelper.clamp(currentRotations[1], -90, 90);
                    currentRotations = RotationUtils.correctRotation(currentRotations);
                } else if (rotate.getValue() == Rotate.Smoothness) {
                    currentRotations = targetRotations;
                    currentRotations[0] += shakingAnimation.animateYaw(MathUtils.randomFloat(-8, 8), 100);
                    currentRotations[1] += shakingAnimation.animatePitch(MathUtils.randomFloat(-15, -15), 100);
                } else currentRotations = targetRotations;
            }
            
            if (target != null) {
                if (rotate.getValue() == Rotate.Packet) MotherHack.getInstance().getRotationManager().addPacketRotation(currentRotations);
                else MotherHack.getInstance().getRotationManager().addRotation(changer);
                if (shouldAttack()) attack();
                
                // AutoJump
                if (autoJump.getValue() && mc.player.isOnGround() && target.isOnGround()) {
                    mc.player.jump();
                }
            }
            
            // Восстановление отмененных ударов
            if (restoreHits.getValue()) {
                processRestoreQueue();
            }
        } else if (target == null) backTimer.reset();
    }

    private LivingEntity findTarget() {
        List<LivingEntity> targets = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!isValidTarget(living)) continue;
            targets.add(living);
        }

        if (targets.isEmpty() || !isToggled()) return null;

        switch (priority.getValue()) {
            case Distance -> targets.sort(Comparator.comparingDouble(entity -> getMultipoint(entity).squaredDistanceTo(mc.player.getEyePos())));
            case Health -> targets.sort(Comparator.comparingDouble(LivingEntity::getHealth));
            case Angle -> targets.sort(Comparator.comparingDouble(entity -> Math.abs(MathHelper.wrapDegrees(RotationUtils.getRotations(entity.getPos().add(0, entity.getHeight() / 2, 0))[0] - mc.player.getYaw())) + Math.abs(MathHelper.wrapDegrees(RotationUtils.getRotations(entity.getPos().add(0, entity.getHeight() / 2, 0))[1] - mc.player.getPitch()))));
            case None -> {}
        }

        return targets.getFirst();
    }
    
    private Vec3d getMultipoint(LivingEntity entity) {
    	return MultipointUtils.getClosestPoint(entity, 25, 25, rotate.getValue() != Rotate.FuntimeSnap ? getMaxAimRange() : range.getValue());
    }
    
    private void attack() {
    	if (unpressShield.getValue() && mc.player.isBlocking()) mc.interactionManager.stopUsingItem(mc.player);
        if (sprint.getValue() == Sprint.Hvh
        		&& MotherHack.getInstance().getServerManager().isServerSprinting()
        ) NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        
        // Сохраняем данные атаки для возможного восстановления
        if (restoreHits.getValue()) {
            AttackData attackData = new AttackData(target, 0, System.currentTimeMillis());
            pendingAttacks.offer(attackData);
        }
        
        if (target instanceof PlayerEntity player && player.isBlocking() && breakShield.getValue()) shieldBreak(player);
        else mc.interactionManager.attackEntity(mc.player, target);
        InventoryUtils.swing(swing.getValue());
        if (sprint.getValue() == Sprint.Hvh
        		&& !MotherHack.getInstance().getServerManager().isServerSprinting()
        ) NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        attackTimer.reset();
    }
    
    private void processRestoreQueue() {
        if (pendingAttacks.isEmpty()) return;
        
        long currentTime = System.currentTimeMillis();
        List<AttackData> toRemove = new ArrayList<>();
        
        for (AttackData data : pendingAttacks) {
            // Проверяем, прошло ли достаточно времени для проверки отмены удара
            if (currentTime - data.timestamp < 100) continue;
            
            // Если цель все еще валидна и удар был отменен сервером
            if (data.entity != null && data.entity.isAlive() && isValidTarget(data.entity)) {
                // Проверяем, получила ли цель урон (если нет - удар был отменен)
                if (data.entity.hurtTime <= 0 && data.attempts < restoreAttempts.getValue().intValue()) {
                    // Восстанавливаем удар
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
        if (sprint.getValue() == Sprint.Hvh
                && MotherHack.getInstance().getServerManager().isServerSprinting()
        ) NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        
        if (entity instanceof PlayerEntity player && player.isBlocking() && breakShield.getValue()) shieldBreak(player);
        else mc.interactionManager.attackEntity(mc.player, entity);
        InventoryUtils.swing(swing.getValue());
        
        if (sprint.getValue() == Sprint.Hvh
                && !MotherHack.getInstance().getServerManager().isServerSprinting()
        ) NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
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
    	return 	mc.player.getMainHandStack().getItem() != Items.AIR
    			&& (mc.player.getMainHandStack().getItem() instanceof SwordItem
    			|| mc.player.getMainHandStack().getItem() instanceof PickaxeItem
    			|| mc.player.getMainHandStack().getItem() instanceof AxeItem
    			|| mc.player.getMainHandStack().getItem() instanceof HoeItem
    			|| mc.player.getMainHandStack().getItem() instanceof ShovelItem
    			|| mc.player.getMainHandStack().getItem() == Items.MACE);
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == null) return false;
        if (rotate.getValue() != Rotate.FuntimeSnap && getMultipoint(entity).squaredDistanceTo(mc.player.getEyePos()) > MathHelper.square(getMaxAimRange())) return false;
        if (rotate.getValue() == Rotate.FuntimeSnap && getMultipoint(entity).squaredDistanceTo(mc.player.getEyePos()) > MathHelper.square(range.getValue())) return false;
        if (!throughWalls.getValue() && !mc.player.canSee(entity)) return false;

        return Server.isValid(entity);
    }

    private boolean shouldAttack() {
        // Режим PvP 1.8 - игнорируем перезарядку и используем CPS
        if (pvpMode.getValue() == PvPMode.V1_8) {
            long cpsDelay = (long) (1000.0 / cps18.getValue());
            if (!attackTimer.passed(cpsDelay)) return false;
        } else {
            // Режим PvP 1.12 - используем стандартную перезарядку
            if (mc.player.getAttackCooldownProgress(0f) < IdealHitUtils.getAICooldown()) return false;
        }
        
        if ((rotate.getValue() == Rotate.Normal || rotate.getValue() == Rotate.Smoothness)
                && !mc.player.isGliding()
                && !MathUtils.inFov(target.getPos(), 12, currentRotations[0])) return false;
        if (multipoint.squaredDistanceTo(mc.player.getEyePos()) > MathHelper.square(range.getValue())) return false;

        return pvpMode.getValue() == PvPMode.V1_8 || IdealHitUtils.canCritical(target);
    }

    private float getMaxAimRange() {
        return mc.player.isGliding() && rotate.getValue() != Rotate.FuntimeSnap	? elytraAimRange.getValue()
        				: rotate.getValue() != Rotate.FuntimeSnap ? range.getValue() + aimRange.getValue() 
        				: 0;
    }
    
    @AllArgsConstructor
    public enum Priority implements Nameable {
        Distance("settings.aura.priority.distance"),
        Health("settings.aura.priority.health"),
        Angle("settings.aura.priority.angle"),
        None("settings.none");

        private final String name;

        @Override
        public String getName() {
            return name;
        }
    }

    @AllArgsConstructor
    public enum Rotate implements Nameable {
        Normal("settings.normal"),
        Smoothness("settings.aura.rotate.smoothness"),
        FuntimeSnap("settings.aura.rotate.funtimesnap"),
        Packet("settings.packet"),
        None("settings.none");

        private final String name;

        @Override
        public String getName() {
            return name;
        }
    }

    @AllArgsConstructor
    public enum Sprint implements Nameable {
        Hvh("settings.aura.sprintreset.hvh"),
        Legit("settings.aura.sprintreset.legit");

        private final String name;

        @Override
        public String getName() {
            return name;
        }
    }

    @AllArgsConstructor
    public enum PvPMode implements Nameable {
        V1_8("settings.aura.pvpmode.1.8"),
        V1_12("settings.aura.pvpmode.1.12");

        private final String name;

        @Override
        public String getName() {
            return name;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (target != null) backTimer.reset();
        target = null;
        pendingAttacks.clear();
    }
}