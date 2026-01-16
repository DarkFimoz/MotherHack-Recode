package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.rotations.EventJump;
import fun.motherhack.api.events.impl.rotations.EventTravel;
import fun.motherhack.modules.impl.render.SwingAnimations;
import fun.motherhack.utils.Wrapper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements Wrapper {

    @Shadow public int handSwingTicks;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Redirect(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    public float jump(LivingEntity instance) {
        if (instance == mc.player) {
            EventJump event = new EventJump(instance.getYaw());
            MotherHack.getInstance().getEventHandler().post(event);
            return event.getYaw();
        } else return instance.getYaw();
    }

    @Redirect(method = "calcGlidingVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d calcGlidingVelocity(LivingEntity instance) {
        if (instance == mc.player) {
            EventTravel event = new EventTravel(instance.getYaw(), instance.getPitch());
            MotherHack.getInstance().getEventHandler().post(event);
            return getRotationVector(event.getPitch(), event.getYaw());
        } else return getRotationVector(instance.getPitch(), instance.getYaw());
    }
    
    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void modifySwingDuration(CallbackInfoReturnable<Integer> cir) {
        if ((Object)this == mc.player) {
            SwingAnimations swingAnimations = MotherHack.getInstance().getModuleManager().getModule(SwingAnimations.class);
            if (swingAnimations != null && swingAnimations.shouldChangeAnimationDuration() && swingAnimations.slowAnimation.getValue()) {
                cir.setReturnValue(swingAnimations.slowAnimationVal.getValue().intValue());
            }
        }
    }
}