package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.impl.misc.NameProtect;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), argsOnly = true)
    private Text modifyMessage(Text message) {
        NameProtect nameProtect = MotherHack.getInstance().getModuleManager().getModule(NameProtect.class);
        if (nameProtect != null && nameProtect.isToggled()) {
            return nameProtect.replaceNameDeep(message);
        }
        return message;
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Text message, CallbackInfo ci) {
        String messageText = message.getString();
        
        // Check if the message is from a player (format: "<PlayerName> message")
        if (messageText.startsWith("<")) {
            int endOfName = messageText.indexOf(">");
            if (endOfName > 0) {
                String playerName = messageText.substring(1, endOfName);
                
                // Check if the player is in the ignore list
                if (MotherHack.getInstance().getIgnoreManager().isIgnored(playerName)) {
                    ci.cancel(); // Cancel the message if the player is ignored
                }
            }
        }
    }
}
