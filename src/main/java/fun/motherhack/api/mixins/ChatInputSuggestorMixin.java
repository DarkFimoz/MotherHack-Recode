package fun.motherhack.api.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import fun.motherhack.MotherHack;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(ChatInputSuggestor.class)
public abstract class ChatInputSuggestorMixin {

    @Final @Shadow TextFieldWidget textField;
    @Shadow boolean completingSuggestions;
    @Shadow private ParseResults<CommandSource> parse;
    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow private ChatInputSuggestor.SuggestionWindow window;
    @Shadow public abstract void show(boolean narrateFirstSuggestion);

    @Inject(method = "refresh", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;canRead()Z", remap = false), cancellable = true)
    public void refresh(CallbackInfo ci, @Local StringReader reader) {
        String prefix = MotherHack.getInstance().getCommandManager().getPrefix();

        if (reader.canRead(prefix.length()) && reader.getString().startsWith(prefix, reader.getCursor()) && !MotherHack.getInstance().isPanic()) {
            int cursor;
            reader.setCursor(reader.getCursor() + prefix.length());
            CommandDispatcher<CommandSource> dispatcher = MotherHack.getInstance().getCommandManager().getDispatcher();
            if (this.parse == null) this.parse = dispatcher.parse(reader, MotherHack.getInstance().getCommandManager().getSource());
            if (!((cursor = this.textField.getCursor()) < 1 || this.window != null && this.completingSuggestions)) {
                this.pendingSuggestions = dispatcher.getCompletionSuggestions(this.parse, cursor);
                this.pendingSuggestions.thenRun(() -> {
                    if (this.pendingSuggestions.isDone()) this.show(false);
                });
            }

            ci.cancel();
        }
    }
}