package fun.motherhack.modules.impl.combat;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.api.events.impl.rotations.EventMotion;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.network.Server;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TargetStrafe extends Module {
    private final BooleanSetting jump = new BooleanSetting("Jump", true);
    private final NumberSetting distance = new NumberSetting("Distance", 1.3f, 0.2f, 7f, 0.1f);
    private final EnumSetting<Boost> boost = new EnumSetting<>("Boost", Boost.None);
    private final NumberSetting setSpeed = new NumberSetting("Speed", 1.3f, 0.0f, 2f, 0.1f);
    private final NumberSetting velReduction = new NumberSetting("Reduction", 6.0f, 0.1f, 10f, 0.1f);
    private final NumberSetting maxVelocitySpeed = new NumberSetting("Max Velocity", 0.8f, 0.1f, 2f, 0.1f);

    private double oldSpeed, fovVal;
    private boolean needSprintState, switchDir, disabled;
    private int noSlowTicks, jumpTicks, waterTicks;
    private long disableTime;
    private LivingEntity target;
    private int elytraSlot = -1;

    public TargetStrafe() {
        super("TargetStrafe", Category.Combat);
        getSettings().add(jump);
        getSettings().add(distance);
        getSettings().add(boost);
        getSettings().add(setSpeed);
        getSettings().add(velReduction);
        getSettings().add(maxVelocitySpeed);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        oldSpeed = 0;
        disabled = false;
        target = null;
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
             
            // Simplified sprint state handling
            if (!mc.player.isOnGround()) {
                needSprintState = !mc.player.isSprinting();
            } else {
                needSprintState = false;
            }
             
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
            // Basic target finding logic
            List<LivingEntity> entities = new ArrayList<>();
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof LivingEntity living && entity != mc.player && living.isAlive()) {
                    entities.add(living);
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
}