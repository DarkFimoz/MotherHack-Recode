package fun.motherhack.modules.impl.render;

import fun.motherhack.api.events.impl.*;
import fun.motherhack.api.events.impl.rotations.EventMotion;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.movement.InputUtils;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.lwjgl.glfw.GLFW;

@Getter
public class FreeCam extends Module {

    private final NumberSetting speed = new NumberSetting("HSpeed", 1f, 0.1f, 3f, 0.05f);
    private final NumberSetting vspeed = new NumberSetting("VSpeed", 0.42f, 0.1f, 3f, 0.05f);
    private final BooleanSetting track = new BooleanSetting("Track", false);
    private final BooleanSetting loadChunks = new BooleanSetting("Load Chunks", true);

    private float fakeYaw, fakePitch, prevFakeYaw, prevFakePitch;
    private double fakeX, fakeY, fakeZ, prevFakeX, prevFakeY, prevFakeZ;
    
    // Сохранённая позиция игрока
    private double playerX, playerY, playerZ;
    private float playerYaw, playerPitch;
    
    public LivingEntity trackEntity;
    
    private int chunkUpdateTicks = 0;

    public FreeCam() {
        super("FreeCam", Category.Render);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (fullNullCheck()) return;
        
        mc.chunkCullingEnabled = false;
        trackEntity = null;
        
        // Сохраняем позицию игрока
        playerX = mc.player.getX();
        playerY = mc.player.getY();
        playerZ = mc.player.getZ();
        playerYaw = mc.player.getYaw();
        playerPitch = mc.player.getPitch();
        
        // Инициализируем камеру на позиции игрока
        fakePitch = mc.player.getPitch();
        fakeYaw = mc.player.getYaw();
        prevFakePitch = fakePitch;
        prevFakeYaw = fakeYaw;
        fakeX = mc.player.getX();
        fakeY = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose());
        fakeZ = mc.player.getZ();
        prevFakeX = fakeX;
        prevFakeY = fakeY;
        prevFakeZ = fakeZ;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (fullNullCheck()) return;
        mc.chunkCullingEnabled = true;
        
        // Возвращаем игрока на сохранённую позицию
        mc.player.setPosition(playerX, playerY, playerZ);
        mc.player.setYaw(playerYaw);
        mc.player.setPitch(playerPitch);
    }

    @EventHandler
    public void onAttack(EventAttackEntity e) {
        if (fullNullCheck()) return;
        if (e.getTarget() instanceof LivingEntity entity && track.getValue()) {
            trackEntity = entity;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMotion(EventMotion e) {
        if (fullNullCheck()) return;
        
        // Фиксируем игрока на месте
        mc.player.setPosition(playerX, playerY, playerZ);
        mc.player.setVelocity(0, 0, 0);
        
        prevFakeYaw = fakeYaw;
        prevFakePitch = fakePitch;

        if (isKeyPressed(GLFW.GLFW_KEY_ESCAPE) || isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT) || isKeyPressed(GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            trackEntity = null;
        }

        if (trackEntity != null) {
            fakeYaw = trackEntity.getYaw();
            fakePitch = trackEntity.getPitch();
            prevFakeX = fakeX;
            prevFakeY = fakeY;
            prevFakeZ = fakeZ;
            fakeX = trackEntity.getX();
            fakeY = trackEntity.getY() + trackEntity.getEyeHeight(trackEntity.getPose());
            fakeZ = trackEntity.getZ();
        } else {
            fakeYaw = mc.player.getYaw();
            fakePitch = mc.player.getPitch();
        }
        
        // Отправляем пакеты для загрузки чанков
        if (loadChunks.getValue()) {
            chunkUpdateTicks++;
            if (chunkUpdateTicks >= 5) { // Каждые 5 тиков (4 раза в секунду)
                chunkUpdateTicks = 0;
                sendChunkLoadPacket();
            }
        }
    }

    @EventHandler
    public void onKeyboardInput(EventKeyboardInput e) {
        if (fullNullCheck()) return;
        
        if (trackEntity == null) {
            double[] motion = forward(speed.getValue().floatValue());
            prevFakeX = fakeX;
            prevFakeY = fakeY;
            prevFakeZ = fakeZ;
            fakeX += motion[0];
            fakeZ += motion[1];

            if (mc.options.jumpKey.isPressed()) {
                fakeY += vspeed.getValue().floatValue();
            }
            if (mc.options.sneakKey.isPressed()) {
                fakeY -= vspeed.getValue().floatValue();
            }
        }

        mc.player.input.movementForward = 0;
        mc.player.input.movementSideways = 0;
        InputUtils.setJumping(false);
        InputUtils.setSneaking(false);
    }

    @EventHandler
    public void onPacketSend(EventPacket.Send e) {
        if (fullNullCheck()) return;
        // Блокируем пакеты движения, кроме тех что мы отправляем для загрузки чанков
        if (e.getPacket() instanceof PlayerMoveC2SPacket packet) {
            if (!loadChunks.getValue()) {
                e.cancel();
            } else {
                // Проверяем, это наш пакет для загрузки чанков или обычный
                double distance = Math.sqrt(
                    Math.pow(packet.getX(playerX) - fakeX, 2) +
                    Math.pow(packet.getY(playerY) - fakeY, 2) +
                    Math.pow(packet.getZ(playerZ) - fakeZ, 2)
                );
                
                // Если пакет не с позиции камеры, блокируем его
                if (distance > 1.0) {
                    e.cancel();
                }
            }
        }
    }
    
    private void sendChunkLoadPacket() {
        if (mc.getNetworkHandler() != null) {
            // Отправляем пакет с позицией камеры для загрузки чанков
            mc.getNetworkHandler().sendPacket(
                new PlayerMoveC2SPacket.Full(fakeX, fakeY, fakeZ, fakeYaw, fakePitch, mc.player.isOnGround(), mc.player.horizontalCollision)
            );
        }
    }

    @EventHandler
    public void onMouse(EventMouse e) {
        if (e.getAction() == 2) {
            if (e.getButton() > 0) {
                speed.setValue(speed.getValue().floatValue() + 0.05f);
            } else {
                speed.setValue(speed.getValue().floatValue() - 0.05f);
            }
        }
    }

    private boolean isKeyPressed(int key) {
        if (mc.getWindow() == null) return false;
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }

    private double[] forward(double speed) {
        float forward = mc.player.input.movementForward;
        float sideways = mc.player.input.movementSideways;
        float yaw = fakeYaw; // Use fake yaw for camera direction

        if (forward == 0 && sideways == 0) {
            return new double[]{0, 0};
        }

        double angle = Math.toRadians(yaw);
        if (forward != 0) {
            if (sideways > 0) {
                angle -= Math.toRadians(45 * (forward > 0 ? 1 : -1));
            } else if (sideways < 0) {
                angle += Math.toRadians(45 * (forward > 0 ? 1 : -1));
            }
            sideways = 0;
            if (forward > 0) {
                forward = 1;
            } else if (forward < 0) {
                forward = -1;
            }
        }

        double motionX = forward * speed * -Math.sin(angle) + sideways * speed * Math.cos(angle);
        double motionZ = forward * speed * Math.cos(angle) + sideways * speed * Math.sin(angle);

        return new double[]{motionX, motionZ};
    }

    public float getFakeYaw() {
        return (float) interpolate(prevFakeYaw, fakeYaw, getTickDelta());
    }

    public float getFakePitch() {
        return (float) interpolate(prevFakePitch, fakePitch, getTickDelta());
    }

    public double getFakeX() {
        return interpolate(prevFakeX, fakeX, getTickDelta());
    }

    public double getFakeY() {
        return interpolate(prevFakeY, fakeY, getTickDelta());
    }

    public double getFakeZ() {
        return interpolate(prevFakeZ, fakeZ, getTickDelta());
    }

    private double interpolate(double prev, double current, float delta) {
        return prev + (current - prev) * delta;
    }

    private float getTickDelta() {
        return mc.getRenderTickCounter().getTickDelta(true);
    }
}
