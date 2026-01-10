package fun.motherhack.modules.impl.combat;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.api.events.impl.rotations.EventMotion;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.client.Targets;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TargetStrafe extends Module {
    // Rich Mode setting
    private final EnumSetting<RichMode> richMode = new EnumSetting<>("RichMode", RichMode.THMode);
    
    // General settings
    private final NumberSetting maxRange = new NumberSetting("MaxRange", 6f, 1f, 20f, 0.5f);
    
    // TH Mode settings
    private final BooleanSetting jump = new BooleanSetting("Jump", true, () -> richMode.getValue() == RichMode.THMode);
    private final NumberSetting distance = new NumberSetting("Distance", 1.3f, 0.2f, 7f, 0.1f, () -> richMode.getValue() == RichMode.THMode);
    private final EnumSetting<Boost> boost = new EnumSetting<>("Boost", Boost.None, () -> richMode.getValue() == RichMode.THMode);
    private final NumberSetting setSpeed = new NumberSetting("Speed", 1.3f, 0.0f, 2f, 0.1f, () -> richMode.getValue() == RichMode.THMode);
    private final NumberSetting velReduction = new NumberSetting("Reduction", 6.0f, 0.1f, 10f, 0.1f, () -> richMode.getValue() == RichMode.THMode);
    private final NumberSetting maxVelocitySpeed = new NumberSetting("Max Velocity", 0.8f, 0.1f, 2f, 0.1f, () -> richMode.getValue() == RichMode.THMode);
    
    // Rich Mode settings
    private final EnumSetting<RichStrafeMode> richStrafeMode = new EnumSetting<>("Mode", RichStrafeMode.Matrix, () -> richMode.getValue() == RichMode.RichMode);
    private final EnumSetting<RichPointType> richPointType = new EnumSetting<>("PointType", RichPointType.Cube, () -> richMode.getValue() == RichMode.RichMode && richStrafeMode.getValue() == RichStrafeMode.Grim);
    private final EnumSetting<RichPointType> richPointTypeMatrix = new EnumSetting<>("MatrixPoint", RichPointType.Circle, () -> richMode.getValue() == RichMode.RichMode && richStrafeMode.getValue() == RichStrafeMode.Matrix);
    private final NumberSetting richGrimRadius = new NumberSetting("GrimRadius", 0.87f, 0.1f, 1.5f, 0.01f, () -> richMode.getValue() == RichMode.RichMode && richStrafeMode.getValue() == RichStrafeMode.Grim);
    private final NumberSetting richRadius = new NumberSetting("RichRadius", 2.5f, 0.1f, 7f, 0.1f, () -> richMode.getValue() == RichMode.RichMode && richStrafeMode.getValue() == RichStrafeMode.Matrix);
    private final NumberSetting richSpeed = new NumberSetting("RichSpeed", 0.3f, 0.1f, 1f, 0.01f, () -> richMode.getValue() == RichMode.RichMode && richStrafeMode.getValue() == RichStrafeMode.Matrix);
    private final BooleanSetting richAutoJump = new BooleanSetting("AutoJump", true, () -> richMode.getValue() == RichMode.RichMode);
    private final BooleanSetting richOnlyKeyPressed = new BooleanSetting("OnlyKeyPressed", false, () -> richMode.getValue() == RichMode.RichMode);
    private final BooleanSetting richInFrontOfTarget = new BooleanSetting("InFrontOfTarget", false, () -> richMode.getValue() == RichMode.RichMode);
    private final EnumSetting<RichDirection> richDirection = new EnumSetting<>("Direction", RichDirection.Clockwise, () -> richMode.getValue() == RichMode.RichMode);

    private double oldSpeed, fovVal;
    private boolean switchDir, disabled;
    private int jumpTicks, waterTicks;
    private long disableTime;
    private LivingEntity target;
    private int elytraSlot = -1;
    private int richPointIndex = 0;

    public TargetStrafe() {
        super("TargetStrafe", Category.Combat);
        getSettings().add(richMode);
        getSettings().add(maxRange);
        // TH Mode settings
        getSettings().add(jump);
        getSettings().add(distance);
        getSettings().add(boost);
        getSettings().add(setSpeed);
        getSettings().add(velReduction);
        getSettings().add(maxVelocitySpeed);
        // Rich Mode settings
        getSettings().add(richStrafeMode);
        getSettings().add(richPointType);
        getSettings().add(richPointTypeMatrix);
        getSettings().add(richGrimRadius);
        getSettings().add(richRadius);
        getSettings().add(richSpeed);
        getSettings().add(richAutoJump);
        getSettings().add(richOnlyKeyPressed);
        getSettings().add(richInFrontOfTarget);
        getSettings().add(richDirection);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        oldSpeed = 0;
        disabled = false;
        target = null;
        richPointIndex = 0;
        if (mc != null && mc.player != null && mc.options != null) {
            try {
                fovVal = mc.options.getFovEffectScale().getValue();
                mc.options.getFovEffectScale().setValue(0.0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDisable() {
        if (mc != null && mc.options != null) {
            try {
                mc.options.getFovEffectScale().setValue(fovVal);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        target = null;
        super.onDisable();
    }

    private boolean canStrafe() {
        if (mc == null || mc.player == null || mc.world == null) return false;
        try {
            if (mc.player.isSneaking()) return false;
            if (mc.player.isInLava()) return false;
            if (mc.player.isSubmergedInWater() || waterTicks > 0) return false;
            return !mc.player.getAbilities().flying;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean needToSwitch(double x, double z) {
        try {
            if (mc.player.horizontalCollision || ((mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed()) && jumpTicks <= 0)) {
                jumpTicks = 10;
                return true;
            }
             
            for (int i = (int) (mc.player.getY() + 4); i >= 0; --i) {
                BlockPos pos = new BlockPos((int) Math.floor(x), i, (int) Math.floor(z));
                if (mc.world.getBlockState(pos).getBlock() == Blocks.LAVA ||
                    mc.world.getBlockState(pos).getBlock() == Blocks.FIRE) {
                    return true;
                }
                if (!mc.world.isAir(pos)) return false;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private double calculateSpeed() {
        try {
            jumpTicks--;
            float speedAttributes = getAIMoveSpeed();
            float frictionFactor = mc.world.getBlockState(new BlockPos.Mutable().set(mc.player.getX(),
                getBoundingBox().minY, mc.player.getZ())).getBlock().getSlipperiness() * 0.91f;
                 
            float n6 = mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST) && mc.player.isUsingItem() ? 0.88f :
                (float) (oldSpeed > 0.32 && mc.player.isUsingItem() ? 0.88 : 0.91f);
                 
            if (mc.player.isOnGround()) {
                n6 = frictionFactor;
            }
             
            float n7 = (float) (0.1631f / Math.pow(n6, 3.0f));
            float n8 = mc.player.isOnGround() ? speedAttributes * n7 : 0.0255f;
             
            if (mc.player.isOnGround() && mc.player.isUsingItem()) {
                n8 += 0.2f;
            }
             
            double max2 = oldSpeed + n8;
             
            return Math.max(0.25, max2);
        } catch (Exception e) {
            e.printStackTrace();
            return 0.25;
        }
    }
    
    private Box getBoundingBox() {
        try {
            return new Box(
                mc.player.getX() - 0.1, mc.player.getY(), mc.player.getZ() - 0.1,
                mc.player.getX() + 0.1, mc.player.getY() + 1, mc.player.getZ() + 0.1
            );
        } catch (Exception e) {
            e.printStackTrace();
            return new Box(0, 0, 0, 0, 0, 0);
        }
    }
    
    private float getAIMoveSpeed() {
        try {
            // Basic movement speed calculation
            float speed = 0.1f;
            if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
                speed *= 1.0 + 0.2 * (mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1);
            }
            if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
                speed *= 1.0 + 0.15 * (mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1);
            }
            return speed;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.1f;
        }
    }
    
    private int getRichDirectionMultiplier() {
        if (richDirection.getValue() == RichDirection.Counterclockwise) {
            return -1;
        } else if (richDirection.getValue() == RichDirection.Random) {
            long time = System.currentTimeMillis() / 3000;
            return (time % 2 == 0) ? 1 : -1;
        }
        return 1;
    }
    
    private boolean isRichKeyPressed() {
        return mc.options.forwardKey.isPressed() ||
               mc.options.backKey.isPressed() ||
               mc.options.leftKey.isPressed() ||
               mc.options.rightKey.isPressed();
    }
    
    @EventHandler
    public void onMotion(EventMotion event) {
        if (mc == null || mc.player == null || mc.world == null) return;
         
        // Update target from Aura if available
        target = findTarget();
        
        try {
            if (MotherHack.getInstance() != null &&
                MotherHack.getInstance().getModuleManager() != null) {
                Aura aura = MotherHack.getInstance().getModuleManager().getModule(Aura.class);
                if (aura != null && aura.isToggled() && aura.getTarget() != null) {
                    target = aura.getTarget();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Rich Mode handling
        if (richMode.getValue() == RichMode.RichMode) {
            handleRichModeMotion();
            return;
        }

        // TH Mode handling
        try {
            if (boost.getValue() == Boost.Elytra && elytraSlot != -1) {
                if (isMoving() && !mc.player.isOnGround() &&
                    !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0, mc.player.getY(), 0)).iterator().hasNext() &&
                    disabled) {
                    oldSpeed = setSpeed.getValue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (canStrafe() && target != null) {
            try {
                double speed = calculateSpeed();
                 
                // Calculate strafing position
                double deltaX = mc.player.getX() - target.getX();
                double deltaZ = mc.player.getZ() - target.getZ();
                double distanceToTarget = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                 
                // Calculate angle for circling
                double angle = Math.atan2(deltaZ, deltaX);
                angle += switchDir ? speed / distanceToTarget : -speed / distanceToTarget;
                 
                // Calculate target position
                double x = target.getX() + distance.getValue() * Math.cos(angle);
                double z = target.getZ() + distance.getValue() * Math.sin(angle);
                 
                // Check if we need to switch direction
                if (needToSwitch(x, z)) {
                    switchDir = !switchDir;
                    angle += 2 * (switchDir ? speed / distanceToTarget : -speed / distanceToTarget);
                    x = target.getX() + distance.getValue() * Math.cos(angle);
                    z = target.getZ() + distance.getValue() * Math.sin(angle);
                }
                 
                // Calculate movement direction
                double wrap = wrapDS(x, z);
                double motionX = speed * -Math.sin(Math.toRadians(wrap));
                double motionZ = speed * Math.cos(Math.toRadians(wrap));
                 
                // Apply movement
                mc.player.setVelocity(motionX, mc.player.getVelocity().y, motionZ);
            } catch (Exception e) {
                e.printStackTrace();
                oldSpeed = 0;
            }
        } else {
            oldSpeed = 0;
        }
    }
    
    private void handleRichModeMotion() {
        if (target == null || !target.isAlive()) return;
        
        if (richStrafeMode.getValue() != RichStrafeMode.Matrix) return;
        
        Vec3d playerPos = mc.player.getPos();
        Vec3d targetPos = target.getPos();
        double r = richRadius.getValue();
        
        if (richOnlyKeyPressed.getValue() && !isRichKeyPressed()) return;
        
        if (richAutoJump.getValue() && mc.player.isOnGround()) {
            mc.player.jump();
        }
        
        int directionMultiplier = getRichDirectionMultiplier();
        
        if (richInFrontOfTarget.getValue()) {
            float targetYaw = target.getYaw();
            double x = targetPos.x - Math.sin(Math.toRadians(targetYaw)) * r * directionMultiplier;
            double z = targetPos.z + Math.cos(Math.toRadians(targetYaw)) * r * directionMultiplier;
            float yaw = (float) Math.toDegrees(Math.atan2(z - playerPos.z, x - playerPos.x)) - 90F;
            double motionSpeed = richSpeed.getValue();
            mc.player.setVelocity(
                -Math.sin(Math.toRadians(yaw)) * motionSpeed,
                mc.player.getVelocity().y,
                Math.cos(Math.toRadians(yaw)) * motionSpeed
            );
            return;
        }
        
        if (richPointTypeMatrix.getValue() == RichPointType.Cube) {
            Vec3d[] points = new Vec3d[]{
                new Vec3d(targetPos.x - r, playerPos.y, targetPos.z - r),
                new Vec3d(targetPos.x - r, playerPos.y, targetPos.z + r),
                new Vec3d(targetPos.x + r, playerPos.y, targetPos.z + r),
                new Vec3d(targetPos.x + r, playerPos.y, targetPos.z - r)
            };
            
            if (playerPos.distanceTo(points[richPointIndex]) < 0.5) {
                richPointIndex = (richPointIndex + directionMultiplier + points.length) % points.length;
            }
            
            Vec3d nextPoint = points[richPointIndex];
            Vec3d dirVec = nextPoint.subtract(playerPos).normalize();
            float yaw = (float) Math.toDegrees(Math.atan2(dirVec.z, dirVec.x)) - 90F;
            double motionSpeed = richSpeed.getValue();
            mc.player.setVelocity(
                -Math.sin(Math.toRadians(yaw)) * motionSpeed,
                mc.player.getVelocity().y,
                Math.cos(Math.toRadians(yaw)) * motionSpeed
            );
        } else if (richPointTypeMatrix.getValue() == RichPointType.Circle) {
            double angle = Math.atan2(playerPos.z - targetPos.z, playerPos.x - targetPos.x);
            angle += directionMultiplier * richSpeed.getValue() / Math.max(playerPos.distanceTo(targetPos), r);
            double x = targetPos.x + r * Math.cos(angle);
            double z = targetPos.z + r * Math.sin(angle);
            float yaw = (float) Math.toDegrees(Math.atan2(z - playerPos.z, x - playerPos.x)) - 90F;
            double motionSpeed = richSpeed.getValue();
            mc.player.setVelocity(
                -Math.sin(Math.toRadians(yaw)) * motionSpeed,
                mc.player.getVelocity().y,
                Math.cos(Math.toRadians(yaw)) * motionSpeed
            );
        }
    }
    
    private double wrapDS(double x, double z) {
        try {
            double diffX = x - mc.player.getX();
            double diffZ = z - mc.player.getZ();
            return Math.toDegrees(Math.atan2(diffZ, diffX)) - 90;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    @EventHandler
    public void onTick(EventPlayerTick event) {
        if (mc == null || mc.player == null) return;
        
        // Rich Mode Grim handling in tick
        if (richMode.getValue() == RichMode.RichMode && richStrafeMode.getValue() == RichStrafeMode.Grim) {
            handleRichGrimTick();
            return;
        }
        
        try {
            if (boost.getValue() == Boost.Elytra && elytraSlot != -1 &&
                !mc.player.isOnGround() && mc.player.fallDistance > 0 && !disabled) {
                disabler(elytraSlot);
            }
             
            if (jump.getValue() && mc.player.isOnGround() && target != null) {
                mc.player.jump();
            }
             
            if (mc.player.isSubmergedInWater()) {
                waterTicks = 10;
            } else if (waterTicks > 0) {
                waterTicks--;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void handleRichGrimTick() {
        if (target == null || !target.isAlive()) return;
        
        if (richOnlyKeyPressed.getValue() && !isRichKeyPressed()) return;
        
        Vec3d playerPos = mc.player.getPos();
        Vec3d targetPos = target.getPos();
        double r = richGrimRadius.getValue();
        
        int directionMultiplier = getRichDirectionMultiplier();
        
        Vec3d nextPoint;
        
        if (richInFrontOfTarget.getValue()) {
            float targetYaw = target.getYaw();
            if (richPointType.getValue() == RichPointType.Center) {
                nextPoint = targetPos.add(
                    -Math.sin(Math.toRadians(targetYaw)) * r * directionMultiplier,
                    0,
                    Math.cos(Math.toRadians(targetYaw)) * r * directionMultiplier
                );
            } else {
                double offset = Math.cos(System.currentTimeMillis() / 500.0) * r * directionMultiplier;
                nextPoint = targetPos.add(
                    -Math.sin(Math.toRadians(targetYaw)) * r + Math.cos(Math.toRadians(targetYaw)) * offset,
                    0,
                    Math.cos(Math.toRadians(targetYaw)) * r + Math.sin(Math.toRadians(targetYaw)) * offset
                );
            }
        } else {
            if (richPointType.getValue() == RichPointType.Cube) {
                Vec3d[] points = new Vec3d[]{
                    new Vec3d(targetPos.x - r, playerPos.y, targetPos.z - r),
                    new Vec3d(targetPos.x - r, playerPos.y, targetPos.z + r),
                    new Vec3d(targetPos.x + r, playerPos.y, targetPos.z + r),
                    new Vec3d(targetPos.x + r, playerPos.y, targetPos.z - r)
                };
                
                if (playerPos.distanceTo(points[richPointIndex]) < 0.5) {
                    richPointIndex = (richPointIndex + directionMultiplier + points.length) % points.length;
                }
                nextPoint = points[richPointIndex];
            } else if (richPointType.getValue() == RichPointType.Circle) {
                double baseAngle = (System.currentTimeMillis() % 3600L) / 3600.0 * 4 * Math.PI;
                double angle = directionMultiplier > 0 ? baseAngle : (2 * Math.PI - baseAngle);
                nextPoint = new Vec3d(
                    targetPos.x + Math.cos(angle) * r,
                    playerPos.y,
                    targetPos.z + Math.sin(angle) * r
                );
            } else {
                nextPoint = new Vec3d(targetPos.x, playerPos.y, targetPos.z);
            }
        }
        
        // Calculate movement direction and apply input simulation
        Vec3d direction = nextPoint.subtract(playerPos).normalize();
        float yaw = mc.player.getYaw();
        float movementAngle = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90F;
        float angleDiff = MathHelper.wrapDegrees(movementAngle - yaw);
        
        // Apply movement based on angle difference
        double motionX = -Math.sin(Math.toRadians(movementAngle)) * 0.2;
        double motionZ = Math.cos(Math.toRadians(movementAngle)) * 0.2;
        
        mc.player.setVelocity(
            mc.player.getVelocity().x + motionX,
            mc.player.getVelocity().y,
            mc.player.getVelocity().z + motionZ
        );
        
        if (richAutoJump.getValue() && mc.player.isOnGround()) {
            mc.player.jump();
        }
    }
    
    @EventHandler
    public void onPacket(EventPacket.Receive event) {
        if (mc == null || mc.player == null) return;
        
        try {
            if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
                oldSpeed = 0;
            } else if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket) {
                EntityVelocityUpdateS2CPacket velocity = (EntityVelocityUpdateS2CPacket) event.getPacket();
                if (velocity.getEntityId() == mc.player.getId() && boost.getValue() == Boost.Damage) {
                    if (mc.player.isOnGround()) return;
                     
                    // Get velocity components using getter methods
                    double vX = Math.abs(velocity.getVelocityX() / 8000.0);
                    double vZ = Math.abs(velocity.getVelocityZ() / 8000.0);
                     
                    oldSpeed = (vX + vZ) / (velReduction.getValue() * 1000.0);
                    oldSpeed = Math.min(oldSpeed, maxVelocitySpeed.getValue());
                     
                    event.cancel();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void disabler(int elytra) {
        if (elytra == -1 || System.currentTimeMillis() - disableTime < 190) return;
        
        try {
            // Simplified elytra handling since we don't have InventoryUtil
            if (mc.player != null && mc.player.networkHandler != null) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player,
                    ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
            
            disableTime = System.currentTimeMillis();
            disabled = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private boolean isMoving() {
        try {
            return mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private LivingEntity findTarget() {
        if (mc == null || mc.world == null || mc.player == null) return null;
        
        try {
            Targets targetsModule = MotherHack.getInstance().getModuleManager().getModule(Targets.class);
            double maxRangeSq = maxRange.getValue() * maxRange.getValue();
            
            List<LivingEntity> entities = new ArrayList<>();
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof LivingEntity living && targetsModule.isValid(living)) {
                    if (mc.player.squaredDistanceTo(living) <= maxRangeSq) {
                        entities.add(living);
                    }
                }
            }
            entities.sort(Comparator.comparingDouble(entity -> mc.player.squaredDistanceTo(entity)));
            return entities.isEmpty() ? null : entities.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private enum Boost implements Nameable {
        None("None"), Elytra("Elytra"), Damage("Damage");

        private final String name;

        Boost(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
    
    private enum RichMode implements Nameable {
        THMode("THMode"), RichMode("RichMode");
        
        private final String name;
        
        RichMode(String name) {
            this.name = name;
        }
        
        @Override
        public String getName() {
            return name;
        }
    }
    
    private enum RichStrafeMode implements Nameable {
        Matrix("Matrix"), Grim("Grim");
        
        private final String name;
        
        RichStrafeMode(String name) {
            this.name = name;
        }
        
        @Override
        public String getName() {
            return name;
        }
    }
    
    private enum RichPointType implements Nameable {
        Cube("Cube"), Center("Center"), Circle("Circle");
        
        private final String name;
        
        RichPointType(String name) {
            this.name = name;
        }
        
        @Override
        public String getName() {
            return name;
        }
    }
    
    private enum RichDirection implements Nameable {
        Clockwise("Clockwise"), Counterclockwise("Counterclockwise"), Random("Random");
        
        private final String name;
        
        RichDirection(String name) {
            this.name = name;
        }
        
        @Override
        public String getName() {
            return name;
        }
    }
}