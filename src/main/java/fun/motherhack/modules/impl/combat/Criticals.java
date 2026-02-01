package fun.motherhack.modules.impl.combat;

import fun.motherhack.api.events.impl.EventAttackEntity;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.EnumSetting;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class Criticals extends Module {

    private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.Vanilla);

    public Criticals() {
        super("Criticals", Category.Combat);
        getSettings().add(mode);
    }

    @EventHandler
    public void onAttackEntity(EventAttackEntity event) {
        if (fullNullCheck()) return;
        if (!mc.player.isOnGround() && mode.getValue() != Mode.Grim) return;
        if (mc.player.isInLava() || mc.player.isSubmergedInWater()) return;
        if (mc.player.getAbilities().flying && mode.getValue() != Mode.Grim) return;
        
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        boolean onGround = mc.player.isOnGround();
        boolean horizontalCollision = mc.player.horizontalCollision;
        
        switch (mode.getValue()) {
            case Vanilla -> {
                sendCritPacket(x, y, z, onGround, horizontalCollision, 0.0625, false);
                sendCritPacket(x, y, z, false, horizontalCollision, 0.0, false);
            }
            case ReallyWorld -> {
                sendCritPacket(x, y, z, onGround, horizontalCollision, 0.05, false);
                sendCritPacket(x, y, z, false, horizontalCollision, 0.0, false);
            }
            case Funtime -> {
                sendCritPacket(x, y, z, onGround, horizontalCollision, 0.0433, false);
                sendCritPacket(x, y, z, false, horizontalCollision, 0.0, false);
            }
            case HollyWorld -> {
                sendCritPacket(x, y, z, onGround, horizontalCollision, 0.11, false);
                sendCritPacket(x, y, z, false, horizontalCollision, 0.0, false);
            }
            case Grim -> {
                // Grim работает даже в воздухе
                if (!mc.player.isOnGround()) {
                    sendCritPacket(x, y, z, false, horizontalCollision, -0.000001, true);
                } else {
                    sendCritPacket(x, y, z, onGround, horizontalCollision, 0.0625, false);
                    sendCritPacket(x, y + 0.0625, z, false, horizontalCollision, 0.0, false);
                }
            }
            case Matrix -> {
                sendCritPacket(x, y, z, onGround, horizontalCollision, 0.05, false);
                sendCritPacket(x, y + 0.05, z, false, horizontalCollision, 0.0, false);
            }
            case NCP -> {
                sendCritPacket(x, y, z, onGround, horizontalCollision, 0.0001, false);
                sendCritPacket(x, y, z, false, horizontalCollision, 0.0, false);
            }
            case StrictNCP -> {
                sendCritPacket(x, y, z, onGround, horizontalCollision, 0.0626003, false);
                sendCritPacket(x, y, z, false, horizontalCollision, 0.00001, false);
                sendCritPacket(x, y, z, false, horizontalCollision, 0.0, false);
            }
            case OldNCP -> {
                sendCritPacket(x, y, z, onGround, horizontalCollision, 0.0000105829, false);
                sendCritPacket(x, y, z, false, horizontalCollision, 0.0000091658, false);
                sendCritPacket(x, y, z, false, horizontalCollision, 0.0, false);
            }
            case UpdatedNCP -> {
                sendCritPacket(x, y, z, onGround, horizontalCollision, 0.0000002718, false);
                sendCritPacket(x, y, z, false, horizontalCollision, 0.0, false);
            }
            case HvH -> {
                sendCritPacket(x, y, z, onGround, horizontalCollision, 0.0625, false);
                sendCritPacket(x, y, z, false, horizontalCollision, 0.0, false);
                sendCritPacket(x, y, z, false, horizontalCollision, 0.01, false);
            }
            case DoubleCrits -> {
                // Двойные криты для HVH серверов (Grim/Matrix)
                // Первый крит
                sendCritPacket(x, y, z, onGround, horizontalCollision, 0.0625, false);
                sendCritPacket(x, y, z, false, horizontalCollision, 0.0, false);
                // Второй крит
                sendCritPacket(x, y, z, false, horizontalCollision, 0.05, false);
                sendCritPacket(x, y, z, false, horizontalCollision, 0.0, false);
            }
        }
    }

    private void sendCritPacket(double x, double y, double z, boolean onGround, boolean horizontalCollision, double yOffset, boolean full) {
        if (full) {
            // Full packet с ротациями (для Grim)
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                x, y + yOffset, z, 
                mc.player.getYaw(), mc.player.getPitch(), 
                onGround, horizontalCollision
            ));
        } else {
            // Только позиция
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                x, y + yOffset, z, 
                onGround, horizontalCollision
            ));
        }
    }

    @Getter
    public enum Mode implements Nameable {
        Vanilla("Vanilla"),
        ReallyWorld("ReallyWorld"),
        Funtime("Funtime"),
        HollyWorld("HollyWorld"),
        Grim("Grim"),
        Matrix("Matrix"),
        NCP("NCP"),
        StrictNCP("StrictNCP"),
        OldNCP("OldNCP"),
        UpdatedNCP("UpdatedNCP"),
        DoubleCrits("DoubleCrits"),
        HvH("HvH");

        private final String name;

        Mode(String name) {
            this.name = name;
        }
    }
}
