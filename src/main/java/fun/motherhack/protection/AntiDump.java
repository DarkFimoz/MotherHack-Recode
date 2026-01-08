package fun.motherhack.protection;

import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Защита от дампа памяти и отладки.
 * Вызывай AntiDump.check() в начале инициализации.
 */
public class AntiDump {
    
    private static volatile boolean _v = false;
    private static final long _t = System.currentTimeMillis();
    
    public static void check() {
        // Проверка на отладчик
        if (isDebuggerAttached()) {
            _crash();
        }
        
        // Проверка на подозрительные аргументы JVM
        if (hasSuspiciousArgs()) {
            _crash();
        }
        
        _v = true;
    }
    
    public static boolean isValid() {
        return _v && (System.currentTimeMillis() - _t) < 86400000L;
    }
    
    private static boolean isDebuggerAttached() {
        // Проверяем аргументы JVM на наличие отладочных флагов
        List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String arg : args) {
            String lower = arg.toLowerCase();
            if (lower.contains("-agentlib:jdwp") || 
                lower.contains("-xdebug") ||
                lower.contains("-xrunjdwp")) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean hasSuspiciousArgs() {
        List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String arg : args) {
            String lower = arg.toLowerCase();
            // Проверяем на инструменты для дампа/анализа
            if (lower.contains("recaf") || 
                lower.contains("bytecode") ||
                lower.contains("decompil") ||
                lower.contains("javaagent")) {
                return true;
            }
        }
        return false;
    }
    
    private static void _crash() {
        // Крашим JVM разными способами
        try {
            Runtime.getRuntime().halt(0);
        } catch (Exception e) {
            System.exit(0);
        }
    }
    
    // Мусорный код
    private static int _0(int a) { return a ^ 0xCAFE; }
    private static void _1() { if (_t < 0) throw new Error(); }
    private static Object _2(Object o) { return o == null ? "" : o; }
}
