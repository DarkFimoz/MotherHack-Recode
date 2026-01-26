package fun.motherhack.modules.impl.movement;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.rotations.RotationChanger;
import fun.motherhack.utils.rotations.RotationUtils;
import fun.motherhack.utils.world.InventoryUtils;
import meteordevelopment.orbit.EventHandler;
import org.lwjgl.glfw.GLFW;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class Fucker extends Module {

    public enum Mode implements Nameable {
        Bed("Bed"),
        Crystal("Crystal"),
        Egg("Egg"),
        All("All"),
        Legit("Legit");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum Switch implements Nameable {
        Silent("settings.switch.silent"),
        Normal("settings.normal"),
        None("settings.none");

        private final String name;

        Switch(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.Bed);
    private final EnumSetting<Switch> autoSwitch = new EnumSetting<>("Switch", Switch.Silent, () -> mode.getValue() != Mode.Legit);
    private final NumberSetting range = new NumberSetting("Range", 4f, 1f, 6f, 0.5f, () -> mode.getValue() != Mode.Legit);
    private final NumberSetting legitRange = new NumberSetting("Legit Range", 3f, 1f, 4f, 0.5f, () -> mode.getValue() == Mode.Legit);
    private final BooleanSetting instant = new BooleanSetting("Instant", false, () -> mode.getValue() != Mode.Legit);
    private final BooleanSetting rotate = new BooleanSetting("Rotate", true, () -> mode.getValue() != Mode.Legit);
    private final BooleanSetting strictDirection = new BooleanSetting("Strict Direction", true, () -> mode.getValue() != Mode.Legit);
    private final NumberSetting delay = new NumberSetting("Delay", 100f, 0f, 500f, 10f, () -> mode.getValue() != Mode.Legit);
    private final NumberSetting rotationSpeed = new NumberSetting("Rotation Speed", 10f, 1f, 20f, 1f, () -> mode.getValue() == Mode.Legit);
    private final BooleanSetting freeLook = new BooleanSetting("FreeLook", true, () -> mode.getValue() == Mode.Legit);

    private BlockPos currentBlock = null;
    private BlockPos breakingBlock = null;
    private Entity currentEntity = null;
    private float[] currentRotations = new float[2];
    private int previousSlot = -1;
    private long lastBreakTime = 0;
    private List<BlockPos> processedBeds = new ArrayList<>();
    private boolean isBreaking = false;
    private int breakingTicks = 0;
    private Direction breakDirection = Direction.UP;
    
    // Legit mode variables
    private BlockPos legitTarget = null;
    private boolean legitBreaking = false;
    private float targetYaw = 0;
    private float targetPitch = 0;
    private boolean isFreeLookActive = false;
    
    private final RotationChanger rotationChanger = new RotationChanger(
            1000,
            () -> new Float[]{currentRotations[0], currentRotations[1]},
            () -> fullNullCheck() || (currentBlock == null && currentEntity == null)
    );

    public Fucker() {
        super("Fucker", Category.Movement);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        MotherHack.getInstance().getRotationManager().removeRotation(rotationChanger);
        processedBeds.clear();
        previousSlot = -1;
        isBreaking = false;
        breakingTicks = 0;
        breakingBlock = null;
        
        // Legit mode cleanup
        legitTarget = null;
        legitBreaking = false;
        isFreeLookActive = false;
        mc.options.attackKey.setPressed(false);
        
        // Отменяем ломание блока при выключении
        if (mc.interactionManager != null) {
            mc.interactionManager.cancelBlockBreaking();
        }
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick event) {
        if (fullNullCheck()) return;

        // Legit mode - отдельная логика
        if (mode.getValue() == Mode.Legit) {
            handleLegitMode();
            return;
        }

        // Очищаем список обработанных кроватей от несуществующих блоков
        processedBeds.removeIf(pos -> !isBed(mc.world.getBlockState(pos).getBlock()));

        // Если уже ломаем блок, продолжаем ломать
        if (isBreaking && breakingBlock != null) {
            Block block = mc.world.getBlockState(breakingBlock).getBlock();
            
            // Проверяем, сломался ли блок
            if (block == Blocks.AIR || !isTargetBlock(block) && !isBed(block)) {
                // Блок сломан, ищем следующий
                isBreaking = false;
                breakingBlock = null;
                breakingTicks = 0;
                
                // Возвращаемся на предыдущий слот
                if (autoSwitch.getValue() != Switch.None && previousSlot != -1) {
                    switchBack();
                }
                
                lastBreakTime = System.currentTimeMillis();
                return;
            }
            
            // Продолжаем ломать блок
            breakingTicks++;
            
            // Таймаут 5 секунд (100 тиков)
            if (breakingTicks > 100) {
                isBreaking = false;
                breakingBlock = null;
                breakingTicks = 0;
                if (autoSwitch.getValue() != Switch.None && previousSlot != -1) {
                    switchBack();
                }
                return;
            }
            
            // Обновляем ротацию
            if (rotate.getValue()) {
                Vec3d targetVec = breakingBlock.toCenterPos();
                currentRotations = RotationUtils.getRotations(targetVec.x, targetVec.y, targetVec.z);
                MotherHack.getInstance().getRotationManager().addRotation(rotationChanger);
            }
            
            // Продолжаем ломать
            if (instant.getValue()) {
                // Instant режим - отправляем пакеты напрямую
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                    breakingBlock,
                    breakDirection
                ));
                mc.player.swingHand(Hand.MAIN_HAND);
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                    breakingBlock,
                    breakDirection
                ));
            } else {
                // Легитимный режим - продолжаем ломать
                mc.interactionManager.updateBlockBreakingProgress(breakingBlock, breakDirection);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
            return;
        }

        // Проверяем задержку между поиском новых целей
        if (System.currentTimeMillis() - lastBreakTime < delay.getValue()) {
            return;
        }

        currentBlock = null;
        currentEntity = null;

        // Сначала ищем кристаллы (энтити)
        if (mode.getValue() == Mode.Crystal || mode.getValue() == Mode.All) {
            currentEntity = findTargetCrystal();
            if (currentEntity != null) {
                attackCrystal(currentEntity);
                lastBreakTime = System.currentTimeMillis();
                return;
            }
        }

        // Затем ищем блоки (кровати, яйца)
        if (mode.getValue() == Mode.Bed || mode.getValue() == Mode.Egg || mode.getValue() == Mode.All) {
            currentBlock = findTargetBlock();
            if (currentBlock != null) {
                // Переключаемся на лучший инструмент, если режим не None
                if (autoSwitch.getValue() != Switch.None) {
                    int toolSlot = findBestTool(currentBlock);
                    if (toolSlot != -1 && toolSlot != mc.player.getInventory().selectedSlot) {
                        previousSlot = mc.player.getInventory().selectedSlot;
                        switchToSlot(toolSlot);
                    }
                }
                
                // Начинаем ломать блок
                startBreaking(currentBlock);
            }
        }
    }

    private void startBreaking(BlockPos pos) {
        breakingBlock = pos;
        isBreaking = true;
        breakingTicks = 0;
        breakDirection = getBlockFacing(pos);
        
        // Обновляем ротацию
        if (rotate.getValue()) {
            Vec3d targetVec = pos.toCenterPos();
            currentRotations = RotationUtils.getRotations(targetVec.x, targetVec.y, targetVec.z);
            MotherHack.getInstance().getRotationManager().addRotation(rotationChanger);
        }
        
        // Начинаем ломать
        if (instant.getValue()) {
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                pos,
                breakDirection
            ));
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                pos,
                breakDirection
            ));
        } else {
            // Начинаем ломать блок
            mc.interactionManager.attackBlock(pos, breakDirection);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private Entity findTargetCrystal() {
        Entity closest = null;
        double closestDistance = range.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity)) continue;

            double distance = mc.player.getPos().distanceTo(entity.getPos());
            if (distance <= range.getValue() && distance < closestDistance) {
                closest = entity;
                closestDistance = distance;
            }
        }

        return closest;
    }

    private BlockPos findTargetBlock() {
        BlockPos playerPos = mc.player.getBlockPos();
        int rangeInt = (int) Math.ceil(range.getValue());
        BlockPos closest = null;
        double closestDistance = range.getValue() + 1;

        for (int x = -rangeInt; x <= rangeInt; x++) {
            for (int y = -rangeInt; y <= rangeInt; y++) {
                for (int z = -rangeInt; z <= rangeInt; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    
                    double distance = mc.player.getPos().distanceTo(pos.toCenterPos());
                    if (distance > range.getValue()) {
                        continue;
                    }

                    Block block = mc.world.getBlockState(pos).getBlock();
                    
                    // Для кроватей проверяем, не обрабатывали ли мы уже эту кровать
                    if (isBed(block)) {
                        BlockPos bedPos = getBedMainPos(pos);
                        if (processedBeds.contains(bedPos)) {
                            continue;
                        }
                    }
                    
                    if (isTargetBlock(block) && distance < closestDistance) {
                        closest = pos;
                        closestDistance = distance;
                    }
                }
            }
        }

        return closest;
    }

    // Получаем позицию основной части кровати (FOOT)
    private BlockPos getBedMainPos(BlockPos pos) {
        if (mc.world.getBlockState(pos).getBlock() instanceof BedBlock) {
            BedPart part = mc.world.getBlockState(pos).get(BedBlock.PART);
            if (part == BedPart.HEAD) {
                Direction facing = mc.world.getBlockState(pos).get(BedBlock.FACING);
                return pos.offset(facing.getOpposite());
            }
        }
        return pos;
    }

    private boolean isTargetBlock(Block block) {
        boolean isBed = block == Blocks.RED_BED || block == Blocks.WHITE_BED || 
                       block == Blocks.ORANGE_BED || block == Blocks.MAGENTA_BED ||
                       block == Blocks.LIGHT_BLUE_BED || block == Blocks.YELLOW_BED ||
                       block == Blocks.LIME_BED || block == Blocks.PINK_BED ||
                       block == Blocks.GRAY_BED || block == Blocks.LIGHT_GRAY_BED ||
                       block == Blocks.CYAN_BED || block == Blocks.PURPLE_BED ||
                       block == Blocks.BLUE_BED || block == Blocks.BROWN_BED ||
                       block == Blocks.GREEN_BED || block == Blocks.BLACK_BED;
        
        boolean isEgg = block == Blocks.DRAGON_EGG;

        return switch (mode.getValue()) {
            case Bed -> isBed;
            case Egg -> isEgg;
            case All -> isBed || isEgg;
            default -> false;
        };
    }

    private void attackCrystal(Entity crystal) {
        if (rotate.getValue()) {
            Vec3d targetVec = crystal.getPos().add(0, crystal.getHeight() / 2, 0);
            currentRotations = RotationUtils.getRotations(targetVec.x, targetVec.y, targetVec.z);
            MotherHack.getInstance().getRotationManager().addRotation(rotationChanger);
        }

        mc.interactionManager.attackEntity(mc.player, crystal);
        InventoryUtils.swing(InventoryUtils.Swing.MainHand);
    }

    // Получаем правильное направление для ломания блока
    private Direction getBlockFacing(BlockPos pos) {
        if (!strictDirection.getValue()) {
            return Direction.UP;
        }
        
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d blockCenter = pos.toCenterPos();
        Vec3d diff = blockCenter.subtract(playerPos);
        
        return Direction.getFacing(diff.x, diff.y, diff.z);
    }

    // Получаем позицию второй части кровати
    private BlockPos getOtherBedPart(BlockPos pos) {
        if (mc.world.getBlockState(pos).getBlock() instanceof BedBlock) {
            BedPart part = mc.world.getBlockState(pos).get(BedBlock.PART);
            Direction facing = mc.world.getBlockState(pos).get(BedBlock.FACING);
            
            if (part == BedPart.FOOT) {
                return pos.offset(facing);
            } else {
                return pos.offset(facing.getOpposite());
            }
        }
        return null;
    }

    private int findBestTool(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        
        // Для кроватей лучше всего подходит топор
        if (isBed(block)) {
            int axeSlot = InventoryUtils.findBestAxe(0, 8);
            // Если топора нет, ищем любой быстрый инструмент
            if (axeSlot == -1) {
                axeSlot = InventoryUtils.findFastItem(mc.world.getBlockState(pos), 0, 8);
            }
            return axeSlot;
        }
        
        // Для яйца дракона и других блоков ищем самый быстрый инструмент
        if (block == Blocks.DRAGON_EGG) {
            return InventoryUtils.findFastItem(mc.world.getBlockState(pos), 0, 8);
        }
        
        return InventoryUtils.findFastItem(mc.world.getBlockState(pos), 0, 8);
    }

    private boolean isBed(Block block) {
        return block == Blocks.RED_BED || block == Blocks.WHITE_BED || 
               block == Blocks.ORANGE_BED || block == Blocks.MAGENTA_BED ||
               block == Blocks.LIGHT_BLUE_BED || block == Blocks.YELLOW_BED ||
               block == Blocks.LIME_BED || block == Blocks.PINK_BED ||
               block == Blocks.GRAY_BED || block == Blocks.LIGHT_GRAY_BED ||
               block == Blocks.CYAN_BED || block == Blocks.PURPLE_BED ||
               block == Blocks.BLUE_BED || block == Blocks.BROWN_BED ||
               block == Blocks.GREEN_BED || block == Blocks.BLACK_BED;
    }

    private void switchToSlot(int slot) {
        if (slot == -1 || slot == previousSlot) return;
        
        switch (autoSwitch.getValue()) {
            case Silent -> InventoryUtils.switchSlot(InventoryUtils.Switch.Silent, slot, previousSlot);
            case Normal -> {
                mc.player.getInventory().selectedSlot = slot;
            }
            case None -> {} // Не переключаем инструмент
        }
    }

    private void switchBack() {
        if (previousSlot == -1) return;
        
        switch (autoSwitch.getValue()) {
            case Silent -> InventoryUtils.switchBack(InventoryUtils.Switch.Silent, mc.player.getInventory().selectedSlot, previousSlot);
            case Normal -> {
                mc.player.getInventory().selectedSlot = previousSlot;
            }
            case None -> {} // Не переключаем обратно
        }
        
        previousSlot = -1;
    }

    // ==================== LEGIT MODE ====================
    
    private void handleLegitMode() {
        // Проверяем, активен ли FreeLook
        if (freeLook.getValue()) {
            isFreeLookActive = isAltKeyPressed();
        } else {
            isFreeLookActive = false;
        }
        
        // Если есть текущая цель, проверяем её
        if (legitTarget != null) {
            Block block = mc.world.getBlockState(legitTarget).getBlock();
            
            // Кровать сломана
            if (!isBed(block)) {
                legitTarget = null;
                legitBreaking = false;
                mc.options.attackKey.setPressed(false);
                return;
            }
            
            // Проверяем дистанцию
            double distance = mc.player.getPos().distanceTo(legitTarget.toCenterPos());
            if (distance > legitRange.getValue()) {
                legitTarget = null;
                legitBreaking = false;
                mc.options.attackKey.setPressed(false);
                return;
            }
            
            // Плавно поворачиваем камеру к кровати (только если FreeLook не активен)
            if (!isFreeLookActive) {
                smoothRotateToBlock(legitTarget);
            }
            
            // Проверяем, смотрим ли мы на кровать (с небольшим допуском)
            if (isLookingAtBlock(legitTarget, 5f)) {
                // Зажимаем ЛКМ
                mc.options.attackKey.setPressed(true);
                legitBreaking = true;
            }
            
            return;
        }
        
        // Ищем новую кровать
        legitTarget = findLegitBed();
        
        if (legitTarget != null) {
            // Вычисляем целевые углы
            Vec3d targetVec = legitTarget.toCenterPos();
            float[] rotations = RotationUtils.getRotations(targetVec.x, targetVec.y, targetVec.z);
            targetYaw = rotations[0];
            targetPitch = rotations[1];
        }
    }
    
    private boolean isAltKeyPressed() {
        if (mc.getWindow() == null) return false;
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS ||
               GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    }
    
    private BlockPos findLegitBed() {
        BlockPos playerPos = mc.player.getBlockPos();
        int rangeInt = (int) Math.ceil(legitRange.getValue());
        BlockPos closest = null;
        double closestDistance = legitRange.getValue() + 1;

        for (int x = -rangeInt; x <= rangeInt; x++) {
            for (int y = -rangeInt; y <= rangeInt; y++) {
                for (int z = -rangeInt; z <= rangeInt; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    
                    double distance = mc.player.getPos().distanceTo(pos.toCenterPos());
                    if (distance > legitRange.getValue()) {
                        continue;
                    }

                    Block block = mc.world.getBlockState(pos).getBlock();
                    
                    if (isBed(block) && distance < closestDistance) {
                        closest = pos;
                        closestDistance = distance;
                    }
                }
            }
        }

        return closest;
    }
    
    private void smoothRotateToBlock(BlockPos pos) {
        Vec3d targetVec = pos.toCenterPos();
        float[] targetRotations = RotationUtils.getRotations(targetVec.x, targetVec.y, targetVec.z);
        targetYaw = targetRotations[0];
        targetPitch = targetRotations[1];
        
        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();
        
        // Вычисляем разницу углов
        float yawDiff = wrapAngle(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;
        
        // Скорость поворота
        float speed = rotationSpeed.getValue().floatValue();
        
        // Плавно поворачиваем
        float newYaw = currentYaw + clamp(yawDiff, -speed, speed);
        float newPitch = currentPitch + clamp(pitchDiff, -speed, speed);
        
        // Ограничиваем pitch
        newPitch = clamp(newPitch, -90f, 90f);
        
        // Применяем поворот к игроку (реальный поворот камеры)
        mc.player.setYaw(newYaw);
        mc.player.setPitch(newPitch);
    }
    
    private boolean isLookingAtBlock(BlockPos pos, float tolerance) {
        Vec3d targetVec = pos.toCenterPos();
        float[] targetRotations = RotationUtils.getRotations(targetVec.x, targetVec.y, targetVec.z);
        
        float yawDiff = Math.abs(wrapAngle(targetRotations[0] - mc.player.getYaw()));
        float pitchDiff = Math.abs(targetRotations[1] - mc.player.getPitch());
        
        return yawDiff <= tolerance && pitchDiff <= tolerance;
    }
    
    private float wrapAngle(float angle) {
        while (angle > 180f) angle -= 360f;
        while (angle < -180f) angle += 360f;
        return angle;
    }
    
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
