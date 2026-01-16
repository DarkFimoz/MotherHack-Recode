package fun.motherhack.modules.impl.movement;

import fun.motherhack.api.events.impl.rotations.EventMotion;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.math.TimerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

public class GrimElytra extends Module {
    private TimerUtils ticks = new TimerUtils();
    int ticksTwo = 0;
    public GrimElytra(){
        super("GrimGlide", Category.Movement);
    }
    @EventHandler
    public void onEvent(EventMotion event) {
        if ((mc.player == null || mc.world == null) || !mc.player.isGliding()) return;
        ticksTwo++;
        Vec3d pos = mc.player.getPos();

        float yaw = mc.player.getYaw();
        double forward = 0.087;
        double motion = Math.hypot(mc.player.prevX - mc.player.getX(), mc.player.prevZ - mc.player.getZ()) * 20.0;

        float valuePidor = 48.0f;
        if (motion >= valuePidor) {
            forward = 0.0;
            motion = 0.0;
        }

        double dx = -Math.sin(Math.toRadians(yaw)) * forward;
        double dz = Math.cos(Math.toRadians(yaw)) * forward;
        mc.player.setVelocity(dx * MathUtils.randomFloat(1.1f, 1.21f), mc.player.getVelocity().y - 0.019999999552965164, dz * MathUtils.randomFloat(1.1f, 1.21f));

        if (ticks.passed(50)) {
            mc.player.setPosition(
                    pos.getX() + dx,
                    pos.getY(),
                    pos.getZ() + dz
            );

            ticks.reset();
        }
        mc.player.setVelocity(dx * MathUtils.randomFloat(1.1f, 1.21f), mc.player.getVelocity().y + 0.01600000075995922, dz * MathUtils.randomFloat(1.1f, 1.21f));
    }
}

