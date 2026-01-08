package fun.motherhack.modules.impl.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.lwjgl.glfw.GLFW;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.modules.settings.impl.StringSetting;
import fun.motherhack.modules.settings.api.Bind;
import fun.motherhack.utils.math.TimerUtils;
import fun.motherhack.utils.movement.MoveUtils;

public class AutoFlyme extends Module {
    public AutoFlyme() {
        super("AutoFlyme", Category.Misc);

        instantSpeed = new BooleanSetting("InstantSpeed", true);
        hover = new BooleanSetting("hover", false);
        useTimer = new BooleanSetting("UseTimer", false);
        hoverY = new NumberSetting("hoverY", 0.228f, 0.0f, 1.0f, 0.01f);
        speed = new NumberSetting("speed", 1.05f, 0.0f, 8.0f, 0.1f);
        command = new StringSetting("command", "flyme", false);
    }

    private final BooleanSetting instantSpeed;
    private final BooleanSetting hover;
    private final BooleanSetting useTimer;
    private final NumberSetting hoverY;
    private final NumberSetting speed;
    private final StringSetting command;

    //фаннигейм перешел на матрикс, и теперь можно летать со скоростью 582 км/ч :skull:
    private final TimerUtils timer = new TimerUtils();

    @Override
    public void onEnable() {
        super.onEnable();
        if (!mc.player.getAbilities().flying) {
            mc.player.networkHandler.sendChatCommand(command.getValue());
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        // Reset player abilities and velocity when disabled
        if (mc.player != null) {
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().allowFlying = false;
            mc.player.setVelocity(0, 0, 0);
        }
        timer.reset();
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive e) {
        if (e.getPacket() instanceof GameMessageS2CPacket packet) {
            String message = packet.content().getString();
            if ((message.contains("Вы атаковали игрока") || message.contains("Возможность летать была удалена")) && timer.passed(1000)) {
                mc.player.networkHandler.sendChatCommand(command.getValue());
                mc.player.networkHandler.sendChatCommand(command.getValue());
                timer.reset();
            }
        }
    }

    @EventHandler
    public void onUpdate(EventPlayerTick e) {
        if (!mc.player.getAbilities().flying && timer.passed(1000) && !mc.player.isOnGround() && mc.options.jumpKey.isPressed()) {
            mc.player.networkHandler.sendChatCommand(command.getValue());
            timer.reset();
        }
        if (!mc.options.jumpKey.isPressed() && hover.getValue() && mc.player.getAbilities().flying && !mc.player.isOnGround() && !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0.0, -hoverY.getValue(), 0.0)).iterator().hasNext()) {
            mc.player.setVelocity(mc.player.getVelocity().x, -0.05, mc.player.getVelocity().z);
        }
        if (!instantSpeed.getValue() || !mc.player.getAbilities().flying) return;
        if (MoveUtils.isMoving()) {
            double yaw = Math.toRadians(mc.player.getYaw());
            double x = -Math.sin(yaw) * speed.getValue();
            double z = Math.cos(yaw) * speed.getValue();
            mc.player.setVelocity(x, mc.player.getVelocity().y, z);
        } else {
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
        }
    }
}
