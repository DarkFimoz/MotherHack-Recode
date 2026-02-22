package fun.motherhack.modules.impl.movement;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
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
        Snap("Snap"),
        GrimMatrix("Grim+Matrix"),
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
    private final EnumSetting<Rotate> rotateMode = new EnumSetting<>("Rotate", Rotate.GrimMatrix);
    private final BooleanSetting lockY = new BooleanSetting("LockY", false);
    private final BooleanSetting autoJump = new BooleanSetting("AutoJump", false);
    private final BooleanSetting allowShift = new BooleanSetting("WorkWhileSneaking", false);
    private final BooleanSetting tower = new BooleanSetting("Tower", false); // Выключен по умолчанию для Grim
    private final NumberSetting lockYDelay = new NumberSetting("LockYDelay", 0, 0, 500, 10);
    
    // Grim+Matrix specific settings
    private final BooleanSetting staticPitch = new BooleanSetting("StaticPitch", true); // Для Matrix
    private final NumberSetting fixedPitch = new NumberSetting("FixedPitch", 80, 75, 85, 1); // Фиксированный pitch для Matrix
    private final NumberSetting grimPitchLimit = new NumberSetting("GrimPitchLimit", 67, 30, 110, 1); // Лимит Grim (67.246 оптимально)
    private final BooleanSetting randomizeDelay = new BooleanSetting("RandomizeDelay", true);
    private final NumberSetting minPlaceDelay = new NumberSetting("MinPlaceDelay", 50, 0, 300, 10);
    private final NumberSetting maxPlaceDelay = new NumberSetting("MaxPlaceDelay", 100, 50, 500, 10);
    private final BooleanSetting buildTrust = new BooleanSetting("BuildTrust", true); // Накопление траста
    private final NumberSetting trustBlocks = new NumberSetting("TrustBlocks", 3, 1, 10, 1); // Сколько блоков ставить легитно
    private final BooleanSetting lastMomentPlace = new BooleanSetting("LastMomentPlace", false); // Выключен по умолчанию
    
    // NoServerRotate setting
    private final BooleanSetting noServerRotate = new BooleanSetting("NoServerRotate", true);
    
    // NCPStrict specific
    private final BooleanSetting onlyNotHoldingSpace = new BooleanSetting("OnlyNotHoldingSpace", false);
    
    // State tracking
    private final TimerUtils lockYTimer = new TimerUtils();
    private BlockPosWithFacing currentBlock;
    private int prevY = -999;
    private boolean wasSneaking = false;
    private BlockPosWithFacing delayedBlock;
    private float[] currentRotations = new float[2];
    private float[] lastRotations = new float[2]; // Для отслеживания изменений pitch
    private float[] snapRotations = null;
    private int blocksPlaced = 0; // Счетчик для траста
    private int trustLevel = 0; // Уровень траста
    private long nextPlaceTime = 0; // Время следующей установки блока
    private final java.util.Random random = new java.util.Random();
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
        delayedBlock = null;
        snapRotations = null;
        lockYTimer.reset();
        blocksPlaced = 0;
        trustLevel = 0;
        nextPlaceTime = 0;
        
        // Инициализируем lastRotations текущими углами игрока
        if (mc.player != null) {
            lastRotations[0] = mc.player.getYaw();
            lastRotations[1] = mc.player.getPitch();
        }
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
    public void onPacketSend(EventPacket.Send e) {
        if (fullNullCheck() || snapRotations == null) return;
        
        // Inject snap rotations into movement packets without affecting camera
        if (e.getPacket() instanceof PlayerMoveC2SPacket packet) {
            if (packet instanceof PlayerMoveC2SPacket.Full) {
                e.setPacket(new PlayerMoveC2SPacket.Full(
                    packet.getX(0), packet.getY(0), packet.getZ(0),
                    snapRotations[0], snapRotations[1], 
                    packet.isOnGround(), mc.player.horizontalCollision
                ));
            } else if (packet instanceof PlayerMoveC2SPacket.LookAndOnGround) {
                e.setPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    snapRotations[0], snapRotations[1], 
                    packet.isOnGround(), mc.player.horizontalCollision
                ));
            }
            snapRotations = null; // Reset after sending
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
                      
                    currentRotations = getRotationsWithGrimMatrix(hitVec);
                    applyRotation();
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
                      
                currentRotations = getRotationsWithGrimMatrix(hitVec);
                applyRotation();
            }
        }
        
        // Place the block if we have one
        if (currentBlock != null) {
            // Проверка траста - первые N блоков ставим легитно
            if (buildTrust.getValue() && blocksPlaced < trustBlocks.getValue().intValue()) {
                // Легитный режим - минимальная задержка
                if (canPlaceBlock()) {
                    placeBlock(prevSlot);
                    blocksPlaced++;
                }
            } else {
                // Полный функционал после накопления траста
                if (trustLevel < 40 && buildTrust.getValue()) {
                    trustLevel++;
                }
                
                // Last moment placement - ставим только когда вот-вот упадем
                if (lastMomentPlace.getValue()) {
                    if (shouldPlaceLastMoment() && canPlaceBlock()) {
                        placeBlock(prevSlot);
                        blocksPlaced++;
                    }
                } else {
                    if (canPlaceBlock()) {
                        placeBlock(prevSlot);
                        blocksPlaced++;
                    }
                }
            }
        }
    }
    
    /**
     * Проверяет, нужно ли ставить блок в последний момент (для обхода Grim)
     */
    private boolean shouldPlaceLastMoment() {
        if (mc.player.isOnGround()) return true;
        
        // Проверяем, близко ли мы к краю блока
        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();
        double playerY = mc.player.getY();
        
        // Расстояние от центра блока
        double offsetX = Math.abs(playerX - Math.floor(playerX) - 0.5);
        double offsetZ = Math.abs(playerZ - Math.floor(playerZ) - 0.5);
        
        // Если близко к краю (больше 0.3 от центра), ставим сразу
        if (offsetX > 0.3 || offsetZ > 0.3) {
            return true;
        }
        
        // Рассчитываем расстояние до падения
        BlockPos targetBlock = new BlockPos(
            (int) Math.floor(playerX),
            (int) (Math.floor(playerY - 1)),
            (int) Math.floor(playerZ)
        );
        
        double fallDistance = playerY - targetBlock.getY() - 1;
        double verticalSpeed = mc.player.getVelocity().y;
        
        if (verticalSpeed < 0) {
            // Рассчитываем тики до падения
            int ticksUntilFall = (int)(fallDistance / Math.abs(verticalSpeed));
            
            // Увеличиваем окно до 3 тиков для более надежной установки
            return ticksUntilFall <= 3;
        }
        
        return false;
    }
    
    /**
     * Проверяет, можно ли ставить блок с учетом рандомизированной задержки
     */
    private boolean canPlaceBlock() {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime < nextPlaceTime) {
            return false;
        }
        
        // Рандомизация задержки для обхода паттерн-детекта
        if (randomizeDelay.getValue()) {
            int minDelay = minPlaceDelay.getValue().intValue();
            int maxDelay = maxPlaceDelay.getValue().intValue();
            int delay = minDelay + random.nextInt(maxDelay - minDelay + 1);
            nextPlaceTime = currentTime + delay;
        } else {
            nextPlaceTime = currentTime + minPlaceDelay.getValue().longValue();
        }
        
        return true;
    }
    
    /**
     * Получает ротацию с учетом Grim+Matrix обхода
     */
    private float[] getRotationsWithGrimMatrix(Vec3d vec) {
        float[] targetRotations = RotationUtils.getRotations(vec.x, vec.y, vec.z);
        
        if (rotateMode.getValue() != Rotate.GrimMatrix) {
            return targetRotations;
        }
        
        float yaw = targetRotations[0];
        float pitch = targetRotations[1];
        
        // Matrix байпас - статичный pitch
        if (staticPitch.getValue()) {
            pitch = fixedPitch.getValue().floatValue();
        } else {
            // Grim байпас - лимитируем скорость изменения pitch
            float pitchDiff = pitch - lastRotations[1];
            float maxPitchChange = grimPitchLimit.getValue().floatValue();
            
            if (Math.abs(pitchDiff) > maxPitchChange) {
                pitch = lastRotations[1] + (pitchDiff > 0 ? maxPitchChange : -maxPitchChange);
            }
        }
        
        // Сохраняем текущие ротации для следующего тика
        lastRotations[0] = yaw;
        lastRotations[1] = pitch;
        
        return new float[]{yaw, pitch};
    }
    
    /**
     * Применяет ротацию в зависимости от выбранного режима
     */
    private void applyRotation() {
        if (rotateMode.getValue() == Rotate.Packet) {
            MotherHack.getInstance().getRotationManager().addPacketRotation(currentRotations);
        } else if (rotateMode.getValue() == Rotate.Snap) {
            snapRotations = currentRotations.clone();
            mc.player.setBodyYaw(currentRotations[0]);
            mc.player.setHeadYaw(currentRotations[0]);
        } else if (rotateMode.getValue() == Rotate.Linear) {
            MotherHack.getInstance().getRotationManager().addRotation(rotationChanger);
        } else if (rotateMode.getValue() == Rotate.GrimMatrix) {
            // Для Grim+Matrix используем packet rotation
            MotherHack.getInstance().getRotationManager().addPacketRotation(currentRotations);
        } else if (rotateMode.getValue() != Rotate.None) {
            MotherHack.getInstance().getRotationManager().addRotation(rotationChanger);
        }
    }
    
    private void placeBlock(int prevSlot) {
        if (currentBlock == null) return;
        
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
        
        // Handle tower logic - только если включен и не в режиме траста
        if (mc.options.jumpKey.isPressed() && !isMoving() && tower.getValue()) {
            if (mc.player.isOnGround()) {
                mc.player.jump();
            }
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