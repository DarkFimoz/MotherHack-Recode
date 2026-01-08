package fun.motherhack.api.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.rotations.EventMotion;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.impl.misc.NoPush;
import fun.motherhack.modules.impl.movement.NoSlow;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Final @Shadow public ClientPlayNetworkHandler networkHandler;
    @Shadow private double lastX, lastBaseY, lastZ;
    @Shadow protected boolean isCamera() {
        return false;
    }
    @Shadow private void sendSprintingPacket() {}
    @Final @Shadow protected MinecraftClient client;
    @Shadow private float lastYaw, lastPitch;
    @Shadow private boolean lastOnGround, lastHorizontalCollision, autoJumpEnabled;
    @Shadow private int ticksSinceLastPositionPacketSent;
    @Unique private final EventMotion event = new EventMotion(0f, 0f);

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        EventPlayerTick event = new EventPlayerTick();
        MotherHack.getInstance().getEventHandler().post(event);
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    public void pushOutOfBlocks(double x, double z, CallbackInfo ci) {
        if (MotherHack.getInstance().getModuleManager().getModule(NoPush.class).isToggled() && MotherHack.getInstance().getModuleManager().getModule(NoPush.class).blocks.getValue()) ci.cancel();
    }

    @ModifyExpressionValue(method = "shouldStopSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    public boolean shouldStopSprinting(boolean original) {
        if (MotherHack.getInstance().getModuleManager().getModule(NoSlow.class).isToggled()) return false;
        else return original;
    }

    @Overwrite
    public void sendMovementPackets() {
        this.sendSprintingPacket();
        if (this.isCamera()) {
            event.setYaw(getYaw());
            event.setPitch(getPitch());

            MotherHack.getInstance().getEventHandler().post(event);

            if (event.isCancelled()) {
                event.resume();
                return;
            }

            double d = this.getX() - this.lastX;
            double e = this.getY() - this.lastBaseY;
            double f = this.getZ() - this.lastZ;
            double g = event.getYaw() - this.lastYaw;
            double h = event.getPitch() - this.lastPitch;
            ++this.ticksSinceLastPositionPacketSent;
            boolean bl = MathHelper.squaredMagnitude(d, e, f) > MathHelper.square(2.0E-4) || this.ticksSinceLastPositionPacketSent >= 20;
            boolean bl2 = g != (double)0.0F || h != (double)0.0F;
            if (bl && bl2) {
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(this.getX(), this.getY(), this.getZ(), event.getYaw(), event.getPitch(), this.isOnGround(), this.horizontalCollision));
            } else if (bl) {
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(this.getX(), this.getY(), this.getZ(), this.isOnGround(), this.horizontalCollision));
            } else if (bl2) {
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(event.getYaw(), event.getPitch(), this.isOnGround(), this.horizontalCollision));
            } else if (this.lastOnGround != this.isOnGround() || this.lastHorizontalCollision != this.horizontalCollision) {
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(this.isOnGround(), this.horizontalCollision));
            }

            if (bl) {
                this.lastX = this.getX();
                this.lastBaseY = this.getY();
                this.lastZ = this.getZ();
                this.ticksSinceLastPositionPacketSent = 0;
            }

            if (bl2) {
                this.lastYaw = event.getYaw();
                this.lastPitch = event.getPitch();
            }

            this.lastOnGround = this.isOnGround();
            this.lastHorizontalCollision = this.horizontalCollision;
            this.autoJumpEnabled = this.client.options.getAutoJump().getValue();
        }
    }
}