package fun.motherhack.modules.impl.render.Particles;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.motherhack.MotherHack;
import fun.motherhack.api.render.providers.ResourceProvider;
import fun.motherhack.utils.Wrapper;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.math.TimerUtils;
import fun.motherhack.utils.render.ColorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

@Setter
@Getter
@Accessors(fluent = true, chain = true)
public class ParticleRender implements Wrapper {
    private static final Random RANDOM = new Random();
    private static final ShaderProgramKey TEXTURE_SHADER_KEY = new ShaderProgramKey(ResourceProvider.getShaderIdentifier("texture"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);
    
    private float prevX, prevY, prevZ;
    private float x, y, z;
    private float motionX, motionY, motionZ;
    private int lifeTime;
    private int maxLife;
    private float prevSize = 0f;
    private float rotation, prevRotation = 0f;
    private float rotateSpeed = 20f, size;
    private int index;
    private Identifier identifier;
    private boolean dropPhysics, rotating;

    private float spawnDuration, dyingDuration;

    private final TimerUtils timerUtil = new TimerUtils();
    private final Animation alphaAnimation = new Animation(1000, 1.0, false, Easing.EASE_OUT_CUBIC);
    private boolean gravityFalls = false;

    private boolean trail;
    private float trailLength = 5f;
    private boolean dyingEffect;
    private final Deque<Vec3d> trailPoints = new ArrayDeque<>();

    public ParticleRender(float x, float y, float z, int lifeTime) {
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.x = x;
        this.y = y;
        this.z = z;
        this.maxLife = RANDOM.nextInt(Math.max(lifeTime / 2, 1), lifeTime + 1);
        this.rotation = RANDOM.nextFloat(-180f, 180f);
    }

    public static Identifier getTexture(ParticlesModule.TextureMode mode) {
        return switch (mode) {
            case Spark -> MotherHack.id("particles/spark_" + RANDOM.nextInt(1, 5) + ".png");
            case Star -> MotherHack.id("particles/star.png");
            case Heart -> MotherHack.id("particles/heart.png");
            case Dollar -> MotherHack.id("particles/dollar.png");
            case Snowflake -> MotherHack.id("particles/snow.png");
            case Glow -> MotherHack.id("particles/glow.png");
            case Firefly -> MotherHack.id("particles/firefly.png");
        };
    }

    public boolean update() {
        float gravity = gravityFalls ? (float) alphaAnimation.getValue() * 0.3f : 1f;

        prevX = x;
        prevY = y;
        prevZ = z;

        x += motionX;
        y += motionY * gravity;
        z += motionZ;

        double speed = Math.sqrt((motionX * motionX + motionZ * motionZ));
        float halfSize = prevSize;

        if (posBlock(x, y - halfSize - 0.05f, z)) {
            motionY = -motionY / 1.1f;
            motionX /= 1.1f;
            motionZ /= 1.1f;
        } else {
            if (posBlock(x - (float) speed - halfSize, y, z - (float) speed - halfSize) ||
                    posBlock(x + (float) speed + halfSize, y, z + (float) speed + halfSize) ||
                    posBlock(x + (float) speed + halfSize, y, z - (float) speed - halfSize) ||
                    posBlock(x - (float) speed - halfSize, y, z + (float) speed + halfSize) ||
                    posBlock(x + (float) speed + halfSize, y, z) ||
                    posBlock(x - (float) speed - halfSize, y, z) ||
                    posBlock(x, y, z + (float) speed + halfSize) ||
                    posBlock(x, y, z - (float) speed - halfSize)) {
                motionX = -motionX;
                motionZ = -motionZ;
                maxLife--;
            } else if (dropPhysics) {
                motionY -= 0.02f;
            }
        }

        prevRotation = rotation;
        rotation -= (prevRotation > 0) ? -rotateSpeed : rotateSpeed;

        if (!gravityFalls) {
            float scale = 1.1f;
            motionX /= scale;
            motionY /= scale;
            motionZ /= scale;
        }

        if (trail) {
            trailPoints.addFirst(new Vec3d(x, y, z));
            while (trailPoints.size() > trailLength) trailPoints.removeLast();
        }

        return mc.player.getPos().distanceTo(new Vec3d(x, y, z)) >= 80 ||
                alphaAnimation.getValue() <= 0.0 && timerUtil.passed((long)((spawnDuration + dyingDuration + maxLife) * 50));
    }

    private float alphaPC() {
        return (float) alphaAnimation.getValue();
    }

    private int alpha() {
        return (int) (255 * alphaPC());
    }

    public void updateAlpha() {
        float alphaAnim = alphaPC();

        if (alphaAnim <= 0.0 && !timerUtil.passed((long)(spawnDuration * 50))) {
            alphaAnimation.setDuration((long) (spawnDuration * 50));
            alphaAnimation.update(true);
        }

        if (alphaAnim >= 1.0 && timerUtil.passed((long)((spawnDuration + maxLife) * 50))) {
            alphaAnimation.setDuration((long) (dyingDuration * 50));
            alphaAnimation.update(false);
        }
    }

    public void render(MatrixStack matrixStack) {
        Camera camera = mc.gameRenderer.getCamera();
        Color primaryColor = ColorUtils.alpha(ColorUtils.getGradientColor(index * 90, 255), alpha());
        Vec3d interpolatedPos = interpolatePosition(prevX, prevY, prevZ, x, y, z);

        float halfSize = MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true), prevSize, (size * alphaPC()));
        prevSize = halfSize;

        // Настройка состояния рендеринга
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, identifier);
        RenderSystem.setShader(TEXTURE_SHADER_KEY);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        
        matrixStack.push();
        matrixStack.translate(interpolatedPos.x, interpolatedPos.y, interpolatedPos.z);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        if (rotating) matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true), prevRotation, rotation)));

        Matrix4f posMatrix = matrixStack.peek().getPositionMatrix();
        
        // Используем стандартный шейдер для текстур
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        
        // Правильный порядок вершин для квада с текстурой
        bufferBuilder.vertex(posMatrix, -halfSize, -halfSize, 0f).texture(0f, 1f).color(primaryColor.getRGB());
        bufferBuilder.vertex(posMatrix, halfSize, -halfSize, 0f).texture(1f, 1f).color(primaryColor.getRGB());
        bufferBuilder.vertex(posMatrix, halfSize, halfSize, 0f).texture(1f, 0f).color(primaryColor.getRGB());
        bufferBuilder.vertex(posMatrix, -halfSize, halfSize, 0f).texture(0f, 0f).color(primaryColor.getRGB());
        
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        
        matrixStack.pop();
        
        // Восстановление состояния
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    public void renderTrail(MatrixStack matrixStack) {
        if (!trail || trailPoints.size() <= 1) return;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(1.5f);
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        Matrix4f mat = matrixStack.peek().getPositionMatrix();
        Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();
        Color col = ColorUtils.alpha(ColorUtils.getGradientColor(index * 90, 255), alpha());

        float delta = mc.getRenderTickCounter().getTickDelta(true);
        double interpX = MathHelper.lerp(delta, prevX, x);
        double interpY = MathHelper.lerp(delta, prevY, y);
        double interpZ = MathHelper.lerp(delta, prevZ, z);

        Vec3d last = null;
        for (Vec3d p : trailPoints) {
            double smoothX = MathHelper.lerp(0.05, p.x, interpX);
            double smoothY = MathHelper.lerp(0.05, p.y, interpY);
            double smoothZ = MathHelper.lerp(0.05, p.z, interpZ);
            Vec3d smooth = new Vec3d(smoothX, smoothY, smoothZ);

            if (last != null) {
                buf.vertex(mat, (float)(last.x - cam.x), (float)(last.y - cam.y), (float)(last.z - cam.z))
                        .color(col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
                buf.vertex(mat, (float)(smooth.x - cam.x), (float)(smooth.y - cam.y), (float)(smooth.z - cam.z))
                        .color(col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
            }
            last = smooth;
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private boolean posBlock(float x, float y, float z) {
        Block block = mc.world != null ? mc.world.getBlockState(BlockPos.ofFloored(x, y, z)).getBlock() : null;
        return block != null &&
                !(block instanceof AirBlock) &&
                block != Blocks.WATER &&
                block != Blocks.LAVA &&
                block != Blocks.SEAGRASS &&
                block != Blocks.TALL_SEAGRASS &&
                block != Blocks.SHORT_GRASS &&
                block != Blocks.TALL_GRASS &&
                block != Blocks.FERN &&
                block != Blocks.DEAD_BUSH &&
                block != Blocks.VINE &&
                block != Blocks.SNOW &&
                block != Blocks.POPPY &&
                block != Blocks.DANDELION &&
                block != Blocks.BROWN_MUSHROOM &&
                block != Blocks.RED_MUSHROOM;
    }

    private Vec3d interpolatePosition(float prevX, float prevY, float prevZ, float currentX, float currentY, float currentZ) {
        Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();
        double cameraX = cameraPos.x;
        double cameraY = cameraPos.y;
        double cameraZ = cameraPos.z;

        float delta = mc.getRenderTickCounter().getTickDelta(true);
        double interpolatedX = MathHelper.lerp(delta, prevX, currentX) - cameraX;
        double interpolatedY = MathHelper.lerp(delta, prevY, currentY) - cameraY;
        double interpolatedZ = MathHelper.lerp(delta, prevZ, currentZ) - cameraZ;

        return new Vec3d(interpolatedX, interpolatedY, interpolatedZ);
    }
}
