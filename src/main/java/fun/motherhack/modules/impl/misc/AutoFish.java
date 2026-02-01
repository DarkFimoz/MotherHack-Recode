package fun.motherhack.modules.impl.misc;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.api.Nameable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.api.events.impl.EventPacket;

public class AutoFish extends Module {
    private final EnumSetting<DetectMode> detectMode = new EnumSetting<>("Режим обнаружения", DetectMode.DataTracker);
    private final BooleanSetting rodSave = new BooleanSetting("Сохранить удочку", true);
    private final BooleanSetting changeRod = new BooleanSetting("Сменить удочку", false);
    private final BooleanSetting autoSell = new BooleanSetting("Автопродажа", false);

    private boolean flag = false;
    private long timeout = System.currentTimeMillis();
    private long cooldown = System.currentTimeMillis();

    @AllArgsConstructor
    @Getter
    public enum DetectMode implements Nameable {
        Sound("Звук"),
        DataTracker("Трекер данных");

        private final String name;
    }

    public AutoFish() {
        super("AutoFish", Category.Misc);
        getSettings().add(detectMode);
        getSettings().add(rodSave);
        getSettings().add(changeRod);
        getSettings().add(autoSell);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (fullNullCheck()) {
            setToggled(false);
            return;
        }
        flag = false;
        timeout = System.currentTimeMillis();
        cooldown = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        flag = false;
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive e) {
        if (e.getPacket() instanceof PlaySoundS2CPacket sound && detectMode.getValue() == DetectMode.Sound) {
            if (sound.getSound().value().equals(SoundEvents.ENTITY_FISHING_BOBBER_SPLASH) && 
                mc.player.fishHook != null && 
                mc.player.fishHook.squaredDistanceTo(sound.getX(), sound.getY(), sound.getZ()) < 4f) {
                catchFish();
            }
        }
    }

    @EventHandler
    public void onTick(EventPlayerTick event) {
        if (fullNullCheck()) return;

        if (mc.player.getMainHandStack().getItem() instanceof FishingRodItem) {
            if (mc.player.getMainHandStack().getDamage() > 52) {
                if (rodSave.getValue() && !changeRod.getValue()) {
                    setToggled(false);
                } else if (changeRod.getValue() && getRodSlot() != -1) {
                    int slot = getRodSlot();
                    mc.player.getInventory().selectedSlot = slot;
                    cooldown = System.currentTimeMillis();
                } else {
                    setToggled(false);
                }
            }
        }

        if (System.currentTimeMillis() - cooldown < 1000) return;

        if (System.currentTimeMillis() - timeout > 45000 && 
            mc.player.getMainHandStack().getItem() instanceof FishingRodItem) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            timeout = System.currentTimeMillis();
            cooldown = System.currentTimeMillis();
        }

        if (mc.player.fishHook != null && detectMode.getValue() == DetectMode.DataTracker) {
            try {
                java.lang.reflect.Field caughtFishField = FishingBobberEntity.class.getDeclaredField("CAUGHT_FISH");
                caughtFishField.setAccessible(true);
                net.minecraft.entity.data.TrackedData<Boolean> CAUGHT_FISH = 
                    (net.minecraft.entity.data.TrackedData<Boolean>) caughtFishField.get(null);
                boolean caughtFish = mc.player.fishHook.getDataTracker().get(CAUGHT_FISH);
                if (!flag && caughtFish) {
                    catchFish();
                    flag = true;
                } else if (!caughtFish) {
                    flag = false;
                }
            } catch (Exception e) {
            }
        }
    }

    private void catchFish() {
        new Thread(() -> {
            try {
                Thread.sleep((int) (Math.random() * 150 + 200));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            
            if (autoSell.getValue() && System.currentTimeMillis() - timeout > 1000) {
                mc.player.networkHandler.sendChatCommand("sellfish");
            }
            
            try {
                Thread.sleep((int) (Math.random() * 500 + 900));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            timeout = System.currentTimeMillis();
        }).start();
    }

    private int getRodSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack item = mc.player.getInventory().getStack(i);
            if (item.getItem() == Items.FISHING_ROD && item.getDamage() < 52) return i;
        }
        return -1;
    }
}
