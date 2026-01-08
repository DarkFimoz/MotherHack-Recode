package fun.motherhack.modules.impl.misc;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.modules.settings.impl.StringSetting;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;


import java.awt.*;

public class NameProtect extends Module {
    
    @AllArgsConstructor @Getter
    public enum ColorMode implements Nameable {
        None("None"),
        Custom("Custom"),
        Rainbow("Rainbow");
        
        private final String name;
    }
    
    public final StringSetting fakeName = new StringSetting("FakeName", "Protected", false);
    public final EnumSetting<ColorMode> colorMode = new EnumSetting<>("ColorMode", ColorMode.None);
    public final StringSetting customFormat = new StringSetting("Format", "&f&l", () -> colorMode.getValue() == ColorMode.Custom, false);
    public final NumberSetting rainbowSpeed = new NumberSetting("RainbowSpeed", 1.0f, 0.1f, 5.0f, 0.1f, () -> colorMode.getValue() == ColorMode.Rainbow);
    
    public NameProtect() {
        super("NameProtect", Category.Misc);
        getSettings().add(fakeName);
        getSettings().add(colorMode);
        getSettings().add(customFormat);
        getSettings().add(rainbowSpeed);
    }
    
    /**
     * Применяет цветовые коды к тексту (например &f&l -> белый жирный)
     */
    private String applyColorCodes(String text) {
        return text.replace("&", "§");
    }
    
    /**
     * Создаёт радужный текст с переливающимися цветами
     */
    private String createRainbowText(String text) {
        StringBuilder result = new StringBuilder();
        long time = System.currentTimeMillis();
        double speed = rainbowSpeed.getValue();
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // Вычисляем hue для каждого символа с учётом времени и позиции
            float hue = (float) (((time * speed / 10.0) + (i * 10)) % 360) / 360f;
            Color color = Color.getHSBColor(hue, 1.0f, 1.0f);
            
            // Конвертируем в Minecraft цветовой код (используем hex формат §x)
            String hexColor = String.format("§x§%c§%c§%c§%c§%c§%c",
                    Character.forDigit((color.getRed() >> 4) & 0xF, 16),
                    Character.forDigit(color.getRed() & 0xF, 16),
                    Character.forDigit((color.getGreen() >> 4) & 0xF, 16),
                    Character.forDigit(color.getGreen() & 0xF, 16),
                    Character.forDigit((color.getBlue() >> 4) & 0xF, 16),
                    Character.forDigit(color.getBlue() & 0xF, 16));
            
            result.append(hexColor).append(c);
        }
        return result.toString();
    }
    
    /**
     * Получает отформатированное имя в зависимости от режима
     */
    public String getFormattedName() {
        String name = fakeName.getValue();
        
        switch (colorMode.getValue()) {
            case Custom:
                return applyColorCodes(customFormat.getValue()) + name;
            case Rainbow:
                return createRainbowText(name);
            default:
                return name;
        }
    }
    
    /**
     * Заменяет реальный ник на фейковый в строке
     */
    public String replaceName(String text) {
        if (text == null || fullNullCheck()) return text;
        String realName = mc.getSession().getUsername();
        if (text.contains(realName)) {
            return text.replace(realName, getFormattedName());
        }
        return text;
    }
    
    /**
     * Заменяет реальный ник на фейковый в Text компоненте
     */
    public Text replaceNameInText(Text text) {
        if (text == null || fullNullCheck()) return text;
        String realName = mc.getSession().getUsername();
        String content = text.getString();
        
        if (content.contains(realName)) {
            String replaced = content.replace(realName, getFormattedName());
            return Text.literal(replaced).setStyle(text.getStyle());
        }
        return text;
    }
    
    /**
     * Рекурсивно заменяет имя во всех частях Text компонента
     */
    public Text replaceNameDeep(Text text) {
        if (text == null || fullNullCheck()) return text;
        String realName = mc.getSession().getUsername();
        
        // Получаем полную строку и проверяем наличие имени
        String fullString = text.getString();
        if (!fullString.contains(realName)) {
            return text;
        }
        
        // Создаем копию текста с сохранением форматирования
        MutableText result = Text.empty().setStyle(text.getStyle());
        
        // Обрабатываем основной контент текста
        String content = text.getString();
        if (!content.isEmpty()) {
            // Получаем только текст без siblings
            String ownContent = "";
            if (text.getSiblings().isEmpty()) {
                ownContent = content;
            } else {
                // Вычисляем собственный контент, исключая siblings
                StringBuilder siblingsText = new StringBuilder();
                for (Text sibling : text.getSiblings()) {
                    siblingsText.append(sibling.getString());
                }
                if (content.length() > siblingsText.length()) {
                    ownContent = content.substring(0, content.length() - siblingsText.length());
                }
            }
            
            // Заменяем имя в собственном контенте
            if (!ownContent.isEmpty() && ownContent.contains(realName)) {
                result.append(Text.literal(ownContent.replace(realName, getFormattedName())).setStyle(text.getStyle()));
            } else if (!ownContent.isEmpty()) {
                result.append(Text.literal(ownContent).setStyle(text.getStyle()));
            }
        }
        
        // Рекурсивно обрабатываем siblings
        for (Text sibling : text.getSiblings()) {
            result.append(replaceNameDeep(sibling));
        }
        
        return result;
    }
}
