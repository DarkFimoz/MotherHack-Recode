package fun.motherhack.api.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventFov;
import fun.motherhack.api.events.impl.EventMouse;
import fun.motherhack.api.events.impl.EventMouseRotation;
import fun.motherhack.modules.impl.render.Zoom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {

    @Final @Shadow private MinecraftClient client;
    @Shadow public double cursorDeltaX, cursorDeltaY;

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    public void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (MotherHack.getInstance() == null || MotherHack.getInstance().getEventHandler() == null) return;
        EventMouse event = new EventMouse(button, action);
        MotherHack.getInstance().getEventHandler().post(event);
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"))
    public void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (MotherHack.getInstance() == null || MotherHack.getInstance().getModuleManager() == null) return;
        if (vertical != 0) {
            Zoom zoom = MotherHack.getInstance().getModuleManager().getModule(Zoom.class);
            if (zoom != null && zoom.isToggled()) {
                zoom.mouseScroll((float) vertical);
            }
        }
    }

    @Inject(method = "updateMouse", at = @At("HEAD"))
    private void onUpdateMouse(double timeDelta, CallbackInfo ci) {
        if (MotherHack.getInstance() == null || MotherHack.getInstance().getEventHandler() == null) return;
        EventFov event = new EventFov();
        MotherHack.getInstance().getEventHandler().post(event);
        if (event.isCancelled()) {
            double slowdown = (double) event.getFov() / client.options.getFov().getValue();
            this.cursorDeltaX *= slowdown;
            this.cursorDeltaY *= slowdown;
        }
    }

    @WrapWithCondition(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"), require = 1, allow = 1)
    private boolean modifyMouseRotationInput(ClientPlayerEntity instance, double cursorDeltaX, double cursorDeltaY) {
        if (MotherHack.getInstance() == null || MotherHack.getInstance().getEventHandler() == null) return true;
        EventMouseRotation event = new EventMouseRotation((float) cursorDeltaX, (float) cursorDeltaY);
        MotherHack.getInstance().getEventHandler().post(event);
        if (event.isCancelled()) return false;
        instance.changeLookDirection(event.getCursorDeltaX(), event.getCursorDeltaY());
        return false;
    }
}