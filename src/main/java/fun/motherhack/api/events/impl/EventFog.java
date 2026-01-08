package fun.motherhack.api.events.impl;

import fun.motherhack.api.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;

@Getter
@Setter
@AllArgsConstructor
public class EventFog extends Event {
    private float start;
    private float end;
    private Color color;
}
