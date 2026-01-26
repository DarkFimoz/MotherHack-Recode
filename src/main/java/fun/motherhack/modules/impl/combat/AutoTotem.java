package fun.motherhack.modules.impl.combat;

import fun.motherhack.api.events.impl.EventKeyboardInput;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.world.InventoryUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class AutoTotem extends Module {

    private final NumberSetting stopTicks = new NumberSetting("settings.autototem.stopticks", 4, 1, 20, 1);
    private final NumberSetting swapDelay = new NumberSetting("settings.autototem.swapdelay", 150, 50, 500, 10);
    private final BooleanSetting strictTiming = new BooleanSetting("settings.autototem.stricttiming", true);
    
    private int freezeTicks = 0;
    private boolean isSwapping = false;
    private long lastSwapTime = 0;
    private boolean wasTotemInOffhand = false;

    public AutoTotem() {
        super("AutoTotem", Category.Combat);
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick e) {
        if (fullNullCheck()) return;

        boolean hasTotemInOffhand = mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;
        
        // Проверяем задержку между свапами (анти-флаг)
        long currentTime = System.currentTimeMillis();
        long timeSinceLastSwap = currentTime - lastSwapTime;
        
        if (timeSinceLastSwap < swapDelay.getValue().longValue()) {
            return; // Ждём перед следующим свапом
        }
        
        // Если уже идёт процесс свапа, не начинаем новый
        if (isSwapping) {
            return;
        }
        
        // Определяем, нужен ли тотем
        boolean needTotem = false;
        
        // Случай 1: Тотем был использован (был в offhand, теперь нет)
        if (wasTotemInOffhand && !hasTotemInOffhand) {
            needTotem = true;
        }
        
        // Случай 2: В offhand нет тотема, но он есть в инвентаре
        if (!hasTotemInOffhand) {
            int totemSlot = InventoryUtils.find(Items.TOTEM_OF_UNDYING);
            if (totemSlot != -1) {
                needTotem = true;
            }
        }
        
        wasTotemInOffhand = hasTotemInOffhand;
        
        // Если нужен тотем, начинаем процесс свапа
        if (needTotem) {
            int totemSlot = InventoryUtils.find(Items.TOTEM_OF_UNDYING);
            if (totemSlot != -1) {
                isSwapping = true;
                freezeTicks = stopTicks.getValue().intValue();
                lastSwapTime = currentTime;
                
                // Выполняем swap в отдельном потоке с задержками для обхода античита
                performAntiCheatSwap(totemSlot, 45);
            }
        }
    }

    @EventHandler
    public void onKeyboardInput(EventKeyboardInput e) {
        if (fullNullCheck()) return;
        
        // Останавливаем движение на короткое время
        if (freezeTicks > 0) {
            e.setMovementForward(0);
            e.setMovementSideways(0);
            freezeTicks--;
        }
    }
    
    private void performAntiCheatSwap(int slot, int targetSlot) {
        if (slot == -1 || targetSlot == -1) return;
        
        // Используем отдельный поток для свапа с задержками
        new Thread(() -> {
            try {
                // Начальная задержка (имитация человеческой реакции)
                if (strictTiming.getValue()) {
                    Thread.sleep(80 + (long)(Math.random() * 70)); // 80-150ms
                } else {
                    Thread.sleep(30 + (long)(Math.random() * 40)); // 30-70ms
                }
                
                // Первый клик - берём предмет
                mc.interactionManager.clickSlot(
                    mc.player.playerScreenHandler.syncId, 
                    InventoryUtils.indexToSlot(slot), 
                    0, 
                    SlotActionType.PICKUP, 
                    mc.player
                );
                
                // Задержка между кликами (важно для Matrix и Grim)
                if (strictTiming.getValue()) {
                    Thread.sleep(120 + (long)(Math.random() * 80)); // 120-200ms
                } else {
                    Thread.sleep(60 + (long)(Math.random() * 60)); // 60-120ms
                }
                
                // Второй клик - ставим в offhand
                mc.interactionManager.clickSlot(
                    mc.player.playerScreenHandler.syncId, 
                    targetSlot, 
                    0, 
                    SlotActionType.PICKUP, 
                    mc.player
                );
                
                // Задержка перед возвратом предмета
                if (strictTiming.getValue()) {
                    Thread.sleep(120 + (long)(Math.random() * 80)); // 120-200ms
                } else {
                    Thread.sleep(60 + (long)(Math.random() * 60)); // 60-120ms
                }
                
                // Третий клик - возвращаем предмет обратно (если что-то было в offhand)
                mc.interactionManager.clickSlot(
                    mc.player.playerScreenHandler.syncId, 
                    InventoryUtils.indexToSlot(slot), 
                    0, 
                    SlotActionType.PICKUP, 
                    mc.player
                );
                
                // Финальная задержка
                if (strictTiming.getValue()) {
                    Thread.sleep(50 + (long)(Math.random() * 50)); // 50-100ms
                }
                
                isSwapping = false;
                
            } catch (Exception ex) {
                ex.printStackTrace();
                isSwapping = false;
            }
        }).start();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        freezeTicks = 0;
        isSwapping = false;
        wasTotemInOffhand = false;
    }
}
