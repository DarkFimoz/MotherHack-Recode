package fun.motherhack.modules.impl.misc;

import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

public class MathSolver extends Module {

    public MathSolver() {
        super("Math", Category.Misc);
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive e) {
        if (fullNullCheck()) return;
        
        if (e.getPacket() instanceof GameMessageS2CPacket packet) {
            String message = packet.content().getString();
            
            // Очищаем сообщение от префиксов чата
            String cleanMessage = cleanChatMessage(message);
            
            // Проверяем, содержит ли сообщение математическое выражение с "+"
            if (containsAdditionExpression(cleanMessage)) {
                try {
                    // Извлекаем и вычисляем выражение
                    String expression = extractMathExpression(cleanMessage);
                    if (expression != null) {
                        double result = evaluateAddition(expression);
                        
                        // Отправляем ответ в чат
                        String answer = String.valueOf(result);
                        // Убираем .0 для целых чисел
                        if (result == (long) result) {
                            answer = String.valueOf((long) result);
                        }
                        
                        mc.player.networkHandler.sendChatMessage(answer);
                    }
                } catch (Exception ex) {
                    // Игнорируем невалидные выражения
                }
            }
        }
    }

    // Очищает сообщение от префиксов чата (ники, теги и т.д.)
    private String cleanChatMessage(String message) {
        if (message == null || message.isEmpty()) return "";
        
        String cleanMessage = message;
        
        // Убираем префиксы типа "[VIP] nick: "
        if (cleanMessage.contains(":")) {
            String[] parts = cleanMessage.split(":", 2);
            if (parts.length > 1) {
                cleanMessage = parts[1].trim();
            }
        }
        
        // Убираем префиксы типа "<nick> "
        if (cleanMessage.startsWith("<") && cleanMessage.contains(">")) {
            int endIndex = cleanMessage.indexOf(">");
            if (endIndex < cleanMessage.length() - 1) {
                cleanMessage = cleanMessage.substring(endIndex + 1).trim();
            }
        }
        
        return cleanMessage;
    }

    // Проверяет, содержит ли сообщение выражение со знаком "+"
    private boolean containsAdditionExpression(String message) {
        if (message == null || message.trim().isEmpty()) return false;
        
        String trimmed = message.trim();
        
        // Игнорируем команды
        if (trimmed.startsWith("/") || trimmed.startsWith("!") || trimmed.startsWith(".")) return false;
        
        // Проверяем наличие знака "+"
        if (!trimmed.contains("+")) return false;
        
        // Пытаемся извлечь математическое выражение
        String extracted = extractMathExpression(trimmed);
        if (extracted == null) return false;
        
        // Проверяем на валидные символы (цифры, +, пробелы, точка)
        String cleanMessage = extracted.replaceAll("\\s+", "");
        if (!cleanMessage.matches("-?[0-9+\\.]+")) return false;
        
        // Проверяем, что выражение не заканчивается на "+"
        if (cleanMessage.endsWith("+")) return false;
        
        // Проверяем, что выражение не начинается с "+"
        if (cleanMessage.startsWith("+")) return false;
        
        // Проверяем, что нет двух "+" подряд
        if (cleanMessage.contains("++")) return false;
        
        return true;
    }

    // Извлекает математическое выражение с "+" из сообщения
    private String extractMathExpression(String message) {
        if (message == null || message.isEmpty()) return null;
        
        // Паттерн для поиска выражений с "+": число + число [+ число...]
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "-?\\d+(?:\\.\\d+)?(?:\\s*\\+\\s*-?\\d+(?:\\.\\d+)?)+");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        
        if (matcher.find()) {
            return matcher.group();
        }
        
        return null;
    }

    // Вычисляет выражение со сложением
    private double evaluateAddition(String expression) throws Exception {
        if (expression == null || expression.isEmpty()) {
            throw new Exception("Empty expression");
        }
        
        // Удаляем все пробелы
        expression = expression.replaceAll("\\s+", "");
        
        // Разбиваем по знаку "+"
        String[] parts = expression.split("\\+");
        
        double result = 0;
        for (String part : parts) {
            if (part.isEmpty()) {
                throw new Exception("Invalid expression");
            }
            try {
                result += Double.parseDouble(part);
            } catch (NumberFormatException e) {
                throw new Exception("Invalid number: " + part);
            }
        }
        
        return result;
    }
}
