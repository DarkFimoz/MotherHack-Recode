package fun.motherhack.modules.impl.misc;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.math.TimerUtils;
import fun.motherhack.utils.rotations.RotationChanger;
import fun.motherhack.utils.rotations.RotationUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.LeverBlock;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.function.Supplier;

public class AutoCasino extends Module {

    private final NumberSetting range = new NumberSetting("settings.autocasino.range", 4.5f, 1f, 6f, 0.1f);
    private final NumberSetting delay = new NumberSetting("settings.autocasino.delay", 100f, 0f, 1000f, 10f);

    private final TimerUtils timer = new TimerUtils();
    private BlockPos targetLever = null;
    private boolean isRotating = false;
    private RotationChanger rotationChanger = null;

    public AutoCasino() {
        super("AutoCasino", Category.Misc);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        targetLever = null;
        isRotating = false;
        rotationChanger = null;
        timer.reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (rotationChanger != null) {
            MotherHack.getInstance().getRotationManager().removeRotation(rotationChanger);
            rotationChanger = null;
        }
        targetLever = null;
        isRotating = false;
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick event) {
        if (fullNullCheck()) return;

        // Если уже поворачиваемся к рычагу
        if (isRotating && targetLever != null) {
            if (timer.passed(delay.getValue().longValue())) {
                // Проверяем, что рычаг все еще существует
                if (mc.world.getBlockState(targetLever).getBlock() instanceof LeverBlock) {
                    // Взаимодействуем с рычагом (ПКМ)
                    Vec3d hitVec = Vec3d.ofCenter(targetLever);
                    BlockHitResult hitResult = new BlockHitResult(
                        hitVec,
                        Direction.UP,
                        targetLever,
                        false
                    );
                    
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
                    mc.player.swingHand(Hand.MAIN_HAND);
                }

                // Убираем ротацию и сбрасываем состояние
                if (rotationChanger != null) {
                    MotherHack.getInstance().getRotationManager().removeRotation(rotationChanger);
                    rotationChanger = null;
                }
                isRotating = false;
                targetLever = null;
                timer.reset();
            }
            return;
        }

        // Ищем ближайший рычаг
        if (!timer.passed(delay.getValue().longValue())) return;

        BlockPos nearestLever = findNearestLever();
        if (nearestLever == null) return;

        // Начинаем поворот к рычагу
        targetLever = nearestLever;
        isRotating = true;

        // Вычисляем ротацию к рычагу
        float[] rotations = RotationUtils.getRotations(Vec3d.ofCenter(targetLever));

        // Создаем клиентский поворот (не поворачивает камеру, но поворачивает тело)
        rotationChanger = new RotationChanger(
            100, // priority
            (Supplier<Float[]>) () -> new Float[]{rotations[0], rotations[1]},
            (Supplier<Boolean>) () -> false // не удалять автоматически
        );

        MotherHack.getInstance().getRotationManager().addRotation(rotationChanger);
        timer.reset();
    }

    private BlockPos findNearestLever() {
        if (mc.player == null || mc.world == null) return null;

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        int rangeInt = (int) Math.ceil(range.getValue());

        for (int x = -rangeInt; x <= rangeInt; x++) {
            for (int y = -rangeInt; y <= rangeInt; y++) {
                for (int z = -rangeInt; z <= rangeInt; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    
                    if (!(mc.world.getBlockState(pos).getBlock() instanceof LeverBlock)) continue;

                    double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(pos));
                    if (distance > range.getValue()) continue;

                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = pos;
                    }
                }
            }
        }

        return nearest;
    }
}
