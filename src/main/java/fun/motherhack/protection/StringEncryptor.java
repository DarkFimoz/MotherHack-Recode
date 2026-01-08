package fun.motherhack.protection;

/**
 * Шифрование строк в runtime.
 * Используй: StringEncryptor.d("зашифрованная_строка")
 * 
 * Для шифрования строки запусти: System.out.println(StringEncryptor.e("твоя строка"));
 */
public class StringEncryptor {
    
    private static final int[] K = {0x4D, 0x48, 0x41, 0x43, 0x4B}; // MHACK
    private static volatile int _c = 0;
    
    /**
     * Расшифровывает строку
     */
    public static String d(String s) {
        if (s == null || s.isEmpty()) return s;
        _c++;
        char[] c = s.toCharArray();
        char[] r = new char[c.length];
        for (int i = 0; i < c.length; i++) {
            r[i] = (char) (c[i] ^ K[i % K.length] ^ (i * 7));
        }
        return new String(r);
    }
    
    /**
     * Шифрует строку - используй для подготовки
     */
    public static String e(String s) {
        if (s == null || s.isEmpty()) return s;
        char[] c = s.toCharArray();
        char[] r = new char[c.length];
        for (int i = 0; i < c.length; i++) {
            r[i] = (char) (c[i] ^ K[i % K.length] ^ (i * 7));
        }
        return new String(r);
    }
    
    // Anti-decompile мусор
    private static int _0(int a) { return a ^ 0xDEAD; }
    private static long _1(long a) { return a ^ 0xBEEFL; }
    private static String _2(String s) { return s == null ? "" : new StringBuilder(s).reverse().toString(); }
    private static boolean _3(Object o) { return o != null && o.hashCode() != 0; }
    private static void _4() { if (_c < 0) throw new RuntimeException(); }
    private static Object _5(Object o) { return o == null ? new Object() : o; }
}
