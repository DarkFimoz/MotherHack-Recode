package fun.motherhack.modules.impl.misc;

import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.modules.settings.impl.StringSetting;
import fun.motherhack.utils.math.TimerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvents;

import java.awt.*;
import java.awt.TrayIcon.MessageType;

public class BedWarsHelper extends Module {

    // Sword type selection
    public enum SwordVersion implements Nameable {
        V1_8("1.8"),
        V1_12("1.12");
         
        private final String name;
         
        SwordVersion(String name) {
            this.name = name;
        }
         
        @Override
        public String toString() {
            return name;
        }
        
        @Override
        public String getName() {
            return name;
        }
    }

    // Material selection
    public enum MaterialType implements Nameable {
        Classic("Classic"),
        Virtual("Virtual");
         
        private final String name;
         
        MaterialType(String name) {
            this.name = name;
        }
         
        @Override
        public String toString() {
            return name;
        }
        
        @Override
        public String getName() {
            return name;
        }
    }

    private final NumberSetting delay = new NumberSetting("settings.bedwarshelper.delay", 200, 0, 1000, 10);
    private final BooleanSetting autoClose = new BooleanSetting("settings.bedwarshelper.autoclose", false);
    private final EnumSetting<SwordVersion> swordVersion = new EnumSetting<>("settings.bedwarshelper.swordversion", SwordVersion.V1_8);
    private final EnumSetting<MaterialType> materialType = new EnumSetting<>("settings.bedwarshelper.materialtype", MaterialType.Virtual);
    private final BooleanSetting joinMessage = new BooleanSetting("settings.bedwarshelper.joinmessage", false);
    private final StringSetting joinMessageText = new StringSetting("settings.bedwarshelper.joinmessagetext", "Если вы подписаны на тгк motherhackrecode, то вероятность вашей смерти ниже", false);
    private final BooleanSetting autoJoin = new BooleanSetting("settings.bedwarshelper.autojoin", false);
    private final BooleanSetting playSound = new BooleanSetting("settings.bedwarshelper.playsound", false);
    private final BooleanSetting autoBuy = new BooleanSetting("settings.bedwarshelper.autobuy", false);
    private final NumberSetting autoBuyDelay = new NumberSetting("settings.bedwarshelper.autobuydelay", 150, 0, 1000, 10);
    private final BooleanSetting autoStealElytra = new BooleanSetting("settings.bedwarshelper.autostealelytra", false);

    private final TimerUtils timer = new TimerUtils();
    private final TimerUtils autoBuyTimer = new TimerUtils();
    
    // Порядок покупки для автобая - просто список предметов для стилла
    private final String[] autoBuyOrder = {
        "shears",           // ножницы
        "chainmail_boots",  // кольчужные ботинки
        "elytra",           // элитра
        "stone_pickaxe",    // каменная кирка (переключит категорию)
        "iron_axe",         // железный топор
        "iron_pickaxe",     // железная кирка
        "tnt",              // тнт (переключит категорию)
        "totem"             // тотем
    };
    
    private int autoBuyIndex = 0;
    private boolean[] purchasedItems; // Отслеживаем что уже купили
    private boolean autoBuyCompleted = false; // Флаг завершения покупок в текущей катке

    public BedWarsHelper() {
        super("BedWarsHelper", Category.Misc);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetAutoBuy();
    }
    
    private void resetAutoBuy() {
        autoBuyIndex = 0;
        purchasedItems = new boolean[autoBuyOrder.length];
        autoBuyCompleted = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick e) {
        if (fullNullCheck()) return;

        // Only work when player manually opens a container
        if (mc.currentScreen instanceof GenericContainerScreen) {
            GenericContainerScreen containerScreen = (GenericContainerScreen) mc.currentScreen;
            String containerTitle = containerScreen.getTitle().getString().toLowerCase();

            // AutoBuy functionality - работает только в магазине
            // Проверяем что InvSaver выключен
            if (autoBuy.getValue() && containerTitle.contains("магазин")) {
                // Проверяем совместимость с другими модулями
                Module invSaver = fun.motherhack.MotherHack.getInstance().getModuleManager().getModule(InvSaver.class);
                
                if (invSaver != null && invSaver.isToggled()) {
                    // Если InvSaver включен - не работаем
                    return;
                }
                
                int playerExp = mc.player.experienceLevel;
                
                // Если опыт меньше 3000 или больше 3500 - ничего не делаем
                if (playerExp < 3000 || playerExp > 3500) {
                    return;
                }
                
                // Если уже все купили - не делаем ничего
                if (autoBuyCompleted) {
                    return;
                }
                
                // Покупаем по порядку
                if (autoBuyTimer.passed(autoBuyDelay.getValue().longValue())) {
                    autoBuyFromShop(containerScreen);
                    autoBuyTimer.reset();
                }
                return;
            }

            // AutoStealElytra functionality - работает в GUI "за элитры"
            if (autoStealElytra.getValue() && containerTitle.contains("за элитры")) {
                if (timer.passed(delay.getValue().longValue())) {
                    autoStealElytraFromContainer(containerScreen);
                    timer.reset();
                }
                return;
            }

            // AutoJoin functionality - handle specific containers
            if (autoJoin.getValue()) {
                if (containerTitle.contains("выбор режима") || containerTitle.contains("выбор сервера")) {
                    autoJoinSteal(containerScreen, Items.PUFFERFISH);
                    return;
                } else if (containerTitle.contains("выбор мини-игр") || containerTitle.contains("МИНИ—ИГРЫ")) {
                    autoJoinSteal(containerScreen, Items.RED_BED);
                    return;
                }
            }

            // Work on specific container names, but NOT on regular "сундук" or "ender chest"
            boolean isAllowedContainer = (containerTitle.contains("категории") ||
                containerTitle.contains("solo") ||
                containerTitle.contains("pvp") ||
                containerTitle.contains("ресурсы")) &&
                !containerTitle.contains("сундук") &&
                !containerTitle.contains("ender chest");

            if (isAllowedContainer) {
                 
                if (timer.passed(delay.getValue().longValue())) {
                    stealFromContainer(containerScreen);
                    timer.reset();
                }
            }
        }
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive e) {
        if (fullNullCheck()) return;
        
        if (e.getPacket() instanceof GameMessageS2CPacket packet) {
            String message = packet.content().getString();
            
            // Сброс AutoBuy при начале новой катки
            if (message.contains("Защити свою кровать") || message.contains("bw-")) {
                resetAutoBuy();
            }
            
            // Check if JoinMessage is enabled and if the message contains "Защити свою кровать"
            if (joinMessage.getValue() && message.contains("Защити свою кровать")) {
                // Send the custom join message
                String customMessage = joinMessageText.getValue();
                if (!customMessage.isEmpty()) {
                    mc.player.networkHandler.sendChatMessage(customMessage);
                }
            }
            
            // Check if PlaySound is enabled and if the message contains "bw-"
            if (playSound.getValue() && message.contains("bw-")) {
                // Play level up sound - громкий и длинный звук, который не прерывается
                mc.getSoundManager().play(
                    PositionedSoundInstance.master(
                        SoundEvents.ENTITY_PLAYER_LEVELUP, 
                        1.0f,  // pitch
                        3.0f   // volume (увеличена громкость)
                    )
                );
                
                // Показываем системное уведомление Windows
                showSystemNotification("TrofimWars", "Вы зашли в лобби, то есть вы зарачены");
            }
        }
    }

    private void stealFromContainer(GenericContainerScreen container) {
        if (container == null || container.getScreenHandler() == null) {
            return;
        }
        
        // Determine which items to steal based on settings
        Item targetSword = swordVersion.getValue() == SwordVersion.V1_8 ? Items.IRON_SWORD : Items.DIAMOND_SWORD;
        Item targetMaterial = materialType.getValue() == MaterialType.Classic ? Items.GOLD_INGOT : Items.IRON_INGOT;
        
        // Only steal specific items based on settings
        for (int i = 0; i < container.getScreenHandler().slots.size() - 36; i++) {
            Slot slot = container.getScreenHandler().getSlot(i);
            if (slot == null || slot.getStack() == null) continue;
            
            ItemStack stack = slot.getStack();
            if (stack.getItem() == Items.CHEST ||
                stack.getItem() == Items.ENDER_EYE ||  // Added: Ender Pearls
                stack.getItem() == targetSword ||
                stack.getItem() == targetMaterial) {
                
                // Quick move the item to our inventory
                mc.interactionManager.clickSlot(container.getScreenHandler().syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                timer.reset();
                return;
            }
        }
        
        // If no target items found, close the container if autoClose is enabled
        if (autoClose.getValue()) {
            mc.player.closeHandledScreen();
        }
    }

    private void autoJoinSteal(GenericContainerScreen container, Item targetItem) {
        if (container == null || container.getScreenHandler() == null) {
            return;
        }
        
        if (timer.passed(delay.getValue().longValue())) {
            // Search for the target item in the container
            for (int i = 0; i < container.getScreenHandler().slots.size() - 36; i++) {
                Slot slot = container.getScreenHandler().getSlot(i);
                if (slot == null || slot.getStack() == null) continue;
                
                ItemStack stack = slot.getStack();
                if (stack.getItem() == targetItem) {
                    // Click on the target item
                    mc.interactionManager.clickSlot(container.getScreenHandler().syncId, i, 0, SlotActionType.PICKUP, mc.player);
                    timer.reset();
                    return;
                }
            }
        }
    }

    private void autoStealElytraFromContainer(GenericContainerScreen container) {
        if (container == null || container.getScreenHandler() == null) {
            return;
        }
        
        // Ищем элитру с названием "Включены" в контейнере
        for (int i = 0; i < container.getScreenHandler().slots.size() - 36; i++) {
            Slot slot = container.getScreenHandler().getSlot(i);
            if (slot == null || slot.getStack() == null || slot.getStack().isEmpty()) continue;
            
            ItemStack stack = slot.getStack();
            
            // Проверяем, что это элитра
            if (stack.getItem() == Items.ELYTRA) {
                // Проверяем название предмета
                String itemName = stack.getName().getString();
                if (itemName.contains("Включены")) {
                    // Кликаем на элитру для покупки/получения
                    mc.interactionManager.clickSlot(container.getScreenHandler().syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                    timer.reset();
                    
                    // Закрываем контейнер если включено автозакрытие
                    if (autoClose.getValue()) {
                        mc.player.closeHandledScreen();
                    }
                    return;
                }
            }
        }
    }

    private void autoBuyFromShop(GenericContainerScreen container) {
        if (container == null || container.getScreenHandler() == null) {
            return;
        }
        
        // Если прошли весь список - завершаем
        if (autoBuyIndex >= autoBuyOrder.length) {
            autoBuyCompleted = true;
            if (autoClose.getValue()) {
                mc.player.closeHandledScreen();
            }
            return;
        }
        
        // Пропускаем уже купленные предметы
        while (autoBuyIndex < autoBuyOrder.length && purchasedItems[autoBuyIndex]) {
            autoBuyIndex++;
        }
        
        if (autoBuyIndex >= autoBuyOrder.length) {
            autoBuyCompleted = true;
            if (autoClose.getValue()) {
                mc.player.closeHandledScreen();
            }
            return;
        }
        
        String targetItemName = autoBuyOrder[autoBuyIndex];
        
        // Ищем предмет в контейнере
        for (int i = 0; i < container.getScreenHandler().slots.size() - 36; i++) {
            Slot slot = container.getScreenHandler().getSlot(i);
            if (slot == null || slot.getStack() == null || slot.getStack().isEmpty()) continue;
            
            ItemStack stack = slot.getStack();
            String itemId = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).getPath().toLowerCase();
            
            if (itemId.contains(targetItemName)) {
                // Кликаем на предмет (стиллим/покупаем)
                mc.interactionManager.clickSlot(container.getScreenHandler().syncId, i, 0, SlotActionType.PICKUP, mc.player);
                
                // Отмечаем что купили
                purchasedItems[autoBuyIndex] = true;
                autoBuyIndex++;
                return;
            }
        }
    }
    
    private void showSystemNotification(String title, String message) {
        try {
            if (SystemTray.isSupported()) {
                SystemTray tray = SystemTray.getSystemTray();
                
                // Создаем иконку для уведомления (используем стандартную иконку Java)
                Image image = Toolkit.getDefaultToolkit().createImage("");
                
                TrayIcon trayIcon = new TrayIcon(image, "BedWars Helper");
                trayIcon.setImageAutoSize(true);
                trayIcon.setToolTip("BedWars Helper");
                
                try {
                    tray.add(trayIcon);
                    trayIcon.displayMessage(title, message, MessageType.INFO);
                    
                    // Удаляем иконку через 3 секунды
                    new Thread(() -> {
                        try {
                            Thread.sleep(3000);
                            tray.remove(trayIcon);
                        } catch (InterruptedException ignored) {}
                    }).start();
                } catch (AWTException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
