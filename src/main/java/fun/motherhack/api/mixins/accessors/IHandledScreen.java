package fun.motherhack.api.mixins.accessors;

import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.client.gui.screen.ingame.HandledScreen.class)
public interface IHandledScreen {
    @Accessor("focusedSlot")
    Slot getFocusedSlot();
}
