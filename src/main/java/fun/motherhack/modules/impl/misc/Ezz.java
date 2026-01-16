package fun.motherhack.modules.impl.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import fun.motherhack.api.events.impl.EventAttackEntity;
import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.utils.math.TimerUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Ezz extends Module {
    public Ezz() {
        super("Ezz", Category.Misc);
    }

    private static final String[] WORDS = new String[] {
        "ГЕТАЙ МАЗЕРХАК РЕКОД",
        "ХАХХАХЫВХАВХЗХВА ТЫ ЧЕ ТАКОЙ СЛАБЫЙ БРУХ",
        "ПОЧУВСТВУЙ СИЛУ ЯИЦ ДМИТРИЯ ФИМОЗОВА",
        "АВЗХЗХАВХВАХЗХЗАВ ВСЁ-ЕЩЁ ТЕРПИШЬ? ЛИВНИ УЖЕ БОЖ",
        "в тебя прилетел мой истребитель",
        "ТЕБЕ НА СТОЛЬКО ЛЕНЬ ПОДПИСАТЬСЯ НА @MOTHERHACKRECODE????",
        "У дженро сопелька упала, а у тебя голова с плеч?",
        "ЫЫЫ БУДУ ИГРАТЬ С НУРИКОМ НА БВ ДЖЕНРО ЫЫЫ ГЕТНУ СИЛУ МАЗЕРХАКА"
    };

    private final TimerUtils timer = new TimerUtils();
    private final Map<Integer, Float> targetHealth = new HashMap<>();
    private final Random random = new Random();

    @EventHandler
    public void onAttack(EventAttackEntity event) {
        if (fullNullCheck()) return;
        if (!(event.getTarget() instanceof PlayerEntity target)) return;
        
        // Запоминаем здоровье цели при атаке
        targetHealth.put(target.getId(), target.getHealth());
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        
        // Проверяем всех запомненных игроков
        targetHealth.entrySet().removeIf(entry -> {
            if (mc.world == null) return true;
            
            var entity = mc.world.getEntityById(entry.getKey());
            
            // Если игрок просто исчез (вышел, телепортировался) - не пишем
            if (entity == null || entity.isRemoved()) {
                return true;
            }
            
            if (entity instanceof LivingEntity living) {
                // Проверяем что игрок умер
                if (living.isDead() || living.getHealth() <= 0) {
                    // Проверяем что игрок в радиусе 5 блоков от нас
                    double distance = mc.player.squaredDistanceTo(living);
                    if (distance <= 25.0) { // 5 блоков в квадрате = 25
                        sendKillMessage();
                    }
                    return true;
                }
            }
            
            return false;
        });
    }

    private void sendKillMessage() {
        if (!timer.passed(6000L)) return; // 6 секунд задержка
        
        String message = WORDS[random.nextInt(WORDS.length)];
        mc.player.networkHandler.sendChatMessage(message);
        timer.reset();
    }
}
