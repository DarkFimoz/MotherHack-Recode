package fun.motherhack.modules.impl.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import fun.motherhack.api.events.impl.EventAttackEntity;
import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.utils.math.TimerUtils;

import java.util.HashMap;
import java.util.Map;

public class Ezz extends Module {
    private final BooleanSetting global;
    
    private static final String[] EZ = new String[] {
        "%player% АНБРЕЙН ГЕТАЙ МАТЕРЬХАК РЕКОД",
        "%player% ТВОЯ МАТЬ БУДЕТ СЛЕДУЮЩЕЙ))))",
        "%player% БИЧАРА БЕЗ МАТЕРЬХАКА",
        "%player% ЧЕ ТАК БЫСТРО СЛИЛСЯ ТО А?",
        "%player% ПЛАЧЬ",
        "%player% УПССС ЗАБЫЛ КИЛЛКУ ВЫРУБИТЬ",
        "ОДНОКЛЕТОЧНЫЙ %player% БЫЛ ВПЕНЕН",
        "%player% ИЗИ БЛЯТЬ АХААХАХАХАХААХ",
        "%player% БОЖЕ МНЕ ТЕБЯ ЖАЛКО ВГЕТАЙ МАТЕРЬХАК",
        "%player% ОПРАВДЫВАЙСЯ В ХУЙ ЧЕ СДОХ ТО)))",
        "%player% СПС ЗА ОТСОС)))"
    };

    private final EnumSetting<ServerMode> server;
    
    private final TimerUtils timer = new TimerUtils();
    private final Map<Integer, Float> targetHealth = new HashMap<>();

    public Ezz() {
        super("Ezz", Category.Misc);
        global = new BooleanSetting("Global", true);
        server = new EnumSetting<>("Server", ServerMode.Universal);
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive e) {
        if (fullNullCheck()) return;
        if (server.getValue() == ServerMode.Universal) return;
        
        if (e.getPacket() instanceof GameMessageS2CPacket packet) {
            if (packet.content().getString().contains("Вы убили игрока")) {
                String name = extractPlayerName(packet.content().getString());
                if (name == null || name.equals("FATAL ERROR")) return;
                sayEZ(name);
            }
        }
    }

    @EventHandler
    public void onAttack(EventAttackEntity event) {
        if (fullNullCheck()) return;
        if (!(event.getTarget() instanceof PlayerEntity target)) return;
        
        targetHealth.put(target.getId(), target.getHealth());
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        if (server.getValue() != ServerMode.Universal) return;
        
        targetHealth.entrySet().removeIf(entry -> {
            if (mc.world == null) return true;
            
            var entity = mc.world.getEntityById(entry.getKey());
            
            if (entity == null || entity.isRemoved()) {
                return true;
            }
            
            if (entity instanceof LivingEntity living) {
                float previousHealth = entry.getValue();
                float currentHealth = living.getHealth();
                
                if (previousHealth > 0 && currentHealth <= 0) {
                    double distance = mc.player.squaredDistanceTo(living);
                    if (distance <= 100.0 && timer.passed(6000L)) {
                        sayEZ(living.getName().getString());
                        timer.reset();
                    }
                    return true;
                }
                
                entry.setValue(currentHealth);
                
                if (currentHealth > previousHealth) {
                    return true;
                }
            }
            
            return false;
        });
    }

    public void sayEZ(String playerName) {
        int n = (int) Math.floor(Math.random() * EZ.length);
        String finalword = EZ[n].replace("%player%", playerName);
        mc.player.networkHandler.sendChatMessage(global.getValue() ? "!" + finalword : finalword);
    }

    private String extractPlayerName(String message) {
        try {
            int start = message.indexOf("игрока ") + 7;
            int end = message.indexOf(" ", start);
            if (end == -1) end = message.length();
            return message.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    public enum ServerMode {
        Universal,
        FunnyGame
    }
}
