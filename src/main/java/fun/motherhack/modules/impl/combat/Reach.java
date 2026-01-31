package fun.motherhack.modules.impl.combat;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import lombok.Getter;

public class Reach extends Module {

    private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.Vanilla);
    private final NumberSetting entityRange = new NumberSetting("Entity Range", 3.5f, 3.0f, 6.0f, 0.1f);
    private final NumberSetting blockRange = new NumberSetting("Block Range", 4.5f, 3.0f, 6.0f, 0.1f);
    private final BooleanSetting creative = new BooleanSetting("Creative", false);
    private final NumberSetting creativeEntityRange = new NumberSetting("Creative Entity", 5.0f, 3.0f, 10.0f, 0.1f, 
            () -> creative.getValue());
    private final NumberSetting creativeBlockRange = new NumberSetting("Creative Block", 5.0f, 3.0f, 10.0f, 0.1f, 
            () -> creative.getValue());

    public Reach() {
        super("Reach", Category.Combat);
        getSettings().add(mode);
        getSettings().add(entityRange);
        getSettings().add(blockRange);
        getSettings().add(creative);
        getSettings().add(creativeEntityRange);
        getSettings().add(creativeBlockRange);
    }

    public float getEntityReachDistance() {
        if (!isToggled()) {
            return 3.0f; // Default Minecraft reach
        }
        
        // Проверяем креативный режим
        if (creative.getValue() && mc.player != null && mc.player.getAbilities().creativeMode) {
            return Math.min(creativeEntityRange.getValue(), 10.0f);
        }
        
        float maxReach = switch (mode.getValue()) {
            case Vanilla -> 6.0f;
            case ReallyWorld -> 4.5f;
            case Funtime -> 4.2f;
            case HollyWorld -> 4.8f;
            case Grim -> 3.5f;
            case Matrix -> 4.0f;
            case NCP -> 4.5f;
            case StrictNCP -> 3.8f;
        };
        
        return Math.min(entityRange.getValue(), maxReach);
    }

    public float getBlockReachDistance() {
        if (!isToggled()) {
            return 4.5f; // Default Minecraft block reach
        }
        
        // Проверяем креативный режим
        if (creative.getValue() && mc.player != null && mc.player.getAbilities().creativeMode) {
            return Math.min(creativeBlockRange.getValue(), 10.0f);
        }
        
        float maxReach = switch (mode.getValue()) {
            case Vanilla -> 6.0f;
            case ReallyWorld -> 5.0f;
            case Funtime -> 4.8f;
            case HollyWorld -> 5.2f;
            case Grim -> 4.5f;
            case Matrix -> 4.8f;
            case NCP -> 5.0f;
            case StrictNCP -> 4.5f;
        };
        
        return Math.min(blockRange.getValue(), maxReach);
    }

    // Для обратной совместимости с Aura
    public float getReachDistance() {
        return getEntityReachDistance();
    }

    public String getDisplayInfo() {
        if (!isToggled()) return null;
        return String.format("E:%.1f B:%.1f", getEntityReachDistance(), getBlockReachDistance());
    }

    @Getter
    public enum Mode implements Nameable {
        Vanilla("Vanilla"),
        ReallyWorld("ReallyWorld"),
        Funtime("Funtime"),
        HollyWorld("HollyWorld"),
        Grim("Grim"),
        Matrix("Matrix"),
        NCP("NCP"),
        StrictNCP("StrictNCP");

        private final String name;

        Mode(String name) {
            this.name = name;
        }
    }
}
