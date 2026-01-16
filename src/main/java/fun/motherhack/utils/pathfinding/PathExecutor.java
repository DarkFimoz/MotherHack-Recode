package fun.motherhack.utils.pathfinding;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.utils.Wrapper;
import fun.motherhack.utils.network.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class PathExecutor implements Wrapper {

    private static PathExecutor INSTANCE;
    private List<BlockPos> currentPath;
    private int currentIndex;
    private boolean active;
    private BlockPos targetPos;
    private int stuckTicks;
    private Vec3d lastPos;

    public static PathExecutor getInstance() {
        if (INSTANCE == null) INSTANCE = new PathExecutor();
        return INSTANCE;
    }

    public void startPath(BlockPos goal) {
        if (mc.player == null || mc.world == null) return;

        BlockPos start = mc.player.getBlockPos();
        targetPos = goal;
        currentPath = PathFinder.findPath(start, goal);

        if (currentPath.isEmpty()) {
            ChatUtils.sendMessage("§cНе удалось найти путь до " + goal.toShortString());
            return;
        }

        currentIndex = 0;
        active = true;
        stuckTicks = 0;
        lastPos = mc.player.getPos();
        
        MotherHack.getInstance().getEventHandler().subscribe(this);
        ChatUtils.sendMessage("§aИду к " + goal.toShortString() + " (" + currentPath.size() + " точек)");
    }

    public void stop() {
        if (active) {
            active = false;
            currentPath = null;
            releaseKeys();
            MotherHack.getInstance().getEventHandler().unsubscribe(this);
            ChatUtils.sendMessage("§eПуть остановлен");
        }
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (!active || mc.player == null || currentPath == null) return;

        if (currentIndex >= currentPath.size()) {
            ChatUtils.sendMessage("§aПрибыл!");
            stop();
            return;
        }

        // Проверка застревания
        Vec3d currentPos = mc.player.getPos();
        if (lastPos != null && lastPos.squaredDistanceTo(currentPos) < 0.01) {
            stuckTicks++;
            if (stuckTicks > 40) {
                recalculatePath();
                stuckTicks = 0;
            }
        } else {
            stuckTicks = 0;
        }
        lastPos = currentPos;

        BlockPos target = currentPath.get(currentIndex);
        double distXZ = Math.sqrt(Math.pow(target.getX() + 0.5 - currentPos.x, 2) + 
                                   Math.pow(target.getZ() + 0.5 - currentPos.z, 2));

        if (distXZ < 0.5) {
            currentIndex++;
            return;
        }

        lookAt(target);
        move();
    }


    private void recalculatePath() {
        if (targetPos == null) return;
        BlockPos start = mc.player.getBlockPos();
        currentPath = PathFinder.findPath(start, targetPos);
        currentIndex = 0;
        if (currentPath.isEmpty()) {
            ChatUtils.sendMessage("§cНе могу найти путь, останавливаюсь");
            stop();
        }
    }

    private void lookAt(BlockPos target) {
        Vec3d eyePos = mc.player.getEyePos();
        double dx = target.getX() + 0.5 - eyePos.x;
        double dz = target.getZ() + 0.5 - eyePos.z;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        mc.player.setYaw(yaw);
    }

    private void move() {
        mc.options.forwardKey.setPressed(true);
        mc.options.sprintKey.setPressed(true);
        mc.options.jumpKey.setPressed(true); // автопрыжок всегда включён
    }

    private void releaseKeys() {
        mc.options.forwardKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
    }

    public boolean isActive() {
        return active;
    }

    public BlockPos getTargetPos() {
        return targetPos;
    }
}
