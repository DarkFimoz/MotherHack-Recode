package fun.motherhack.api.events.impl.rotations;

import fun.motherhack.api.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor @Getter @Setter
public class EventTravel extends Event {
    private float yaw, pitch;
}