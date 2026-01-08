package fun.motherhack.utils.rotations;

import lombok.Getter;
import net.minecraft.util.math.MathHelper;

@Getter
public class RotationData {
    private float yaw, pitch;

    public void updateRotation(double deltaX, double deltaY) {
        this.yaw += (float) deltaX * 0.15f;
        this.pitch += (float) deltaY * 0.15f;
        this.pitch = MathHelper.clamp(pitch, -90f, 90f);
    }

    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }
}