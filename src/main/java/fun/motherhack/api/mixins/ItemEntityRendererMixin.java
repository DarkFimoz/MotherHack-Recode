package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.impl.render.ItemPhysics;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin extends EntityRenderer<ItemEntity, ItemEntityRenderState> {

    @Unique
    private boolean isOnGround = false;

    protected ItemEntityRendererMixin(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V",
            at = @At("RETURN"))
    private void onUpdateRenderState(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        isOnGround = entity.isOnGround();
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"))
    private void onRenderHead(ItemEntityRenderState state, MatrixStack matrices, 
                         VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (MotherHack.getInstance() == null) return;
        ItemPhysics module = MotherHack.getInstance().getModuleManager().getModule(ItemPhysics.class);
        if (module == null || !module.isToggled()) return;
        
        // Применяем эффект только если предмет на земле (не летит)
        if (isOnGround) {
            matrices.push();
            
            // Поворачиваем предмет плоско на 90 градусов
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            
            // Поднимаем предмет вверх, чтобы он лежал НА земле, а не В земле
            matrices.translate(0, 0.125, 0);
        }
    }
    
    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("RETURN"))
    private void onRenderReturn(ItemEntityRenderState state, MatrixStack matrices, 
                         VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (MotherHack.getInstance() == null) return;
        ItemPhysics module = MotherHack.getInstance().getModuleManager().getModule(ItemPhysics.class);
        if (module == null || !module.isToggled()) return;
        
        if (isOnGround) {
            matrices.pop();
        }
    }
    
    // Отменяем вращение предметов на земле
    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
              at = @At(value = "INVOKE", 
                       target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionf;)V"))
    private void cancelRotation(MatrixStack matrices, Quaternionf quaternion, ItemEntityRenderState state) {
        if (MotherHack.getInstance() == null) {
            matrices.multiply(quaternion);
            return;
        }
        ItemPhysics module = MotherHack.getInstance().getModuleManager().getModule(ItemPhysics.class);
        if (module == null || !module.isToggled()) {
            matrices.multiply(quaternion);
            return;
        }
        
        // Если предмет на земле - не применяем вращение
        if (!isOnGround) {
            matrices.multiply(quaternion);
        }
    }
    
    // Отменяем покачивание (bobbing) предметов на земле
    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
              at = @At(value = "INVOKE", 
                       target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V",
                       ordinal = 0))
    private void cancelBobbing(MatrixStack matrices, float x, float y, float z) {
        if (MotherHack.getInstance() == null) {
            matrices.translate(x, y, z);
            return;
        }
        ItemPhysics module = MotherHack.getInstance().getModuleManager().getModule(ItemPhysics.class);
        if (module == null || !module.isToggled()) {
            matrices.translate(x, y, z);
            return;
        }
        
        // Если предмет на земле - не применяем покачивание (только x и z, без y)
        if (isOnGround) {
            matrices.translate(x, 0, z);
        } else {
            matrices.translate(x, y, z);
        }
    }
}
