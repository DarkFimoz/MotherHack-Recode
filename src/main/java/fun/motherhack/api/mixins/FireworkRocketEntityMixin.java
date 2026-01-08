package fun.motherhack.api.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.rotations.EventTravel;
import fun.motherhack.modules.impl.movement.ElytraBooster;
import fun.motherhack.utils.Wrapper;
import fun.motherhack.utils.combat.BoostUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin extends ProjectileEntity implements Wrapper {

    @Unique private Vec3d rotation;
    @Shadow private LivingEntity shooter;

    public FireworkRocketEntityMixin(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d tick(Vec3d original) {
        if (shooter == mc.player) {
            EventTravel event = new EventTravel(shooter.getYaw(), shooter.getPitch());
            MotherHack.getInstance().getEventHandler().post(event);
            rotation = getRotationVector(event.getPitch(), event.getYaw());
        } else rotation = original;

        return rotation;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;add(DDD)Lnet/minecraft/util/math/Vec3d;", ordinal = 0))
    public Vec3d tick(Vec3d instance, double x, double y, double z) {
        ElytraBooster elytraBooster = MotherHack.getInstance().getModuleManager().getModule(ElytraBooster.class);
        if (elytraBooster.isToggled()) {
            if (elytraBooster.mode.getValue() == ElytraBooster.Mode.Auto) {
                double boost = BoostUtils.getBoost();
                return instance.add(
                        rotation.x * 0.1 + (rotation.x * boost - instance.x) * 0.5D,
                        rotation.y * 0.1 + (rotation.y * boost - instance.y) * 0.5D,
                        rotation.z * 0.1 + (rotation.z * boost - instance.z) * 0.5D
                );
            } else {
                Vec3d boost = new Vec3d(
                        elytraBooster.boost.getValue() / 2 + 0.3,
                        elytraBooster.boost.getValue() / 2 + 0.3,
                        elytraBooster.boost.getValue() / 2 + 0.3
                );
                return instance.add(
                        rotation.x * 0.1 + (rotation.x * 1.5D - instance.x) * 0.5D,
                        rotation.y * 0.1 + (rotation.y * 1.5D - instance.y) * 0.5D,
                        rotation.z * 0.1 + (rotation.z * 1.5D - instance.z) * 0.5D
                ).multiply(boost);
            }
        } else return instance.add(
                rotation.x * 0.1 + (rotation.x * 1.5D - instance.x) * 0.5D,
                rotation.y * 0.1 + (rotation.y * 1.5D - instance.y) * 0.5D,
                rotation.z * 0.1 + (rotation.z * 1.5D - instance.z) * 0.5D
        );
    }
}