package fun.motherhack.commands.impl;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import fun.motherhack.MotherHack;
import fun.motherhack.commands.Command;
import fun.motherhack.utils.network.ChatUtils;
import fun.motherhack.utils.waypoint.Waypoint;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.command.CommandSource;

public class WaypointCommand extends Command {

	public WaypointCommand() {
		super("waypoint");
	}

	@Override
	public void execute(LiteralArgumentBuilder<CommandSource> builder) {
		builder
        .then(literal("add")
                .then(arg("name", word())
                        .then(arg("X", integer())
                        		.then(arg("Z", integer())
                                		.executes(context -> {
                                			String name = context.getArgument("name", String.class);
                                			int x = context.getArgument("X", Integer.class);
                                			int z = context.getArgument("Z", Integer.class);
                                			if (!MotherHack.getInstance().getWaypointManager().getNames().contains(name)) {
                                				MotherHack.getInstance().getWaypointManager().add(new Waypoint(name, x, z));
                                				ChatUtils.sendMessage(I18n.translate("commands.waypoint.added", name));
                                			} else ChatUtils.sendMessage(I18n.translate("commands.waypoint.already", name));
                                			return SINGLE_SUCCESS;
                                		})
                                )
                        )
                )
        )
        .then(literal("remove")
                .then(arg("name", word())
                        .suggests((context, builder1) -> {
                            MotherHack.getInstance().getWaypointManager().getNames().stream()
                                    .filter(name -> name.startsWith(builder1.getRemaining()))
                                    .forEach(builder1::suggest);
                            return builder1.buildFuture();
                        })
                        .executes(context -> {
                            String name = context.getArgument("name", String.class);
                            if (MotherHack.getInstance().getWaypointManager().getWaypoints().isEmpty()
                                    || MotherHack.getInstance().getWaypointManager().getNames().isEmpty()
                            ) return SINGLE_SUCCESS;
                            if (!MotherHack.getInstance().getWaypointManager().getNames().contains(name)) {
                                ChatUtils.sendMessage(I18n.translate("commands.waypoint.notfound", name));
                                return SINGLE_SUCCESS;
                            } else {
                                MotherHack.getInstance().getWaypointManager().remove(MotherHack.getInstance().getWaypointManager().getWaypoint(name));
                                ChatUtils.sendMessage(I18n.translate("commands.waypoint.removed", name));
                            }
                            return SINGLE_SUCCESS;
                        })
                )
        )
        .then(literal("list")
                .executes(context -> {
                    StringBuilder builder1 = new StringBuilder();

                    if (MotherHack.getInstance().getWaypointManager().getNames().isEmpty()) ChatUtils.sendMessage(I18n.translate("commands.waypoint.empty"));
                    else {
                        for (int i = 0; i < MotherHack.getInstance().getWaypointManager().getNames().size(); i++) {
                            builder1.append(MotherHack.getInstance().getWaypointManager().getNames().get(i));
                            if (i < MotherHack.getInstance().getWaypointManager().getNames().size() - 1) builder1.append(", ");
                        }
                        builder1.append(".");
                        ChatUtils.sendMessage(I18n.translate("commands.waypoint.waypoints") + builder1);
                    }

                    return SINGLE_SUCCESS;
                })
        )
        .then(literal("clear")
                .executes(context -> {
                    if (!MotherHack.getInstance().getWaypointManager().isEmpty()) {
                        MotherHack.getInstance().getWaypointManager().clear();
                        ChatUtils.sendMessage(I18n.translate("commands.waypoint.cleared"));
                    } else ChatUtils.sendMessage(I18n.translate("commands.waypoint.empty"));
                    return SINGLE_SUCCESS;
                })
        );
	}
}