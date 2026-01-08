package fun.motherhack.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fun.motherhack.MotherHack;
import fun.motherhack.commands.Command;
import fun.motherhack.utils.network.ChatUtils;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class IgnoreCommand extends Command {

    public IgnoreCommand() {
        super("ignore");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder
            .then(literal("add")
                .then(arg("player", word())
                    .executes(context -> {
                        String player = context.getArgument("player", String.class);
                        if (!MotherHack.getInstance().getIgnoreManager().isIgnored(player)) {
                            MotherHack.getInstance().getIgnoreManager().addPlayer(player);
                            ChatUtils.sendMessage(Formatting.GREEN + "Игнорирую игрока: " + player);
                        } else {
                            ChatUtils.sendMessage(Formatting.RED + "Игрок уже в списке игнорируемых");
                        }
                        return SINGLE_SUCCESS;
                    })))
            .then(literal("remove")
                .then(arg("player", word())
                    .executes(context -> {
                        String player = context.getArgument("player", String.class);
                        if (MotherHack.getInstance().getIgnoreManager().isIgnored(player)) {
                            MotherHack.getInstance().getIgnoreManager().removePlayer(player);
                            ChatUtils.sendMessage(Formatting.GREEN + "Больше не игнорирую игрока: " + player);
                        } else {
                            ChatUtils.sendMessage(Formatting.RED + "Игрок не найден в списке игнорируемых");
                        }
                        return SINGLE_SUCCESS;
                    })))
            .then(literal("list")
                .executes(context -> {
                    if (MotherHack.getInstance().getIgnoreManager().getIgnoredPlayers().isEmpty()) {
                        ChatUtils.sendMessage(Formatting.YELLOW + "Список игнорируемых игроков пуст");
                    } else {
                        StringBuilder sb = new StringBuilder(Formatting.GREEN + "Игнорируемые игроки: " + Formatting.WHITE);
                        for (String player : MotherHack.getInstance().getIgnoreManager().getIgnoredPlayers()) {
                            sb.append(player).append(", ");
                        }
                        ChatUtils.sendMessage(sb.substring(0, sb.length() - 2));
                    }
                    return SINGLE_SUCCESS;
                }));
    }
}
