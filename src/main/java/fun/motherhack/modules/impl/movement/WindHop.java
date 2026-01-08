package fun.motherhack.modules.impl.movement;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.utils.network.NetworkUtils;
import fun.motherhack.utils.rotations.RotationChanger;
import fun.motherhack.utils.rotations.RotationUtils;
import fun.motherhack.utils.world.InventoryUtils;
import lombok.AllArgsConstructor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;

public class WindHop extends Module {

    private final EnumSetting<InventoryUtils.Switch> autoSwitch = new EnumSetting<>("settings.switch", InventoryUtils.Switch.Silent);
    private final EnumSetting<InventoryUtils.Swing> swing = new EnumSetting<>("settings.swing", InventoryUtils.Swing.MainHand);
    private final EnumSetting<Rotate> rotate = new EnumSetting<>("settings.rotate", Rotate.Normal);

    private float[] rotations;
    private final RotationChanger changer = new RotationChanger(
            2500,
            () -> new Float[]{rotations[0], rotations[1]},
            Module::fullNullCheck
    );

    public WindHop() {
        super("WindHop", Category.Movement);
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick e) {
        if (fullNullCheck()) return;
        int previousSlot = mc.player.getInventory().selectedSlot;
        int slot = InventoryUtils.findHotbar(Items.WIND_CHARGE);
        if (slot == -1 || !mc.player.isOnGround()) return;
        InventoryUtils.switchSlot(autoSwitch.getValue(), slot, previousSlot);
        rotations = RotationUtils.getRotations(Direction.DOWN);
        if (rotate.getValue() == Rotate.Normal) MotherHack.getInstance().getRotationManager().addRotation(changer);
        else if (rotate.getValue() == Rotate.Packet) MotherHack.getInstance().getRotationManager().addPacketRotation(rotations);
        NetworkUtils.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, rotations[0], rotations[1]));
        InventoryUtils.swing(swing.getValue());
        InventoryUtils.switchBack(autoSwitch.getValue(), slot, previousSlot);
        setToggled(false);
    }

    @AllArgsConstructor
    public enum Rotate implements Nameable {
        Normal("settings.normal"),
        Packet("settings.packet"),
        None("settings.none");

        private final String name;

        @Override
        public String getName() {
            return name;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        MotherHack.getInstance().getRotationManager().removeRotation(changer);
    }
}