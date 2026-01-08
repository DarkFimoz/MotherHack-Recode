package fun.motherhack.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import fun.motherhack.utils.Wrapper;
import lombok.Getter;
import net.minecraft.command.CommandSource;

@Getter
public abstract class Command implements Wrapper {
    private final String command;
    private final String[] aliases;

    public Command(String command, String... aliases) {
        this.command = command;
        this.aliases = aliases;
    }

    public abstract void execute(LiteralArgumentBuilder<CommandSource> builder);

    public void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> builder = LiteralArgumentBuilder.literal(command);
        execute(builder);
        dispatcher.register(builder);
        
        // Register aliases
        for (String alias : aliases) {
            LiteralArgumentBuilder<CommandSource> aliasBuilder = LiteralArgumentBuilder.literal(alias);
            execute(aliasBuilder);
            dispatcher.register(aliasBuilder);
        }
    }

    protected <T> RequiredArgumentBuilder<CommandSource, T> arg(String name, final ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    protected LiteralArgumentBuilder<CommandSource> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }
}