package fun.motherhack.commands.impl;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import fun.motherhack.commands.Command;
import fun.motherhack.utils.pathfinding.PathExecutor;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.BlockPos;

public class GotoCommand extends Command {

    public GotoCommand() {
        super("goto", "go");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder
            .then(arg("x", integer())
                .then(arg("y", integer())
                    .then(arg("z", integer())
                        .executes(context -> {
                            int x = context.getArgument("x", Integer.class);
                            int y = context.getArgument("y", Integer.class);
                            int z = context.getArgument("z", Integer.class);
                            PathExecutor.getInstance().startPath(new BlockPos(x, y, z));
                            return SINGLE_SUCCESS;
                        })
                    )
                )
            )
            .then(arg("x", integer())
                .then(arg("z", integer())
                    .executes(context -> {
                        int x = context.getArgument("x", Integer.class);
                        int z = context.getArgument("z", Integer.class);
                        int y = mc.player != null ? mc.player.getBlockPos().getY() : 64;
                        PathExecutor.getInstance().startPath(new BlockPos(x, y, z));
                        return SINGLE_SUCCESS;
                    })
                )
            )
            .then(literal("stop")
                .executes(context -> {
                    PathExecutor.getInstance().stop();
                    return SINGLE_SUCCESS;
                })
            );
    }
}
