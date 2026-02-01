package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.impl.misc.FakeFine;
import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientBrandRetriever.class, remap = false)
public class ClientBrandRetrieverMixin {
    
    @Inject(method = "getClientModName", at = @At("RETURN"), cancellable = true)
    private static void onGetClientModName(CallbackInfoReturnable<String> cir) {
        try {
            FakeFine fakeFine = MotherHack.getInstance().getModuleManager().getModule(FakeFine.class);
            if (fakeFine != null && fakeFine.isToggled()) {
                cir.setReturnValue(fakeFine.getBrand());
            }
        } catch (Exception ignored) {}
    }
}
