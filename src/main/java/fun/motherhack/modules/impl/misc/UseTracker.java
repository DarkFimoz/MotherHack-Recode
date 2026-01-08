package fun.motherhack.modules.impl.misc;

import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.utils.network.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.PotionItem;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Formatting;

public class UseTracker extends Module {

	public UseTracker() {
		super("UseTracker", Category.Misc);
	}
	
	@EventHandler
	public void onPacketReceive(EventPacket.Receive e) {
		if (fullNullCheck()) return;
		if (e.getPacket() instanceof EntityStatusS2CPacket packet) {
			if (!(packet.getEntity(mc.world) instanceof PlayerEntity player) || packet.getStatus() != 9) return;
			String name = player.getDisplayName().getString();
			String item = player.getMainHandStack().getItem().getName().getString();
			boolean potion = player.getMainHandStack().getItem() instanceof PotionItem;
			ChatUtils.sendMessage(
					potion ? I18n.translate("modules.usetracker.message2",
							Formatting.RED + name + Formatting.WHITE,
							Formatting.GOLD + item + Formatting.WHITE)
					: I18n.translate("modules.usetracker.message",
							Formatting.RED + name + Formatting.WHITE,
							Formatting.GOLD + item + Formatting.WHITE)
			);
		}
	}
}