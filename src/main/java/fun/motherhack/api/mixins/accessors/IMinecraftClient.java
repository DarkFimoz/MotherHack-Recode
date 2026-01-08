package fun.motherhack.api.mixins.accessors;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface IMinecraftClient {

    @Accessor("itemUseCooldown") void setItemUseCooldown(int itemUseCooldown);
    @Accessor("attackCooldown") void setAttackCooldown(int attackCooldown);
}