package fun.motherhack.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fun.motherhack.MotherHack;
import fun.motherhack.commands.Command;
import fun.motherhack.modules.impl.misc.MessageAppend;
import fun.motherhack.utils.network.ChatUtils;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;

public class MessageAppendWordCommand extends Command {

    public MessageAppendWordCommand() {
        super("DMITRIYTFIMOZOVNOTGAY");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(arg("word", greedyString())
                .executes(context -> {
                    String newWord = context.getArgument("word", String.class);
                    MessageAppend module = MotherHack.getInstance().getModuleManager().getModule(MessageAppend.class);
                    if (module != null) {
                        module.forceSetWord(newWord);
                        ChatUtils.sendMessage("Word changed to: " + newWord);
                    }
                    return SINGLE_SUCCESS;
                }));
    }
}
