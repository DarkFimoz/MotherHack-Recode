package fun.motherhack.utils.movement;

import fun.motherhack.utils.Wrapper;
import lombok.experimental.UtilityClass;
import net.minecraft.util.PlayerInput;

@UtilityClass
public class InputUtils implements Wrapper {

    public void setForward(boolean forward) {
        PlayerInput input = mc.player.input.playerInput;
        mc.player.input.playerInput = new PlayerInput(forward, input.backward(), input.left(), input.right(), input.jump(), input.sneak(), input.sprint());
    }

    public void setBackward(boolean backward) {
        PlayerInput input = mc.player.input.playerInput;
        mc.player.input.playerInput = new PlayerInput(input.forward(), backward, input.left(), input.right(), input.jump(), input.sneak(), input.sprint());
    }

    public void setLeft(boolean left) {
        PlayerInput input = mc.player.input.playerInput;
        mc.player.input.playerInput = new PlayerInput(input.forward(), input.backward(), left, input.right(), input.jump(), input.sneak(), input.sprint());
    }

    public void setRight(boolean right) {
        PlayerInput input = mc.player.input.playerInput;
        mc.player.input.playerInput = new PlayerInput(input.forward(), input.backward(), input.left(), right, input.jump(), input.sneak(), input.sprint());
    }

    public void setJumping(boolean jumping) {
        PlayerInput input = mc.player.input.playerInput;
        mc.player.input.playerInput = new PlayerInput(input.forward(), input.backward(), input.left(), input.right(), jumping, input.sneak(), input.sprint());
    }

    public void setSneaking(boolean sneaking) {
        PlayerInput input = mc.player.input.playerInput;
        mc.player.input.playerInput = new PlayerInput(input.forward(), input.backward(), input.left(), input.right(), input.jump(), sneaking, input.sprint());
    }

    public void setSprinting(boolean sprinting) {
        PlayerInput input = mc.player.input.playerInput;
        mc.player.input.playerInput = new PlayerInput(input.forward(), input.backward(), input.left(), input.right(), input.jump(), input.sneak(), sprinting);
    }
}