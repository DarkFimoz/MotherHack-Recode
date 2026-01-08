package fun.motherhack.utils.rotations;

import fun.motherhack.utils.Wrapper;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@UtilityClass
public class RotationUtils implements Wrapper {

    public float[] getRotations(Entity entity) {
        return getRotations(entity.getX(), entity.getY(), entity.getZ());
    }

    public float[] getRotations(Vec3d vec3d) {
        return getRotations(vec3d.x, vec3d.y, vec3d.z);
    }

    public float[] getRotations(double x, double y, double z) {
        double deltaX = x - mc.player.getX();
        double deltaY = y - mc.player.getEyeY();
        double deltaZ = z - mc.player.getZ();
        double distance = MathHelper.sqrt((float) (deltaX * deltaX + deltaZ * deltaZ));

        float yaw = (float) (MathHelper.atan2(deltaZ, deltaX) * (180D / Math.PI) - 90.0F);
        float pitch = (float) (-MathHelper.atan2(deltaY, distance) * (180D / Math.PI));
        return new float[]{yaw, pitch};
    }

    public float[] getRotations(Direction direction) {
        return switch (direction) {
            case DOWN -> new float[]{mc.player.getYaw(), 90.0f};
            case UP -> new float[]{mc.player.getYaw(), -90.0f};
            case NORTH -> new float[]{180.0f, mc.player.getPitch()};
            case SOUTH -> new float[]{0.0f, mc.player.getPitch()};
            case WEST -> new float[]{90.0f, mc.player.getPitch()};
            case EAST -> new float[]{-90.0f, mc.player.getPitch()};
        };
    }
    
    public float[] correctRotation(float[] rotations) {
    	rotations[0] -= rotations[0] % getGCDValue();
    	rotations[1] -= rotations[1] % getGCDValue();
    	return new float[]{rotations[0], rotations[1]};
    }
    
    public float getGCDValue() {
		double d4 = mc.options.getMouseSensitivity().getValue() * 0.8D;
		return (float) (d4 * d4 * d4 * 8.0D * 0.15);
    }
}