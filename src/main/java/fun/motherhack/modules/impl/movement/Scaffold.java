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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.block.BlockState;
import java.util.LinkedHashMap;
import java.util.Map;

public class Scaffold extends Module {
    
    private enum Mode implements fun.motherhack.modules.settings.api.Nameable {
        NCP("NCP"),
        NCPStrict("NCPStrict"),
        Eagle("Eagle"),
        CatLean("CatLean");
         
        private final String name;
         
        Mode(String name) {
            this.name = name;
        }
         
        @Override
        public String getName() {
            return name;
        }
    }

    private enum NCPRotate implements fun.motherhack.modules.settings.api.Nameable {
        Linear("Linear"),
        Packet("Packet"),
        Smooth("Smooth"),
        Normal("Normal");
        
        private final String name;
        
        NCPRotate(String name) {
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
    
    private enum CatLeanSwitch implements fun.motherhack.modules.settings.api.Nameable {
        Normal("Normal"),
        Silent("Silent"),
        Inventory("Inventory"),
        None("None");
        
        private final String name;
        
        CatLeanSwitch(String name) {
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
    
    // Common settings
    private final BooleanSetting lockY = new BooleanSetting("LockY", false);
    private final BooleanSetting autoJump = new BooleanSetting("AutoJump", false);
    private final BooleanSetting allowShift = new BooleanSetting("WorkWhileSneaking", false);
    private final BooleanSetting tower = new BooleanSetting("Tower", false);
    
    // NCP specific settings
    private final BooleanSetting safewalk = new BooleanSetting("SafeWalk", true);
    private final BooleanSetting onlyNotHoldingSpace = new BooleanSetting("OnlyNotHoldingSpace", false);
    private final EnumSetting<NCPRotate> ncpRotateMode = new EnumSetting<>("Rotate", NCPRotate.Normal);
    private final NumberSetting smoothSpeed = new NumberSetting("SmoothSpeed", 15, 5, 30, 1);
    private final BooleanSetting ncpDelay = new BooleanSetting("PlaceDelay", false);
    private final NumberSetting ncpMinDelay = new NumberSetting("MinDelay", 50, 0, 300, 10);
    private final NumberSetting ncpMaxDelay = new NumberSetting("MaxDelay", 100, 50, 500, 10);
    
    // NCPStrict specific settings
    private final NumberSetting lockYDelay = new NumberSetting("LockYDelay", 0, 0, 500, 10);
    private final BooleanSetting staticPitch = new BooleanSetting("StaticPitch", true);
    private final NumberSetting fixedPitch = new NumberSetting("FixedPitch", 80, 75, 85, 1);
    private final NumberSetting grimPitchLimit = new NumberSetting("GrimPitchLimit", 67, 30, 110, 1);
    private final BooleanSetting randomizeDelay = new BooleanSetting("RandomizeDelay", true);
    private final NumberSetting minPlaceDelay = new NumberSetting("MinPlaceDelay", 50, 0, 300, 10);
    private final NumberSetting maxPlaceDelay = new NumberSetting("MaxPlaceDelay", 100, 50, 500, 10);
    private final BooleanSetting buildTrust = new BooleanSetting("BuildTrust", true);
    private final NumberSetting trustBlocks = new NumberSetting("TrustBlocks", 3, 1, 10, 1);
    private final BooleanSetting lastMomentPlace = new BooleanSetting("LastMomentPlace", false);
    private final BooleanSetting noServerRotate = new BooleanSetting("NoServerRotate", true);
    
    // CatLean specific settings
    private final NumberSetting catLeanKeepHeight = new NumberSetting("KeepHeight", 0f, 0f, 5f, 1f);
    private final NumberSetting catLeanDelay = new NumberSetting("Delay", 1f, 0f, 10f, 1f);
    private final EnumSetting<CatLeanSwitch> catLeanSwitchMode = new EnumSetting<>("Switch", CatLeanSwitch.Normal);
    private final BooleanSetting catLeanTower = new BooleanSetting("Tower", true);
    private final BooleanSetting catLeanSafeWalk = new BooleanSetting("SafeWalk", false);
    private final BooleanSetting catLeanAllowShift = new BooleanSetting("AllowShift", true);
    private final BooleanSetting catLeanLegit = new BooleanSetting("Legit", false);
    private final BooleanSetting catLeanSaveAngles = new BooleanSetting("SaveAngles", false);
    private final BooleanSetting catLeanTelly = new BooleanSetting("Telly", false);
    private final BooleanSetting catLeanAirPlace = new BooleanSetting("AirPlace", false);
    
    // State tracking
    private final TimerUtils lockYTimer = new TimerUtils();
    private final TimerUtils timer = new TimerUtils(); // For tower logic in NCP mode
    private BlockPosWithFacing currentBlock;
    private int prevY = -999;
    private boolean wasSneaking = false;
    private BlockPosWithFacing delayedBlock;
    private float[] currentRotations = new float[2];
    private float[] lastRotations = new float[2]; // Для отслеживания изменений pitch
    private float[] targetRotationsNCP = new float[2]; // Целевые ротации для Smooth режима
    private float[] snapRotations = null;
    private int blocksPlaced = 0; // Счетчик для траста
    private int trustLevel = 0; // Уровень траста
    private long nextPlaceTime = 0; // Время следующей установки блока
    private long ncpNextPlaceTime = 0; // Время следующей установки для NCP режима
    private final java.util.Random random = new java.util.Random();
    
    // CatLean state tracking
    private int catLeanLastPlaceTick = 0;
    private double catLeanStartY = 0;
    private final RotationChanger rotationChanger = new RotationChanger(
            5000,
            () -> new Float[]{currentRotations[0], currentRotations[1]},
            () -> fullNullCheck() || currentBlock == null
    );
    
    public Scaffold() {
        super("Scaffold", Category.Movement);
        
        // Set visibility conditions for settings
        rotateMode.setVisible(() -> mode.getValue() != Mode.NCP && mode.getValue() != Mode.CatLean);
        
        lockY.setVisible(() -> mode.getValue() != Mode.Eagle && mode.getValue() != Mode.CatLean);
        autoJump.setVisible(() -> mode.getValue() != Mode.Eagle && mode.getValue() != Mode.CatLean);
        allowShift.setVisible(() -> mode.getValue() != Mode.Eagle && mode.getValue() != Mode.CatLean);
        tower.setVisible(() -> mode.getValue() != Mode.Eagle && mode.getValue() != Mode.CatLean);
        
        safewalk.setVisible(() -> mode.getValue() == Mode.NCP);
        onlyNotHoldingSpace.setVisible(() -> mode.getValue() == Mode.NCP);
        ncpRotateMode.setVisible(() -> mode.getValue() == Mode.NCP);
        smoothSpeed.setVisible(() -> mode.getValue() == Mode.NCP && ncpRotateMode.getValue() == NCPRotate.Smooth);
        ncpDelay.setVisible(() -> mode.getValue() == Mode.NCP);
        ncpMinDelay.setVisible(() -> mode.getValue() == Mode.NCP && ncpDelay.getValue());
        ncpMaxDelay.setVisible(() -> mode.getValue() == Mode.NCP && ncpDelay.getValue());
        
        lockYDelay.setVisible(() -> mode.getValue() == Mode.NCPStrict && lockY.getValue());
        staticPitch.setVisible(() -> mode.getValue() == Mode.NCPStrict && rotateMode.getValue() == Rotate.GrimMatrix);
        fixedPitch.setVisible(() -> mode.getValue() == Mode.NCPStrict && rotateMode.getValue() == Rotate.GrimMatrix && staticPitch.getValue());
        grimPitchLimit.setVisible(() -> mode.getValue() == Mode.NCPStrict && rotateMode.getValue() == Rotate.GrimMatrix && !staticPitch.getValue());
        randomizeDelay.setVisible(() -> mode.getValue() == Mode.NCPStrict);
        minPlaceDelay.setVisible(() -> mode.getValue() == Mode.NCPStrict);
        maxPlaceDelay.setVisible(() -> mode.getValue() == Mode.NCPStrict && randomizeDelay.getValue());
        buildTrust.setVisible(() -> mode.getValue() == Mode.NCPStrict);
        trustBlocks.setVisible(() -> mode.getValue() == Mode.NCPStrict && buildTrust.getValue());
        lastMomentPlace.setVisible(() -> mode.getValue() == Mode.NCPStrict);
        noServerRotate.setVisible(() -> mode.getValue() == Mode.NCPStrict);
        
        catLeanKeepHeight.setVisible(() -> mode.getValue() == Mode.CatLean);
        catLeanDelay.setVisible(() -> mode.getValue() == Mode.CatLean);
        catLeanSwitchMode.setVisible(() -> mode.getValue() == Mode.CatLean);
        catLeanTower.setVisible(() -> mode.getValue() == Mode.CatLean);
        catLeanSafeWalk.setVisible(() -> mode.getValue() == Mode.CatLean);
        catLeanAllowShift.setVisible(() -> mode.getValue() == Mode.CatLean);
        catLeanLegit.setVisible(() -> mode.getValue() == Mode.CatLean);
        catLeanSaveAngles.setVisible(() -> mode.getValue() == Mode.CatLean);
        catLeanTelly.setVisible(() -> mode.getValue() == Mode.CatLean);
        catLeanAirPlace.setVisible(() -> mode.getValue() == Mode.CatLean);
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
        ncpNextPlaceTime = 0;
        catLeanLastPlaceTick = 0;
        
        // Инициализируем lastRotations и currentRotations текущими углами игрока
        if (mc.player != null) {
            lastRotations[0] = mc.player.getYaw();
            lastRotations[1] = mc.player.getPitch();
            currentRotations[0] = mc.player.getYaw();
            currentRotations[1] = mc.player.getPitch();
            targetRotationsNCP[0] = mc.player.getYaw();
            targetRotationsNCP[1] = mc.player.getPitch();
            catLeanStartY = mc.player.getY();
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
        } else if (mode.getValue() == Mode.NCP) {
            handleNCPMode();
        } else if (mode.getValue() == Mode.NCPStrict) {
            handleNCPStrictMode();
        } else if (mode.getValue() == Mode.CatLean) {
            handleCatLeanMode();
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
    
    private void handleNCPMode() {
        // Safewalk logic for NCP mode
        if (safewalk.getValue() && mc.player.isOnGround() && !mc.player.noClip) {
            Vec3d velocity = mc.player.getVelocity();
            double x = velocity.x;
            double z = velocity.z;
            double increment = 0.05D;
            
            // Check X axis
            while (x != 0.0D && isOffsetBBEmpty(x, 0.0D)) {
                if (x < increment && x >= -increment) {
                    x = 0.0D;
                } else if (x > 0.0D) {
                    x -= increment;
                } else {
                    x += increment;
                }
            }
            
            // Check Z axis
            while (z != 0.0D && isOffsetBBEmpty(0.0D, z)) {
                if (z < increment && z >= -increment) {
                    z = 0.0D;
                } else if (z > 0.0D) {
                    z -= increment;
                } else {
                    z += increment;
                }
            }
            
            // Check both axes
            while (x != 0.0D && z != 0.0D && isOffsetBBEmpty(x, z)) {
                if (x < increment && x >= -increment) {
                    x = 0.0D;
                } else if (x > 0.0D) {
                    x -= increment;
                } else {
                    x += increment;
                }
                
                if (z < increment && z >= -increment) {
                    z = 0.0D;
                } else if (z > 0.0D) {
                    z -= increment;
                } else {
                    z += increment;
                }
            }
            
            if (x != velocity.x || z != velocity.z) {
                mc.player.setVelocity(x, velocity.y, z);
            }
        }
        
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
        
        // Calculate target position
        BlockPos targetPos = lockY.getValue() && prevY != -999 ?
            BlockPos.ofFloored(mc.player.getX(), prevY, mc.player.getZ()) :
            new BlockPos(
                (int) Math.floor(mc.player.getX()),
                (int) (Math.floor(mc.player.getY() - 1)),
                (int) Math.floor(mc.player.getZ())
            );
        
        if (!mc.world.getBlockState(targetPos).isReplaceable()) return;
        
        // Find placeable block
        currentBlock = checkNearBlocksExtended(targetPos);
        
        if (currentBlock != null) {
            // Calculate target rotations
            Vec3d hitVec = new Vec3d(
                currentBlock.position().getX() + 0.5,
                currentBlock.position().getY() + 0.5,
                currentBlock.position().getZ() + 0.5
            ).add(new Vec3d(currentBlock.facing().getUnitVector()).multiply(0.5));
            
            currentRotations = RotationUtils.getRotations(hitVec.x, hitVec.y, hitVec.z);
            
            // Apply rotation based on mode
            applyNCPRotation();
            
            // Check if we can place with delay
            if (canPlaceBlockNCP()) {
                placeBlockNCP(prevSlot);
            }
        }
    }
    
    /**
     * Применяет ротацию в зависимости от выбранного режима NCP
     */
    private void applyNCPRotation() {
        switch (ncpRotateMode.getValue()) {
            case Linear:
                // Linear rotation - uses rotation changer for smooth interpolation
                MotherHack.getInstance().getRotationManager().addRotation(rotationChanger);
                // Also set body for visual feedback
                mc.player.setBodyYaw(currentRotations[0]);
                mc.player.setHeadYaw(currentRotations[0]);
                break;
                
            case Packet:
                // Packet only - no client rotation, only server sees it
                // Don't set any client rotations, only send packet in placeBlockNCP
                break;
                
            case Smooth:
                // Smooth rotation - gradual interpolation towards target
                float yawDiff = currentRotations[0] - targetRotationsNCP[0];
                float pitchDiff = currentRotations[1] - targetRotationsNCP[1];
                
                // Normalize yaw difference to -180 to 180
                while (yawDiff > 180) yawDiff -= 360;
                while (yawDiff < -180) yawDiff += 360;
                
                float speed = smoothSpeed.getValue().floatValue();
                
                // Calculate smooth step
                float yawStep = Math.max(-speed, Math.min(speed, yawDiff));
                float pitchStep = Math.max(-speed, Math.min(speed, pitchDiff));
                
                // Apply smooth rotation
                targetRotationsNCP[0] += yawStep;
                targetRotationsNCP[1] += pitchStep;
                
                // Clamp pitch
                targetRotationsNCP[1] = Math.max(-90, Math.min(90, targetRotationsNCP[1]));
                
                // Set body rotation
                mc.player.setBodyYaw(targetRotationsNCP[0]);
                mc.player.setHeadYaw(targetRotationsNCP[0]);
                
                // Send packet rotation
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    targetRotationsNCP[0], 
                    targetRotationsNCP[1], 
                    mc.player.isOnGround(),
                    mc.player.horizontalCollision
                ));
                break;
                
            case Normal:
                // Normal rotation - instant body rotation without camera
                mc.player.setBodyYaw(currentRotations[0]);
                mc.player.setHeadYaw(currentRotations[0]);
                break;
        }
    }
    
    /**
     * Проверяет, можно ли ставить блок с учетом задержки для NCP режима
     */
    private boolean canPlaceBlockNCP() {
        if (!ncpDelay.getValue()) {
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        
        if (currentTime < ncpNextPlaceTime) {
            return false;
        }
        
        // Рандомизация задержки
        int minDelay = ncpMinDelay.getValue().intValue();
        int maxDelay = ncpMaxDelay.getValue().intValue();
        int delay = minDelay + random.nextInt(Math.max(1, maxDelay - minDelay + 1));
        ncpNextPlaceTime = currentTime + delay;
        
        return true;
    }
    
    private boolean isOffsetBBEmpty(double x, double z) {
        return !mc.world.getBlockCollisions(
            mc.player, 
            mc.player.getBoundingBox().expand(-0.1, 0, -0.1).offset(x, -2, z)
        ).iterator().hasNext();
    }
    
    private void placeBlockNCP(int prevSlot) {
        if (currentBlock == null) return;
        
        // Check if we should place (safewalk check)
        float offset = 0.2f;
        if (mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-offset, 0, -offset).offset(0, -0.5, 0)).iterator().hasNext()) {
            return;
        }
        
        // Tower logic
        if (mc.options.jumpKey.isPressed() && !isMoving() && tower.getValue()) {
            mc.player.setVelocity(0.0, 0.42, 0.0);
            if (timer.passed(1500)) {
                mc.player.setVelocity(mc.player.getVelocity().x, -0.28, mc.player.getVelocity().z);
                timer.reset();
            }
        } else {
            timer.reset();
        }
        
        // Create hit result with randomized position
        BlockHitResult hitResult = new BlockHitResult(
            new Vec3d(
                (double) currentBlock.position().getX() + Math.random(),
                currentBlock.position().getY() + 0.99f,
                (double) currentBlock.position().getZ() + Math.random()
            ),
            currentBlock.facing(),
            currentBlock.position(),
            false
        );
        
        // Send rotation packet to server (only if not Smooth mode, as Smooth sends its own packets)
        if (ncpRotateMode.getValue() != NCPRotate.Smooth) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                currentRotations[0], 
                currentRotations[1], 
                mc.player.isOnGround(),
                mc.player.horizontalCollision
            ));
        }
        
        // Handle sneaking
        boolean needSneak = needSneak(mc.world.getBlockState(hitResult.getBlockPos()).getBlock());
        if (needSneak && !mc.player.isSneaking()) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        }
        
        // Place block
        Hand hand = (prevSlot == -2) ? Hand.OFF_HAND : Hand.MAIN_HAND;
        mc.interactionManager.interactBlock(mc.player, hand, hitResult);
        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
        
        prevY = currentBlock.position().getY();
        
        if (needSneak) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }
        
        postPlace(prevSlot);
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
    
    // ============================================
    // CATLEAN MODE IMPLEMENTATION
    // ============================================
    
    private void handleCatLeanMode() {
        if (!catLeanAllowShift.getValue() && mc.player.isSneaking()) {
            return;
        }
        
        boolean isMoving = Math.abs(mc.player.getVelocity().x) > 0.01 || Math.abs(mc.player.getVelocity().z) > 0.01;
        if (!isMoving && !mc.options.jumpKey.isPressed()) {
            return;
        }
        
        if (!mc.player.isOnGround() && !catLeanTower.getValue() && !catLeanAirPlace.getValue()) {
            return;
        }
        
        int currentTick = mc.player.age;
        int ticksSinceLastPlace = currentTick - catLeanLastPlaceTick;
        if (ticksSinceLastPlace < catLeanDelay.getValue().intValue()) {
            return;
        }
        
        double keepHeightValue = catLeanKeepHeight.getValue().doubleValue();
        if (keepHeightValue > 0 && mc.player.getY() < catLeanStartY - keepHeightValue) {
            return;
        }
        
        BlockPos bestPos = findCatLeanBestPlacement();
        if (bestPos == null) {
            return;
        }
        
        if (placeCatLeanBlock(bestPos)) {
            catLeanLastPlaceTick = currentTick;
        }
        
        if (catLeanTower.getValue() && mc.options.jumpKey.isPressed() && mc.player.isOnGround()) {
            Vec3d velocity = mc.player.getVelocity();
            mc.player.setVelocity(velocity.x, 0.42, velocity.z);
        }
    }
    
    private BlockPos findCatLeanBestPlacement() {
        Vec3d playerPos = mc.player.getPos();
        BlockPos currentBlockPos = mc.player.getBlockPos();
        BlockPos posBelow = currentBlockPos.down();
        double reach = mc.player.getBlockInteractionRange();
        int rangeInt = (int)Math.ceil(reach);
        
        Map<BlockPos, Float> possiblePositions = new LinkedHashMap<>();
        
        if (catLeanSafeWalk.getValue()) {
            BlockState belowState = mc.world.getBlockState(posBelow);
            if (belowState.isAir() || belowState.isReplaceable()) {
                possiblePositions.put(posBelow, 0.0f);
            }
        }
        
        int minY = catLeanTelly.getValue() ? (int)Math.floor(catLeanStartY) : currentBlockPos.getY() - rangeInt;
        int maxY = catLeanTelly.getValue() ? (int)Math.floor(catLeanStartY) : currentBlockPos.getY() + 2;
        
        for (int y = minY; y <= maxY; y++) {
            for (int x = -rangeInt; x <= rangeInt; x++) {
                for (int z = -rangeInt; z <= rangeInt; z++) {
                    BlockPos checkPos = new BlockPos(
                        currentBlockPos.getX() + x,
                        y,
                        currentBlockPos.getZ() + z
                    );
                    
                    BlockState state = mc.world.getBlockState(checkPos);
                    if (!state.isAir() && !state.isReplaceable()) {
                        continue;
                    }
                    
                    boolean hasSupport = false;
                    Direction supportDir = null;
                    for (Direction dir : Direction.values()) {
                        BlockPos neighborPos = checkPos.offset(dir);
                        BlockState neighborState = mc.world.getBlockState(neighborPos);
                        if (!neighborState.isAir() && neighborState.isSolidBlock(mc.world, neighborPos)) {
                            hasSupport = true;
                            supportDir = dir;
                            break;
                        }
                    }
                    
                    if (!hasSupport || supportDir == null) {
                        continue;
                    }
                    
                    double distance = playerPos.squaredDistanceTo(
                        checkPos.getX() + 0.5,
                        checkPos.getY() + 0.5,
                        checkPos.getZ() + 0.5
                    );
                    
                    if (distance > reach * reach) {
                        continue;
                    }
                    
                    Vec3d hitVec = new Vec3d(
                        checkPos.getX() + 0.5 + supportDir.getOffsetX() * 0.5,
                        checkPos.getY() + 0.5 + supportDir.getOffsetY() * 0.5,
                        checkPos.getZ() + 0.5 + supportDir.getOffsetZ() * 0.5
                    );
                    
                    BlockHitResult rayTrace = mc.world.raycast(new RaycastContext(
                        mc.player.getEyePos(),
                        hitVec,
                        RaycastContext.ShapeType.OUTLINE,
                        RaycastContext.FluidHandling.NONE,
                        mc.player
                    ));
                    
                    if (rayTrace != null && !rayTrace.getBlockPos().equals(checkPos.offset(supportDir.getOpposite()))) {
                        continue;
                    }
                    
                    float yawDiff = calculateCatLeanYawDifference(checkPos);
                    possiblePositions.put(checkPos, yawDiff);
                }
            }
        }
        
        if (possiblePositions.isEmpty()) {
            return null;
        }
        
        BlockPos bestPos = null;
        float bestYaw = Float.MAX_VALUE;
        for (Map.Entry<BlockPos, Float> entry : possiblePositions.entrySet()) {
            if (entry.getValue() < bestYaw) {
                bestYaw = entry.getValue();
                bestPos = entry.getKey();
            }
        }
        
        return bestPos;
    }
    
    private boolean placeCatLeanBlock(BlockPos pos) {
        BlockState targetState = mc.world.getBlockState(pos);
        if (!targetState.isReplaceable()) {
            return false;
        }
        
        int blockSlot = findCatLeanBlockSlot();
        if (blockSlot == -1) {
            return false;
        }
        
        Direction placementSide = findCatLeanPlacementSide(pos);
        if (placementSide == null) {
            return false;
        }
        
        BlockPos placeOn = pos.offset(placementSide.getOpposite());
        int previousSlot = mc.player.getInventory().selectedSlot;
        
        // Switch slot
        if (catLeanSwitchMode.getValue() == CatLeanSwitch.Inventory) {
            mc.interactionManager.clickSlot(
                mc.player.playerScreenHandler.syncId,
                blockSlot < 9 ? blockSlot + 36 : blockSlot,
                mc.player.getInventory().selectedSlot,
                SlotActionType.SWAP,
                mc.player
            );
        } else if (catLeanSwitchMode.getValue() != CatLeanSwitch.None) {
            mc.player.getInventory().selectedSlot = blockSlot;
        }
        
        Vec3d hitVec = new Vec3d(
            placeOn.getX() + 0.5 + placementSide.getOffsetX() * 0.5,
            placeOn.getY() + 0.5 + placementSide.getOffsetY() * 0.5,
            placeOn.getZ() + 0.5 + placementSide.getOffsetZ() * 0.5
        );
        
        BlockHitResult hitResult = new BlockHitResult(hitVec, placementSide, placeOn, false);
        
        // Apply rotation
        if (!catLeanLegit.getValue()) {
            float[] rotation = calculateCatLeanRotation(hitVec);
            
            if (catLeanSwitchMode.getValue() == CatLeanSwitch.Silent) {
                // Silent rotation - send packet only, no visual rotation
                mc.player.networkHandler.sendPacket(
                    new PlayerMoveC2SPacket.LookAndOnGround(
                        rotation[0], rotation[1], mc.player.isOnGround(), mc.player.horizontalCollision
                    )
                );
            } else if (!catLeanSaveAngles.getValue()) {
                // Normal rotation - change visual angles
                mc.player.setYaw(rotation[0]);
                mc.player.setPitch(rotation[1]);
            }
        }
        
        // Place block
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        
        // Swing hand
        if (catLeanSwitchMode.getValue() == CatLeanSwitch.Silent) {
            mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        } else {
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        
        // Restore slot
        if (catLeanSwitchMode.getValue() == CatLeanSwitch.Inventory) {
            mc.interactionManager.clickSlot(
                mc.player.playerScreenHandler.syncId,
                blockSlot < 9 ? blockSlot + 36 : blockSlot,
                mc.player.getInventory().selectedSlot,
                SlotActionType.SWAP,
                mc.player
            );
        } else if (catLeanSwitchMode.getValue() != CatLeanSwitch.None) {
            mc.player.getInventory().selectedSlot = previousSlot;
        }
        
        return true;
    }
    
    private Direction findCatLeanPlacementSide(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.offset(dir);
            BlockState neighborState = mc.world.getBlockState(neighborPos);
            if (!neighborState.isAir() && neighborState.isSolidBlock(mc.world, neighborPos)) {
                return dir;
            }
        }
        return null;
    }
    
    private int findCatLeanBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                return i;
            }
        }
        return -1;
    }
    
    private float calculateCatLeanYawDifference(BlockPos pos) {
        Vec3d blockCenter = pos.toCenterPos();
        double deltaX = blockCenter.x - mc.player.getX();
        double deltaZ = blockCenter.z - mc.player.getZ();
        float targetYaw = (float)Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;
        float currentYaw = mc.player.getYaw();
        return Math.abs(MathHelper.wrapDegrees(targetYaw - currentYaw));
    }
    
    private float[] calculateCatLeanRotation(Vec3d target) {
        Vec3d eyePos = mc.player.getEyePos();
        double deltaX = target.x - eyePos.x;
        double deltaY = target.y - eyePos.y;
        double deltaZ = target.z - eyePos.z;
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yaw = (float)Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;
        float pitch = (float)-Math.toDegrees(Math.atan2(deltaY, distance));
        return new float[]{yaw, pitch};
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