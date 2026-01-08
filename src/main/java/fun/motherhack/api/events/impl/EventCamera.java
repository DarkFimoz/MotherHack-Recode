package fun.motherhack.api.events.impl;

import fun.motherhack.api.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor @Getter @Setter
public class EventCamera extends Event {
    private float yaw;
    private float pitch;
    private float distance;
    private boolean cameraClip;
}
