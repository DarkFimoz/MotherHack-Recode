package fun.motherhack.utils.world;

import fun.motherhack.MotherHack;
import fun.motherhack.api.mixins.accessors.IClientPlayerInteractionManager;
import fun.motherhack.modules.impl.movement.GuiMove;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.utils.Wrapper;
import fun.motherhack.utils.network.NetworkUtils;
import lombok.AllArgsConstructor;
import lombok.experimental.UtilityClass;
import net.minecraft.block.BlockState;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

@UtilityClass
public class InventoryUtils implements Wrapper {

    @AllArgsConstructor
    public enum Switch implements Nameable {
        Normal("settings.normal"),
        Silent("settings.switch.silent"),
        Alternative("settings.switch.alternative"),
        None("settings.none");

        private final String name;

        @Override
        public String getName() {
            return name;
        }
    }

    public enum Swap {
        Pickup,
        Swap
    }

    @AllArgsConstructor
    public enum Swing implements Nameable {
        MainHand("settings.swing.mainhand"),
        OffHand("settings.swing.offhand"),
        Packet("settings.packet"),
        None("settings.none");

        private final String name;

        @Override
        public String getName() {
            return name;
        }
    }

    public int indexToSlot(int index) {
        if (index >= 0 && index <= 8) return 36 + index;
        return index;
    }

    public void switchSlot(Switch mode, int slot, int previousSlot) {
        if (slot == -1 || previousSlot == -1 || slot == MotherHack.getInstance().getServerManager().getServerSlot()) return;

        switch (mode) {
            case Normal -> {
                mc.player.getInventory().selectedSlot = slot;
                ((IClientPlayerInteractionManager) mc.interactionManager).syncSelectedSlot$drug();
            }
            case Silent -> NetworkUtils.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            case Alternative -> swap(Swap.Swap, slot, previousSlot);
        }
    }

    public void switchBack(Switch mode, int slot, int previousSlot) {
        if (slot == -1 || previousSlot == -1 || slot == MotherHack.getInstance().getServerManager().getServerSlot()) return;

        switch (mode) {
            case Normal -> {
                mc.player.getInventory().selectedSlot = previousSlot;
                ((IClientPlayerInteractionManager) mc.interactionManager).syncSelectedSlot$drug();
            }
            case Silent -> NetworkUtils.sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
            case Alternative -> swap(Swap.Swap, slot, previousSlot);
        }
    }

    public void swap(Swap mode, int slot, int targetSlot) {
        if (slot == -1 || targetSlot == -1) return;
        switch (mode) {
            case Pickup -> {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, indexToSlot(slot), 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, indexToSlot(targetSlot), 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, indexToSlot(slot), 0, SlotActionType.PICKUP, mc.player);
            }
            case Swap -> mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, targetSlot, SlotActionType.SWAP, mc.player);
        }
    }
    
    public void bypassSwap(int slot, int targetSlot) {
        if (slot == -1 || targetSlot == -1) return;
        
        if (MotherHack.getInstance().getModuleManager().getModule(GuiMove.class).funtime.getValue()) {
           	MotherHack.getInstance().getModuleManager().getModule(GuiMove.class).setTicks(8);
           	
        	new Thread(() -> {
                try {
                	if (MotherHack.getInstance().getModuleManager().getModule(GuiMove.class).funtime.getValue()) Thread.sleep(MotherHack.getInstance().getModuleManager().getModule(GuiMove.class).getTicks() * 50L);
                    mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, indexToSlot(slot), 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, indexToSlot(slot), 0, SlotActionType.PICKUP, mc.player);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        } else swap(slot, targetSlot);
    }
    
    public void swap(int slot, int targetSlot) {
        if (slot == -1 || targetSlot == -1) return;
		 mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, InventoryUtils.indexToSlot(slot), 0, SlotActionType.PICKUP, mc.player);
	     mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
	     mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, InventoryUtils.indexToSlot(slot), 0, SlotActionType.PICKUP, mc.player);
    }

    public void swing(Swing mode) {
        switch (mode) {
            case MainHand -> mc.player.swingHand(Hand.MAIN_HAND);
            case OffHand -> mc.player.swingHand(Hand.OFF_HAND);
            case Packet -> NetworkUtils.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
    }

    public int findBestSword(int start, int end) {
        int netheriteSlot = -1;
        int diamondSlot = -1;
        int ironSlot = -1;
        int goldenSlot = -1;
        int stoneSlot = -1;
        int woodenSlot = -1;

        for (int i = end; i >= start; i--) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.NETHERITE_SWORD) netheriteSlot = i;
            else if (stack.getItem() == Items.DIAMOND_SWORD) diamondSlot = i;
            else if (stack.getItem() == Items.IRON_SWORD) ironSlot = i;
            else if (stack.getItem() == Items.GOLDEN_SWORD) goldenSlot = i;
            else if (stack.getItem() == Items.STONE_SWORD) stoneSlot = i;
            else if (stack.getItem() == Items.WOODEN_SWORD) woodenSlot = i;
        }

        if (netheriteSlot != -1) return netheriteSlot;
        if (diamondSlot != -1) return diamondSlot;
        if (ironSlot != -1) return ironSlot;
        if (goldenSlot != -1) return goldenSlot;
        if (stoneSlot != -1) return stoneSlot;

        return woodenSlot;
    }

    public int findBestAxe(int start, int end) {
        int netheriteSlot = -1;
        int diamondSlot = -1;
        int ironSlot = -1;
        int goldenSlot = -1;
        int stoneSlot = -1;
        int woodenSlot = -1;

        for (int i = end; i >= start; i--) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.NETHERITE_AXE) netheriteSlot = i;
            else if (stack.getItem() == Items.DIAMOND_AXE) diamondSlot = i;
            else if (stack.getItem() == Items.IRON_AXE) ironSlot = i;
            else if (stack.getItem() == Items.GOLDEN_AXE) goldenSlot = i;
            else if (stack.getItem() == Items.STONE_AXE) stoneSlot = i;
            else if (stack.getItem() == Items.WOODEN_AXE) woodenSlot = i;
        }

        if (netheriteSlot != -1) return netheriteSlot;
        if (diamondSlot != -1) return diamondSlot;
        if (ironSlot != -1) return ironSlot;
        if (goldenSlot != -1) return goldenSlot;
        if (stoneSlot != -1) return stoneSlot;

        return woodenSlot;
    }

    public int findFastItem(BlockState blockState, int start, int end) {
        double bestScore = -1;
        int bestSlot = -1;

        for (int i = start; i <= end; i++) {
            double score = mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(blockState);

            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    public int findBestChestplate(int start, int end) {
        int leatherSlot = -1;
        int chainmail = -1;
        int ironSlot = -1;
        int goldenSlot = -1;
        int diamondSlot = -1;
        int netheriteSlot = -1;

        for (int i = end; i >= start; i--) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.LEATHER_CHESTPLATE) leatherSlot = i;
            else if (stack.getItem() == Items.CHAINMAIL_CHESTPLATE) chainmail = i;
            else if (stack.getItem() == Items.IRON_CHESTPLATE) ironSlot = i;
            else if (stack.getItem() == Items.GOLDEN_CHESTPLATE) goldenSlot = i;
            else if (stack.getItem() == Items.DIAMOND_CHESTPLATE) diamondSlot = i;
            else if (stack.getItem() == Items.NETHERITE_CHESTPLATE) netheriteSlot = i;
        }

        if (chainmail != -1) return chainmail;
        if (ironSlot != -1) return ironSlot;
        if (goldenSlot != -1) return goldenSlot;
        if (diamondSlot != -1) return diamondSlot;
        if (netheriteSlot != -1) return netheriteSlot;

        return leatherSlot;
    }

    public int getArmorColor(PlayerEntity entity, int slot) {
        ItemStack stack = entity.getInventory().getArmorStack(slot);
        if (stack.isIn(ItemTags.DYEABLE)) return DyedColorComponent.getColor(stack, -6265536);
        return -1;
    }

    public int find(Item item) { return find(item, 0, 35); }
    public int findHotbar(Item item) { return find(item, 0, 8); }
    public int findInventory(Item item) { return find(item, 9, 35); }
    public int find(Class<? extends Item> item) { return find(item, 0, 35); }
    public int findHotbar(Class<? extends Item> item) { return find(item, 0, 8); }
    public int findInventory(Class<? extends Item> item) { return find(item, 9, 35); }

    public int find(Item item, int start, int end) {
        for (int i = end; i >= start; i--) if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        return -1;
    }
    
    public int find(Class<? extends Item> item, int start, int end) {
        for (int i = end; i >= start; i--) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.getItem().getClass().isAssignableFrom(item)) continue;

            return i;
        }

        return -1;
    }

    public int findEmptySlot(int start, int end) {
        for (int i = end; i >= start; i--) if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        return -1;
    }
}