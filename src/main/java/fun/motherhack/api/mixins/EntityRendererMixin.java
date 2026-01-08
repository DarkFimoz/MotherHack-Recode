package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.impl.misc.NameProtect;
import fun.motherhack.modules.impl.render.NameTags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity>  {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    public void getDisplayName(T entity, CallbackInfoReturnable<Text> cir) {
        if (MotherHack.getInstance().getModuleManager().getModule(NameTags.class).isToggled() && entity instanceof PlayerEntity) {
            cir.setReturnValue(null);
            return;
        }
        
        // NameProtect - заменяем имя игрока на фейковое
        NameProtect nameProtect = MotherHack.getInstance().getModuleManager().getModule(NameProtect.class);
        if (nameProtect != null && nameProtect.isToggled() && entity instanceof PlayerEntity player) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null && player.getUuid().equals(mc.player.getUuid())) {
                Text original = cir.getReturnValue();
                if (original != null) {
                    cir.setReturnValue(nameProtect.replaceNameDeep(original));
                }
            }
        }
    }
}