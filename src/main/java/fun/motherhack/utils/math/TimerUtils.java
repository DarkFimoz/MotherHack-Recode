package fun.motherhack.utils.math;

import lombok.Setter;

public class TimerUtils {
    @Setter private long startTime = System.currentTimeMillis();

    public void reset() {
        startTime = System.currentTimeMillis();
    }

    public boolean passed(long time) {
        return System.currentTimeMillis() - startTime > time;
    }

    public long getElapsed() {
        return System.currentTimeMillis() - startTime;
    }
}