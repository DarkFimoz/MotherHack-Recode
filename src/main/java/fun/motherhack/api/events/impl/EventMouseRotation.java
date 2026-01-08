package fun.motherhack.api.events.impl;

import fun.motherhack.api.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor @Getter @Setter
public class EventMouseRotation extends Event {
    private float cursorDeltaX;
    private float cursorDeltaY;
}
