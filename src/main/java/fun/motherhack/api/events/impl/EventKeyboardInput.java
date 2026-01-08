package fun.motherhack.api.events.impl;

import fun.motherhack.api.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.MathHelper;

@AllArgsConstructor @Getter @Setter
public class EventKeyboardInput extends Event {
    private float movementForward, movementSideways;

    public void setYaw(float yaw, float yaw2) {
        float forward = getMovementForward();
        float sideways = getMovementSideways();
        double angle = MathHelper.wrapDegrees(Math.toDegrees(direction(yaw2, forward, sideways)));
        if (forward == 0 && sideways == 0) return;
        float closestForward = 0, closestSideways = 0, closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1F; predictedForward <= 1F; predictedForward += 1F) {
            for (float predictedSideways = -1F; predictedSideways <= 1F; predictedSideways += 1F) {
                if (predictedSideways == 0 && predictedForward == 0) continue;

                double predictedAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(yaw, predictedForward, predictedSideways)));
                double difference = Math.abs(angle - predictedAngle);

                if (difference < closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestSideways = predictedSideways;
                }
            }
        }

        setMovementForward(closestForward);
        setMovementSideways(closestSideways);
    }

    private double direction(float yaw, double movementForward, double movementSideways) {
        if (movementForward < 0F) yaw += 180F;
        float forward = 1F;
        if (movementForward < 0F) forward = -0.5F;
        else if (movementForward > 0F) forward = 0.5F;
        if (movementSideways > 0F) yaw -= 90F * forward;
        if (movementSideways < 0F) yaw += 90F * forward;
        return Math.toRadians(yaw);
    }
}