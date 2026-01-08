package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.impl.misc.NameProtect;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Team.class)
public class ScoreboardMixin {
    
    @Inject(method = "decorateName", at = @At("RETURN"), cancellable = true)
    private void onDecorateName(Text name, CallbackInfoReturnable<MutableText> cir) {
        NameProtect nameProtect = MotherHack.getInstance().getModuleManager().getModule(NameProtect.class);
        if (nameProtect != null && nameProtect.isToggled()) {
            MutableText original = cir.getReturnValue();
            if (original != null) {
                Text replaced = nameProtect.replaceNameDeep(original);
                if (replaced instanceof MutableText mutable) {
                    cir.setReturnValue(mutable);
                } else {
                    cir.setReturnValue(Text.literal(replaced.getString()).setStyle(original.getStyle()));
                }
            }
        }
    }
}
