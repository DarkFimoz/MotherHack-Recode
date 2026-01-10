package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.impl.render.Xray;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class BlockMixin {

    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private static void onShouldDrawSide(BlockState state, BlockState otherState, Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (MotherHack.getInstance() == null) return;
        Xray xray = MotherHack.getInstance().getModuleManager().getModule(Xray.class);
        if (xray != null && xray.isToggled()) {
            cir.setReturnValue(xray.isBlockVisible(state.getBlock()));
        }
    }
}
