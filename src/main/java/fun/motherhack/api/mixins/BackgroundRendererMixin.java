package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventFog;
import fun.motherhack.modules.impl.render.AmbienceModule;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {

    @Inject(method = "applyFog", at = @At("RETURN"), cancellable = true)
    private static void applyFog(Camera camera, BackgroundRenderer.FogType fogType, Vector4f color, float viewDistance, boolean thickenFog, float tickDelta, CallbackInfoReturnable<Fog> cir) {
        AmbienceModule ambience = MotherHack.getInstance().getModuleManager().getModule(AmbienceModule.class);
        if (ambience != null && ambience.isToggled() && ambience.customFog.getValue()) {
            Color fogColor = ambience.getFogColor();
            float distance = ambience.distanceFog.getValue();
            
            EventFog event = new EventFog(0, distance, fogColor);
            MotherHack.getInstance().getEventHandler().post(event);
            
            if (!event.isCancelled()) {
                Fog customFog = new Fog(
                    event.getStart(),
                    event.getEnd(),
                    FogShape.SPHERE,
                    event.getColor().getRed() / 255f,
                    event.getColor().getGreen() / 255f,
                    event.getColor().getBlue() / 255f,
                    event.getColor().getAlpha() / 255f
                );
                cir.setReturnValue(customFog);
            }
        }
    }
}
