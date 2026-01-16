package fun.motherhack.modules.impl.client;

import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.ButtonSetting;
import fun.motherhack.utils.network.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

public class VanillaDisabler extends Module {

    private final BooleanSetting godMode = new BooleanSetting("God Mode", false);
    private final BooleanSetting creativeMode = new BooleanSetting("Creative", false);
    private final ButtonSetting dupeButton = new ButtonSetting("Dupe Item", this::dupeItem);

    private GameMode previousGameMode = GameMode.SURVIVAL;

    public VanillaDisabler() {
        super("VanillaDisabler", Category.Client);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (fullNullCheck()) return;
        
        if (!mc.isInSingleplayer()) {
            ChatUtils.sendMessage(I18n.translate("modules.vanilladisabler.singleplayeronly"));
            setToggled(false);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (fullNullCheck()) return;
        
        ServerPlayerEntity serverPlayer = getServerPlayer();
        if (serverPlayer != null) {
            serverPlayer.getAbilities().invulnerable = serverPlayer.interactionManager.getGameMode() == GameMode.CREATIVE;
            serverPlayer.sendAbilitiesUpdate();
            
            if (creativeMode.getValue()) {
                serverPlayer.changeGameMode(previousGameMode);
            }
        }
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        
        if (!mc.isInSingleplayer()) {
            setToggled(false);
            return;
        }

        ServerPlayerEntity serverPlayer = getServerPlayer();
        if (serverPlayer == null) return;

        if (godMode.getValue()) {
            serverPlayer.getAbilities().invulnerable = true;
            serverPlayer.sendAbilitiesUpdate();
        }

        if (creativeMode.getValue()) {
            if (serverPlayer.interactionManager.getGameMode() != GameMode.CREATIVE) {
                previousGameMode = serverPlayer.interactionManager.getGameMode();
                serverPlayer.changeGameMode(GameMode.CREATIVE);
            }
        } else {
            if (serverPlayer.interactionManager.getGameMode() == GameMode.CREATIVE && previousGameMode != GameMode.CREATIVE) {
                serverPlayer.changeGameMode(previousGameMode);
            }
        }
    }

    private void dupeItem() {
        if (fullNullCheck()) {
            return;
        }
        
        if (!mc.isInSingleplayer()) {
            ChatUtils.sendMessage(I18n.translate("modules.vanilladisabler.singleplayeronly"));
            return;
        }

        ServerPlayerEntity serverPlayer = getServerPlayer();
        if (serverPlayer == null) return;
        
        ItemStack heldItem = serverPlayer.getMainHandStack();
        if (heldItem.isEmpty()) {
            ChatUtils.sendMessage(I18n.translate("modules.vanilladisabler.noitem"));
            return;
        }
        
        ItemStack duped = heldItem.copy();
        duped.setCount(heldItem.getMaxCount());
        
        int emptySlot = serverPlayer.getInventory().getEmptySlot();
        if (emptySlot != -1) {
            serverPlayer.getInventory().setStack(emptySlot, duped);
            serverPlayer.playerScreenHandler.sendContentUpdates();
            ChatUtils.sendMessage(I18n.translate("modules.vanilladisabler.duped"));
        } else {
            ChatUtils.sendMessage(I18n.translate("modules.vanilladisabler.noslot"));
        }
    }

    private ServerPlayerEntity getServerPlayer() {
        if (mc.getServer() == null || mc.player == null) return null;
        return mc.getServer().getPlayerManager().getPlayer(mc.player.getUuid());
    }
}
