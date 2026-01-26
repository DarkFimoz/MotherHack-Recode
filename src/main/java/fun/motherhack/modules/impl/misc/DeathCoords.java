package fun.motherhack.modules.impl.misc;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.utils.network.ChatUtils;
import fun.motherhack.utils.waypoint.Waypoint;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DeathScreen;

public class DeathCoords extends Module {

    public final EnumSetting<Mode> mode = new EnumSetting<>("settings.deathcoords.mode", Mode.BOTH);
    
    private boolean wasDead = false;
    private int deathCounter = 1;

    public DeathCoords() {
        super("DeathCoords", Category.Misc);
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        boolean isDead = mc.currentScreen instanceof DeathScreen;
        
        if (isDead && !wasDead) {
            int x = (int) mc.player.getX();
            int y = (int) mc.player.getY();
            int z = (int) mc.player.getZ();
            
            switch (mode.getValue()) {
                case CHAT:
                    sendDeathMessage(x, y, z);
                    break;
                case WAYPOINT:
                    createWaypoint(x, z);
                    break;
                case BOTH:
                    sendDeathMessage(x, y, z);
                    createWaypoint(x, z);
                    break;
            }
            
            wasDead = true;
        } else if (!isDead && wasDead) {
            wasDead = false;
        }
    }

    private void sendDeathMessage(int x, int y, int z) {
        ChatUtils.sendMessage("Death coordinates: X: " + x + " Y: " + y + " Z: " + z);
    }

    private void createWaypoint(int x, int z) {
        String waypointName = "Die" + deathCounter;
        Waypoint waypoint = new Waypoint(waypointName, x, z);
        MotherHack.getInstance().getWaypointManager().add(waypoint);
        deathCounter++;
    }

    public enum Mode implements Nameable {
        CHAT, WAYPOINT, BOTH;

        @Override
        public String getName() {
            return name();
        }
    }
}
