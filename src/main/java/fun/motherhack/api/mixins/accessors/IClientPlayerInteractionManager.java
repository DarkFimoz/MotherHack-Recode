package fun.motherhack.api.mixins.accessors;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface IClientPlayerInteractionManager {

    @Invoker("syncSelectedSlot") void syncSelectedSlot$drug();
    
    @Accessor("currentBreakingPos") BlockPos getCurrentBreakingPos();
    
    @Accessor("currentBreakingProgress") float getCurrentBreakingProgress();
}