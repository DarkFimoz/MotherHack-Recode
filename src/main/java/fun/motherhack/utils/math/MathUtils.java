package fun.motherhack.utils.math;

import fun.motherhack.utils.Wrapper;
import lombok.experimental.UtilityClass;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class MathUtils implements Wrapper {

    public boolean isHovered(float x, float y, float width, float height, float mouseX, float mouseY) {
        return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
    }

    public float randomFloat(float min, float max) {
        return (float) (Math.random() * (max - min) + min);
    }

    public int randomInt(int min, int max) {
        return (int) (Math.random() * (max - min) + min);
    }

    public float round(float number) {
        return Math.round(number * 10f) / 10f;
    }

    public float round(float num, float increment) {
        float value = Math.round(num / increment) * increment;
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public Vec3d transform(Matrix4f matrix, float x, float y, float z) {
        Vector3f vector3f = matrix.transformPosition(x, y, z, new Vector3f());
        return new Vec3d(vector3f.x(), vector3f.y(), vector3f.z());
    }

    public String getCurrentTime() {
        ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), ZoneId.of("Europe/Moscow"));
        return date.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public boolean inFov(Vec3d pos, int fov, float yaw) {
        double deltaX = pos.getX() - mc.player.getX();
        double deltaZ = pos.getZ() - mc.player.getZ();
        float angle = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90;
        float yawDelta = MathHelper.wrapDegrees(angle - yaw);

        return Math.abs(yawDelta) <= fov;
    }
    
    public float getStep(float current, float target, float step) {
        if (Math.abs(target - current) <= step) return target;

        return current + Math.signum(target - current) * step;
    }

    public double interpolate(double current, double target, double delta) {
        return current + (target - current) * delta;
    }
}
