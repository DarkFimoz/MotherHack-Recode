package fun.motherhack.modules.impl.client;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.utils.Wrapper;
import fun.motherhack.utils.network.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ResourcePackPositionManager extends Module {
    
    private long lastActionTime = 0;
    private static final long COOLDOWN_MS = 1000; // 1 секунда кулдауна
    
    public ResourcePackPositionManager() {
        super("ResourcePackManager", Category.Client);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        
        // Защита от спама
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastActionTime < COOLDOWN_MS) {
            return;
        }
        lastActionTime = currentTime;
        
        moveModPackToFirstPosition();
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        
        // Защита от спама
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastActionTime < COOLDOWN_MS) {
            return;
        }
        lastActionTime = currentTime;
        
        moveModPackToLastPosition();
    }
    
    private void moveModPackToFirstPosition() {
        if (fullNullCheck()) return;
        
        try {
            // Сначала выведем отладочную информацию
            debugListPacksSimple();
            
            // Получаем текущие настройки ресурс-паков
            List<String> enabledPacks = new ArrayList<>(mc.options.resourcePacks);
            
            // Ищем пакет модов в текущем списке
            String modPackId = findModPackId(enabledPacks);
            
            if (modPackId != null) {
                ChatUtils.sendMessage("§aНайден пакет: " + modPackId);
                
                // Удаляем из текущей позиции
                enabledPacks.remove(modPackId);
                // Добавляем на первое место (это будет применено последним)
                enabledPacks.add(0, modPackId);
                
                // Обновляем настройки
                mc.options.resourcePacks.clear();
                mc.options.resourcePacks.addAll(enabledPacks);
                mc.options.write();
                
                // Перезагружаем ресурсы
                mc.reloadResources();
                
                if (!fullNullCheck()) ChatUtils.sendMessage("§aПакет модов перемещен на первое место.");
                
                // Показываем новый порядок
                debugListPacksSimple();
            } else {
                if (!fullNullCheck()) {
                    ChatUtils.sendMessage("§cПакет модов не найден. Ищем по следующим критериям:");
                    ChatUtils.sendMessage("§7- Содержит 'моды', 'mod', 'fabric' в названии");
                    ChatUtils.sendMessage("§7Текущие пакеты:");
                    for (String packId : enabledPacks) {
                        ChatUtils.sendMessage("§7- " + packId);
                    }
                }
            }
        } catch (Exception e) {
            if (!fullNullCheck()) ChatUtils.sendMessage("§cОшибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void moveModPackToLastPosition() {
        if (fullNullCheck()) return;
        
        try {
            // Получаем текущие настройки ресурс-паков
            List<String> enabledPacks = new ArrayList<>(mc.options.resourcePacks);
            
            // Ищем пакет модов в текущем списке
            String modPackId = findModPackId(enabledPacks);
            
            if (modPackId != null) {
                // Удаляем из текущей позиции
                enabledPacks.remove(modPackId);
                // Добавляем в конец (это будет применено первым)
                enabledPacks.add(modPackId);
                
                // Обновляем настройки
                mc.options.resourcePacks.clear();
                mc.options.resourcePacks.addAll(enabledPacks);
                mc.options.write();
                
                // Перезагружаем ресурсы
                mc.reloadResources();
                
                if (!fullNullCheck()) ChatUtils.sendMessage("§aПакет модов перемещен на последнее место.");
            } else {
                if (!fullNullCheck()) ChatUtils.sendMessage("§cПакет модов не найден в настройках.");
            }
        } catch (Exception e) {
            if (!fullNullCheck()) ChatUtils.sendMessage("§cОшибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String findModPackId(List<String> enabledPackIds) {
        if (enabledPackIds.isEmpty()) return null;
        
        // Сначала просто ищем по всем известным вариантам
        for (String packId : enabledPackIds) {
            String lowerId = packId.toLowerCase();
            
            // Расширенный поиск по разным вариантам
            if (lowerId.contains("моды") ||
                lowerId.contains("мод") ||
                lowerId.contains("fabric") ||
                lowerId.contains("mods") ||
                lowerId.contains("mod") ||
                lowerId.contains("фабрик") ||
                lowerId.contains("fabric-api") ||
                lowerId.contains("fabricmod") ||
                lowerId.contains("minecraftforge") ||
                lowerId.contains("forge") ||
                lowerId.contains("vanillatweaks") || // популярные пакеты модов
                lowerId.contains("optifine") ||
                lowerId.contains("iris") ||
                lowerId.contains("sodium")) {
                return packId;
            }
        }
        
        // Если не нашли по ключевым словам, вернем последний пакет (часто это пакет модов)
        // или проверим все доступные профили
        try {
            ResourcePackManager resourcePackManager = mc.getResourcePackManager();
            Collection<ResourcePackProfile> allProfiles = resourcePackManager.getProfiles();
            
            for (String packId : enabledPackIds) {
                // Найдем профиль пакета
                for (ResourcePackProfile profile : allProfiles) {
                    try {
                        // Попробуем сравнить ID
                        String profileId = getProfileId(profile);
                        if (profileId != null && profileId.equals(packId)) {
                            // Проверим отображаемое имя
                            String displayName = profile.getDisplayName().getString().toLowerCase();
                            if (displayName.contains("моды") ||
                                displayName.contains("мод") ||
                                displayName.contains("fabric") ||
                                displayName.contains("mod")) {
                                return packId;
                            }
                        }
                    } catch (Exception e) {
                        // Игнорируем ошибки
                    }
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки при поиске
        }
        
        return null;
    }
    
    private String getProfileId(ResourcePackProfile profile) {
        try {
            // Пробуем получить через отражение
            java.lang.reflect.Field[] fields = profile.getClass().getDeclaredFields();
            
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(profile);
                
                if (value instanceof String) {
                    String stringValue = (String) value;
                    // ID обычно содержит "file/" или похожий префикс
                    if (stringValue.startsWith("file/") || 
                        stringValue.contains("mod") ||
                        stringValue.contains("fabric")) {
                        return stringValue;
                    }
                }
            }
            
            // Если не нашли, вернем отображаемое имя
            return profile.getDisplayName().getString();
            
        } catch (Exception e) {
            return profile.getDisplayName().getString();
        }
    }
    
    // Простой метод для отладки
    private void debugListPacksSimple() {
        if (fullNullCheck()) return;
        
        try {
            List<String> enabledPacks = new ArrayList<>(mc.options.resourcePacks);
            
            if (enabledPacks.isEmpty()) {
                ChatUtils.sendMessage("§cНет активных ресурс-паков.");
                return;
            }
            
            ChatUtils.sendMessage("§6=== Активные ресурс-паки (" + enabledPacks.size() + ") ===");
            for (int i = 0; i < enabledPacks.size(); i++) {
                String packId = enabledPacks.get(i);
                ChatUtils.sendMessage("§7[" + i + "] §f" + packId);
            }
            
            // Объясняем порядок
            ChatUtils.sendMessage("§6Примечание: загружаются снизу вверх!");
            ChatUtils.sendMessage("§7[" + (enabledPacks.size()-1) + "] загружается ПЕРВЫМ (высший приоритет)");
            ChatUtils.sendMessage("§7[0] загружается ПОСЛЕДНИМ (низший приоритет)");
            
        } catch (Exception e) {
            // Игнорируем ошибки
        }
    }
    
    // Метод для принудительного поиска по паттернам
    public void findAndMovePack(String searchPattern, boolean toFirst) {
        if (fullNullCheck()) return;
        
        try {
            List<String> enabledPacks = new ArrayList<>(mc.options.resourcePacks);
            String lowerPattern = searchPattern.toLowerCase();
            
            // Ищем пакет по паттерну
            String foundPackId = null;
            for (String packId : enabledPacks) {
                if (packId.toLowerCase().contains(lowerPattern)) {
                    foundPackId = packId;
                    break;
                }
            }
            
            if (foundPackId != null) {
                ChatUtils.sendMessage("§aНайден пакет: " + foundPackId);
                
                // Перемещаем
                enabledPacks.remove(foundPackId);
                if (toFirst) {
                    enabledPacks.add(0, foundPackId);
                    ChatUtils.sendMessage("§aПеремещен на первое место.");
                } else {
                    enabledPacks.add(foundPackId);
                    ChatUtils.sendMessage("§aПеремещен на последнее место.");
                }
                
                // Применяем
                mc.options.resourcePacks.clear();
                mc.options.resourcePacks.addAll(enabledPacks);
                mc.options.write();
                mc.reloadResources();
                
            } else {
                ChatUtils.sendMessage("§cПакет с паттерном '" + searchPattern + "' не найден.");
                debugListPacksSimple();
            }
            
        } catch (Exception e) {
            ChatUtils.sendMessage("§cОшибка: " + e.getMessage());
        }
    }
}