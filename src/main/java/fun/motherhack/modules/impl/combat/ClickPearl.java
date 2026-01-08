package fun.motherhack.modules.impl.combat;

import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.utils.network.NetworkUtils;
import fun.motherhack.utils.world.InventoryUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

public class ClickPearl extends Module {

    private final EnumSetting<InventoryUtils.Swing> swing = new EnumSetting<>("settings.swing", InventoryUtils.Swing.MainHand);

    public ClickPearl() {
        super("ClickPearl", Category.Combat);
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick e) {
        if (fullNullCheck()) return;

        int slot = InventoryUtils.findHotbar(Items.ENDER_PEARL);
        int previousSlot = mc.player.getInventory().selectedSlot;

        if (slot == -1 || mc.player.getItemCooldownManager().isCoolingDown(mc.player.getInventory().getStack(slot))) {
            setToggled(false);
            return;
        }

        InventoryUtils.switchSlot(InventoryUtils.Switch.Silent, slot, previousSlot);
        NetworkUtils.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
        InventoryUtils.swing(swing.getValue());
        InventoryUtils.switchBack(InventoryUtils.Switch.Silent, slot, previousSlot);
        setToggled(false);
    }
}