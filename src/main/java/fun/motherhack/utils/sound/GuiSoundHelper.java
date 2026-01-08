package fun.motherhack.utils.sound;

import fun.motherhack.MotherHack;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import static fun.motherhack.utils.Wrapper.mc;

public class GuiSoundHelper {
    
    private static final Identifier GUI_ON_ID = MotherHack.id("gui.on");
    private static final Identifier GUI_OFF_ID = MotherHack.id("gui.off");
    private static final Identifier GUI_REVERSE_ID = MotherHack.id("gui.reverse");
    private static final Identifier GUI_OPEN_ID = MotherHack.id("gui.open");
    private static final Identifier GUI_CLOSE_ID = MotherHack.id("gui.close");
    
    public static SoundEvent GUI_ON;
    public static SoundEvent GUI_OFF;
    public static SoundEvent GUI_REVERSE;
    public static SoundEvent GUI_OPEN;
    public static SoundEvent GUI_CLOSE;
    
    private static boolean isSoundsEnabled() {
        try {
            return MotherHack.getInstance() != null 
                && MotherHack.getInstance().getModuleManager() != null
                && MotherHack.getInstance().getModuleManager().getModule(fun.motherhack.modules.impl.client.UI.class) != null
                && MotherHack.getInstance().getModuleManager().getModule(fun.motherhack.modules.impl.client.UI.class).isSoundsEnabled();
        } catch (Exception e) {
            return false;
        }
    }
    
    public static void playToggleSound(boolean enabled) {
        if (!isSoundsEnabled()) return;
        if (mc.getSoundManager() != null && GUI_ON != null && GUI_OFF != null) {
            try {
                SoundEvent sound = enabled ? GUI_ON : GUI_OFF;
                mc.getSoundManager().play(PositionedSoundInstance.master(sound, 1.0f, 1.0f));
            } catch (Exception e) {
                MotherHack.LOGGER.error("[GuiSoundHelper] Error playing toggle sound", e);
            }
        }
    }
    
    public static void playExpandSound() {
        if (!isSoundsEnabled()) return;
        if (mc.getSoundManager() != null && GUI_REVERSE != null) {
            try {
                mc.getSoundManager().play(PositionedSoundInstance.master(GUI_REVERSE, 1.0f, 1.0f));
            } catch (Exception e) {
                MotherHack.LOGGER.error("[GuiSoundHelper] Error playing expand sound", e);
            }
        }
    }
    
    public static void playOpenSound() {
        if (!isSoundsEnabled()) return;
        if (mc.getSoundManager() != null && GUI_OPEN != null) {
            try {
                mc.getSoundManager().play(PositionedSoundInstance.master(GUI_OPEN, 1.0f, 1.0f));
            } catch (Exception e) {
                MotherHack.LOGGER.error("[GuiSoundHelper] Error playing open sound", e);
            }
        }
    }
    
    public static void playCloseSound() {
        if (!isSoundsEnabled()) return;
        if (mc.getSoundManager() != null && GUI_CLOSE != null) {
            try {
                mc.getSoundManager().play(PositionedSoundInstance.master(GUI_CLOSE, 1.0f, 1.0f));
            } catch (Exception e) {
                MotherHack.LOGGER.error("[GuiSoundHelper] Error playing close sound", e);
            }
        }
    }
    
    public static void init() {
        // Регистрируем звуки в Registry
        GUI_ON = Registry.register(Registries.SOUND_EVENT, GUI_ON_ID, SoundEvent.of(GUI_ON_ID));
        GUI_OFF = Registry.register(Registries.SOUND_EVENT, GUI_OFF_ID, SoundEvent.of(GUI_OFF_ID));
        GUI_REVERSE = Registry.register(Registries.SOUND_EVENT, GUI_REVERSE_ID, SoundEvent.of(GUI_REVERSE_ID));
        GUI_OPEN = Registry.register(Registries.SOUND_EVENT, GUI_OPEN_ID, SoundEvent.of(GUI_OPEN_ID));
        GUI_CLOSE = Registry.register(Registries.SOUND_EVENT, GUI_CLOSE_ID, SoundEvent.of(GUI_CLOSE_ID));
        
        MotherHack.LOGGER.info("[GuiSoundHelper] GUI sounds registered");
    }
}
