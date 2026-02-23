package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.managers.CapeManager;
import fun.motherhack.modules.impl.client.Cape;
import fun.motherhack.utils.Wrapper;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin implements Wrapper {

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void onGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        if (mc.player == null) return;
        
        Cape capeModule = MotherHack.getInstance().getModuleManager().getModule(Cape.class);
        if (capeModule == null || !capeModule.isEnabled()) return;

        // Проверяем, это наш игрок
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
        if (!self.getUuid().equals(mc.player.getUuid())) return;

        CapeManager.Cape selectedCape = MotherHack.getInstance().getCapeManager().getSelectedCape();
        if (selectedCape == null) return;

        SkinTextures original = cir.getReturnValue();
        SkinTextures modified = new SkinTextures(
            original.texture(),
            original.textureUrl(),
            selectedCape.getTexture(), // Кастомный плащ
            original.elytraTexture(),
            original.model(),
            original.secure()
        );
        
        cir.setReturnValue(modified);
    }
}
