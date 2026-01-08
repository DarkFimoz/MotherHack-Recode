package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.impl.client.VersionSpoof;
import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientBrandRetriever.class)
public class ClientBrandRetrieverMixin {

    @Inject(method = "getClientModName", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onGetClientModName(CallbackInfoReturnable<String> cir) {
        VersionSpoof versionSpoof = MotherHack.getInstance().getModuleManager().getModule(VersionSpoof.class);
        
        if (versionSpoof != null && versionSpoof.isToggled() && versionSpoof.customBrand.getValue()) {
            cir.setReturnValue(versionSpoof.brandName.getValue());
        }
    }
}
