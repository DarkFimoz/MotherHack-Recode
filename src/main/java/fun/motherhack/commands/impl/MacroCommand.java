package fun.motherhack.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fun.motherhack.MotherHack;
import fun.motherhack.commands.Command;
import fun.motherhack.modules.settings.api.Bind;
import fun.motherhack.utils.macro.Macro;
import fun.motherhack.utils.network.ChatUtils;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.command.CommandSource;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class MacroCommand extends Command {

    public MacroCommand() {
        super("macro");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder
                .then(literal("add")
                        .then(arg("name", word())
                                .then(arg("bind", word())
                                        .suggests((context, builder1) -> {
                                            for (Field field : GLFW.class.getDeclaredFields()) {
                                                String name = field.getName();
                                                if (name.startsWith("GLFW_KEY_")) {
                                                    String bind = name
                                                            .replace("GLFW_KEY_", "");
                                                    if (bind.startsWith(builder1.getRemaining())) builder1.suggest(bind);
                                                }
                                            }

                                            if (builder1.getRemaining().startsWith("NONE")) builder1.suggest("NONE");
                                            return builder1.buildFuture();
                                        })
                                        .then(arg("command", greedyString())
                                                .executes(context -> {
                                                    String name = context.getArgument("name", String.class);
                                                    String bind = context.getArgument("bind", String.class).toUpperCase();
                                                    String command = context.getArgument("command", String.class);
                                                    if (!MotherHack.getInstance().getMacroManager().getNames().contains(name)) {
                                                        try {
                                                            Macro macro = new Macro(
                                                                    name,
                                                                    command,
                                                                    new Bind(GLFW.class.getField("GLFW_KEY_" + bind).getInt(null), false)
                                                            );
                                                            MotherHack.getInstance().getMacroManager().add(macro);
                                                            ChatUtils.sendMessage(I18n.translate("commands.macro.added", name));
                                                        } catch (Exception ignored) {
                                                            return SINGLE_SUCCESS;
                                                        }
                                                    } else ChatUtils.sendMessage(I18n.translate("commands.macro.already", name));
                                                    return SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )
                )
                .then(literal("remove")
                        .then(arg("name", word())
                                .suggests((context, builder1) -> {
                                    MotherHack.getInstance().getMacroManager().getNames().stream()
                                            .filter(name -> name.startsWith(builder1.getRemaining()))
                                            .forEach(builder1::suggest);
                                    return builder1.buildFuture();
                                })
                                .executes(context -> {
                                    String name = context.getArgument("name", String.class);
                                    if (MotherHack.getInstance().getMacroManager().getMacros().isEmpty()
                                            || MotherHack.getInstance().getMacroManager().getNames().isEmpty()
                                    ) return SINGLE_SUCCESS;
                                    if (!MotherHack.getInstance().getMacroManager().getNames().contains(name)) {
                                        ChatUtils.sendMessage(I18n.translate("commands.macro.notfound", name));
                                        return SINGLE_SUCCESS;
                                    } else {
                                        MotherHack.getInstance().getMacroManager().remove(MotherHack.getInstance().getMacroManager().getMacro(name));
                                        ChatUtils.sendMessage(I18n.translate("commands.macro.removed", name));
                                    }
                                    return SINGLE_SUCCESS;
                                })
                        )
                )
                .then(literal("list")
                        .executes(context -> {
                            StringBuilder builder1 = new StringBuilder();

                            if (MotherHack.getInstance().getMacroManager().getNames().isEmpty()) ChatUtils.sendMessage(I18n.translate("commands.macro.empty"));
                            else {
                                for (int i = 0; i < MotherHack.getInstance().getMacroManager().getNames().size(); i++) {
                                    builder1.append(MotherHack.getInstance().getMacroManager().getNames().get(i));
                                    if (i < MotherHack.getInstance().getMacroManager().getNames().size() - 1) builder1.append(", ");
                                }
                                builder1.append(".");
                                ChatUtils.sendMessage(I18n.translate("commands.macro.macros") + builder1);
                            }

                            return SINGLE_SUCCESS;
                        })
                )
                .then(literal("clear")
                        .executes(context -> {
                            if (!MotherHack.getInstance().getMacroManager().isEmpty()) {
                                MotherHack.getInstance().getMacroManager().clear();
                                ChatUtils.sendMessage(I18n.translate("commands.macro.cleared"));
                            } else ChatUtils.sendMessage(I18n.translate("commands.macro.empty"));
                            return SINGLE_SUCCESS;
                        })
                );
    }
}