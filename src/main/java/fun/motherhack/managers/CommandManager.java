package fun.motherhack.managers;

import com.mojang.brigadier.CommandDispatcher;
import fun.motherhack.commands.Command;
import fun.motherhack.commands.impl.*;
import fun.motherhack.utils.Wrapper;
import lombok.*;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandSource;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CommandManager implements Wrapper {

    private final CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();
    private final CommandSource source = new ClientCommandSource(null, mc);
    private final List<Command> commands = new ArrayList<>();
    @Setter private String prefix = "@";

    public CommandManager() {
        addCommands(
                new FriendCommand(),
                new ConfigCommand(),
                new MacroCommand(),
                new WaypointCommand(),
                new IgnoreCommand(),
                new MessageAppendCommand(),
                new MessageAppendWordCommand(),
                new GotoCommand()
        );
    }

    private void addCommands(Command... command) {
        for (Command cmd : command) {
            cmd.register(dispatcher);
            commands.add(cmd);
        }
    }
}