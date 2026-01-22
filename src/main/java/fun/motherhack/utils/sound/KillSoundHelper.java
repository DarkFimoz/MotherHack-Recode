package fun.motherhack.utils.sound;

import fun.motherhack.MotherHack;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.Random;

import static fun.motherhack.utils.Wrapper.mc;

public class KillSoundHelper {
    
    private static final Random RANDOM = new Random();
    
    private static final Identifier KILL1_ID = MotherHack.id("kill1");
    private static final Identifier KILL2_ID = MotherHack.id("kill2");
    private static final Identifier KILL3_ID = MotherHack.id("kill3");
    private static final Identifier KILL4_ID = MotherHack.id("kill4");
    private static final Identifier KILL5_ID = MotherHack.id("kill5");
    private static final Identifier KILL6_ID = MotherHack.id("kill6");
    
    public static SoundEvent KILL1;
    public static SoundEvent KILL2;
    public static SoundEvent KILL3;
    public static SoundEvent KILL4;
    public static SoundEvent KILL5;
    public static SoundEvent KILL6;
    
    private static SoundEvent[] killSounds;
    private static long lastSoundTime = 0;
    private static final long SOUND_COOLDOWN = 300; // минимальная задержка между звуками в мс
    
    public static void playRandomKillSound() {
        long currentTime = System.currentTimeMillis();
        
        // Проверяем, прошло ли достаточно времени с последнего звука
        if (currentTime - lastSoundTime < SOUND_COOLDOWN) {
            return;
        }
        
        if (mc.getSoundManager() != null && killSounds != null && killSounds.length > 0) {
            try {
                SoundEvent sound = killSounds[RANDOM.nextInt(killSounds.length)];
                mc.getSoundManager().play(PositionedSoundInstance.master(sound, 1.0f, 1.0f));
                lastSoundTime = currentTime;
            } catch (Exception e) {
                MotherHack.LOGGER.error("[KillSoundHelper] Error playing kill sound", e);
            }
        }
    }
    
    public static void init() {
        KILL1 = Registry.register(Registries.SOUND_EVENT, KILL1_ID, SoundEvent.of(KILL1_ID));
        KILL2 = Registry.register(Registries.SOUND_EVENT, KILL2_ID, SoundEvent.of(KILL2_ID));
        KILL3 = Registry.register(Registries.SOUND_EVENT, KILL3_ID, SoundEvent.of(KILL3_ID));
        KILL4 = Registry.register(Registries.SOUND_EVENT, KILL4_ID, SoundEvent.of(KILL4_ID));
        KILL5 = Registry.register(Registries.SOUND_EVENT, KILL5_ID, SoundEvent.of(KILL5_ID));
        KILL6 = Registry.register(Registries.SOUND_EVENT, KILL6_ID, SoundEvent.of(KILL6_ID));
        
        killSounds = new SoundEvent[]{KILL1, KILL2, KILL3, KILL4, KILL5, KILL6};
        
        MotherHack.LOGGER.info("[KillSoundHelper] Kill sounds registered");
    }
}
