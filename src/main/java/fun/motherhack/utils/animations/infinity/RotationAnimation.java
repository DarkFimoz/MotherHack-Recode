package fun.motherhack.utils.animations.infinity;

import fun.motherhack.utils.animations.Easing;

public class RotationAnimation {
    private final InfinityAnimation yaw = new InfinityAnimation(Easing.LINEAR);
    private final InfinityAnimation pitch = new InfinityAnimation(Easing.LINEAR);

    public RotationAnimation() {}

    public RotationAnimation(Easing yawEasing, Easing pitchEasing) {
        setEasing(yawEasing, pitchEasing);
    }

    public float animateYaw(float yaw, long duration) {
        return this.yaw.animate(yaw, duration);
    }

    public float animatePitch(float pitch, long duration) {
        return this.pitch.animate(pitch, duration);
    }

    public float getYaw() {
        return yaw.getValue();
    }

    public float getPitch() {
        return pitch.getValue();
    }

    public boolean finished() {
        return yaw.finished() || pitch.finished();
    }

    public void setEasing(Easing yawEasing, Easing pitchEasing) {
        yaw.setEasing(yawEasing);
        pitch.setEasing(pitchEasing);
    }
}