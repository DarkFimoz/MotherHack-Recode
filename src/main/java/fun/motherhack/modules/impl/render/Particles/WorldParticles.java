package fun.motherhack.modules.impl.render.Particles;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender3D;
import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.api.mixins.accessors.IWorldRenderer;
import fun.motherhack.api.render.providers.ResourceProvider;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.Wrapper;
import fun.motherhack.utils.math.TimerUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WorldParticles extends ParticlesModule.BaseSettings implements Wrapper {
    private static final Random RANDOM = new Random();
    private static final ShaderProgramKey TEXTURE_SHADER_KEY = new ShaderProgramKey(ResourceProvider.getShaderIdentifier("texture"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);
    
    private final NumberSetting distance = new NumberSetting("Distance", 15f, 5f, 50f, 1f);
    private final NumberSetting height = new NumberSetting("Height", 8f, 5f, 15f, 1f);
    private final NumberSetting gravity = new NumberSetting("Gravity", 0.3f, 0.1f, 1f, 0.1f);

    private final List<Particle> particles = new ArrayList<>();
    private final TimerUtils timerUtil = new TimerUtils();

    public WorldParticles() {
        super();
        getSettings().add(distance);
        getSettings().add(height);
        getSettings().add(gravity);
    }

    public void toggle() {
        particles.clear();
        timerUtil.reset();
    }

    public void onEvent() {
        MotherHack.getInstance().getEventHandler().subscribe(this);
    }

    private boolean isInView(Vec3d pos) {
        return ((IWorldRenderer) mc.worldRenderer).getFrustum().isVisible(new Box(pos.add(-0.2, -0.2, -0.2), pos.add(0.2, 0.2, 0.2)));
    }

    @EventHandler
    private void onTick(EventTick event) {
        if (mc.player == null) return;
        
        particles.removeIf(p -> p.isDead() || mc.player.getPos().distanceTo(p.pos) > 80);

        int diff = count().getValue().intValue() - particles.size();
        if (particles.size() < count().getValue().intValue()) {
            float d = distance.getValue();
            for (int i = 0; i < diff; i++) {
                particles.add(new Particle(
                        new Vec3d(
                            mc.player.getX() + RANDOM.nextFloat(-d, d),
                            mc.player.getY() + height.getValue(),
                            mc.player.getZ() + RANDOM.nextFloat(-d, d)
                        ),
                        ParticleRender.getTexture(textureMode().getValue()),
                        size().getValue(),
                        rotate().getValue(),
                        lifeTime().getValue().intValue(),
                        gravity.getValue()
                ));
            }
        }
    }

    @EventHandler
    private void onRender3D(EventRender3D.Game event) {
        if (mc.player == null || mc.world == null) return;

        // Получаем цвет из UI
        UI uiModule = MotherHack.getInstance().getModuleManager().getModule(UI.class);
        Color uiColor = uiModule != null ? uiModule.getTheme().getAccentColor() : new Color(255, 0, 0);

        MatrixStack matrices = event.getMatrixStack();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();

        // Включаем blend и depth test
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();

        for (Particle particle : particles) {
            if (isInView(particle.pos)) {
                particle.update();
                
                float alpha = particle.getAlpha();
                int alphaInt = Math.max(0, Math.min(255, (int) (255 * alpha)));
                Color particleColor = new Color(
                    uiColor.getRed(),
                    uiColor.getGreen(),
                    uiColor.getBlue(),
                    alphaInt
                );

                matrices.push();
                
                // Позиционируем частицу в мире
                matrices.translate(
                    particle.pos.x - cameraPos.x,
                    particle.pos.y - cameraPos.y,
                    particle.pos.z - cameraPos.z
                );
                
                // Поворачиваем к камере (billboard)
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

                // Рендерим текстуру
                RenderSystem.setShaderTexture(0, particle.texture);
                RenderSystem.setShader(TEXTURE_SHADER_KEY);
                Matrix4f posMatrix = matrices.peek().getPositionMatrix();
                
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                
                float halfSize = particle.size / 2;
                buffer.vertex(posMatrix, -halfSize, -halfSize, 0).texture(0, 1).color(particleColor.getRGB());
                buffer.vertex(posMatrix, halfSize, -halfSize, 0).texture(1, 1).color(particleColor.getRGB());
                buffer.vertex(posMatrix, halfSize, halfSize, 0).texture(1, 0).color(particleColor.getRGB());
                buffer.vertex(posMatrix, -halfSize, halfSize, 0).texture(0, 0).color(particleColor.getRGB());
                
                BufferRenderer.drawWithGlobalProgram(buffer.end());
                
                matrices.pop();
            }
        }

        // Восстанавливаем состояние
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private class Particle {
        private Vec3d pos;
        private Vec3d velocity;
        private final Identifier texture;
        private final float size;
        private final long startTime;
        private final int maxLife;
        private final float gravityStrength;

        public Particle(Vec3d pos, Identifier texture, float size, boolean rotate, int lifetime, float gravity) {
            this.pos = pos;
            this.texture = texture;
            this.size = size; // Размер в мировых координатах для 3D рендера
            this.startTime = System.currentTimeMillis();
            this.maxLife = RANDOM.nextInt(Math.max(lifetime / 2, 1), lifetime + 1) * 50;
            this.gravityStrength = gravity;
            
            this.velocity = new Vec3d(0, -RANDOM.nextFloat(gravity * 0.01f, gravity * 0.02f), 0);
        }

        public void update() {
            velocity = velocity.subtract(0, gravityStrength * 0.0001, 0);
            pos = pos.add(velocity);
            velocity = velocity.multiply(0.995);
        }

        public boolean isDead() {
            return System.currentTimeMillis() - startTime > maxLife;
        }

        public float getAlpha() {
            long elapsed = System.currentTimeMillis() - startTime;
            float progress = (float) elapsed / maxLife;
            
            if (progress < 0.2f) {
                // Появление
                return progress / 0.2f;
            } else if (progress > 0.8f) {
                // Исчезновение
                return 1.0f - ((progress - 0.8f) / 0.2f);
            }
            return 1.0f;
        }
    }
}
