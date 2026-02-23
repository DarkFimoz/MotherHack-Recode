package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventBlockShape;
import fun.motherhack.modules.impl.render.Xray;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class AbstractBlockStateMixin {

    @Shadow
    public abstract Block getBlock();

    @Inject(method = "getAmbientOcclusionLightLevel", at = @At("HEAD"), cancellable = true)
    private void onGetAmbientOcclusionLightLevel(BlockView world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (MotherHack.getInstance() == null) return;
        Xray xray = MotherHack.getInstance().getModuleManager().getModule(Xray.class);
        if (xray != null && xray.isToggled()) {
            cir.setReturnValue(1.0f);
        }
    }

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/shape/VoxelShape;", 
            at = @At("RETURN"), cancellable = true)
    private void onGetCollisionShape(BlockView world, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        if (MotherHack.getInstance() == null) return;
        
        VoxelShape originalShape = cir.getReturnValue();
        EventBlockShape event = new EventBlockShape(pos, originalShape);
        MotherHack.getInstance().getEventHandler().post(event);
        
        if (event.getShape() != originalShape) {
            cir.setReturnValue(event.getShape());
        }
    }
}
