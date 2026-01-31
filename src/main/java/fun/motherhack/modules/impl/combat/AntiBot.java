package fun.motherhack.modules.impl.combat;

import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.utils.math.TimerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

public class AntiBot extends Module {

    public AntiBot() {
        super("AntiBot", Category.Combat);
    }

    public final List<PlayerEntity> bots = new ArrayList<>();
    private final TimerUtils timer = new TimerUtils();

    @EventHandler
    public void onTick(EventTick e) {
        if (fullNullCheck()) return;

        // Очищаем список ботов каждые 10 секунд
        if (timer.passed(10000)) {
            timer.reset();
            // Удаляем только тех ботов, которые больше не в мире или прошли проверку
            bots.removeIf(bot -> bot.isRemoved() || !armorCheck(bot));
        }

        //bots.forEach(bot -> {
        	//mc.world.removeEntity(bot.getId(), Entity.RemovalReason.KILLED);
        //});
        
        //if (mc.crosshairTarget instanceof EntityHitResult result) {
        	//if (!result.getEntity().getUuid().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + result.getEntity().getName().getString()).getBytes(StandardCharsets.UTF_8))))
        		//ChatUtils.sendMessage("Bot: " + result.getEntity().getName().getString());
        //}
        
        // Проверяем игроков и добавляем ботов
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null) continue;
            if (player == mc.player) continue;
            
            // Если игрок уже в списке ботов, пропускаем
            if (bots.contains(player)) continue;
            
            // Проверяем, является ли игрок ботом
            if (armorCheck(player)) {
                bots.add(player);
            }
        }
        
        // Дополнительная проверка: удаляем из списка ботов тех, кто больше не проходит проверку
        bots.removeIf(bot -> !bot.isRemoved() && !armorCheck(bot));
    }

    private boolean armorCheck(PlayerEntity entity) {
        return (getArmor(entity, 3).getItem() == Items.LEATHER_HELMET && isNotColored(entity, 3) && !getArmor(entity, 3).hasEnchantments()
                || getArmor(entity, 2).getItem() == Items.LEATHER_CHESTPLATE && isNotColored(entity, 2) && !getArmor(entity, 2).hasEnchantments()
                || getArmor(entity, 1).getItem() == Items.LEATHER_LEGGINGS && isNotColored(entity, 1) && !getArmor(entity, 1).hasEnchantments()
                || getArmor(entity, 0).getItem() == Items.LEATHER_BOOTS && isNotColored(entity, 0) && !getArmor(entity, 0).hasEnchantments()
                || getArmor(entity, 2).getItem() == Items.IRON_CHESTPLATE && !getArmor(entity, 2).hasEnchantments()
                || getArmor(entity, 1).getItem() == Items.IRON_LEGGINGS && !getArmor(entity, 1).hasEnchantments());
    }
   
    private ItemStack getArmor(PlayerEntity entity, int slot) {
        return entity.getInventory().getArmorStack(slot);
    }
    
    private boolean isNotColored(PlayerEntity entity, int slot) {
        return !getArmor(entity, slot).contains(DataComponentTypes.DYED_COLOR);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (!bots.isEmpty()) bots.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (!bots.isEmpty()) bots.clear();
    }
}