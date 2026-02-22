package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.impl.render.SwingAnimations;
import fun.motherhack.utils.Wrapper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin implements Wrapper {
	
    @Shadow @Final private MinecraftClient client;
    @Shadow private ItemStack mainHand;
    @Shadow private float equipProgressMainHand;
    @Shadow private float prevEquipProgressMainHand;
    @Shadow protected abstract void renderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);
    @Shadow protected abstract void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress);
    @Shadow private float prevEquipProgressOffHand;
    @Shadow private float equipProgressOffHand;
    @Shadow private ItemStack offHand;
    
	@Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
	public void renderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo info) {
        SwingAnimations swingAnimations = MotherHack.getInstance().getModuleManager().getModule(SwingAnimations.class);
        
        if (swingAnimations != null && swingAnimations.isToggled()) {
            // Применяем анимацию к обеим рукам, если в них есть предмет
            if (!item.isEmpty()) {
                swingAnimations.handleRenderItem(player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light);
                info.cancel();
            }
        }
	}
}