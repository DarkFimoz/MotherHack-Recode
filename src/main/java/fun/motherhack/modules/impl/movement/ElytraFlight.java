package fun.motherhack.modules.impl.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.utils.math.TimerUtils;
// Утилиты для движения

public class ElytraFlight extends Module {
    public ElytraFlight() {
        super("ElytraFlight", Category.Movement);
    }

    public final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.Boost);
    private final BooleanSetting twoBee = new BooleanSetting("2b2t", false);
    private final BooleanSetting onlySpace = new BooleanSetting("OnlySpace", true, () -> twoBee.getValue());
    private final BooleanSetting cruiseControl = new BooleanSetting("CruiseControl", false);
    private final NumberSetting factor = new NumberSetting("Factor", 1.5f, 0.1f, 50.0f, 0.1f);
    private final NumberSetting upSpeed = new NumberSetting("UpSpeed", 1.0f, 0.01f, 5.0f, 0.01f, () -> !twoBee.getValue());
    private final NumberSetting downFactor = new NumberSetting("Glide", 1.0f, 0.0f, 2.0f, 0.1f, () -> !twoBee.getValue());
    private final BooleanSetting stopMotion = new BooleanSetting("StopMotion", true, () -> !twoBee.getValue());
    private final NumberSetting minUpSpeed = new NumberSetting("MinUpSpeed", 0.5f, 0.1f, 5.0f, 0.1f, () -> cruiseControl.getValue());
    private final BooleanSetting forceHeight = new BooleanSetting("ForceHeight", false, () -> cruiseControl.getValue());
    private final NumberSetting manualHeight = new NumberSetting("Height", 121f, 1f, 256f, 1f, () -> forceHeight.getValue());
    private final BooleanSetting speedLimit = new BooleanSetting("SpeedLimit", true);
    private final NumberSetting maxSpeed = new NumberSetting("MaxSpeed", 2.5f, 0.1f, 10.0f, 0.1f);
    private final NumberSetting redeployInterval = new NumberSetting("RedeployInterval", 1F, 0.1F, 5F, 0.1f, () -> !twoBee.getValue());
    private final NumberSetting redeployTimeOut = new NumberSetting("RedeployTimeout", 5f, 0.1f, 20f, 0.1f, () -> !twoBee.getValue());
    private final NumberSetting redeployDelay = new NumberSetting("RedeployDelay", 0.5F, 0.1F, 1F, 0.1f, () -> !twoBee.getValue());
    
    // Extreme Boost settings
    private final NumberSetting extremeSpeed = new NumberSetting("ExtremeSpeed", 3.5f, 1.0f, 10.0f, 0.1f, () -> mode.getValue() == Mode.ExtremeBoost);
    private final NumberSetting extremeAcceleration = new NumberSetting("Acceleration", 0.05f, 0.01f, 0.2f, 0.01f, () -> mode.getValue() == Mode.ExtremeBoost);
    private final BooleanSetting smoothAcceleration = new BooleanSetting("SmoothAccel", true, () -> mode.getValue() == Mode.ExtremeBoost);
    private final NumberSetting verticalBoost = new NumberSetting("VerticalBoost", 0.02f, 0.0f, 0.1f, 0.01f, () -> mode.getValue() == Mode.ExtremeBoost);
    
    // Liquid settings
    private final NumberSetting liquidVerticalSpeed = new NumberSetting("LiquidVertical", 0.5f, 0.0f, 5.0f, 0.1f, () -> mode.getValue() == Mode.Liquid);
    private final NumberSetting liquidHorizontalSpeed = new NumberSetting("LiquidHorizontal", 1.0f, 0.0f, 8.0f, 0.1f, () -> mode.getValue() == Mode.Liquid);
    private final BooleanSetting liquidDurabilityExploit = new BooleanSetting("DurabilityExploit", false, () -> mode.getValue() == Mode.Liquid);

    public enum Mode implements Nameable { 
        Boost, ExtremeBoost, Liquid;

        @Override
        public String getName() {
            return name();
        }
    }

    private final TimerUtils startTimer = new TimerUtils();
    private final TimerUtils redeployTimer = new TimerUtils();
    private boolean hasTouchedGround;

    @Override
    public void onEnable() {
        super.onEnable();
        hasTouchedGround = false;
    }

    @EventHandler
    public void onTick(EventPlayerTick e) {
        if (mc.player == null) return;
        
        // Выбираем режим работы
        if (mode.getValue() == Mode.ExtremeBoost) {
            doExtremeBoost();
        } else if (mode.getValue() == Mode.Liquid) {
            doLiquid();
        } else {
            // For now, just run both methods since we don't have phase separation
            doPreLegacy();
            doBoost();
        }
        
        // Инициализируем таймер при первом вызове
        if (redeployTimer.getElapsed() == 0) {
            redeployTimer.reset();
        }
    }

    private void doPreLegacy() {
        if (twoBee.getValue() || mc.player == null) return;
        
        // Проверяем, есть ли у игрока элитры
        if (!mc.player.getInventory().getArmorStack(2).isOf(Items.ELYTRA)) {
            hasTouchedGround = false;
            return;
        }
        
        // Проверяем, не сломаны ли элитры
        if (mc.player.getInventory().getArmorStack(2).getMaxDamage() - mc.player.getInventory().getArmorStack(2).getDamage() < 2) {
            return;
        }
        
        if (mc.player.isOnGround()) {
            hasTouchedGround = true;
        }
        
        // Улучшенная логика респавна элитр с защитой от спама
        if (!mc.player.isGliding()) {
            if (hasTouchedGround && !mc.player.isOnGround() && mc.player.fallDistance > 0.5f) {
                if (startTimer.passed((long)(1000 * redeployDelay.getValue()))) {
                    startTimer.reset();
                    // Проверяем, что игрок не в воде и не в лаве
                    if (!mc.player.isTouchingWater() && !mc.player.isInLava()) {
                        sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                        hasTouchedGround = false;
                    }
                }
            }
        } else {
            startTimer.reset();
        }
    }

    private void doBoost() {
        if (mc.player == null || !mc.player.isGliding() || mc.player.isTouchingWater() || mc.player.isInLava()) {
            return;
        }
        
        // Проверяем, есть ли у игрока элитры и не сломаны ли они
        if (!mc.player.getInventory().getArmorStack(2).isOf(Items.ELYTRA) || 
            mc.player.getInventory().getArmorStack(2).getMaxDamage() - mc.player.getInventory().getArmorStack(2).getDamage() < 2) {
            return;
        }

        float moveForward = mc.player.input.movementForward;

        if (cruiseControl.getValue()) {
            float height = (float) mc.player.getY();
            if (mc.options.jumpKey.isPressed()) height++;
            else if (mc.options.sneakKey.isPressed()) height--;
            if (forceHeight.getValue()) height = manualHeight.getValue();

            if (twoBee.getValue()) {
                if (getCurrentPlayerSpeed() >= minUpSpeed.getValue())
                    mc.player.setPitch((float) MathHelper.clamp(Math.toDegrees(Math.atan2((height - mc.player.getY()) * -1.0, 10)), -50, 50));
                else
                    mc.player.setPitch(0.25F);
            } else {
                double heightPct = 1 - Math.sqrt(MathHelper.clamp(getCurrentPlayerSpeed() / 1.7, 0.0, 1.0));
                if (getCurrentPlayerSpeed() >= minUpSpeed.getValue() && startTimer.passed((long) (2000 * redeployInterval.getValue()))) {
                    double pitch = -(44.4 * heightPct + 0.6);
                    double diff = (height + 1 - mc.player.getY()) * 2;
                    double pDist = -Math.toDegrees(Math.atan2(Math.abs(diff), getCurrentPlayerSpeed() * 30.0)) * Math.signum(diff);
                    mc.player.setPitch((float) (pitch + (pDist - pitch) * MathHelper.clamp(Math.abs(diff), 0.0, 1.0)));
                } else {
                    mc.player.setPitch(0.25F);
                    moveForward = 1;
                }
            }
        }

        if (twoBee.getValue()) {
            if ((mc.options.jumpKey.isPressed() || !onlySpace.getValue() || cruiseControl.getValue())) {
                float yaw = (float) Math.toRadians(mc.player.getYaw());
                double motionX = -Math.sin(yaw) * (factor.getValue() / 10f);
                double motionZ = Math.cos(yaw) * (factor.getValue() / 10f);
                
                // Применяем ограничение скорости
                double speed = Math.hypot(motionX, motionZ);
                if (speed > 0) {
                    if (speedLimit.getValue() && speed > maxSpeed.getValue()) {
                        double multiplier = maxSpeed.getValue() / speed;
                        motionX *= multiplier;
                        motionZ *= multiplier;
                    }
                    
                    // Устанавливаем скорость
                    mc.player.setVelocity(motionX, mc.player.getVelocity().y, motionZ);
                }
            }
        } else {
            Vec3d velocity = mc.player.getVelocity();
            double eX = velocity.x;
            double eY = velocity.y;
            double eZ = velocity.z;
            Vec3d rotationVec = mc.player.getRotationVec(1.0f);
            double d6 = Math.hypot(rotationVec.x, rotationVec.z);
            double currentSpeed = Math.hypot(eX, eZ);
            float f4 = (float) (Math.pow(Math.cos(Math.toRadians(mc.player.getPitch())), 2) * Math.min(1, rotationVec.length() / 0.4));
            eY += (-0.08D + (double) f4 * 0.06);

            if (eY < 0 && d6 > 0) {
                double ySpeed = eY * -0.1 * (double) f4;
                eY += ySpeed;
                eX += rotationVec.x * ySpeed / d6;
                eZ += rotationVec.z * ySpeed / d6;
            }

            if (mc.player.getPitch() < 0) {
                double ySpeed = currentSpeed * -Math.sin(Math.toRadians(mc.player.getPitch())) * 0.04;
                eY += ySpeed * 3.2;
                eX -= rotationVec.x * ySpeed / d6;
                eZ -= rotationVec.z * ySpeed / d6;
            }

            if (d6 > 0) {
                eX += (rotationVec.x / d6 * currentSpeed - eX) * 0.1D;
                eZ += (rotationVec.z / d6 * currentSpeed - eZ) * 0.1D;
            }

            // Улучшенная обработка редеплоя элитр с защитой от спама
            if (mc.player.getPitch() > 0 && eY < -0.5) {
                // Проверяем, нужно ли делать редеплой
                if (moveForward != 0 && redeployTimer.passed((long)(1000 * redeployTimeOut.getValue()))) {
                    if (stopMotion.getValue()) {
                        eX *= 0.3; // Плавное замедление вместо резкой остановки
                        eZ *= 0.3;
                    }
                    
                    // Отправляем пакет редеплоя с проверкой на кулдаун
                    if (startTimer.passed(500)) { // Защита от спама - минимум 500мс между редеплоями
                        sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                        redeployTimer.reset();
                        startTimer.reset();
                    }
                } 
                // Ускоряемся после редеплоя
                else if (redeployTimer.getElapsed() < 200) {
                    float yaw = (float) Math.toRadians(mc.player.getYaw());
                    double boost = factor.getValue() * (1.0 - redeployTimer.getElapsed() / 200.0);
                    eX = -Math.sin(yaw) * (boost / 4f); // Увеличил множитель для лучшего ускорения
                    eZ = Math.cos(yaw) * (boost / 4f);
                    eY = 0.1; // Небольшой подъем при ускорении
                }
            }

            double speed = Math.hypot(eX, eZ);
            if (speedLimit.getValue() && speed > maxSpeed.getValue()) {
                eX *= maxSpeed.getValue() / speed;
                eZ *= maxSpeed.getValue() / speed;
            }

            mc.player.setVelocity(eX, eY, eZ);
        }
    }

    private double getCurrentPlayerSpeed() {
        if (mc.player == null) return 0;
        return Math.hypot(mc.player.getVelocity().x, mc.player.getVelocity().z);
    }
    
    private void doExtremeBoost() {
        if (mc.player == null || !mc.player.isGliding() || mc.player.isTouchingWater() || mc.player.isInLava()) {
            return;
        }
        
        // Проверяем, есть ли у игрока элитры и не сломаны ли они
        if (!mc.player.getInventory().getArmorStack(2).isOf(Items.ELYTRA) || 
            mc.player.getInventory().getArmorStack(2).getMaxDamage() - mc.player.getInventory().getArmorStack(2).getDamage() < 2) {
            return;
        }

        Vec3d velocity = mc.player.getVelocity();
        double currentSpeed = Math.hypot(velocity.x, velocity.z);
        
        // Получаем направление движения игрока
        float yaw = (float) Math.toRadians(mc.player.getYaw());
        float pitch = (float) Math.toRadians(mc.player.getPitch());
        
        // Вычисляем целевую скорость
        double targetSpeed = extremeSpeed.getValue();
        
        // Плавное ускорение к целевой скорости
        double acceleration = extremeAcceleration.getValue();
        if (smoothAcceleration.getValue()) {
            // Чем ближе к целевой скорости, тем медленнее ускорение
            double speedRatio = currentSpeed / targetSpeed;
            if (speedRatio < 1.0) {
                acceleration *= (1.0 - speedRatio * 0.5);
            }
        }
        
        // Вычисляем новую скорость
        double newSpeed = currentSpeed;
        if (currentSpeed < targetSpeed) {
            newSpeed = Math.min(currentSpeed + acceleration, targetSpeed);
        } else if (currentSpeed > targetSpeed) {
            // Плавное замедление если превысили целевую скорость
            newSpeed = Math.max(currentSpeed - acceleration * 0.5, targetSpeed);
        }
        
        // Применяем скорость в направлении взгляда игрока
        double motionX = -Math.sin(yaw) * Math.cos(pitch) * newSpeed;
        double motionZ = Math.cos(yaw) * Math.cos(pitch) * newSpeed;
        double motionY = velocity.y;
        
        // Добавляем вертикальный буст при движении вперед
        if (mc.player.input.movementForward > 0) {
            motionY += verticalBoost.getValue();
        }
        
        // Компенсируем гравитацию для более стабильного полета
        if (pitch > -0.5 && pitch < 0.5) {
            motionY += 0.04; // Небольшая компенсация гравитации
        }
        
        // Обработка управления высотой
        if (mc.options.jumpKey.isPressed()) {
            motionY += 0.08;
        } else if (mc.options.sneakKey.isPressed()) {
            motionY -= 0.08;
        }
        
        // Ограничиваем вертикальную скорость
        motionY = MathHelper.clamp(motionY, -2.0, 2.0);
        
        // Применяем новую скорость
        mc.player.setVelocity(motionX, motionY, motionZ);
    }
    
    private void doLiquid() {
        if (mc.player == null) return;
        
        // Проверяем, есть ли у игрока элитры
        if (!mc.player.getInventory().getArmorStack(2).isOf(Items.ELYTRA)) {
            return;
        }
        
        // Проверяем, не сломаны ли элитры
        if (mc.player.getInventory().getArmorStack(2).getMaxDamage() - mc.player.getInventory().getArmorStack(2).getDamage() < 2) {
            return;
        }
        
        // Автоматический старт полета
        if (!mc.player.isGliding() && !mc.player.isOnGround() && mc.player.fallDistance > 0.5f) {
            if (!mc.player.isTouchingWater() && !mc.player.isInLava()) {
                sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
        }
        
        // Логика полета в режиме Liquid
        if (mc.player.isGliding()) {
            Vec3d velocity = mc.player.getVelocity();
            float yaw = (float) Math.toRadians(mc.player.getYaw());
            float pitch = (float) Math.toRadians(mc.player.getPitch());
            
            // Вычисляем направление движения
            double motionX = -Math.sin(yaw) * Math.cos(pitch) * liquidHorizontalSpeed.getValue();
            double motionZ = Math.cos(yaw) * Math.cos(pitch) * liquidHorizontalSpeed.getValue();
            double motionY = velocity.y;
            
            // Управление вертикальной скоростью
            if (mc.options.jumpKey.isPressed()) {
                motionY = liquidVerticalSpeed.getValue();
            } else if (mc.options.sneakKey.isPressed()) {
                motionY = -liquidVerticalSpeed.getValue();
            } else {
                // Небольшая компенсация гравитации для стабильного полета
                motionY += 0.04;
                motionY = MathHelper.clamp(motionY, -0.5, 0.5);
            }
            
            // Применяем движение только если игрок нажимает клавиши движения
            if (mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0) {
                mc.player.setVelocity(motionX, motionY, motionZ);
            } else {
                // Если не двигаемся, просто поддерживаем высоту
                mc.player.setVelocity(velocity.x * 0.9, motionY, velocity.z * 0.9);
            }
            
            // Durability exploit - спамим пакет старта полета для экономии прочности
            if (liquidDurabilityExploit.getValue()) {
                sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
        }
    }
    
    // Вспомогательный метод для отправки пакетов
    private void sendPacket(ClientCommandC2SPacket packet) {
        try {
            if (mc.player != null && mc.player.networkHandler != null) {
                mc.player.networkHandler.sendPacket(packet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
