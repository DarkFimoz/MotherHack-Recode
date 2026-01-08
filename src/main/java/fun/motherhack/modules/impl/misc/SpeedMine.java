package fun.motherhack.modules.impl.misc;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import fun.motherhack.api.events.impl.EventPlayerTick;
import meteordevelopment.orbit.EventHandler;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Bind;
import fun.motherhack.utils.math.TimerUtils;
import fun.motherhack.utils.world.InventoryUtils;

public class SpeedMine extends Module {

    private int previousSlot = -1;
    private int originalSlot = -1;
    private final TimerUtils switchBackTimer = new TimerUtils();

    public SpeedMine() {
        super("SpeedMine", Category.Misc);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player != null) {
            originalSlot = mc.player.getInventory().selectedSlot;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (originalSlot != -1 && originalSlot != mc.player.getInventory().selectedSlot) {
            mc.player.getInventory().selectedSlot = originalSlot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
        }
        previousSlot = -1;
        originalSlot = -1;
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick event) {
        if (mc.player == null || mc.world == null) return;

        // Check targeted block
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            net.minecraft.util.hit.BlockHitResult hit = (net.minecraft.util.hit.BlockHitResult) mc.crosshairTarget;
            BlockPos pos = hit.getBlockPos();
            if (canBreak(pos)) {
                int toolSlot = getTool(pos);
                if (toolSlot != -1 && toolSlot != mc.player.getInventory().selectedSlot) {
                    previousSlot = mc.player.getInventory().selectedSlot;
                    mc.player.getInventory().selectedSlot = toolSlot;
                    mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(toolSlot));
                    switchBackTimer.reset();
                }
            }
        } else if (previousSlot != -1 && switchBackTimer.passed(100)) {
            mc.player.getInventory().selectedSlot = previousSlot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
            previousSlot = -1;
        }
    }

    private boolean canBreak(BlockPos pos) {
        if (mc.world == null || mc.player == null) return false;
        BlockState state = mc.world.getBlockState(pos);
        return state.getBlock().getHardness() != -1 && mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) <= 36;
    }

    public int getTool(final BlockPos pos) {
        int index = -1;
        float currentFastest = 1.f;

        if (mc.world == null || mc.player == null || mc.world.getBlockState(pos).getBlock() == Blocks.AIR) return -1;

        for (int i = 0; i < 36; ++i) {
            final net.minecraft.item.ItemStack stack = mc.player.getInventory().getStack(i);

            if (stack.isEmpty()) continue;

            if (!(stack.getMaxDamage() - stack.getDamage() > 10)) continue;

            final float destroySpeed = stack.getMiningSpeedMultiplier(mc.world.getBlockState(pos));

            if (destroySpeed > currentFastest) {
                currentFastest = destroySpeed;
                index = i;
            }
        }

        return index;
    }
}
