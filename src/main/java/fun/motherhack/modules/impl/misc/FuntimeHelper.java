package fun.motherhack.modules.impl.misc;

import fun.motherhack.api.events.impl.EventKey;
import fun.motherhack.api.events.impl.EventMouse;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Bind;
import fun.motherhack.modules.settings.impl.BindSetting;
import fun.motherhack.utils.network.NetworkUtils;
import fun.motherhack.utils.world.InventoryUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;

public class FuntimeHelper extends Module {
	
	private final BindSetting bindDesor = new BindSetting("settings.funtimehelper.binddesor", new Bind(-1, false));
	private final BindSetting bindTrapka = new BindSetting("settings.funtimehelper.bindtrapka", new Bind(-1, false));
	private final BindSetting bindYavka = new BindSetting("settings.funtimehelper.bindyavka", new Bind(-1, false));
	private final BindSetting bindBozhka = new BindSetting("settings.funtimehelper.bindbozhka", new Bind(-1, false));
	private final BindSetting bindPlast = new BindSetting("settings.funtimehelper.bindplast", new Bind(-1, false));

	public FuntimeHelper() {
		super("FuntimeHelper", Category.Misc);
	}

	@EventHandler
	public void onKey(EventKey e) {
        if (fullNullCheck() || mc.currentScreen != null) return;
        
		if (e.getAction() == 1) {
			if (e.getKey() == bindDesor.getValue().getKey() && !bindDesor.getValue().isMouse()) prekol(Items.ENDER_EYE);
			if (e.getKey() == bindTrapka.getValue().getKey() && !bindTrapka.getValue().isMouse()) prekol(Items.NETHERITE_SCRAP);
			if (e.getKey() == bindYavka.getValue().getKey() && !bindYavka.getValue().isMouse()) prekol(Items.SUGAR);
			if (e.getKey() == bindBozhka.getValue().getKey() && !bindBozhka.getValue().isMouse()) prekol(Items.PHANTOM_MEMBRANE);
			if (e.getKey() == bindPlast.getValue().getKey() && !bindPlast.getValue().isMouse()) prekol(Items.DRIED_KELP);
		}
	}
	
	@EventHandler
	public void onMouse(EventMouse e) {
        if (fullNullCheck() || mc.currentScreen != null) return;
        
		if (e.getAction() == 1) {
			if (e.getButton() == bindDesor.getValue().getKey() && bindDesor.getValue().isMouse()) prekol(Items.ENDER_EYE);
			if (e.getButton() == bindTrapka.getValue().getKey() && bindTrapka.getValue().isMouse()) prekol(Items.NETHERITE_SCRAP);
			if (e.getButton() == bindYavka.getValue().getKey() && bindYavka.getValue().isMouse()) prekol(Items.SUGAR);
			if (e.getButton() == bindBozhka.getValue().getKey() && bindBozhka.getValue().isMouse()) prekol(Items.PHANTOM_MEMBRANE);
			if (e.getButton() == bindPlast.getValue().getKey() && bindPlast.getValue().isMouse()) prekol(Items.DRIED_KELP);
		}
	}
	
	private void prekol(Item item) {
		int slot = InventoryUtils.findHotbar(item);
		int previousSlot = mc.player.getInventory().selectedSlot;
		if (slot == -1 || mc.player.getItemCooldownManager().isCoolingDown(mc.player.getInventory().getStack(slot))) return;
		InventoryUtils.switchSlot(InventoryUtils.Switch.Normal, slot, previousSlot);
        NetworkUtils.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
        InventoryUtils.swing(InventoryUtils.Swing.MainHand);
        InventoryUtils.switchBack(InventoryUtils.Switch.Normal, slot, previousSlot);
	}
}