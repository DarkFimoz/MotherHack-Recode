package fun.motherhack.modules.impl.movement;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.combat.Aura;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class AKB extends Module {
    
    public AKB() {
        super("AKB", Category.Movement);
    }

    // Settings
    private final BooleanSetting onlyAura = new BooleanSetting("Only During Aura", false);
    private final BooleanSetting pauseInWater = new BooleanSetting("Pause In Liquids", false);
    private final BooleanSetting explosions = new BooleanSetting("Explosions", true);
    private final BooleanSetting pauseOnFlag = new BooleanSetting("Pause On Flag", true);
    private final BooleanSetting pauseOnFire = new BooleanSetting("Pause On Fire", false);
    
    private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.GrimAdvanced);
    
    private final NumberSetting vertical = new NumberSetting("Vertical", 0.0f, 0.0f, 100.0f, 0.1f, () -> mode.getValue() == Mode.Custom);
    private final NumberSetting horizontal = new NumberSetting("Horizontal", 0.0f, 0.0f, 100.0f, 0.1f, () -> mode.getValue() == Mode.Custom || mode.getValue() == Mode.Jump);
    
    private final NumberSetting motion = new NumberSetting("Motion", 0.42f, 0.4f, 0.5f, 0.01f, () -> mode.getValue() == Mode.Jump);
    private final BooleanSetting smartFail = new BooleanSetting("Smart Fail", true, () -> mode.getValue() == Mode.Jump);
    private final NumberSetting failRate = new NumberSetting("Fail Rate", 0.3f, 0.0f, 1.0f, 0.05f, () -> mode.getValue() == Mode.Jump && smartFail.getValue());
    private final NumberSetting jumpRate = new NumberSetting("Fail Jump Rate", 0.25f, 0.0f, 1.0f, 0.05f, () -> mode.getValue() == Mode.Jump && smartFail.getValue());
    
    private final EnumSetting<JumpMode> jumpMode = new EnumSetting<>("Jump Mode", JumpMode.Jump, () -> mode.getValue() == Mode.Jump);
    
    // Advanced settings
    private final BooleanSetting onlyWhileMoving = new BooleanSetting("Only While Moving", true, () -> mode.getValue() == Mode.MatrixAdvanced || mode.getValue() == Mode.GrimAdvanced);
    private final BooleanSetting reduceY = new BooleanSetting("Reduce Y", true, () -> mode.getValue() == Mode.MatrixAdvanced);
    private final NumberSetting yReduction = new NumberSetting("Y Reduction", 65.0f, 0.0f, 100.0f, 1.0f, () -> mode.getValue() == Mode.MatrixAdvanced && reduceY.getValue());
    
    // Grim Advanced settings
    private final BooleanSetting grimSmartDelay = new BooleanSetting("Smart Delay", true, () -> mode.getValue() == Mode.GrimAdvanced);
    private final NumberSetting grimDelayMin = new NumberSetting("Delay Min", 1, 0, 10, 1, () -> mode.getValue() == Mode.GrimAdvanced && grimSmartDelay.getValue());
    private final NumberSetting grimDelayMax = new NumberSetting("Delay Max", 3, 0, 10, 1, () -> mode.getValue() == Mode.GrimAdvanced && grimSmartDelay.getValue());
    private final BooleanSetting grimAdaptive = new BooleanSetting("Adaptive Reduction", true, () -> mode.getValue() == Mode.GrimAdvanced);
    private final NumberSetting grimReduction = new NumberSetting("Base Reduction", 15.0f, 0.0f, 50.0f, 1.0f, () -> mode.getValue() == Mode.GrimAdvanced);
    
    // Matrix Advanced settings
    private final BooleanSetting matrixSmooth = new BooleanSetting("Smooth Reduction", true, () -> mode.getValue() == Mode.MatrixAdvanced);
    private final BooleanSetting matrixRandomize = new BooleanSetting("Randomize", true, () -> mode.getValue() == Mode.MatrixAdvanced);
    private final NumberSetting matrixRandomness = new NumberSetting("Randomness", 5.0f, 0.0f, 15.0f, 0.5f, () -> mode.getValue() == Mode.MatrixAdvanced && matrixRandomize.getValue());
    private final BooleanSetting matrixGroundCheck = new BooleanSetting("Ground Check", true, () -> mode.getValue() == Mode.MatrixAdvanced);

    // State variables
    private boolean doJump, failJump, skip, flag, matrixFlag;
    private int grimTicks, flagCooldown, grimDelayTicks, velocityTicks;
    private double storedVelocityX, storedVelocityY, storedVelocityZ;
    private final Random random = new Random();
    private int hitCount = 0;

    @EventHandler
    public void onPacketReceive(EventPacket.Receive event) {
        if (fullNullCheck()) return;
        
        // Check pause conditions
        if (shouldPause()) return;
        
        // Handle flag cooldown
        if (flagCooldown > 0) {
            flagCooldown--;
            return;
        }

        // Handle velocity packets
        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
            if (packet.getEntityId() == mc.player.getId() && shouldProcess()) {
                handleVelocity(event, packet);
            }
        }

        // Handle explosion packets
        if (event.getPacket() instanceof ExplosionS2CPacket packet && explosions.getValue()) {
            handleExplosion(event, packet);
        }

        // Handle lagback/flag detection
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            if (pauseOnFlag.getValue() || mode.getValue() == Mode.GrimNew) {
                flagCooldown = 5;
            }
        }
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick event) {
        if (fullNullCheck() || shouldPause()) return;

        switch (mode.getValue()) {
            case Matrix -> handleMatrixTick();
            case MatrixAdvanced -> handleMatrixAdvancedTick();
            case Jump -> handleJumpTick();
            case GrimNew -> handleGrimNewTick();
            case GrimAdvanced -> handleGrimAdvancedTick();
            default -> {} // Остальные режимы не требуют tick обработки
        }

        if (grimTicks > 0) grimTicks--;
        if (velocityTicks > 0) velocityTicks--;
        if (grimDelayTicks > 0) grimDelayTicks--;
    }

    private void handleVelocity(EventPacket.Receive event, EntityVelocityUpdateS2CPacket packet) {
        switch (mode.getValue()) {
            case Matrix -> {
                // Check if we should only apply while moving
                if (onlyWhileMoving.getValue()) {
                    boolean isMoving = mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;
                    if (!isMoving) return;
                }
                
                if (!matrixFlag) {
                    event.cancel();
                    matrixFlag = true;
                } else {
                    matrixFlag = false;
                    // Reduce velocity by 90% horizontally
                    float yMult = reduceY.getValue() ? (yReduction.getValue().floatValue() / 100f) : 1.0f;
                    modifyVelocityPacket(packet, -0.1f, yMult, -0.1f);
                }
            }
            case MatrixAdvanced -> {
                // Продвинутый Matrix обход - НАМНОГО менее флагающий
                if (onlyWhileMoving.getValue()) {
                    boolean isMoving = mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;
                    if (!isMoving) return;
                }
                
                // Ground check для избежания флагов
                if (matrixGroundCheck.getValue() && !mc.player.isOnGround() && mc.player.fallDistance > 0.5f) {
                    // Не применяем если уже в воздухе после падения
                    return;
                }
                
                hitCount++;
                
                // Плавное снижение с рандомизацией
                float baseReduction = 0.25f; // 75% reduction
                
                if (matrixRandomize.getValue()) {
                    float randomFactor = (random.nextFloat() * matrixRandomness.getValue().floatValue()) / 100f;
                    baseReduction += randomFactor;
                }
                
                // Адаптивное снижение в зависимости от количества ударов
                if (matrixSmooth.getValue() && hitCount > 1) {
                    // Постепенно увеличиваем reduction для избежания паттернов
                    baseReduction = Math.min(0.4f, baseReduction + (hitCount * 0.02f));
                }
                
                float yMult = reduceY.getValue() ? (yReduction.getValue().floatValue() / 100f) : 0.7f;
                
                // Добавляем микро-вариации для Y
                if (matrixRandomize.getValue()) {
                    yMult += (random.nextFloat() * 0.1f - 0.05f);
                }
                
                modifyVelocityPacket(packet, baseReduction, yMult, baseReduction);
                
                // Сохраняем velocity для плавного применения
                velocityTicks = 3;
            }
            case Redirect -> {
                double vX = Math.abs(packet.getVelocityX() / 8000.0);
                double vZ = Math.abs(packet.getVelocityZ() / 8000.0);
                double totalVelocity = vX + vZ;
                
                // Redirect velocity forward
                double yaw = Math.toRadians(mc.player.getYaw());
                double motionX = -Math.sin(yaw) * totalVelocity;
                double motionZ = Math.cos(yaw) * totalVelocity;
                
                setVelocityPacket(packet, (int)(motionX * 8000), 0, (int)(motionZ * 8000));
            }
            case Custom -> {
                float hMult = horizontal.getValue().floatValue() / 100f;
                float vMult = vertical.getValue().floatValue() / 100f;
                modifyVelocityPacket(packet, hMult, vMult, hMult);
            }
            case Sunrise -> {
                event.cancel();
                mc.player.networkHandler.sendPacket(
                    new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(), -999.0, mc.player.getZ(), mc.player.isOnGround(), mc.player.horizontalCollision
                    )
                );
            }
            case Cancel -> event.cancel();
            case Jump -> {
                float hMult = horizontal.getValue().floatValue() / 100f;
                modifyVelocityPacket(packet, hMult, 1.0f, hMult);
            }
            case OldGrim -> {
                event.cancel();
                grimTicks = 6;
            }
            case GrimNew -> {
                // Check if we should only apply while moving
                if (onlyWhileMoving.getValue()) {
                    boolean isMoving = mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;
                    if (!isMoving) return;
                }
                event.cancel();
                flag = true;
            }
            case GrimAdvanced -> {
                // ПРОДВИНУТЫЙ GRIM ОБХОД - НЕ ФЛАГАЕТ!
                if (onlyWhileMoving.getValue()) {
                    boolean isMoving = mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;
                    if (!isMoving) return;
                }
                
                // Сохраняем оригинальную velocity
                storedVelocityX = packet.getVelocityX() / 8000.0;
                storedVelocityY = packet.getVelocityY() / 8000.0;
                storedVelocityZ = packet.getVelocityZ() / 8000.0;
                
                // Умная задержка для избежания детекта
                if (grimSmartDelay.getValue()) {
                    grimDelayTicks = grimDelayMin.getValue().intValue() + 
                                    random.nextInt(grimDelayMax.getValue().intValue() - grimDelayMin.getValue().intValue() + 1);
                } else {
                    grimDelayTicks = 2;
                }
                
                // Адаптивное снижение velocity
                float reduction = grimReduction.getValue().floatValue() / 100f;
                
                if (grimAdaptive.getValue()) {
                    // Адаптируемся к силе удара
                    double totalVelocity = Math.abs(storedVelocityX) + Math.abs(storedVelocityZ);
                    
                    if (totalVelocity > 0.8) {
                        // Сильный удар - больше снижаем
                        reduction = Math.min(0.35f, reduction + 0.1f);
                    } else if (totalVelocity < 0.3) {
                        // Слабый удар - меньше снижаем для легитимности
                        reduction = Math.max(0.05f, reduction - 0.05f);
                    }
                    
                    // Добавляем рандомизацию
                    reduction += (random.nextFloat() * 0.08f - 0.04f);
                }
                
                // Применяем снижение постепенно
                modifyVelocityPacket(packet, 1.0f - reduction, 1.0f - (reduction * 0.5f), 1.0f - reduction);
                
                flag = true;
                grimTicks = 8 + random.nextInt(4); // Рандомизируем длительность
            }
            case Vulcan -> {
                // Vulcan bypass - reduce velocity significantly but not completely
                modifyVelocityPacket(packet, 0.36f, 0.36f, 0.36f);
            }
            case Intave -> {
                // Intave bypass - cancel and send position
                event.cancel();
                if (mc.player.isOnGround()) {
                    mc.player.networkHandler.sendPacket(
                        new PlayerMoveC2SPacket.PositionAndOnGround(
                            mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround(), mc.player.horizontalCollision
                        )
                    );
                }
            }
        }
    }

    private void handleExplosion(EventPacket.Receive event, ExplosionS2CPacket packet) {
        switch (mode.getValue()) {
            case Cancel -> {
                // Cancel explosion knockback by clearing the optional
                if (packet.playerKnockback().isPresent()) {
                    setExplosionVelocity(packet, 0, 0, 0);
                }
            }
            case Custom -> {
                if (packet.playerKnockback().isPresent()) {
                    float hMult = horizontal.getValue().floatValue() / 100f;
                    float vMult = vertical.getValue().floatValue() / 100f;
                    var knockback = packet.playerKnockback().get();
                    setExplosionVelocity(packet, 
                        (float)(knockback.getX() * hMult),
                        (float)(knockback.getY() * vMult),
                        (float)(knockback.getZ() * hMult)
                    );
                }
            }
            case GrimNew, GrimAdvanced -> {
                if (packet.playerKnockback().isPresent()) {
                    // Для Grim режимов применяем адаптивное снижение
                    if (mode.getValue() == Mode.GrimAdvanced) {
                        var knockback = packet.playerKnockback().get();
                        float reduction = grimReduction.getValue().floatValue() / 100f;
                        setExplosionVelocity(packet,
                            (float)(knockback.getX() * (1.0f - reduction)),
                            (float)(knockback.getY() * (1.0f - reduction * 0.5f)),
                            (float)(knockback.getZ() * (1.0f - reduction))
                        );
                    } else {
                        setExplosionVelocity(packet, 0, 0, 0);
                    }
                    flag = true;
                }
            }
            case MatrixAdvanced -> {
                if (packet.playerKnockback().isPresent()) {
                    var knockback = packet.playerKnockback().get();
                    float baseReduction = 0.3f;
                    
                    if (matrixRandomize.getValue()) {
                        baseReduction += (random.nextFloat() * 0.1f);
                    }
                    
                    setExplosionVelocity(packet,
                        (float)(knockback.getX() * baseReduction),
                        (float)(knockback.getY() * (baseReduction + 0.2f)),
                        (float)(knockback.getZ() * baseReduction)
                    );
                }
            }
            default -> {
                // Для остальных режимов не обрабатываем взрывы
            }
        }
    }

    private void handleMatrixTick() {
        if (mc.player.hurtTime > 0 && !mc.player.isOnGround()) {
            // Redirect velocity in movement direction
            double yaw = Math.toRadians(mc.player.getYaw());
            Vec3d velocity = mc.player.getVelocity();
            double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            
            mc.player.setVelocity(
                -Math.sin(yaw) * horizontalSpeed,
                velocity.y,
                Math.cos(yaw) * horizontalSpeed
            );
            
            // Sprint reset bypass
            mc.player.setSprinting(mc.player.age % 2 != 0);
        }
    }

    private void handleJumpTick() {
        if ((failJump || mc.player.hurtTime > 6) && mc.player.isOnGround()) {
            if (failJump) failJump = false;
            
            if (!doJump) skip = true;
            
            // Smart fail logic
            if (Math.random() <= failRate.getValue().floatValue() && smartFail.getValue()) {
                if (Math.random() <= jumpRate.getValue().floatValue()) {
                    doJump = true;
                    failJump = true;
                } else {
                    doJump = false;
                    failJump = false;
                }
            } else {
                doJump = true;
                failJump = false;
            }
            
            if (skip) {
                skip = false;
                return;
            }
            
            // Execute jump
            switch (jumpMode.getValue()) {
                case Jump -> mc.player.jump();
                case Motion -> mc.player.setVelocity(
                    mc.player.getVelocity().x,
                    motion.getValue().floatValue(),
                    mc.player.getVelocity().z
                );
                case Both -> {
                    mc.player.jump();
                    mc.player.setVelocity(
                        mc.player.getVelocity().x,
                        motion.getValue().floatValue(),
                        mc.player.getVelocity().z
                    );
                }
            }
        }
    }

    private void handleGrimNewTick() {
        if (flag && flagCooldown <= 0) {
            // Send position packet to bypass
            mc.player.networkHandler.sendPacket(
                new PlayerMoveC2SPacket.Full(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    mc.player.getYaw(),
                    mc.player.getPitch(),
                    mc.player.isOnGround(),
                    mc.player.horizontalCollision
                )
            );
            flag = false;
        }
    }
    
    private void handleMatrixAdvancedTick() {
        if (velocityTicks > 0 && mc.player.hurtTime > 0) {
            // Плавная коррекция движения для избежания флагов
            Vec3d velocity = mc.player.getVelocity();
            
            if (matrixSmooth.getValue()) {
                // Постепенно снижаем velocity каждый тик
                double smoothFactor = 0.92 + (random.nextDouble() * 0.04); // 0.92-0.96
                
                mc.player.setVelocity(
                    velocity.x * smoothFactor,
                    velocity.y,
                    velocity.z * smoothFactor
                );
            }
            
            // Имитация легитимного sprint reset
            if (mc.player.age % 3 == 0) {
                mc.player.setSprinting(false);
            } else {
                mc.player.setSprinting(true);
            }
        }
        
        // Сброс счетчика ударов через время
        if (mc.player.hurtTime == 0 && hitCount > 0) {
            if (mc.player.age % 60 == 0) {
                hitCount = Math.max(0, hitCount - 1);
            }
        }
    }
    
    private void handleGrimAdvancedTick() {
        if (flag && grimDelayTicks <= 0 && flagCooldown <= 0) {
            // Отправляем пакет с микро-задержкой для обхода
            if (grimTicks > 0) {
                // Плавное применение velocity с микро-коррекциями
                Vec3d currentVel = mc.player.getVelocity();
                
                // Добавляем легитимные микро-движения
                double microX = (random.nextDouble() - 0.5) * 0.001;
                double microZ = (random.nextDouble() - 0.5) * 0.001;
                
                mc.player.setVelocity(
                    currentVel.x + microX,
                    currentVel.y,
                    currentVel.z + microZ
                );
                
                // Отправляем position packet с небольшими вариациями
                if (grimTicks % 2 == 0) {
                    mc.player.networkHandler.sendPacket(
                        new PlayerMoveC2SPacket.Full(
                            mc.player.getX(),
                            mc.player.getY(),
                            mc.player.getZ(),
                            mc.player.getYaw(),
                            mc.player.getPitch(),
                            mc.player.isOnGround(),
                            mc.player.horizontalCollision
                        )
                    );
                }
            } else {
                flag = false;
            }
        }
    }

    private boolean shouldPause() {
        // Pause in water/lava
        if (pauseInWater.getValue() && (mc.player.isTouchingWater() || mc.player.isSubmergedInWater() || mc.player.isInLava())) {
            return true;
        }
        
        // Pause on fire
        if (pauseOnFire.getValue() && mc.player.isOnFire() && mc.player.hurtTime > 0) {
            return true;
        }
        
        return false;
    }

    private boolean shouldProcess() {
        if (!onlyAura.getValue()) return true;
        
        Aura aura = MotherHack.getInstance().getModuleManager().getModule(Aura.class);
        return aura != null && aura.isToggled();
    }

    private void modifyVelocityPacket(EntityVelocityUpdateS2CPacket packet, float xMult, float yMult, float zMult) {
        try {
            var field = packet.getClass().getDeclaredField("velocityX");
            field.setAccessible(true);
            field.setInt(packet, (int)(packet.getVelocityX() * xMult));
            
            field = packet.getClass().getDeclaredField("velocityY");
            field.setAccessible(true);
            field.setInt(packet, (int)(packet.getVelocityY() * yMult));
            
            field = packet.getClass().getDeclaredField("velocityZ");
            field.setAccessible(true);
            field.setInt(packet, (int)(packet.getVelocityZ() * zMult));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setVelocityPacket(EntityVelocityUpdateS2CPacket packet, int x, int y, int z) {
        try {
            var field = packet.getClass().getDeclaredField("velocityX");
            field.setAccessible(true);
            field.setInt(packet, x);
            
            field = packet.getClass().getDeclaredField("velocityY");
            field.setAccessible(true);
            field.setInt(packet, y);
            
            field = packet.getClass().getDeclaredField("velocityZ");
            field.setAccessible(true);
            field.setInt(packet, z);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setExplosionVelocity(ExplosionS2CPacket packet, float x, float y, float z) {
        try {
            // In 1.21.4, explosion knockback is stored as Optional<Vec3d>
            var field = packet.getClass().getDeclaredField("playerKnockback");
            field.setAccessible(true);
            
            if (x == 0 && y == 0 && z == 0) {
                // Set to empty optional to cancel knockback
                field.set(packet, java.util.Optional.empty());
            } else {
                // Set to new Vec3d with modified values
                field.set(packet, java.util.Optional.of(new Vec3d(x, y, z)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        grimTicks = 0;
        flagCooldown = 0;
        matrixFlag = false;
        flag = false;
        grimDelayTicks = 0;
        velocityTicks = 0;
        hitCount = 0;
        storedVelocityX = 0;
        storedVelocityY = 0;
        storedVelocityZ = 0;
    }

    public enum Mode implements Nameable {
        GrimAdvanced("Grim Advanced"),
        MatrixAdvanced("Matrix Advanced"),
        Matrix("Matrix"),
        Cancel("Cancel"),
        Sunrise("Sunrise"),
        Custom("Custom"),
        Redirect("Redirect"),
        OldGrim("Old Grim"),
        Jump("Jump"),
        GrimNew("Grim New"),
        Vulcan("Vulcan"),
        Intave("Intave");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum JumpMode implements Nameable {
        Motion("Motion"),
        Jump("Jump"),
        Both("Both");

        private final String name;

        JumpMode(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
