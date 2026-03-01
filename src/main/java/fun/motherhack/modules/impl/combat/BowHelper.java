package fun.motherhack.modules.impl.combat;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.managers.RotationManager;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.rotations.RotationChanger;
import fun.motherhack.utils.rotations.RotationUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.hit.HitResult;
import net.minecraft.client.network.ClientPlayerEntity;

public class BowHelper extends Module {
    
    private final NumberSetting fov;
    private final NumberSetting rotationSpeed;
    private final EnumSetting<RotationMode> rotationMode;
    private final BooleanSetting autoShoot;
    private final BooleanSetting autoRecharge;
    
    private LivingEntity target;
    private float lastYaw;
    private float lastPitch;
    private RotationChanger rotationChanger;
    private boolean wasUsingBow;
    private boolean isRotating;
    
    public BowHelper() {
        super("BowHelper", Category.Combat);
        
        this.fov = new NumberSetting("settings.bowhelper.fov", 360.0f, 30.0f, 360.0f, 10.0f);
        this.rotationSpeed = new NumberSetting("settings.bowhelper.rotationspeed", 80.0f, 10.0f, 180.0f, 5.0f);
        this.rotationMode = new EnumSetting<>("settings.bowhelper.rotationmode", RotationMode.Client);
        this.autoShoot = new BooleanSetting("settings.bowhelper.autoshoot", true);
        this.autoRecharge = new BooleanSetting("settings.bowhelper.autorecharge", true);
        
        this.wasUsingBow = false;
        this.isRotating = false;
        
        this.rotationChanger = new RotationChanger(100, 
            () -> new Float[]{this.lastYaw, this.lastPitch},
            () -> !this.isRotating
        );
    }
    
    @meteordevelopment.orbit.EventHandler
    public void onPlayerTick(EventPlayerTick event) {
        if (fullNullCheck()) {
            return;
        }
        
        var mainHandStack = mc.player.getMainHandStack();
        var offHandStack = mc.player.getOffHandStack();
        
        boolean hasBow = mainHandStack.getItem() instanceof BowItem || 
                        offHandStack.getItem() instanceof BowItem;
        
        if (!hasBow) {
            resetRotation();
            return;
        }
        
        this.target = findTarget();
        
        if (this.target != null) {
            this.isRotating = true;
            
            Vec3d predictedPos = getPredictedTargetPos();
            float[] rotations = RotationUtils.getRotations(predictedPos);
            
            smoothRotate(rotations[0], rotations[1]);
            
            if (this.rotationMode.getValue() == RotationMode.Client) {
                MotherHack.getInstance().getRotationManager().addRotation(this.rotationChanger);
            } else if (this.rotationMode.getValue() == RotationMode.Legit) {
                mc.player.setYaw(this.lastYaw);
                mc.player.setPitch(this.lastPitch);
            }
            
            if (mc.player.isUsingItem()) {
                this.wasUsingBow = true;
                int useTime = mc.player.getItemUseTime();
                
                if (this.autoShoot.getValue() && useTime >= 20) {
                    mc.options.useKey.setPressed(false);
                    mc.interactionManager.stopUsingItem(mc.player);
                }
            } else if (this.wasUsingBow && this.autoRecharge.getValue()) {
                this.wasUsingBow = false;
                mc.options.useKey.setPressed(true);
            }
        } else {
            resetRotation();
        }
    }
    
    private void resetRotation() {
        this.target = null;
        this.isRotating = false;
        this.wasUsingBow = false;
        
        if (this.rotationMode.getValue() == RotationMode.Legit) {
            this.lastYaw = mc.player.getYaw();
            this.lastPitch = mc.player.getPitch();
        } else {
            float currentYaw = mc.player.getYaw();
            float currentPitch = mc.player.getPitch();
            
            float yawDiff = MathHelper.wrapDegrees(currentYaw - this.lastYaw);
            float pitchDiff = currentPitch - this.lastPitch;
            
            if (Math.abs(yawDiff) > 1.0f || Math.abs(pitchDiff) > 1.0f) {
                this.lastYaw += yawDiff * 0.3f;
                this.lastPitch += pitchDiff * 0.3f;
            } else {
                this.lastYaw = currentYaw;
                this.lastPitch = currentPitch;
            }
        }
    }
    
    private LivingEntity findTarget() {
        var targets = MotherHack.getInstance().getModuleManager()
            .getModule(fun.motherhack.modules.impl.client.Targets.class);
        
        if (targets == null) {
            return null;
        }
        
        java.util.List<LivingEntity> validTargets = new java.util.ArrayList<>();
        
        for (var entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }
            
            if (!targets.isValid(livingEntity)) {
                continue;
            }
            
            double distance = mc.player.distanceTo(livingEntity);
            if (distance > 100.0) {
                continue;
            }
            
            if (!canSeeTarget(livingEntity)) {
                continue;
            }
            
            if (this.fov.getValue() < 360.0f) {
                Vec3d targetPos = livingEntity.getPos()
                    .add(0, livingEntity.getHeight() / 2.0, 0);
                float[] rotations = RotationUtils.getRotations(targetPos);
                
                float yawDiff = Math.abs(MathHelper.wrapDegrees(
                    rotations[0] - mc.player.getYaw()));
                float pitchDiff = Math.abs(MathHelper.wrapDegrees(
                    rotations[1] - mc.player.getPitch()));
                
                float totalDiff = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
                
                if (totalDiff > this.fov.getValue() / 2.0f) {
                    continue;
                }
            }
            
            validTargets.add(livingEntity);
        }
        
        if (validTargets.isEmpty()) {
            return null;
        }
        
        validTargets.sort(java.util.Comparator.comparingDouble(
            e -> mc.player.distanceTo(e)));
        
        return validTargets.get(0);
    }
    
    private boolean canSeeTarget(LivingEntity target) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetPos = target.getPos()
            .add(0, target.getHeight() / 2.0, 0);
        
        var hitResult = mc.world.raycast(new net.minecraft.world.RaycastContext(
            eyePos, targetPos,
            net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
            net.minecraft.world.RaycastContext.FluidHandling.NONE,
            mc.player
        ));
        
        return hitResult.getType() == HitResult.Type.MISS;
    }
    
    private Vec3d getPredictedTargetPos() {
        if (this.target == null) {
            return Vec3d.ZERO;
        }
        
        Vec3d velocity = this.target.getVelocity();
        double distance = mc.player.distanceTo(this.target);
        
        float arrowSpeed = 3.0f;
        float gravity = 0.05f;
        
        Vec3d targetCenter = this.target.getPos()
            .add(0, this.target.getHeight() / 2.0, 0);
        Vec3d eyePos = mc.player.getEyePos();
        
        double travelTime = distance / arrowSpeed;
        Vec3d predictedPos = targetCenter.add(velocity.multiply(travelTime));
        
        double horizontalDist = Math.sqrt(
            Math.pow(predictedPos.x - eyePos.x, 2) +
            Math.pow(predictedPos.z - eyePos.z, 2)
        );
        
        double verticalDiff = predictedPos.y - eyePos.y;
        
        double v2 = arrowSpeed * arrowSpeed;
        double v4 = v2 * v2;
        double discriminant = v4 - gravity * (gravity * horizontalDist * horizontalDist + 
            2 * verticalDiff * v2);
        
        if (discriminant < 0) {
            return predictedPos.add(0, horizontalDist * 0.1, 0);
        }
        
        double angle = Math.atan((v2 - Math.sqrt(discriminant)) / (gravity * horizontalDist));
        double time = horizontalDist / (arrowSpeed * Math.cos(angle));
        double drop = 0.5 * gravity * time * time;
        
        return predictedPos.add(0, drop, 0);
    }
    
    private void smoothRotate(float targetYaw, float targetPitch) {
        float speed = this.rotationSpeed.getValue() / 20.0f;
        
        if (this.lastYaw == 0.0f && this.lastPitch == 0.0f) {
            this.lastYaw = mc.player.getYaw();
            this.lastPitch = mc.player.getPitch();
        }
        
        float yawDiff = MathHelper.wrapDegrees(targetYaw - this.lastYaw);
        float pitchDiff = targetPitch - this.lastPitch;
        
        float maxStep = speed;
        
        if (Math.abs(yawDiff) > maxStep) {
            yawDiff = Math.signum(yawDiff) * maxStep;
        }
        
        if (Math.abs(pitchDiff) > maxStep) {
            pitchDiff = Math.signum(pitchDiff) * maxStep;
        }
        
        this.lastYaw = MathHelper.wrapDegrees(this.lastYaw + yawDiff);
        this.lastPitch = MathHelper.clamp(this.lastPitch + pitchDiff, -90.0f, 90.0f);
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        
        if (this.rotationMode.getValue() == RotationMode.Legit && this.isRotating) {
            // Cleanup
        }
        
        resetRotation();
    }
    
    public LivingEntity getTarget() {
        return this.target;
    }
    
    public enum RotationMode implements Nameable {
        Client("settings.bowhelper.rotationmode.client"),
        Legit("settings.bowhelper.rotationmode.legit");
        
        private final String name;
        
        RotationMode(String name) {
            this.name = name;
        }
        
        @Override
        public String getName() {
            return net.minecraft.text.Text.translatable(this.name).getString();
        }
    }
}
