package fun.motherhack.modules.impl.misc;

import com.mojang.authlib.GameProfile;
import fun.motherhack.api.events.impl.EventAttackEntity;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.StringSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FakePlayer extends Module {
    private final BooleanSetting copyInventory = new BooleanSetting("CopyInventory", false);
    private final BooleanSetting record = new BooleanSetting("Record", false);
    private final BooleanSetting play = new BooleanSetting("Play", false);
    private final BooleanSetting autoTotem = new BooleanSetting("AutoTotem", false);
    private final StringSetting name = new StringSetting("Name", "FakePlayer", false);

    public static OtherClientPlayerEntity fakePlayer;
    private final List<PlayerState> positions = new ArrayList<>();
    private int movementTick, deathTime;

    public FakePlayer() {
        super("FakePlayer", Category.Misc);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (fullNullCheck()) return;

        fakePlayer = new OtherClientPlayerEntity(mc.world, new GameProfile(
                UUID.fromString("66123666-6666-6666-6666-666666666600"), name.getValue()));
        fakePlayer.copyPositionAndRotation(mc.player);

        if (copyInventory.getValue()) {
            fakePlayer.setStackInHand(Hand.MAIN_HAND, mc.player.getMainHandStack().copy());
            fakePlayer.setStackInHand(Hand.OFF_HAND, mc.player.getOffHandStack().copy());
            fakePlayer.getInventory().setStack(36, mc.player.getInventory().getStack(36).copy());
            fakePlayer.getInventory().setStack(37, mc.player.getInventory().getStack(37).copy());
            fakePlayer.getInventory().setStack(38, mc.player.getInventory().getStack(38).copy());
            fakePlayer.getInventory().setStack(39, mc.player.getInventory().getStack(39).copy());
        }

        mc.world.addEntity(fakePlayer);
        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 9999, 2));
        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 9999, 4));
        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 9999, 1));
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive e) {
        if (fullNullCheck() || fakePlayer == null) return;

        if (e.getPacket() instanceof ExplosionS2CPacket explosion && fakePlayer.hurtTime == 0) {
            // В 1.21.4 позиция взрыва получается через center()
            Vec3d explosionPos = explosion.center();
            fakePlayer.onDamaged(mc.world.getDamageSources().generic());
            float damage = calculateExplosionDamage(explosionPos, fakePlayer);
            fakePlayer.setHealth(fakePlayer.getHealth() + fakePlayer.getAbsorptionAmount() - damage);

            if (fakePlayer.isDead()) {
                // Симулируем использование тотема
                if (fakePlayer.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
                    fakePlayer.setHealth(10f);
                    fakePlayer.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick e) {
        if (fullNullCheck()) return;

        if (record.getValue()) {
            positions.add(new PlayerState(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    mc.player.getYaw(), mc.player.getPitch()));
            return;
        }

        if (fakePlayer != null) {
            if (play.getValue() && !positions.isEmpty()) {
                movementTick++;
                if (movementTick >= positions.size()) {
                    movementTick = 0;
                    return;
                }
                PlayerState p = positions.get(movementTick);
                fakePlayer.setYaw(p.yaw);
                fakePlayer.setPitch(p.pitch);
                fakePlayer.setHeadYaw(p.yaw);
                fakePlayer.updateTrackedPosition(p.x, p.y, p.z);
                fakePlayer.updateTrackedPositionAndAngles(p.x, p.y, p.z, p.yaw, p.pitch, 3);
            } else {
                movementTick = 0;
            }

            if (autoTotem.getValue() && fakePlayer.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                fakePlayer.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            }

            if (fakePlayer.isDead()) {
                deathTime++;
                if (deathTime > 10) toggle();
            }
        }
    }

    @EventHandler
    public void onAttack(EventAttackEntity e) {
        if (fullNullCheck() || fakePlayer == null) return;
        if (e.getTarget() != fakePlayer || fakePlayer.hurtTime != 0) return;

        mc.world.playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1f, 1f);

        if (mc.player.fallDistance > 0) {
            mc.world.playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1f, 1f);
        }

        fakePlayer.onDamaged(mc.world.getDamageSources().generic());

        float cooldown = mc.player.getAttackCooldownProgress(0f);
        float damage = cooldown >= 0.85f ? calculateHitDamage(mc.player.getMainHandStack(), fakePlayer) : 1f;
        fakePlayer.setHealth(fakePlayer.getHealth() + fakePlayer.getAbsorptionAmount() - damage);

        if (fakePlayer.isDead()) {
            // Симулируем использование тотема
            if (fakePlayer.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
                fakePlayer.setHealth(10f);
                fakePlayer.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
                new EntityStatusS2CPacket(fakePlayer, EntityStatuses.USE_TOTEM_OF_UNDYING).apply(mc.player.networkHandler);
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (fakePlayer != null) {
            fakePlayer.setRemoved(Entity.RemovalReason.KILLED);
            fakePlayer.onRemoved();
            fakePlayer = null;
        }
        positions.clear();
        deathTime = 0;
    }

    private float calculateExplosionDamage(Vec3d explosionPos, Entity target) {
        double distance = explosionPos.distanceTo(target.getPos());
        if (distance > 12) return 0;
        double exposure = 1.0 - (distance / 12.0);
        return (float) (exposure * exposure * 7 * 6 + 1);
    }

    private float calculateHitDamage(ItemStack stack, Entity target) {
        float baseDamage = 1.0f;
        if (stack != null && !stack.isEmpty()) {
            if (stack.getItem() == Items.DIAMOND_SWORD) baseDamage = 7.0f;
            else if (stack.getItem() == Items.NETHERITE_SWORD) baseDamage = 8.0f;
            else if (stack.getItem() == Items.IRON_SWORD) baseDamage = 6.0f;
            else if (stack.getItem() == Items.STONE_SWORD) baseDamage = 5.0f;
            else if (stack.getItem() == Items.WOODEN_SWORD || stack.getItem() == Items.GOLDEN_SWORD) baseDamage = 4.0f;
            else if (stack.getItem() == Items.DIAMOND_AXE) baseDamage = 9.0f;
            else if (stack.getItem() == Items.NETHERITE_AXE) baseDamage = 10.0f;
            else if (stack.getItem() == Items.IRON_AXE) baseDamage = 9.0f;
        }
        return baseDamage;
    }

    private record PlayerState(double x, double y, double z, float yaw, float pitch) {}
}
