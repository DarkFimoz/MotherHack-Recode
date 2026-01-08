package fun.motherhack.api.mixins.accessors;

import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;

@Mixin(BossBarHud.class)
public interface IBossBarHud {
	
	@Accessor("bossBars") Map<UUID, ClientBossBar> getBossBars();
}