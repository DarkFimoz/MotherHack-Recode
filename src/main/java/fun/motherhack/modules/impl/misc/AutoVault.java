package fun.motherhack.modules.impl.misc;

import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.utils.math.TimerUtils;
import fun.motherhack.utils.world.InventoryUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.VaultBlock;
import net.minecraft.block.entity.VaultBlockEntity;
import net.minecraft.block.enums.VaultState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AutoVault extends Module {

    private final EnumSetting<InventoryUtils.Switch> autoSwitch = new EnumSetting<>("settings.switch", InventoryUtils.Switch.Normal);
    private final EnumSetting<InventoryUtils.Swing> swing = new EnumSetting<>("settings.swing", InventoryUtils.Swing.MainHand);
    private final BooleanSetting enchantedGoldenApple = new BooleanSetting("settings.autovault.enchantedgoldenapple", true);
    private final BooleanSetting trident = new BooleanSetting("settings.autovault.trident", true);
    private final BooleanSetting ominousBottle = new BooleanSetting("settings.autovault.ominousbottle", true);
    private final BooleanSetting heavyCore = new BooleanSetting("settings.autovault.heavycore", true);

    public AutoVault() {
        super("AutoVault", Category.Misc);
    }

    private final TimerUtils timer = new TimerUtils();

    @EventHandler
    public void onPlayerTick(EventPlayerTick e) {
        if (fullNullCheck()) return;

        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos pos = mc.player.getBlockPos().add(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() != Blocks.VAULT) continue;
                    if (mc.world.getBlockState(pos).get(VaultBlock.VAULT_STATE) == VaultState.INACTIVE
                            || mc.world.getBlockState(pos).get(VaultBlock.VAULT_STATE) == VaultState.EJECTING
                            || mc.world.getBlockState(pos).get(VaultBlock.VAULT_STATE) == VaultState.UNLOCKING
                    ) continue;
                    if (!(mc.world.getBlockEntity(pos) instanceof VaultBlockEntity entity)) continue;
                    if (shouldClickVault(entity) && timer.passed(100)) {
                        clickVault(pos, mc.world.getBlockState(pos), entity);
                        setToggled(false);
                    }
                }
            }
        }
    }

    private boolean shouldClickVault(VaultBlockEntity entity) {
        ItemStack stack = entity.getSharedData().getDisplayItem();
        if (stack.isEmpty()) return false;
        if (enchantedGoldenApple.getValue() && stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) return true;
        if (trident.getValue() && stack.getItem() == Items.TRIDENT) return true;
        if (ominousBottle.getValue() && stack.getItem() == Items.OMINOUS_BOTTLE) return true;

        return heavyCore.getValue() && stack.getItem() == Items.HEAVY_CORE;
    }

    private void clickVault(BlockPos pos, BlockState state, VaultBlockEntity entity) {
        int previousSlot = mc.player.getInventory().selectedSlot;
        int slot = InventoryUtils.findHotbar(state.get(VaultBlock.OMINOUS) ? Items.OMINOUS_TRIAL_KEY : Items.TRIAL_KEY);
        if (slot == -1) {
            mc.player.sendMessage(Text.of("[MotherHack] Slot with ominous is empty"), false);
            return;
        }
        mc.player.sendMessage(Text.of("[MotherHack] " + entity.getSharedData().getDisplayItem().getItem().getName().getString()), false);
        InventoryUtils.switchSlot(autoSwitch.getValue(), slot, previousSlot);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
        InventoryUtils.swing(swing.getValue());
        InventoryUtils.switchBack(autoSwitch.getValue(), slot, previousSlot);
        timer.reset();
    }
}