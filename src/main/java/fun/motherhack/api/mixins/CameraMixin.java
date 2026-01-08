package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventCamera;
import fun.motherhack.modules.impl.render.FreeCam;
import fun.motherhack.modules.impl.render.NoRender;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow private float yaw;
    @Shadow private float pitch;

    @Shadow public abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void moveBy(float f, float g, float h);
    @Shadow protected abstract float clipToSpace(float f);

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;moveBy(FFF)V", ordinal = 0))
    public void update2(Args args) {
        if (MotherHack.getInstance() == null || MotherHack.getInstance().getModuleManager() == null) return;
        NoRender noRender = MotherHack.getInstance().getModuleManager().getModule(NoRender.class);
        if (noRender != null && noRender.isToggled() && noRender.clip.getValue()) args.set(0, -3.5f);
    }

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    public void clipToSpace(float f, CallbackInfoReturnable<Float> cir) {
        if (MotherHack.getInstance() == null || MotherHack.getInstance().getModuleManager() == null) return;
        NoRender noRender = MotherHack.getInstance().getModuleManager().getModule(NoRender.class);
        if (noRender != null && noRender.isToggled() && noRender.clip.getValue()) cir.setReturnValue(3.5f);
    }

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V", shift = At.Shift.AFTER), cancellable = true)
    private void onCameraUpdate(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (MotherHack.getInstance() == null || MotherHack.getInstance().getModuleManager() == null) return;
        
        // FreeCam handling
        FreeCam freeCam = MotherHack.getInstance().getModuleManager().getModule(FreeCam.class);
        if (freeCam != null && freeCam.isToggled()) {
            setPos(freeCam.getFakeX(), freeCam.getFakeY(), freeCam.getFakeZ());
            setRotation(freeCam.getFakeYaw(), freeCam.getFakePitch());
            ci.cancel();
            return;
        }
        
        if (MotherHack.getInstance().getEventHandler() == null) return;
        
        EventCamera event = new EventCamera(yaw, pitch, 4.0f, false);
        MotherHack.getInstance().getEventHandler().post(event);

        if (event.isCancelled() && focusedEntity instanceof ClientPlayerEntity player && !player.isSleeping() && thirdPerson) {
            float pitch = inverseView ? -event.getPitch() : event.getPitch();
            float yaw = event.getYaw() - (inverseView ? 180 : 0);
            float distance = event.getDistance();

            setRotation(yaw, pitch);
            moveBy(event.isCameraClip() ? -distance : -clipToSpace(distance), 0.0f, 0.0f);
            ci.cancel();
        }
    }
}