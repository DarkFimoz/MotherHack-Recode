package fun.motherhack.api.mixins.accessors;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldRenderer.class)
public interface IWorldRenderer {

    @Accessor("frustum") Frustum getFrustum();
}