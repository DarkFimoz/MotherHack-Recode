package fun.motherhack.utils.world;

import fun.motherhack.utils.Wrapper;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;

@UtilityClass
public class MultipointUtils implements Wrapper {

    public ArrayList<Vec3d> getMultiPoints(Entity entity, int hCount, int vCount) {
        double height = entity.getHeight();
        double width = entity.getWidth();
        double vOffset = height / (vCount + 1);
        double hOffset = width / (hCount + 1);
        ArrayList<Vec3d> list = new ArrayList<>();
        for (int i = 0; i <= vCount + 1; i++) {
            double currentVOffset = vOffset * i;
            for (int j = 0; j <= hCount; j++) {
                double currentHOffset = (width / 2) - hOffset * j;
                list.add(entity.getPos().add(width / 2, currentVOffset, currentHOffset));
                list.add(entity.getPos().add(-currentHOffset, currentVOffset, width / 2));
                list.add(entity.getPos().add(-width / 2, currentVOffset, -currentHOffset));
                list.add(entity.getPos().add(currentHOffset, currentVOffset, -width / 2));
            }
        }

        for (int i = 1; i <= hCount; i++) {
            double offset1 = (width / 2) - hOffset * i;
            for (int j = 1; j <= hCount; j++) {
                double offset2 = (width / 2) - hOffset * j;
                list.add(entity.getPos().add(offset1, 0, offset2));
                list.add(entity.getPos().add(offset1, height, offset2));
            }
        }

        return list;
    }

    public Vec3d getClosestPoint(Entity entity, int hCount, int vCount, double range) {
        return getMultiPoints(entity, hCount, vCount).parallelStream()
                .filter(point -> mc.player.getEyePos().squaredDistanceTo(point) <= MathHelper.square(range))
                .min(Comparator.comparingDouble(mc.player.getEyePos()::squaredDistanceTo))
                .orElse(Vec3d.ZERO);
    }
}