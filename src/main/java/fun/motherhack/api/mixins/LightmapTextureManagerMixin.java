package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.impl.render.Fullbright;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {

    @Final @Shadow private SimpleFramebuffer lightmapFramebuffer;

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/SimpleFramebuffer;endWrite()V", shift = At.Shift.BEFORE))
    public void update(float delta, CallbackInfo ci) {
        if (MotherHack.getInstance().getModuleManager().getModule(Fullbright.class).isToggled()) lightmapFramebuffer.clear();
    }
}