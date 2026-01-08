package fun.motherhack.utils.combat;

import fun.motherhack.utils.Wrapper;
import lombok.experimental.UtilityClass;
import net.minecraft.util.math.MathHelper;

@UtilityClass
public class BoostUtils implements Wrapper {

    public double getBoost() {
        float countableSpeed;
        int[] vectors = {-45, 45, 135, -135};
        int[] addVectors = {-90, 90, 180, -180, 0};
        int[] pitchVectors = {-45, 45};

        float lastYaw = mc.player.prevYaw;
        float lastPitch = mc.player.prevPitch;
        int minDist = findClosestVector(lastYaw, vectors);
        float maxDist = Math.abs(MathHelper.wrapDegrees(lastYaw) - vectors[minDist]);
        int addMinDist = findClosestVector(lastYaw, addVectors);
        float addMaxDist = Math.abs(MathHelper.wrapDegrees(lastYaw) - addVectors[addMinDist]);
        countableSpeed = (minDist == -1) ? 1.5f : 2.06f - maxDist * 0.56F / 45F;
        if (addMaxDist < 10) countableSpeed += 0.1f - 0.1f * addMaxDist / 10F;
        int pitchMinDist = findClosestVector(lastPitch, pitchVectors);
        float pitchMaxDist = Math.abs(Math.abs(lastPitch) - Math.abs(pitchVectors[pitchMinDist]));

        if (pitchMaxDist < 26) {
            countableSpeed = Math.max(1.94f, countableSpeed);
            countableSpeed += 0.05f - pitchMaxDist * 0.05F / 26F;
        }

        countableSpeed = Math.min(2.045f, countableSpeed);
        if (mc.player.prevPitch > -55 && mc.player.prevPitch < -19f) countableSpeed = 1.91f;
        else if (mc.player.prevPitch < -55) countableSpeed = 1.54f;
        if (mc.player.prevPitch > 19f && mc.player.prevPitch < 55) countableSpeed = 1.8f;
        else if (mc.player.prevPitch > 55) countableSpeed = 1.54f;

        return countableSpeed;
    }

    private int findClosestVector(float lastYaw, int[] vectors) {
        int index = 0;
        int minDistIndex = -1;
        float minDist = Float.MAX_VALUE;

        for (int vector : vectors) {
            float dist = Math.abs(MathHelper.wrapDegrees(lastYaw) - vector);
            if (dist < minDist) {
                minDist = dist;
                minDistIndex = index;
            }

            index++;
        }

        return minDistIndex;
    }
}