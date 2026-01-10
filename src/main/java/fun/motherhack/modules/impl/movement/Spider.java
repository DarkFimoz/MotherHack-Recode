package fun.motherhack.modules.impl.movement;

import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.api.events.impl.rotations.EventMotion;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.math.TimerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.stream.Stream;

public class Spider extends Module {
    private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.SlimeBlock);
    private final NumberSetting speed = new NumberSetting("Speed", 0.2f, 0.1f, 1f, 0.01f,
            () -> mode.getValue() == Mode.Vanilla || mode.getValue() == Mode.Matrix);
    private final NumberSetting delay = new NumberSetting("Delay", 2f, 1f, 10f, 1f,
            () -> mode.getValue() == Mode.Matrix || mode.getValue() == Mode.MatrixNew);

    private final TimerUtils stopWatch = new TimerUtils();
    private int cooldown = 0;
    
    // WaterClimb state
    private BlockPos lastWaterPos = null;
    private double lastPlaceY = 0;
    private int waterState = 0; // 0 = ready, 1 = placed water, 2 = waiting to pickup

    public Spider() {
        super("Spider", Category.Movement);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (!fullNullCheck()) {
            mc.options.jumpKey.setPressed(false);
            // Reset water climb state
            lastWaterPos = null;
            waterState = 0;
            lastPlaceY = 0;
        }
    }

    @EventHandler
    public void onMotion(EventMotion event) {
        if (fullNullCheck()) return;
        
        // SlimeBlock режим - ротация для размещения блоков
        if (mode.getValue() == Mode.SlimeBlock) {
            BlockPos blockPos = findPlacePos();
            if (!blockPos.equals(BlockPos.ORIGIN)) {
                int slotId = getBlockSlot();
                boolean offHand = mc.player.getOffHandStack().getItem() instanceof BlockItem;
                
                if (offHand || slotId != -1) {
                    Vec3d vec = blockPos.toCenterPos();
                    Direction direction = Direction.getFacing(
                        vec.x - mc.player.getX(), 
                        vec.y - mc.player.getY(), 
                        vec.z - mc.player.getZ()
                    );
                    
                    // Вычисляем углы для размещения блока
                    Vec3d targetVec = vec.subtract(new Vec3d(direction.getUnitVector()).multiply(0.1));
                    float[] rotations = getRotations(targetVec);
                    event.setYaw(rotations[0]);
                    event.setPitch(rotations[1]);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick event) {
        if (fullNullCheck()) return;

        Mode currentMode = mode.getValue();

        switch (currentMode) {
            case Vanilla -> handleVanilla();
            case Matrix -> handleMatrix();
            case MatrixNew -> handleMatrixNew();
            case FunTime -> handleFunTime();
            case SlimeBlock -> handleSlimeBlock();
            case WaterBucket -> handleWaterBucket();
            case SpookyTime -> handleSpookyTime();
            case WaterClimb -> handleWaterClimb();
        }
    }

    private void handleVanilla() {
        if (!mc.player.horizontalCollision) return;
        mc.player.setVelocity(
            mc.player.getVelocity().x, 
            speed.getValue().doubleValue(), 
            mc.player.getVelocity().z
        );
    }

    private void handleMatrix() {
        if (!mc.player.horizontalCollision) return;
        mc.player.setOnGround(mc.player.age % delay.getValue().intValue() == 0);
        mc.player.prevY -= 2.0E-232;
        
        if (mc.player.isOnGround()) {
            mc.player.setVelocity(
                mc.player.getVelocity().x, 
                speed.getValue().doubleValue() + 0.22, 
                mc.player.getVelocity().z
            );
        }
    }

    private void handleMatrixNew() {
        if (!mc.player.horizontalCollision) return;
        if (mc.options.jumpKey.isPressed() && mc.player.getVelocity().y <= -0.374) {
            mc.player.setOnGround(true);
            mc.player.setVelocity(
                mc.player.getVelocity().x, 
                0.48, 
                mc.player.getVelocity().z
            );
        }
    }

    private void handleFunTime() {
        if (mc.options.jumpKey.isPressed()) return;
        
        Box playerBox = mc.player.getBoundingBox().expand(-1e-3);
        Box box = new Box(playerBox.minX, playerBox.minY, playerBox.minZ, 
                         playerBox.maxX, playerBox.minY + 0.5, playerBox.maxZ);
        
        if (stopWatch.passed(400) && hasBoxCollision(box)) {
            box = new Box(playerBox.minX - 0.3, playerBox.minY + 1, playerBox.minZ - 0.3, 
                         playerBox.maxX, playerBox.maxY, playerBox.maxZ);
            if (hasBoxCollision(box)) {
                mc.player.setOnGround(true);
                mc.player.setVelocity(mc.player.getVelocity().x, 0.6, mc.player.getVelocity().z);
            } else {
                mc.player.setOnGround(true);
                mc.player.jump();
            }
            stopWatch.reset();
        }
    }

    // SlimeBlock - ставит любые блоки, а слаймы рядом нужны для отпрыгивания
    private void handleSlimeBlock() {
        // Проверяем есть ли слайм блок рядом (для отпрыгивания)
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos[] adjacentBlocks = {
            playerPos.east(),
            playerPos.west(),
            playerPos.north(),
            playerPos.south()
        };

        boolean hasAdjacentSlime = false;
        for (BlockPos pos : adjacentBlocks) {
            if (getBlock(pos) == Blocks.SLIME_BLOCK) {
                hasAdjacentSlime = true;
                break;
            }
        }

        // Если нет слайма рядом или не касаемся стены или падаем слишком быстро - выходим
        if (!hasAdjacentSlime || !mc.player.horizontalCollision || mc.player.getVelocity().y <= -1) {
            return;
        }

        // Ищем позицию для размещения блока
        BlockPos blockPos = findPlacePos();
        if (blockPos.equals(BlockPos.ORIGIN)) return;

        // Ищем блок в инвентаре (любой блок, не только слайм)
        boolean offHand = mc.player.getOffHandStack().getItem() instanceof BlockItem;
        int slotId = getBlockSlot();

        if (!offHand && slotId == -1) return;

        ItemStack stack = offHand ? mc.player.getOffHandStack() : mc.player.getInventory().getStack(slotId);
        Hand hand = offHand ? Hand.OFF_HAND : Hand.MAIN_HAND;

        // Проверяем можно ли поставить блок
        if (!canPlace(stack, blockPos)) return;

        // Ставим блок
        Vec3d vec = blockPos.toCenterPos();
        Direction direction = Direction.getFacing(
            vec.x - mc.player.getX(), 
            vec.y - mc.player.getY(), 
            vec.z - mc.player.getZ()
        );

        int prevSlot = mc.player.getInventory().selectedSlot;
        if (!offHand) {
            mc.player.getInventory().selectedSlot = slotId;
        }

        mc.interactionManager.interactBlock(mc.player, hand, 
            new BlockHitResult(vec, direction.getOpposite(), blockPos, false));
        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));

        if (!offHand) {
            mc.player.getInventory().selectedSlot = prevSlot;
        }

        // Даём вертикальную скорость с кулдауном
        if (cooldown >= 1) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.63, mc.player.getVelocity().z);
            cooldown = 0;
        } else {
            cooldown++;
        }
    }

    private void handleWaterBucket() {
        if (!mc.player.horizontalCollision) return;
        if (mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.player.setVelocity(mc.player.getVelocity().x, 0.3, mc.player.getVelocity().z);
        }
    }

    private void handleSpookyTime() {
        if (!mc.player.horizontalCollision) return;
        if (!stopWatch.passed(310)) return;
        
        if (mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET) {
            mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(
                Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
            mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.player.setVelocity(mc.player.getVelocity().x, 0.35, mc.player.getVelocity().z);
        }
        stopWatch.reset();
    }

    // WaterClimb - ставит воду, поднимается, забирает воду, повторяет
    private void handleWaterClimb() {
        if (!mc.player.horizontalCollision) {
            // Сброс состояния если отошли от стены
            if (waterState != 0 && lastWaterPos != null) {
                // Попробуем забрать воду если она осталась
                tryPickupWater();
            }
            waterState = 0;
            return;
        }

        int bucketSlot = findWaterBucketSlot();
        int emptyBucketSlot = findEmptyBucketSlot();

        switch (waterState) {
            case 0 -> { // Ready - ищем место и ставим воду
                if (bucketSlot == -1) return;
                
                BlockPos placePos = findWaterPlacePos();
                if (placePos == null) return;

                // Переключаем на ведро воды
                int prevSlot = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = bucketSlot;

                // Ставим воду на блок
                Direction face = getPlaceFace(placePos);
                BlockHitResult hit = new BlockHitResult(
                    Vec3d.ofCenter(placePos), 
                    face, 
                    placePos, 
                    false
                );
                
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                mc.player.swingHand(Hand.MAIN_HAND);

                lastWaterPos = placePos.offset(face);
                lastPlaceY = mc.player.getY();
                waterState = 1;
                stopWatch.reset();
                
                mc.player.getInventory().selectedSlot = prevSlot;
            }
            case 1 -> { // Placed water - ждём пока поднимемся
                // Плывём вверх в воде
                if (mc.player.isTouchingWater()) {
                    mc.player.setVelocity(mc.player.getVelocity().x, 0.4, mc.player.getVelocity().z);
                }
                
                // Если поднялись достаточно или прошло время - забираем воду
                if (mc.player.getY() > lastPlaceY + 1.5 || stopWatch.passed(800)) {
                    waterState = 2;
                    stopWatch.reset();
                }
            }
            case 2 -> { // Waiting to pickup - забираем воду
                if (emptyBucketSlot == -1) {
                    waterState = 0;
                    return;
                }

                if (tryPickupWater()) {
                    waterState = 0;
                    stopWatch.reset();
                } else if (stopWatch.passed(500)) {
                    // Таймаут - сброс
                    waterState = 0;
                }
            }
        }
    }

    private boolean tryPickupWater() {
        if (lastWaterPos == null) return false;
        
        int emptySlot = findEmptyBucketSlot();
        if (emptySlot == -1) return false;

        // Ищем воду рядом с игроком
        BlockPos waterPos = findNearbyWater();
        if (waterPos == null) return false;

        int prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = emptySlot;

        BlockHitResult hit = new BlockHitResult(
            Vec3d.ofCenter(waterPos),
            Direction.UP,
            waterPos,
            false
        );
        
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);
        
        mc.player.getInventory().selectedSlot = prevSlot;
        lastWaterPos = null;
        return true;
    }

    private BlockPos findNearbyWater() {
        BlockPos playerPos = mc.player.getBlockPos();
        
        // Проверяем позиции вокруг и под игроком
        for (int y = -2; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() == Blocks.WATER) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private BlockPos findWaterPlacePos() {
        BlockPos playerPos = mc.player.getBlockPos();
        
        // Ищем твёрдый блок рядом со стеной на который можно поставить воду
        Direction[] horizontalDirs = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        
        for (Direction dir : horizontalDirs) {
            BlockPos wallPos = playerPos.offset(dir);
            // Проверяем что это стена (твёрдый блок)
            if (mc.world.getBlockState(wallPos).isSolidBlock(mc.world, wallPos)) {
                // Проверяем блок под игроком рядом со стеной
                BlockPos belowWall = playerPos.down().offset(dir);
                if (mc.world.getBlockState(belowWall).isSolidBlock(mc.world, belowWall)) {
                    // Проверяем что место для воды свободно
                    BlockPos waterTarget = playerPos.down();
                    if (mc.world.getBlockState(waterTarget).isReplaceable()) {
                        return belowWall;
                    }
                }
            }
        }
        
        // Альтернатива - блок прямо под игроком
        BlockPos below = playerPos.down();
        if (mc.world.getBlockState(below).isSolidBlock(mc.world, below)) {
            return below;
        }
        
        return null;
    }

    private Direction getPlaceFace(BlockPos pos) {
        BlockPos playerPos = mc.player.getBlockPos();
        double dx = playerPos.getX() - pos.getX();
        double dz = playerPos.getZ() - pos.getZ();
        
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    private int findWaterBucketSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.WATER_BUCKET) {
                return i;
            }
        }
        return -1;
    }

    private int findEmptyBucketSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.BUCKET) {
                return i;
            }
        }
        return -1;
    }

    // Находит позицию для размещения блока под игроком
    private BlockPos findPlacePos() {
        BlockPos blockPos = getPlayerBlockPos();
        if (!mc.world.getBlockState(blockPos).isReplaceable()) return BlockPos.ORIGIN;
        
        return Stream.of(blockPos.west(), blockPos.east(), blockPos.south(), blockPos.north())
            .filter(pos -> !mc.world.getBlockState(pos).isReplaceable())
            .findFirst()
            .orElse(BlockPos.ORIGIN);
    }

    // Позиция блока под игроком (с небольшим смещением вниз)
    private BlockPos getPlayerBlockPos() {
        return BlockPos.ofFloored(mc.player.getPos().add(0, -0.5, 0));
    }

    private boolean canPlace(ItemStack stack, BlockPos blockPos) {
        if (blockPos.getY() >= mc.player.getBlockY()) return false;
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        
        VoxelShape shape = blockItem.getBlock().getDefaultState().getCollisionShape(mc.world, blockPos);
        if (shape.isEmpty()) return false;
        
        Box box = shape.getBoundingBox().offset(blockPos);
        return !box.intersects(mc.player.getBoundingBox());
    }

    private int getBlockSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem) {
                return i;
            }
        }
        return -1;
    }

    private Block getBlock(BlockPos blockPos) {
        return mc.world.getBlockState(blockPos).getBlock();
    }

    private boolean hasBoxCollision(Box box) {
        for (int x = (int) Math.floor(box.minX); x <= (int) Math.floor(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y <= (int) Math.floor(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z <= (int) Math.floor(box.maxZ); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private float[] getRotations(Vec3d vec) {
        double deltaX = vec.x - mc.player.getX();
        double deltaY = vec.y - mc.player.getEyeY();
        double deltaZ = vec.z - mc.player.getZ();
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float yaw = (float) (Math.atan2(deltaZ, deltaX) * (180.0 / Math.PI) - 90.0);
        float pitch = (float) (-Math.atan2(deltaY, distance) * (180.0 / Math.PI));
        return new float[]{yaw, pitch};
    }

    public enum Mode implements Nameable {
        Vanilla("Vanilla"),
        Matrix("Matrix"),
        MatrixNew("Matrix New"),
        FunTime("FunTime"),
        SlimeBlock("Slime Block"),
        WaterBucket("Water Bucket"),
        SpookyTime("SpookyTime"),
        WaterClimb("Water Climb");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
