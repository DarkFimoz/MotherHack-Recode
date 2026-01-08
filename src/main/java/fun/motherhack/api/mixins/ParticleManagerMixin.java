package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.impl.render.NoRender;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {

    @Inject(method = "renderParticles(Lnet/minecraft/client/render/Camera;FLnet/minecraft/client/render/VertexConsumerProvider$Immediate;)V", at = @At("HEAD"), cancellable = true)
    public void renderParticles(CallbackInfo ci) {
        if (MotherHack.getInstance().getModuleManager().getModule(NoRender.class).isToggled() && MotherHack.getInstance().getModuleManager().getModule(NoRender.class).particles.getValue()) ci.cancel();
    }

    @Inject(method = "renderCustomParticles", at = @At("HEAD"), cancellable = true)
    private static void renderCustomParticles(Camera camera, float tickDelta, VertexConsumerProvider.Immediate vertexConsumers, Queue<Particle> particles, CallbackInfo ci) {
        if (MotherHack.getInstance().getModuleManager().getModule(NoRender.class).isToggled() && MotherHack.getInstance().getModuleManager().getModule(NoRender.class).particles.getValue()) ci.cancel();
    }

    @Inject(method = "renderParticles(Lnet/minecraft/client/render/Camera;FLnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/particle/ParticleTextureSheet;Ljava/util/Queue;)V", at = @At("HEAD"), cancellable = true)
    private static void renderParticles(Camera camera, float tickDelta, VertexConsumerProvider.Immediate vertexConsumers, ParticleTextureSheet sheet, Queue<Particle> particles, CallbackInfo ci) {
        if (MotherHack.getInstance().getModuleManager().getModule(NoRender.class).isToggled() && MotherHack.getInstance().getModuleManager().getModule(NoRender.class).particles.getValue()) ci.cancel();
    }
}