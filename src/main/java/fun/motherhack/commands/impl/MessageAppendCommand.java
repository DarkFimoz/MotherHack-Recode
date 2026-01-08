package fun.motherhack.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fun.motherhack.MotherHack;
import fun.motherhack.commands.Command;
import fun.motherhack.modules.impl.misc.MessageAppend;
import fun.motherhack.utils.network.ChatUtils;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class MessageAppendCommand extends Command {

    public MessageAppendCommand() {
        super("DMITRYTFIMOZOVGAY");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            MessageAppend module = MotherHack.getInstance().getModuleManager().getModule(MessageAppend.class);
            if (module != null) {
                module.forceDisable();
                ChatUtils.sendMessage("MessageAppend disabled!");
            }
            return SINGLE_SUCCESS;
        });
    }
}
