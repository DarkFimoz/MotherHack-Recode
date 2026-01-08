package fun.motherhack.api.events;

import lombok.Getter;

@Getter
public class Event {
    private boolean cancelled;

    public void cancel() {
        cancelled = true;
    }

    public void resume() {
        cancelled = false;
    }
}