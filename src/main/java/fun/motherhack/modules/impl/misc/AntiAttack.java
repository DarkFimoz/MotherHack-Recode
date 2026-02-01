package fun.motherhack.modules.impl.misc;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import org.jetbrains.annotations.NotNull;
import fun.motherhack.api.events.impl.EventPacket;

public class AntiAttack extends Module {
    private final BooleanSetting friend = new BooleanSetting("Друзья", true);
    private final BooleanSetting zoglin = new BooleanSetting("Зомбифицированный пиглин", true);
    private final BooleanSetting villager = new BooleanSetting("Житель", false);
    private final BooleanSetting oneHp = new BooleanSetting("Одно HP", false);
    private final NumberSetting hp = new NumberSetting("HP", 1f, 0f, 20f, 0.5f);

    public AntiAttack() {
        super("AntiAttack", Category.Misc);
        getSettings().add(friend);
        getSettings().add(zoglin);
        getSettings().add(villager);
        getSettings().add(oneHp);
        getSettings().add(hp);
    }

    @EventHandler
    private void onPacketSend(EventPacket.@NotNull Send e) {
        if (e.getPacket() instanceof PlayerInteractEntityC2SPacket pac) {
            Entity entity = getEntity(pac);
            if (entity == null) return;

            if (friend.getValue() && isFriend(entity.getName().getString()))
                e.cancel();

            if (entity instanceof ZombifiedPiglinEntity && zoglin.getValue())
                e.cancel();

            if (entity instanceof VillagerEntity && villager.getValue())
                e.cancel();

            if (oneHp.getValue() && entity instanceof LivingEntity lent) {
                if (lent.getHealth() <= hp.getValue())
                    e.cancel();
            }
        }
    }

    private Entity getEntity(PlayerInteractEntityC2SPacket packet) {
        if (mc.world == null) return null;
        try {
            java.lang.reflect.Field entityIdField = PlayerInteractEntityC2SPacket.class.getDeclaredField("entityId");
            entityIdField.setAccessible(true);
            int entityId = (int) entityIdField.get(packet);
            return mc.world.getEntityById(entityId);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isFriend(String name) {
        return false;
    }
}
