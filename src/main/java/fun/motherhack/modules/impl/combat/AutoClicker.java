package fun.motherhack.modules.impl.combat;

import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.math.TimerUtils;
import fun.motherhack.utils.network.Server;
import fun.motherhack.utils.world.InventoryUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;

public class AutoClicker extends Module {

    private final NumberSetting cps = new NumberSetting("settings.autoclicker.cps", 10f, 1f, 1000f, 1f);
    private final TimerUtils timer = new TimerUtils();

    public AutoClicker() {
        super("AutoClicker", Category.Combat);
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick e) {
        if (fullNullCheck()) return;

        if (mc.crosshairTarget instanceof EntityHitResult result) {
            LivingEntity entity = (LivingEntity) result.getEntity();
            if (Server.isValid(entity)) {
                long delay = (long) (1000f / cps.getValue());
                if (timer.passed(delay)) {
                    mc.interactionManager.attackEntity(mc.player, entity);
                    InventoryUtils.swing(InventoryUtils.Swing.MainHand);
                    timer.reset();
                }
            }
        }
    }
}