package fun.motherhack.modules.impl.movement;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.api.events.impl.rotations.EventMotion;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.Setting;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.math.TimerUtils;
import fun.motherhack.utils.rotations.RotationChanger;
import fun.motherhack.utils.rotations.RotationUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Scaffold extends Module {
    
    private enum Mode implements fun.motherhack.modules.settings.api.Nameable {
        NCPStrict("NCPStrict"),
        Eagle("Eagle");
         
        private final String name;
         
        Mode(String name) {
            this.name = name;
        }
         
        @Override
        public String getName() {
            return name;
        }
    }

    private enum Rotate implements fun.motherhack.modules.settings.api.Nameable {
        Normal("Normal"),
        Smooth("Smooth"),
        Packet("Packet"),
        Linear("Linear"),
        None("None");
        
        private final String name;
        
        Rotate(String name) {
            this.name = name;
        }
        
        @Override
        public String getName() {
            return name;
        }
    }

    private enum Switch implements fun.motherhack.modules.settings.api.Nameable {
        Normal("Normal"),
        Silent("Silent"),
        Inventory("Inventory"),
        None("None");
        
        private final String name;
        
        Switch(String name) {
            this.name = name;
        }
        
        @Override
        public String getName() {
            return name;
        }
    }
    
    // Main settings
    private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.NCPStrict);
    private final EnumSetting<Switch> autoSwitch = new EnumSetting<>("Switch", Switch.Silent);
    private final EnumSetting<Rotate> rotateMode = new EnumSetting<>("Rotate", Rotate.Normal);
    private final BooleanSetting lockY = new BooleanSetting("LockY", false);
    private final BooleanSetting autoJump = new BooleanSetting("AutoJump", false);
    private final BooleanSetting allowShift = new BooleanSetting("WorkWhileSneaking", false);
    private final BooleanSetting tower = new BooleanSetting("Tower", true);
    private final BooleanSetting safeWalk = new BooleanSetting("SafeWalk", true);
    private final NumberSetting lockYDelay = new NumberSetting("LockYDelay", 0, 0, 500, 10);
    
    // NoServerRotate setting - можно включать и выключать
    private final BooleanSetting noServerRotate = new BooleanSetting("NoServerRotate", true);
    
    // NCPStrict specific
    private final BooleanSetting onlyNotHoldingSpace = new BooleanSetting("OnlyNotHoldingSpace", false);
    
    // State tracking
    private final TimerUtils timer = new TimerUtils();
    private final TimerUtils lockYTimer = new TimerUtils();
    private BlockPosWithFacing currentBlock;
    private int prevY = -999;
    private boolean wasSneaking = false;
    private boolean wasJumping = false;
    private boolean shouldPlaceOnLanding = false;
    private BlockPosWithFacing delayedBlock;
    private float[] currentRotations = new float[2];
    private final RotationChanger rotationChanger = new RotationChanger(
            5000,
            () -> new Float[]{currentRotations[0], currentRotations[1]},
            () -> fullNullCheck() || currentBlock == null
    );
    
    public Scaffold() {
        super("Scaffold", Category.Movement);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        prevY = -999;
        wasJumping = false;
        shouldPlaceOnLanding = false;
        delayedBlock = null;
        lockYTimer.reset();
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        if (wasSneaking && mc != null) {
            mc.options.sneakKey.setPressed(false);
            wasSneaking = false;
        }
        // Clean up rotation changer
        MotherHack.getInstance().getRotationManager().removeRotation(rotationChanger);
    }
    
    @EventHandler
    public void onPacketReceive(EventPacket.Receive e) {
        if (!noServerRotate.getValue() || fullNullCheck()) return;
        
        // Отменяем серверные пакеты, которые пытаются изменить вращение игрока
        if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
            e.cancel();
        }
    }
    
    @EventHandler
    public void onPlayerTick(EventPlayerTick event) {
        if (fullNullCheck()) return;
         
        if (mode.getValue() == Mode.Eagle) {
            handleEagleMode();
        } else if (mode.getValue() == Mode.NCPStrict) {
            handleNCPStrictMode();
        }
    }
    
    private void handleEagleMode() {
        BlockPos checkPos = BlockPos.ofFloored(
            mc.player.getX(),
            mc.player.getY() - 1.0,
            mc.player.getZ()
        );
        boolean shouldSneak = mc.world.getBlockState(checkPos).isAir();
         
        mc.options.sneakKey.setPressed(shouldSneak);
        wasSneaking = shouldSneak;
    }
    
    
    private void handleNCPStrictMode() {
        currentBlock = null;
        
        if (mc.player.isSneaking() && !allowShift.getValue()) return;
        
        int prevSlot = prePlace(false);
        if (prevSlot == -1) return;
        
        // Handle jump logic
        if (mc.options.jumpKey.isPressed() && !isMoving()) {
            prevY = (int) (Math.floor(mc.player.getY() - 1));
        }
        
        if (isMoving() && autoJump.getValue()) {
            if (mc.options.jumpKey.isPressed()) {
                if (onlyNotHoldingSpace.getValue()) {
                    prevY = (int) (Math.floor(mc.player.getY() - 1));
                }
            } else if (mc.player.isOnGround()) {
                mc.player.jump();
            }
        }
        
        // Track jump state for LockY mode
        boolean isJumping = !mc.player.isOnGround() && mc.player.getVelocity().y > 0;
        
        // Handle LockY mode placement timing
        if (lockY.getValue() && prevY != -999) {
            // Check if we should place delayed blocks
            if (delayedBlock != null) {
                if (isJumping || (lockYDelay.getValue() > 0 && lockYTimer.passed(lockYDelay.getValue().longValue()))) {
                    // Place the delayed block during jump or after delay
                    currentBlock = delayedBlock;
                    delayedBlock = null;
                    lockYTimer.reset();
                }
            }
            
            // Calculate target position for LockY
            BlockPos targetPos = BlockPos.ofFloored(mc.player.getX(), prevY, mc.player.getZ());
            
            if (!mc.world.getBlockState(targetPos).isReplaceable()) return;
            
            // Find placeable block but don't place immediately in LockY mode
            BlockPosWithFacing foundBlock = checkNearBlocksExtended(targetPos);
            
            if (foundBlock != null) {
                if (rotateMode.getValue() != Rotate.None) {
                    Vec3d hitVec = new Vec3d(
                        foundBlock.position().getX() + 0.5,
                        foundBlock.position().getY() + 0.5,
                        foundBlock.position().getZ() + 0.5
                    ).add(new Vec3d(foundBlock.facing().getUnitVector()).multiply(0.5));
                      
                    currentRotations = getRotations(hitVec);
                    
                    // Use RotationManager for smooth rotations like Aura
                    if (rotateMode.getValue() == Rotate.Packet) {
                        MotherHack.getInstance().getRotationManager().addPacketRotation(currentRotations);
                    } else if (rotateMode.getValue() == Rotate.Linear) {
                        // Linear rotation - use smooth interpolation like Normal mode
                        MotherHack.getInstance().getRotationManager().addRotation(rotationChanger);
                    } else if (rotateMode.getValue() != Rotate.None) {
                        MotherHack.getInstance().getRotationManager().addRotation(rotationChanger);
                    }
                }
                
                // In LockY mode, delay placement until jump or after configured delay
                if (isJumping || lockYDelay.getValue() == 0) {
                    currentBlock = foundBlock;
                } else {
                    // Store for delayed placement
                    delayedBlock = foundBlock;
                    if (!lockYTimer.passed(lockYDelay.getValue().longValue())) {
                        lockYTimer.reset();
                    }
                }
            }
        } else {
            // Normal mode (non-LockY)
            BlockPos targetPos = new BlockPos(
                (int) Math.floor(mc.player.getX()),
                (int) (Math.floor(mc.player.getY() - 1)),
                (int) Math.floor(mc.player.getZ())
            );
            
            if (!mc.world.getBlockState(targetPos).isReplaceable()) return;
            
            // Find placeable block
            currentBlock = checkNearBlocksExtended(targetPos);
            
            // Add rotations for NCP Strict mode
            if (currentBlock != null && rotateMode.getValue() != Rotate.None) {
                Vec3d hitVec = new Vec3d(
                    currentBlock.position().getX() + 0.5,
                    currentBlock.position().getY() + 0.5,
                    currentBlock.position().getZ() + 0.5
                ).add(new Vec3d(currentBlock.facing().getUnitVector()).multiply(0.5));
                      
                currentRotations = getRotations(hitVec);
                
                // Use RotationManager for smooth rotations
                if (rotateMode.getValue() == Rotate.Packet) {
                    MotherHack.getInstance().getRotationManager().addPacketRotation(currentRotations);
                } else if (rotateMode.getValue() == Rotate.Linear) {
                    // Linear rotation - use smooth interpolation like Normal mode
                    MotherHack.getInstance().getRotationManager().addRotation(rotationChanger);
                } else if (rotateMode.getValue() != Rotate.None) {
                    MotherHack.getInstance().getRotationManager().addRotation(rotationChanger);
                }
            }
        }
        
        // Place the block if we have one
        if (currentBlock != null) {
            placeBlock(prevSlot);
        }
    }
    
    private void placeBlock(int prevSlot) {
        if (currentBlock == null) return;
        
        float offset = 0.2f;
        if (mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-offset, 0, -offset).offset(0, -0.5, 0)).iterator().hasNext()) {
            return;
        }
        
        BlockHitResult hitResult = new BlockHitResult(
            new Vec3d(
                currentBlock.position().getX() + 0.5, 
                currentBlock.position().getY() + 0.5, 
                currentBlock.position().getZ() + 0.5
            ).add(new Vec3d(currentBlock.facing().getUnitVector()).multiply(0.5)),
            currentBlock.facing(),
            currentBlock.position(),
            false
        );
        
        // Handle tower logic
        if (mc.options.jumpKey.isPressed() && !isMoving() && tower.getValue()) {
            mc.player.setVelocity(0.0, 0.42, 0.0);
            if (timer.passed(1500)) {
                mc.player.setVelocity(mc.player.getVelocity().x, -0.28, mc.player.getVelocity().z);
                timer.reset();
            }
        } else {
            timer.reset();
        }
        
        // Place the block
        boolean needSneak = needSneak(mc.world.getBlockState(currentBlock.position()).getBlock());
        
        if (needSneak && !mc.player.isSneaking()) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        }
        
        Hand hand = (prevSlot == -2) ? Hand.OFF_HAND : Hand.MAIN_HAND;
        mc.interactionManager.interactBlock(mc.player, hand, hitResult);
        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
        
        prevY = currentBlock.position().getY();
        
        if (needSneak) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }
        
        postPlace(prevSlot);
    }
    
    private int prePlace(boolean shouldSwitch) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return -1;
        }
        
        // Check offhand
        if (mc.player.getOffHandStack().getItem() instanceof BlockItem bi && !bi.getBlock().getDefaultState().isReplaceable()) {
            return -2;
        }
        
        // Check main hand
        if (mc.player.getMainHandStack().getItem() instanceof BlockItem bi && !bi.getBlock().getDefaultState().isReplaceable()) {
            return mc.player.getInventory().selectedSlot;
        }
        
        int currentSlot = mc.player.getInventory().selectedSlot;
        
        // Find block in hotbar
        int hotbarSlot = findBlockInHotbar();
        int inventorySlot = findBlockInInventory();
        
        if (shouldSwitch) {
            if (autoSwitch.getValue() == Switch.Inventory) {
                if (inventorySlot != -1) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, inventorySlot, currentSlot, SlotActionType.SWAP, mc.player);
                    return inventorySlot;
                }
            } else if (autoSwitch.getValue() == Switch.Normal || autoSwitch.getValue() == Switch.Silent) {
                if (hotbarSlot != -1) {
                    if (autoSwitch.getValue() == Switch.Silent) {
                        mc.player.getInventory().selectedSlot = hotbarSlot;
                    }
                    return hotbarSlot;
                }
            }
        }
        
        return (hotbarSlot != -1) ? hotbarSlot : -1;
    }
    
    private void postPlace(int prevSlot) {
        if (prevSlot == -1 || prevSlot == -2) return;
        
        if (autoSwitch.getValue() == Switch.Inventory) {
            int currentSlot = mc.player.getInventory().selectedSlot;
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, prevSlot, currentSlot, SlotActionType.SWAP, mc.player);
        } else if (autoSwitch.getValue() == Switch.Silent) {
            mc.player.getInventory().selectedSlot = prevSlot;
        }
    }
    
    private boolean isMoving() {
        return mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0 || mc.options.jumpKey.isPressed();
    }
    
    private boolean isOffsetBBEmpty(double x, double z) {
        return !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-0.1, 0, -0.1).offset(x, -2, z)).iterator().hasNext();
    }
    
    private int findBlockInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != null && stack.getItem() instanceof BlockItem bi && !bi.getBlock().getDefaultState().isReplaceable()) {
                return i;
            }
        }
        return -1;
    }
    
    private int findBlockInInventory() {
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != null && stack.getItem() instanceof BlockItem bi && !bi.getBlock().getDefaultState().isReplaceable()) {
                return i;
            }
        }
        return -1;
    }
    
    private BlockPosWithFacing checkNearBlocksExtended(BlockPos blockPos) {
        BlockPosWithFacing result = checkNearBlocks(blockPos);
        if (result != null) return result;
        
        // Check surrounding positions
        BlockPos[] offsets = {
            new BlockPos(-1, 0, 0), new BlockPos(1, 0, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1),
            new BlockPos(-2, 0, 0), new BlockPos(2, 0, 0),
            new BlockPos(0, 0, 2), new BlockPos(0, 0, -2),
            new BlockPos(0, -1, 0), new BlockPos(1, -1, 0),
            new BlockPos(-1, -1, 0), new BlockPos(0, -1, 1),
            new BlockPos(0, -1, -1)
        };
        
        for (BlockPos offset : offsets) {
            result = checkNearBlocks(blockPos.add(offset));
            if (result != null) return result;
        }
        
        return null;
    }
    
    
    private BlockPosWithFacing checkNearBlocks(BlockPos blockPos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = blockPos.offset(direction);
            if (!mc.world.getBlockState(neighbor).isAir()) {
                return new BlockPosWithFacing(neighbor, direction.getOpposite());
            }
        }
        return null;
    }
    
    private boolean needSneak(net.minecraft.block.Block block) {
        // Simplified sneak check - some blocks require sneaking to place
        return false; // Can be expanded for specific blocks
    }
    
    private float[] getRotations(Vec3d vec) {
        return RotationUtils.getRotations(vec.x, vec.y, vec.z);
    }
    
    public String getSuffix() {
        return mode.getValue().name();
    }
    
    // Helper class for block position with facing
    private static class BlockPosWithFacing {
        private final BlockPos position;
        private final Direction facing;
        
        public BlockPosWithFacing(BlockPos position, Direction facing) {
            this.position = position;
            this.facing = facing;
        }
        
        public BlockPos position() {
            return position;
        }
        
        public Direction facing() {
            return facing;
        }
    }
}