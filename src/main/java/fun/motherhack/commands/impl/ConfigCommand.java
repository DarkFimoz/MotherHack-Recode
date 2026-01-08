package fun.motherhack.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fun.motherhack.MotherHack;
import fun.motherhack.commands.Command;
import fun.motherhack.utils.network.ChatUtils;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.command.CommandSource;

import java.awt.Desktop;
import java.io.File;
import java.util.Arrays;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class ConfigCommand extends Command {

    public ConfigCommand() {
        super("config", "cfg");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder
                .then(literal("save")
                        .then(arg("config", word())
                                .suggests((context, builder1) -> {
                                    if (MotherHack.getInstance().getConfigsDir().exists() && MotherHack.getInstance().getConfigsDir().isDirectory()) {
                                        File[] files = MotherHack.getInstance().getConfigsDir().listFiles((dir, name) -> name.toLowerCase().endsWith(".mhack"));

                                        if (files != null) {
                                            Arrays.stream(files)
                                                    .map(File::getName)
                                                    .map(name -> name.replace(".mhack", ""))
                                                    .forEach(builder1::suggest);
                                        }
                                    }

                                    return builder1.buildFuture();
                                })
                                .executes(context -> {
                                    String config = context.getArgument("config", String.class);
                                    try {
                                        MotherHack.getInstance().getConfigManager().saveConfig(config);
                                        ChatUtils.sendMessage(I18n.translate("commands.config.save", config));
                                    } catch (Exception e) {
                                        System.err.println("Failed to save config: " + e.getMessage());
                                        ChatUtils.sendMessage("§cError saving config: " + e.getMessage());
                                    }

                                    return SINGLE_SUCCESS;
                                })
                        )
                )
                .then(literal("load")
                        .then(arg("config", word())
                                .suggests((context, builder1) -> {
                                     if (MotherHack.getInstance().getConfigsDir().exists() && MotherHack.getInstance().getConfigsDir().isDirectory()) {
                                         File[] files = MotherHack.getInstance().getConfigsDir().listFiles((dir, name) -> name.toLowerCase().endsWith(".mhack"));

                                         if (files != null) {
                                             Arrays.stream(files)
                                                     .map(File::getName)
                                                     .map(name -> name.replace(".mhack", ""))
                                                     .forEach(builder1::suggest);
                                         }
                                     }

                                     return builder1.buildFuture();
                                 })
                                .executes(context -> {
                                    String config = context.getArgument("config", String.class);
                                    try {
                                        MotherHack.getInstance().getConfigManager().loadConfig(config);
                                        ChatUtils.sendMessage(I18n.translate("commands.config.load", config));
                                    } catch (Exception e) {
                                        System.err.println("Failed to load config: " + e.getMessage());
                                        ChatUtils.sendMessage("§cError loading config: " + e.getMessage());
                                    }

                                    return SINGLE_SUCCESS;
                                })
                        )
                )
                .then(literal("list")
                        .executes(context -> {
                            StringBuilder builder1 = new StringBuilder();
                            File[] files = MotherHack.getInstance().getConfigsDir().listFiles((dir, name) -> name.toLowerCase().endsWith(".mhack"));

                            if (files == null || files.length == 0) ChatUtils.sendMessage(I18n.translate("commands.config.empty"));
                            else {
                                for (int i = 0; i < files.length; i++) {
                                    String fileName = files[i].getName().replace(".mhack", "");
                                    builder1.append(fileName);
                                    if (i < files.length - 1) builder1.append(", ");
                                }

                                builder1.append(".");
                                ChatUtils.sendMessage(I18n.translate("commands.config.list") + builder1);
                            }

                            return SINGLE_SUCCESS;
                        })
                )
                .then(literal("openfolder")
                        .executes(context -> {
                            try {
                                Desktop.getDesktop().open(MotherHack.getInstance().getConfigsDir());
                                ChatUtils.sendMessage("Opened configs folder");
                            } catch (Exception e) {
                                ChatUtils.sendMessage("§cFailed to open configs folder: " + e.getMessage());
                            }

                            return SINGLE_SUCCESS;
                        })
                );
    }
}