package fun.motherhack.modules.impl.combat;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.modules.settings.api.Nameable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import fun.motherhack.api.events.impl.EventPlayerTick;

public class AutoBuff extends Module {
    private final BooleanSetting strength = new BooleanSetting("Сила", true);
    private final BooleanSetting speed = new BooleanSetting("Скорость", true);
    private final BooleanSetting fire = new BooleanSetting("Огнестойкость", true);
    private final BooleanSetting heal = new BooleanSetting("Лечение", true);
    private final NumberSetting healthH = new NumberSetting("Здоровье для лечения", 8f, 0f, 20f, 0.5f);
    private final BooleanSetting regen = new BooleanSetting("Регенерация", true);
    private final EnumSetting<TriggerMode> triggerOn = new EnumSetting<>("Триггер", TriggerMode.LackOfRegen);
    private final NumberSetting healthR = new NumberSetting("HP для регена", 8f, 0f, 20f, 0.5f);
    private final BooleanSetting onDaGround = new BooleanSetting("Только на земле", true);
    private final BooleanSetting pauseAura = new BooleanSetting("Пауза ауры", false);

    private long timer = 0;
    private boolean spoofed = false;

    @AllArgsConstructor
    @Getter
    public enum TriggerMode implements Nameable {
        LackOfRegen("Отсутствие регена"),
        Health("Здоровье");

        private final String name;
    }

    public AutoBuff() {
        super("AutoBuff", Category.Combat);
        getSettings().add(strength);
        getSettings().add(speed);
        getSettings().add(fire);
        getSettings().add(heal);
        getSettings().add(healthH);
        getSettings().add(regen);
        getSettings().add(triggerOn);
        getSettings().add(healthR);
        getSettings().add(onDaGround);
        getSettings().add(pauseAura);
    }

    public static int getPotionSlot(Potions potion) {
        for (int i = 0; i < 9; ++i)
            if (isStackPotion(mc.player.getInventory().getStack(i), potion)) return i;
        return -1;
    }

    public static boolean isPotionOnHotBar(Potions potions) {
        return getPotionSlot(potions) != -1;
    }

    public static boolean isStackPotion(ItemStack stack, Potions potion) {
        if (stack == null) return false;
        if (stack.getItem() instanceof SplashPotionItem) {
            PotionContentsComponent potionContentsComponent = stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
            RegistryEntry<StatusEffect> id = null;
            switch (potion) {
                case STRENGTH -> id = StatusEffects.STRENGTH;
                case SPEED -> id = StatusEffects.SPEED;
                case FIRERES -> id = StatusEffects.FIRE_RESISTANCE;
                case HEAL -> id = StatusEffects.INSTANT_HEALTH;
                case REGEN -> id = StatusEffects.REGENERATION;
            }
            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
                if (effect.getEffectType() == id) return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onTick(EventPlayerTick event) {
        if (fullNullCheck()) return;

        Aura auraModule = MotherHack.getInstance().getModuleManager().getModule(Aura.class);
        if (auraModule != null && auraModule.isToggled() && mc.player.getAttackCooldownProgress(1) > 0.5f) return;

        if (mc.player.age > 80 && shouldThrow()) {
            spoofed = true;
        }

        if (onDaGround.getValue() && !mc.player.isOnGround()) return;

        if (mc.player.age > 80 && shouldThrow() && System.currentTimeMillis() - timer > 1000 && spoofed) {
            if (!mc.player.hasStatusEffect(StatusEffects.SPEED) && isPotionOnHotBar(Potions.SPEED) && speed.getValue())
                throwPotion(Potions.SPEED);
            if (!mc.player.hasStatusEffect(StatusEffects.STRENGTH) && isPotionOnHotBar(Potions.STRENGTH) && strength.getValue())
                throwPotion(Potions.STRENGTH);
            if (!mc.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE) && isPotionOnHotBar(Potions.FIRERES) && fire.getValue())
                throwPotion(Potions.FIRERES);
            if (mc.player.getHealth() + mc.player.getAbsorptionAmount() < healthH.getValue() && heal.getValue() && isPotionOnHotBar(Potions.HEAL))
                throwPotion(Potions.HEAL);
            if (((!mc.player.hasStatusEffect(StatusEffects.REGENERATION) && triggerOn.getValue() == TriggerMode.LackOfRegen) || (mc.player.getHealth() + mc.player.getAbsorptionAmount() < healthR.getValue() && triggerOn.getValue() == TriggerMode.Health)) && isPotionOnHotBar(Potions.REGEN) && regen.getValue())
                throwPotion(Potions.REGEN);

            if (mc.player.networkHandler != null) {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
            }
            timer = System.currentTimeMillis();
            spoofed = false;
        }
    }

    private boolean shouldThrow() {
        return (!mc.player.hasStatusEffect(StatusEffects.SPEED) && isPotionOnHotBar(Potions.SPEED) && speed.getValue()) ||
                (!mc.player.hasStatusEffect(StatusEffects.STRENGTH) && isPotionOnHotBar(Potions.STRENGTH) && strength.getValue()) ||
                (!mc.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE) && isPotionOnHotBar(Potions.FIRERES) && fire.getValue()) ||
                (mc.player.getHealth() + mc.player.getAbsorptionAmount() < healthH.getValue() && isPotionOnHotBar(Potions.HEAL) && heal.getValue()) ||
                (!mc.player.hasStatusEffect(StatusEffects.REGENERATION) && triggerOn.getValue() == TriggerMode.LackOfRegen && isPotionOnHotBar(Potions.REGEN) && regen.getValue()) ||
                (mc.player.getHealth() + mc.player.getAbsorptionAmount() < healthR.getValue() && triggerOn.getValue() == TriggerMode.Health && isPotionOnHotBar(Potions.REGEN) && regen.getValue());
    }

    public void throwPotion(Potions potion) {
        if (pauseAura.getValue()) {
            Aura auraModule = MotherHack.getInstance().getModuleManager().getModule(Aura.class);
            if (auraModule != null && auraModule.isToggled()) {
            }
        }
        int slot = getPotionSlot(potion);
        if (slot == -1) return;
        
        int oldSlot = mc.player.getInventory().selectedSlot;
        if (mc.player.networkHandler != null) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
        }
    }

    public enum Potions {
        STRENGTH, SPEED, FIRERES, HEAL, REGEN
    }
}
