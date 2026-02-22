package fun.motherhack.modules.impl.misc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.api.events.impl.EventRender3D;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.ColorSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.math.TimerUtils;
import fun.motherhack.utils.render.Render3D;
import fun.motherhack.utils.world.InventoryUtils;
import fun.motherhack.utils.world.InventoryUtils.Swap;
import fun.motherhack.utils.world.InventoryUtils.Switch;

import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;

public final class FastBreak extends Module {
    
    public final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.Packet);
    public final BooleanSetting doubleMine = new BooleanSetting("DoubleMine", false, () -> mode.getValue() != Mode.GrimInstantPlus);
    private final EnumSetting<StartMode> startMode = new EnumSetting<>("StartMode", StartMode.StartAbort, () -> mode.getValue() == Mode.Packet && !doubleMine.getValue());
    private final EnumSetting<SwitchMode> switchMode = new EnumSetting<>("SwitchMode", SwitchMode.Alternative, () -> mode.getValue() != Mode.Damage);
    private final NumberSetting swapDelay = new NumberSetting("SwapDelay", 50f, 0f, 1000f, 1f, () -> mode.getValue() != Mode.Damage);
    private final NumberSetting factor = new NumberSetting("Factor", 1f, 0.5f, 2f, 0.1f, () -> mode.getValue() != Mode.Damage);
    private final NumberSetting speed = new NumberSetting("Speed", 0.5f, 0f, 1f, 0.01f, () -> mode.getValue() == Mode.Damage);
    public final NumberSetting range = new NumberSetting("Range", 4.2f, 3.0f, 10.0f, 0.1f, () -> mode.getValue() != Mode.Damage);
    private final BooleanSetting rotate = new BooleanSetting("Rotate", false, () -> mode.getValue() != Mode.Damage);
    private final BooleanSetting resetOnSwitch = new BooleanSetting("ResetOnSwitch", true, () -> mode.getValue() != Mode.Damage);
    private final NumberSetting breakAttempts = new NumberSetting("BreakAttempts", 10f, 1f, 50f, 1f, () -> mode.getValue() == Mode.Packet);
    private final NumberSetting grimPlusDelay = new NumberSetting("GrimPlus Delay", 150f, 50f, 500f, 10f, () -> mode.getValue() == Mode.GrimInstantPlus);
    private final NumberSetting grimPlusProgress = new NumberSetting("GrimPlus Progress", 0.85f, 0.5f, 0.99f, 0.01f, () -> mode.getValue() == Mode.GrimInstantPlus);
    private final BooleanSetting pauseEat = new BooleanSetting("Pause On Eat", false);
    private final BooleanSetting clientRemove = new BooleanSetting("ClientRemove", true);

    private final BooleanSetting stop = new BooleanSetting("Stop", true, () -> mode.getValue() == Mode.Packet && !doubleMine.getValue());
    private final BooleanSetting abort = new BooleanSetting("Abort", true, () -> mode.getValue() == Mode.Packet && !doubleMine.getValue());
    private final BooleanSetting start = new BooleanSetting("Start", true, () -> mode.getValue() == Mode.Packet && !doubleMine.getValue());
    private final BooleanSetting stop2 = new BooleanSetting("Stop2", true, () -> mode.getValue() == Mode.Packet && !doubleMine.getValue());

    private final BooleanSetting render = new BooleanSetting("Render", false, () -> mode.getValue() != Mode.Damage);
    private final BooleanSetting smooth = new BooleanSetting("Smooth", true, () -> mode.getValue() != Mode.Damage && render.getValue());
    private final EnumSetting<RenderMode> renderMode = new EnumSetting<>("Render Mode", RenderMode.Shrink, () -> mode.getValue() != Mode.Damage && render.getValue());
    private final ColorSetting startLineColor = new ColorSetting(new Color(255, 0, 0, 200));
    private final ColorSetting endLineColor = new ColorSetting(new Color(47, 255, 0, 200));
    private final NumberSetting lineWidth = new NumberSetting("Line Width", 2f, 1f, 10f, 1f, () -> mode.getValue() != Mode.Damage && render.getValue());
    private final ColorSetting startFillColor = new ColorSetting(new Color(255, 0, 0, 120));
    private final ColorSetting endFillColor = new ColorSetting(new Color(47, 255, 0, 120));

    public ArrayList<MineAction> actions = new ArrayList<>();

    public FastBreak() {
        super("FastBreak", Category.Misc);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        actions.forEach(MineAction::reset);
        actions.clear();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        actions.forEach(MineAction::reset);
        actions.clear();
    }

    @EventHandler
    public void onTick(EventPlayerTick event) {
        if (fullNullCheck() || mc.player.getAbilities().creativeMode)
            return;

        if (mc.player.isUsingItem() && pauseEat.getValue()) 
            return;

        if (mode.getValue() == Mode.Damage) {
            if (mc.interactionManager.isBreakingBlock()) {
                float progress = mc.interactionManager.getBlockBreakingProgress();
                if (progress < speed.getValue()) {
                    if (mc.crosshairTarget != null && 
                        mc.crosshairTarget.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
                        net.minecraft.util.hit.BlockHitResult hit = 
                            (net.minecraft.util.hit.BlockHitResult) mc.crosshairTarget;
                        for (int i = 0; i < 5; i++) {
                            mc.interactionManager.updateBlockBreakingProgress(
                                hit.getBlockPos(), 
                                hit.getSide()
                            );
                        }
                    }
                }
            }
            return;
        }

        actions.removeIf(MineAction::update);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (mode.getValue() == Mode.Damage || !render.getValue() || mc.world == null)
            return;

        actions.forEach(a -> {
            if (!mc.world.isAir(a.getPos())) {
                float noom = (float) Math.min(Math.max(interpolate(a.getPrevProgress(), a.getProgress(), event.getTickCounter().getTickDelta(false)), 0f), 1f);
                Box renderBox = switch (renderMode.getValue()) {
                    case Block -> new Box(a.getPos());
                    case Grow -> new Box(a.getPos().getX(), a.getPos().getY(), a.getPos().getZ(), 
                                        a.getPos().getX() + 1, a.getPos().getY() + noom, a.getPos().getZ() + 1);
                    case Shrink -> new Box(a.getPos().getX() + 0.5 - noom * 0.5, a.getPos().getY() + 0.5 - noom * 0.5, a.getPos().getZ() + 0.5 - noom * 0.5,
                                          a.getPos().getX() + 0.5 + noom * 0.5, a.getPos().getY() + 0.5 + noom * 0.5, a.getPos().getZ() + 0.5 + noom * 0.5);
                };

                Color fillColor = getColor(startFillColor.getColor(), endFillColor.getColor(), a.getProgress(), smooth.getValue());
                Color lineColor = getColor(startLineColor.getColor(), endLineColor.getColor(), a.getProgress(), smooth.getValue());
                
                Render3D.renderBox(event.getMatrixStack(), renderBox, fillColor);
                Render3D.renderBoxOutline(event.getMatrixStack(), renderBox, lineColor);
            }
        });
    }

    @EventHandler
    public void onAttackBlock(EventPlayerTick event) {
        if (fullNullCheck() || mc.player.getAbilities().creativeMode || mode.getValue() == Mode.Damage)
            return;

        if (mc.options.attackKey.isPressed() && mc.crosshairTarget != null && 
            mc.crosshairTarget.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            
            net.minecraft.util.hit.BlockHitResult hit = (net.minecraft.util.hit.BlockHitResult) mc.crosshairTarget;
            BlockPos pos = hit.getBlockPos();
            
            if (!canBreak(pos)) return;

            if (!alreadyActing(pos)) {
                // GrimInstant+ only allows 1 block at a time for more legit behavior
                if (mode.getValue() == Mode.GrimInstantPlus) {
                    if (!actions.isEmpty())
                        actions.removeFirst().cancel();
                    actions.add(new MineAction(pos, hit.getSide()));
                } else if (!doubleMine.getValue() || actions.size() >= 2) {
                    if (!actions.isEmpty())
                        actions.removeFirst().cancel();
                    actions.add(new MineAction(pos, hit.getSide()));
                } else {
                    actions.add(new MineAction(pos, hit.getSide()));
                }
            }
        }
    }

    public boolean alreadyActing(BlockPos blockPos) {
        return actions.stream().anyMatch(a -> a.pos.equals(blockPos));
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onPacketSend(EventPacket.Send e) {
        if (e.getPacket() instanceof UpdateSelectedSlotC2SPacket && resetOnSwitch.getValue() && 
            switchMode.getValue() != SwitchMode.Silent && mode.getValue() != Mode.GrimInstant && mode.getValue() != Mode.GrimInstantPlus)
            actions.forEach(MineAction::reset);
    }

    private void closeScreen() {
        if (mc.player == null) return;
        mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
    }

    public float getBlockStrength(@NotNull BlockState state, BlockPos position) {
        if (state == Blocks.AIR.getDefaultState())
            return 0.02f;

        float hardness = state.getHardness(mc.world, position);
        if (hardness < 0) return 0;

        return getDigSpeed(state, position) / hardness / (canBreak(position) ? 30f : 100f);
    }

    private float getDestroySpeed(BlockPos position, BlockState state) {
        float destroySpeed = 1;
        int slot = getTool(position);

        if (mc.player == null) return 0;
        if (slot != -1 && mc.player.getInventory().getStack(slot) != null && !mc.player.getInventory().getStack(slot).isEmpty()) {
            destroySpeed *= mc.player.getInventory().getStack(slot).getMiningSpeedMultiplier(state);
        }
        return destroySpeed;
    }

    public float getDigSpeed(BlockState state, BlockPos position) {
        if (mc.player == null) return 0;
        float digSpeed = getDestroySpeed(position, state);

        if (digSpeed > 1) {
            int slot = getTool(position);
            if (slot != -1) {
                ItemStack itemstack = mc.player.getInventory().getStack(slot);
                // Simplified efficiency check
                if (!itemstack.isEmpty()) {
                    digSpeed += 2.0f; // Simple bonus
                }
            }
        }

        if (mc.player.hasStatusEffect(StatusEffects.HASTE))
            digSpeed *= 1 + (Objects.requireNonNull(mc.player.getStatusEffect(StatusEffects.HASTE)).getAmplifier() + 1) * 0.2F;

        if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE))
            digSpeed *= (float) Math.pow(0.3f, Objects.requireNonNull(mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE)).getAmplifier() + 1);

        if (mc.player.isSubmergedInWater())
            digSpeed *= 0.2f;

        if (!mc.player.isOnGround())
            digSpeed /= 5;

        return digSpeed < 0 ? 0 : digSpeed * factor.getValue();
    }

    public int getTool(final BlockPos pos) {
        int index = -1;
        float currentFastest = 1.f;

        if (mc.world == null || mc.player == null || mc.world.getBlockState(pos).getBlock() instanceof AirBlock)
            return -1;

        for (int i = 9; i < 45; ++i) {
            final ItemStack stack = mc.player.getInventory().getStack(i >= 36 ? i - 36 : i);

            if (stack != ItemStack.EMPTY) {
                if (!(stack.getMaxDamage() - stack.getDamage() > 10))
                    continue;

                final float destroySpeed = stack.getMiningSpeedMultiplier(mc.world.getBlockState(pos));

                if (destroySpeed > currentFastest) {
                    currentFastest = destroySpeed;
                    index = i;
                }
            }
        }

        return index >= 36 ? index - 36 : index;
    }

    private boolean canBreak(BlockPos pos) {
        if (mc.world == null || mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()) > range.getValue() * range.getValue())
            return false;

        final BlockState blockState = mc.world.getBlockState(pos);
        final Block block = blockState.getBlock();
        return block.getHardness() != -1;
    }

    public boolean isBlockDrop(Entity ent) {
        if (ent instanceof ItemEntity && isToggled() && ent.age < 3)
            for (MineAction a : actions)
                if (a.getPos().toCenterPos().squaredDistanceTo(ent.getPos()) <= 1f)
                    return true;
        return false;
    }

    private double interpolate(double prev, double current, float delta) {
        return prev + (current - prev) * delta;
    }

    private Color getColor(Color start, Color end, float progress, boolean smooth) {
        if (!smooth) return progress > 0.5f ? end : start;
        
        progress = Math.max(0, Math.min(1, progress));
        int r = (int) (start.getRed() + (end.getRed() - start.getRed()) * progress);
        int g = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * progress);
        int b = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * progress);
        int a = (int) (start.getAlpha() + (end.getAlpha() - start.getAlpha()) * progress);
        return new Color(r, g, b, a);
    }

    public class MineAction {
        @NotNull
        private final BlockPos pos;
        private float progress, prevProgress;
        private int mineBreaks;
        private final TimerUtils attackTimer = new TimerUtils();

        public MineAction(@NotNull BlockPos pos, Direction direction) {
            this.pos = pos;
            progress = 0;
            mineBreaks = 0;
            start(direction);
        }

        public void start(Direction direction) {
            Direction startDirection = direction == null ? mc.player.getHorizontalFacing() : direction;

            if (startDirection != null) {
                // GrimInstant+ uses single packet for more legit behavior
                if (mode.getValue() == Mode.GrimInstantPlus) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, startDirection));
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, startDirection));
                } else if (doubleMine.getValue()) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, startDirection));
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, startDirection));
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, startDirection));
                } else {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, startDirection));
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(startMode.getValue() == StartMode.StartAbort ? 
                        PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK : PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, startDirection));
                }
            }
        }

        public boolean update() {
            Direction dir = getDirection(pos);

            if (mineBreaks >= breakAttempts.getValue().intValue() && mode.getValue() != Mode.GrimInstant && mode.getValue() != Mode.GrimInstantPlus)
                return true;

            if (mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()) > range.getValue() * range.getValue()) {
                cancel();
                return true;
            }

            if (mc.world.isAir(pos)) {
                progress = 0;
                prevProgress = -1;
                return false;
            }

            if (progress == 0 && prevProgress == -1 && mode.getValue() == Mode.Packet && attackTimer.passed(800)) {
                start(dir);
                mc.player.swingHand(Hand.MAIN_HAND);
            }

            int pickSlot = getTool(pos);
            int prevSlot = mc.player.getInventory().selectedSlot;

            if (pickSlot == -1)
                return false;

            boolean instant = mineBreaks > 0 && mode.getValue() == Mode.GrimInstant;
            boolean instantPlus = mineBreaks > 0 && mode.getValue() == Mode.GrimInstantPlus && 
                                 progress >= grimPlusProgress.getValue() && 
                                 attackTimer.passed(grimPlusDelay.getValue().longValue());

            if (progress >= 1 || instant || instantPlus) {
                switchTo(pickSlot, -1);

                if (mode.getValue() == Mode.GrimInstant || mode.getValue() == Mode.GrimInstantPlus || doubleMine.getValue()) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, dir));
                } else {
                    if (stop.getValue())
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, dir));
                    if (abort.getValue())
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, dir));
                    if (start.getValue())
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, dir));
                    if (stop2.getValue())
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, dir));
                }

                if (clientRemove.getValue())
                    mc.interactionManager.breakBlock(pos);

                int delay = doubleMine.getValue() ? 100 : swapDelay.getValue().intValue();

                if (delay != 0) {
                    new Thread(() -> {
                        try {
                            Thread.sleep(delay);
                            switchTo(prevSlot, pickSlot);
                        } catch (InterruptedException ignored) {}
                    }).start();
                } else {
                    switchTo(prevSlot, pickSlot);
                }

                mineBreaks++;
                
                if (mode.getValue() == Mode.GrimInstantPlus) {
                    attackTimer.reset();
                }
                
                progress = prevProgress = 0;

                if (doubleMine.getValue() && mode.getValue() == Mode.GrimInstant && actions.size() >= 2)
                    return true;
                    
                if (mode.getValue() == Mode.GrimInstantPlus && actions.size() >= 1)
                    return true;
            } else {
                prevProgress = progress;
                progress += getBlockStrength(mc.world.getBlockState(pos), pos);
            }

            return false;
        }

        private void switchTo(int slot, int from) {
            if (switchMode.getValue() == SwitchMode.Alternative || slot >= 9) {
                if (from == -1)
                    InventoryUtils.swap(Swap.Swap, slot < 9 ? slot + 36 : slot, mc.player.getInventory().selectedSlot);
                else
                    InventoryUtils.swap(Swap.Swap, from < 9 ? from + 36 : from, mc.player.getInventory().selectedSlot);
                closeScreen();
            } else if (switchMode.getValue() == SwitchMode.Silent) {
                InventoryUtils.switchSlot(Switch.Silent, slot, mc.player.getInventory().selectedSlot);
            } else {
                InventoryUtils.switchSlot(Switch.Normal, slot, mc.player.getInventory().selectedSlot);
            }
        }

        private Direction getDirection(BlockPos pos) {
            for (Direction dir : Direction.values()) {
                if (mc.world.getBlockState(pos.offset(dir)).isAir()) {
                    return dir;
                }
            }
            return mc.player.getHorizontalFacing();
        }

        public BlockPos getPos() {
            return pos;
        }

        public float getPrevProgress() {
            return prevProgress;
        }

        public float getProgress() {
            return progress;
        }

        public void reset() {
            if (progress == 0) return;

            prevProgress = progress = 0;
            Direction dir = getDirection(pos);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, dir));
            start(dir);
        }

        public void cancel() {
            if (progress != 0)
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, Direction.DOWN));
        }

        public boolean instantBreaking() {
            return mineBreaks > 0 && (mode.getValue() == Mode.GrimInstant || mode.getValue() == Mode.GrimInstantPlus);
        }
    }

    @AllArgsConstructor
    @Getter
    public enum Mode implements Nameable {
        Packet("Packet"),
        GrimInstant("GrimInstant"),
        GrimInstantPlus("GrimInstant+"),
        Damage("Damage");

        private final String name;
    }

    @AllArgsConstructor
    @Getter
    public enum RenderMode implements Nameable {
        Block("Block"),
        Shrink("Shrink"),
        Grow("Grow");

        private final String name;
    }

    @AllArgsConstructor
    @Getter
    public enum SwitchMode implements Nameable {
        Silent("Silent"),
        Normal("Normal"),
        Alternative("Alternative");

        private final String name;
    }

    @AllArgsConstructor
    @Getter
    public enum StartMode implements Nameable {
        StartAbort("StartAbort"),
        StartStop("StartStop");

        private final String name;
    }
}
