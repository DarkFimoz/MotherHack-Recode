package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.impl.render.Models;
import fun.motherhack.modules.impl.render.models.ModelsRenderer;
import fun.motherhack.utils.Wrapper;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class PlayerEntityRendererMixin implements Wrapper {

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true)
    private void onRender(LivingEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!(state instanceof PlayerEntityRenderState playerState)) return;
        
        Models models = MotherHack.getInstance().getModuleManager().getModule(Models.class);
        if (models == null || !models.isToggled()) return;
        if (mc.player == null) return;

        boolean shouldApply = !models.onlySelf.getValue() 
            || playerState.name.equals(mc.player.getName().getString())
            || (models.friends.getValue() && MotherHack.getInstance().getFriendManager().isFriend(playerState.name));

        if (!shouldApply) return;

        // Custom mode just scales the default Steve model, don't cancel render
        if (models.mode.getValue() == Models.Mode.Custom) {
            float scale = (float) models.customScale.getValue();
            matrices.scale(scale, scale, scale);
            return;
        }

        // Cancel default render and render our custom model
        ci.cancel();
        
        ModelsRenderer.renderModel(playerState, matrices, models);
    }
}
