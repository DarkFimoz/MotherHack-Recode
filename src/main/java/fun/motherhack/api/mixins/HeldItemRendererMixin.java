package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.impl.render.SwingAnimations;
import fun.motherhack.modules.impl.render.ViewModel;
import fun.motherhack.utils.Wrapper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
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
    @Shadow private float prevEquipProgressOffHand;
    @Shadow private float equipProgressOffHand;
    @Shadow private ItemStack offHand;
    
	@Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
	public void renderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo info) {
        // SwingAnimations
        SwingAnimations swingAnimations = MotherHack.getInstance().getModuleManager().getModule(SwingAnimations.class);
        if (swingAnimations != null && swingAnimations.shouldAnimate()) {
            boolean bl = hand == Hand.MAIN_HAND;
            Arm arm = bl ? player.getMainArm() : player.getMainArm().getOpposite();
            swingAnimations.renderSwordAnimation(matrices, pitch, swingProgress, equipProgress, arm);
        }
        
        // ViewModel
        ViewModel viewModel = MotherHack.getInstance().getModuleManager().getModule(ViewModel.class);
		if (viewModel != null && viewModel.isToggled()) {
            boolean bl = client.player.getActiveItem().getItem() == item.getItem();
            boolean bl2 = item.contains(DataComponentTypes.FOOD) || item.getItem() instanceof PotionItem;

            if (hand == Hand.MAIN_HAND) {
                float f6 = bl2 && bl ? 0.0f : -viewModel.mainX.getValue();
                float f5 = bl2 && bl ? 0.0f : viewModel.mainZ.getValue();
                if (client.player.getMainArm() == Arm.LEFT) f6 = -f6;
                matrices.translate(f6, viewModel.mainY.getValue(), f5);
                matrices.scale(viewModel.mainScaleX.getValue(), viewModel.mainScaleY.getValue(), viewModel.mainScaleZ.getValue());
            } else {
                float f9 = bl2 && bl ? 0.0f : viewModel.offX.getValue();
                float f8 = bl2 && bl ? 0.0f : viewModel.offZ.getValue();
                if (client.player.getMainArm() == Arm.LEFT) f9 = -f9;
                matrices.translate(f9, viewModel.offY.getValue(), f8);
                matrices.scale(viewModel.offScaleX.getValue(), viewModel.offScaleY.getValue(), viewModel.offScaleZ.getValue());
            }
        }
	}
}