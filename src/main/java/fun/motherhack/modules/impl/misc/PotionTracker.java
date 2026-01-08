package fun.motherhack.modules.impl.misc;

import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.utils.network.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.util.Formatting;

public class PotionTracker extends Module {

	public PotionTracker() {
		super("PotionTracker", Category.Misc);
	}

	@EventHandler
	public void onPacketReceive(EventPacket.Receive e) {
		if (fullNullCheck()) return;
		
		if (e.getPacket() instanceof EntityStatusEffectS2CPacket packet) {
			if (!(mc.world.getEntityById(packet.getEntityId()) instanceof PlayerEntity player)) return;
			if (player.hasStatusEffect(packet.getEffectId())) return;
			String name = mc.world.getEntityById(packet.getEntityId()).getDisplayName().getString();
			String potion = I18n.translate(packet.getEffectId().value().getTranslationKey());
			String[] romans = {
				    "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
				    "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX",
				    "XXI", "XXII", "XXIII", "XXIV", "XXV", "XXVI", "XXVII", "XXVIII", "XXIX", "XXX"
			};
			String amplifier = (packet.getAmplifier() + 1 < romans.length) ? romans[packet.getAmplifier() + 1] : packet.getAmplifier() + 1 + "";
			String duration = String.format("%02d:%02d", packet.getDuration() / 20 / 60, packet.getDuration() / 20 % 60);
			ChatUtils.sendMessage(
					I18n.translate("modules.potiontracker.message", 
							Formatting.RED + name + Formatting.WHITE,
							Formatting.GOLD + potion + " " + amplifier + Formatting.WHITE,
							Formatting.GRAY + duration + Formatting.WHITE
					)
			);
		}
	}
}