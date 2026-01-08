package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.impl.misc.NameProtect;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void onGetPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        NameProtect nameProtect = MotherHack.getInstance().getModuleManager().getModule(NameProtect.class);
        if (nameProtect != null && nameProtect.isToggled()) {
            Text original = cir.getReturnValue();
            if (original != null) {
                Text replaced = nameProtect.replaceNameDeep(original);
                cir.setReturnValue(replaced);
            }
        }
    }
}
