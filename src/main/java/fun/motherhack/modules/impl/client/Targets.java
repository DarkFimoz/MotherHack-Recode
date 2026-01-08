package fun.motherhack.modules.impl.client;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.misc.Teams;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.ListSetting;
import fun.motherhack.utils.network.Server;
import fun.motherhack.utils.world.InventoryUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;

public class Targets extends Module {

    private final ListSetting targets = new ListSetting("Targets",
            new BooleanSetting("settings.targets.players", true),
            new BooleanSetting("settings.targets.invisibles", true),
            new BooleanSetting("settings.targets.passives", false),
            new BooleanSetting("settings.targets.hostiles", true)
    );

    public Targets() {
        super("Targets", Category.Client);
        setToggled(true);
    }

    public boolean isValid(LivingEntity entity) {
        if (entity == mc.player) return false;
        if (!entity.isAlive() || entity.getHealth() <= 0) return false;
        if (!mc.player.isAlive() || mc.player.getHealth() <= 0) return false;
        if (entity instanceof PlayerEntity player) {
            if (!targets.getName("settings.targets.players").getValue()) return false;
            if (player.hasStatusEffect(StatusEffects.INVISIBILITY) && !targets.getName("settings.targets.invisibles").getValue()) return false;
            if (MotherHack.getInstance().getFriendManager().isFriend(entity.getName().getString())) return false;
            if (Server.isBot(player)) return false;
            return !MotherHack.getInstance().getModuleManager().getModule(Teams.class).isToggled()
            		|| InventoryUtils.getArmorColor(player, 3) == InventoryUtils.getArmorColor(mc.player, 3);
        }
        if (entity instanceof PassiveEntity && !targets.getName("settings.targets.passives").getValue()) return false;
        if (entity instanceof HostileEntity && !targets.getName("settings.targets.hostiles").getValue()) return false;

        return !(entity instanceof ArmorStandEntity);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        setToggled(true);
    }
}