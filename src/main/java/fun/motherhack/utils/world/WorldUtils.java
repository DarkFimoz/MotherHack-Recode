package fun.motherhack.utils.world;

import fun.motherhack.api.mixins.accessors.IGameRenderer;
import fun.motherhack.utils.Wrapper;
import lombok.experimental.UtilityClass;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

@UtilityClass
public class WorldUtils implements Wrapper {

    public final Matrix4f lastWorld = new Matrix4f(), lastProj = new Matrix4f(), lastModelView = new Matrix4f();

    public Vec3d getPosition(Vec3d pos) {
        Camera camera = mc.getEntityRenderDispatcher().camera;
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        Vector3f target = new Vector3f();

        double deltaX = pos.x - camera.getPos().x;
        double deltaY = pos.y - camera.getPos().y;
        double deltaZ = pos.z - camera.getPos().z;

        Vector4f coords = new Vector4f((float) deltaX, (float) deltaY, (float) deltaZ, 1f).mul(lastWorld);
        Matrix4f matrixProj = new Matrix4f(lastProj);
        Matrix4f matrixModel = new Matrix4f(lastModelView);
        matrixProj.mul(matrixModel).project(coords.x(), coords.y(), coords.z(), viewport, target);

        return new Vec3d(target.x / mc.getWindow().getScaleFactor(), (mc.getWindow().getHeight() - target.y) / mc.getWindow().getScaleFactor(), target.z);
    }

    public double getScale(Vec3d position, double size) {
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        double distance = cam.distanceTo(position);
        double fov = ((IGameRenderer) mc.gameRenderer).getFov$drug(mc.gameRenderer.getCamera(), mc.getRenderTickCounter().getTickDelta(true), true);
        return Math.max(10f, 1000 / distance) * (size / 30f) / (fov == 70 ? 1 : fov / 70.0f);
    }
}