package fun.motherhack.api.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import fun.motherhack.MotherHack;
import fun.motherhack.modules.impl.render.NoRender;
import net.minecraft.entity.LimbAnimator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LimbAnimator.class)
public abstract class LimbAnimatorMixin {

    @ModifyReturnValue(method = "getPos()F", at = @At("RETURN"))
    public float getPos(float original) {
        if (MotherHack.getInstance().getModuleManager().getModule(NoRender.class).isToggled() && MotherHack.getInstance().getModuleManager().getModule(NoRender.class).limbs.getValue()) return 0;
        else return original;
    }

    @ModifyReturnValue(method = "getPos(F)F", at = @At("RETURN"))
    public float getPos2(float original) {
        if (MotherHack.getInstance().getModuleManager().getModule(NoRender.class).isToggled() && MotherHack.getInstance().getModuleManager().getModule(NoRender.class).limbs.getValue()) return 0;
        else return original;
    }

    @ModifyReturnValue(method = "getSpeed()F", at = @At("RETURN"))
    public float getSpeed(float original) {
        if (MotherHack.getInstance().getModuleManager().getModule(NoRender.class).isToggled() && MotherHack.getInstance().getModuleManager().getModule(NoRender.class).limbs.getValue()) return 0;
        else return original;
    }

    @ModifyReturnValue(method = "getSpeed(F)F", at = @At("RETURN"))
    public float getSpeed2(float original) {
        if (MotherHack.getInstance().getModuleManager().getModule(NoRender.class).isToggled() && MotherHack.getInstance().getModuleManager().getModule(NoRender.class).limbs.getValue()) return 0;
        else return original;
    }
}