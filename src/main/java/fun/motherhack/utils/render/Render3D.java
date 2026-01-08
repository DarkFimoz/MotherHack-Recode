package fun.motherhack.utils.render;

import fun.motherhack.api.mixins.accessors.IWorldRenderer;
import fun.motherhack.utils.Wrapper;
import lombok.experimental.UtilityClass;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class Render3D implements Wrapper {

    public boolean rendering = false;

    public List<VertexCollection> QUADS = new ArrayList<>();
    public List<VertexCollection> DEBUG_LINES = new ArrayList<>();
    public List<VertexCollection> SHINE_QUADS = new ArrayList<>();
    public List<VertexCollection> SHINE_DEBUG_LINES = new ArrayList<>();

    public void renderCube(MatrixStack matrices, Vec3d vec3d, double size, boolean fill, Color fillColor, boolean outline, Color outlineColor) {
        if (outline) renderBoxOutline(matrices, new Box(vec3d.add(-0.5 * size, -0.5 * size,-0.5 * size), vec3d.add(0.5 * size, 0.5 * size,0.5 * size)), outlineColor);
        if (fill) renderBox(matrices, new Box(vec3d.add(-0.5 * size, -0.5 * size, -0.5 * size), vec3d.add(0.5 * size, 0.5 * size, 0.5 * size)), fillColor);
    }

    public void renderCube(MatrixStack matrices, Box box, boolean fill, Color fillColor, boolean outline, Color outlineColor) {
        if (outline) renderBoxOutline(matrices, box, outlineColor);
        if (fill) renderBox(matrices, box, fillColor);
    }

    public void renderBox(MatrixStack matrices, Box box, Color color) {
        renderGradientBox(matrices, box, color, color);
    }

    public void renderBoxOutline(MatrixStack matrices, Box box, Color color) {
        renderGradientBoxOutline(matrices, box, color, color);
    }

    public void renderGradientBox(MatrixStack matrices, Box box, Color startColor, Color endColor) {
        if (!rendering) return;
        if (!isVisible(box)) return;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        box = cameraTransform(box);

        QUADS.add(new VertexCollection(new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB())));

        QUADS.add(new VertexCollection(new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())));

        QUADS.add(new VertexCollection(new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB())));

        QUADS.add(new VertexCollection(new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB())));

        QUADS.add(new VertexCollection(new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())));

        QUADS.add(new VertexCollection(new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB())));
    }

    public void renderGradientBoxOutline(MatrixStack matrices, Box box, Color startColor, Color endColor) {
        if (!rendering) return;
        if (!isVisible(box)) return;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        box = cameraTransform(box);

        DEBUG_LINES.add(new VertexCollection(new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB())));

        DEBUG_LINES.add(new VertexCollection(new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB())));

        DEBUG_LINES.add(new VertexCollection(new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())));

        DEBUG_LINES.add(new VertexCollection(new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB())));

        DEBUG_LINES.add(new VertexCollection(new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB())));

        DEBUG_LINES.add(new VertexCollection(new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
                new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())));
    }

    public void prepare() {
        QUADS = new ArrayList<>();
        DEBUG_LINES = new ArrayList<>();
        SHINE_QUADS = new ArrayList<>();
        SHINE_DEBUG_LINES = new ArrayList<>();
        rendering = true;
    }

    public void draw(List<VertexCollection> quads, List<VertexCollection> debugLines, boolean shine) {
        RenderSystem.enableBlend();
        if (shine) RenderSystem.blendFunc(770, 32772);
        else RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.disableDepthTest();

        if (!quads.isEmpty()) {
            BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (VertexCollection collection : quads) collection.vertex(buffer);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.disableCull();
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.enableCull();
        }

        if (!debugLines.isEmpty()) {
            BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (VertexCollection collection : debugLines) collection.vertex(buffer);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private boolean isVisible(Box box) {
        return ((IWorldRenderer) mc.worldRenderer).getFrustum().isVisible(box);
    }

    private Vec3d cameraTransform(Vec3d vec3d) {
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        Vector4f vec = new Vector4f((float) (vec3d.x - camera.x), (float) (vec3d.y - camera.y), (float) (vec3d.z - camera.z), 1.0f);
        vec.mul(new MatrixStack().peek().getPositionMatrix());
        return new Vec3d(vec.x(), vec.y(), vec.z());
    }

    private Box cameraTransform(Box box) {
        Vec3d min = cameraTransform(new Vec3d(box.minX, box.minY, box.minZ));
        Vec3d max = cameraTransform(new Vec3d(box.maxX, box.maxY, box.maxZ));
        return new Box(min, max);
    }

    private record VertexCollection(Vertex... vertices) {
        public void vertex(BufferBuilder buffer) {
            for (Vertex vertex : vertices) buffer.vertex(vertex.matrix, vertex.x, vertex.y, vertex.z).color(vertex.color);
        }
    }

    private record Vertex(Matrix4f matrix, float x, float y, float z, int color) { }
}