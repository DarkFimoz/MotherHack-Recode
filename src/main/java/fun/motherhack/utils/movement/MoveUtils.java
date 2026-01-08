package fun.motherhack.utils.movement;

import fun.motherhack.utils.Wrapper;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MoveUtils implements Wrapper {

    public boolean isMoving() {
        return mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;
    }
}