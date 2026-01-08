package fun.motherhack.modules.impl.misc;

import fun.motherhack.api.events.impl.EventKey;
import fun.motherhack.api.events.impl.EventMouse;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Bind;
import fun.motherhack.modules.settings.impl.BindSetting;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.utils.network.ChatUtils;
import fun.motherhack.utils.network.NetworkUtils;
import fun.motherhack.utils.world.InventoryUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

public class ElytraHelper extends Module {

    private final EnumSetting<InventoryUtils.Swing> swing = new EnumSetting<>(I18n.translate("settings.swing"), InventoryUtils.Swing.MainHand);
    private final BindSetting bindElytra = new BindSetting("settings.elytrahelper.bindelytra", new Bind(-1, false));
    private final BindSetting bindFirework = new BindSetting("settings.elytrahelper.bindfirework", new Bind(-1, false));
    private final BooleanSetting sync = new BooleanSetting("settings.sync", false);
    
    public ElytraHelper() {
        super("ElytraHelper", Category.Misc);
    }
    
    @EventHandler
    public void onKey(EventKey e) {
        if (fullNullCheck() || mc.currentScreen != null) return;

        if (e.getAction() == 1) {
        	if (e.getKey() == bindFirework.getValue().getKey() && !bindFirework.getValue().isMouse()) throwFirework();
        	else if (e.getKey() == bindElytra.getValue().getKey() && !bindElytra.getValue().isMouse()) swapElytra();
        }
    }

    @EventHandler
    public void onMouse(EventMouse e) {
        if (fullNullCheck() || mc.currentScreen != null) return;

        if (e.getAction() == 1) {
        	if (e.getButton() == bindFirework.getValue().getKey() && bindFirework.getValue().isMouse()) throwFirework();
        	else if (e.getButton() == bindElytra.getValue().getKey() && bindElytra.getValue().isMouse()) swapElytra();
        }
    }
    
    private void throwFirework() {
    	 if (!mc.player.isGliding()) return;
         int slot = InventoryUtils.find(Items.FIREWORK_ROCKET, 0, 8);
         int previousSlot = mc.player.getInventory().selectedSlot;
         if (slot == -1) return;
         InventoryUtils.switchSlot(InventoryUtils.Switch.Silent, slot, previousSlot);
         NetworkUtils.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
         InventoryUtils.swing(swing.getValue());
         InventoryUtils.switchBack(InventoryUtils.Switch.Silent, slot, previousSlot);
         ChatUtils.sendMessage(I18n.translate("modules.elytrahelper.threwfirework"));
    }
    
    private void swapElytra() {
    	 boolean elytra = mc.player.getInventory().getArmorStack(2).getItem() == Items.ELYTRA;
    	 int slot = elytra ? InventoryUtils.findBestChestplate(0, 35) : InventoryUtils.find(Items.ELYTRA);
         if (slot == -1) {
             ChatUtils.sendMessage(I18n.translate(elytra ? "modules.elytrahelper.chestplatenotfound" : "modules.elytrahelper.elytranotfound"));
             return;
         }

         if (sync.getValue()) InventoryUtils.bypassSwap(slot, 6);
         else InventoryUtils.swap(slot, 6);
         ChatUtils.sendMessage(I18n.translate(elytra ? "modules.elytrahelper.swappedonchestplate" : "modules.elytrahelper.swappedonelytra"));
    }
}