package fun.motherhack.api.events.impl;

import fun.motherhack.api.events.Event;
import fun.motherhack.modules.settings.Setting;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public class EventSettingChange extends Event {
    private final Setting<?> setting;
}