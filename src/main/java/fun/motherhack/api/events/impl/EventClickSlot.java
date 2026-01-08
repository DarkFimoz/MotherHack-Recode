package fun.motherhack.api.events.impl;

import fun.motherhack.api.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.screen.slot.SlotActionType;

@Getter
@AllArgsConstructor
public class EventClickSlot extends Event {
    private final int syncId;
    private final int slot;
    private final int button;
    private final SlotActionType slotActionType;
}
