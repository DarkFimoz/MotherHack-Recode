package fun.motherhack.api.events.impl.rotations;

import fun.motherhack.api.events.Event;
import lombok.*;

@AllArgsConstructor @Getter @Setter
public class EventJump extends Event {
    private float yaw;
}