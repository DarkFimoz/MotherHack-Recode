package fun.motherhack.modules.impl.movement;

import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.network.NetworkUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class Glide extends Module {
    private final EnumSetting<Mode> mode = new EnumSetting<>("settings.glide.mode", Mode.Grim);
    private final BooleanSetting smart = new BooleanSetting("settings.glide.smart", false);
    private final NumberSetting speed = new NumberSetting("settings.glide.speed", 2f, 0.1f, 10f, 0.1f);
    private final NumberSetting height = new NumberSetting("settings.glide.height", 1.0f, 0.1f, 10f, 0.1f);
    private final NumberSetting interval = new NumberSetting("settings.glide.interval", 5f, 1f, 100f, 1f);

    @AllArgsConstructor
    @Getter
    public enum Mode implements Nameable {
        Grim("Grim"),
        GrimGlide("GrimGlide"),
        GrimAC("GrimAC");
        
        private final String name;
    }

    private boolean flying = false;
    private int ticks = 0;

    public Glide() {
        super("Glide", Category.Movement);
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick event) {
        if (fullNullCheck()) return;

        if (mode.getValue() == Mode.Grim) {
            handleGrimMode();
        } else if (mode.getValue() == Mode.GrimGlide) {
            handleGrimGlideMode();
        } else if (mode.getValue() == Mode.GrimAC) {
            handleGrimACMode();
        }

        // Jump to start flying
        if (mc.options.jumpKey.isPressed() && !flying) {
            startFlying();
        }
        
        // Sneak to stop flying
        if (mc.options.sneakKey.isPressed() && flying) {
            stopFlying();
        }
    }

    private void handleGrimMode() {
        if (!flying) return;

        mc.player.setVelocity(mc.player.getVelocity().x, 0.1, mc.player.getVelocity().z);
        
        if (ticks % interval.getValue().intValue() == 0) {
            NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(),
                mc.player.getY(),
                mc.player.getZ(),
                mc.player.isOnGround(),
                mc.player.horizontalCollision
            ));
        }
        ticks++;
    }

    private void handleGrimGlideMode() {
        if (!flying) return;

        mc.player.setVelocity(mc.player.getVelocity().x, 0.0, mc.player.getVelocity().z);
        
        if (ticks % interval.getValue().intValue() == 0) {
            NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.Full(
                mc.player.getX(),
                mc.player.getY(),
                mc.player.getZ(),
                mc.player.getYaw(),
                mc.player.getPitch(),
                mc.player.isOnGround(),
                mc.player.horizontalCollision
            ));
        }
        ticks++;
    }

    private void handleGrimACMode() {
        if (!flying) return;

        double motionY = smart.getValue() ? (speed.getValue() * 0.025) : height.getValue();
        mc.player.setVelocity(mc.player.getVelocity().x, motionY, mc.player.getVelocity().z);
        
        if (ticks % interval.getValue().intValue() == 0) {
            NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(),
                mc.player.getY(),
                mc.player.getZ(),
                false,
                mc.player.horizontalCollision
            ));
        }
        ticks++;
    }

    private void startFlying() {
        flying = true;
        ticks = 0;
    }

    private void stopFlying() {
        flying = false;
        mc.player.setVelocity(mc.player.getVelocity().x, 0, mc.player.getVelocity().z);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        stopFlying();
        ticks = 0;
    }
}