package fun.motherhack.modules.settings.impl;

import fun.motherhack.modules.settings.Setting;

import java.util.function.Supplier;

public class ButtonSetting extends Setting<Void> {

    private final Runnable action;

    public ButtonSetting(String name, Runnable action) {
        super(name, null);
        this.action = action;
    }

    public ButtonSetting(String name, Runnable action, Supplier<Boolean> visible) {
        super(name, null, visible);
        this.action = action;
    }

    public void run() {
        if (action != null) action.run();
    }
}