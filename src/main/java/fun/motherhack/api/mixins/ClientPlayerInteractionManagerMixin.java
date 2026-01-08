package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventAttackEntity;
import fun.motherhack.api.events.impl.EventClickSlot;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    public void attackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        EventAttackEntity event = new EventAttackEntity(player, target);
        MotherHack.getInstance().getEventHandler().post(event);
    }

    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    public void clickSlot(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        EventClickSlot event = new EventClickSlot(syncId, slotId, button, actionType);
        MotherHack.getInstance().getEventHandler().post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}