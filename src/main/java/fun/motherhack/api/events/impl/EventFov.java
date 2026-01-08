package fun.motherhack.api.events.impl;

import fun.motherhack.api.events.Event;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EventFov extends Event {
    private float fov = 1.0f;
}
