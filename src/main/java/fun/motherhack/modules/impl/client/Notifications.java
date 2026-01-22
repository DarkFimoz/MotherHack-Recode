package fun.motherhack.modules.impl.client;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class Notifications extends Module {

    @AllArgsConstructor
    @Getter
    public enum NotificationPosition implements Nameable {
        BottomLeft("Bottom Left"),
        BottomCenter("Bottom Center"),
        BottomRight("Bottom Right");

        private final String name;
    }

    private final BooleanSetting enabled = new BooleanSetting("Enabled", true);
    private final BooleanSetting moduleToggle = new BooleanSetting("Module Toggle", true);
    private final EnumSetting<NotificationPosition> position = new EnumSetting<>("Position", NotificationPosition.BottomCenter);

    public Notifications() {
        super("Notifications", Category.Client);
        
        getSettings().add(enabled);
        getSettings().add(moduleToggle);
        getSettings().add(position);
        
        setToggled(true);
    }

    public boolean isEnabled() {
        return enabled.getValue();
    }

    public boolean isModuleToggleEnabled() {
        return moduleToggle.getValue();
    }

    public NotificationPosition getPosition() {
        return position.getValue();
    }

    @Override
    public void onEnable() {
        toggled = true;
        MotherHack.getInstance().getEventHandler().subscribe(this);
    }

    @Override
    public void onDisable() {
        toggled = false;
        MotherHack.getInstance().getEventHandler().unsubscribe(this);
    }
}
