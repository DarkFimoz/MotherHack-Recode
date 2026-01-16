package fun.motherhack.modules.impl.render;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventAttackEntity;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.api.mixins.accessors.IWorldRenderer;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.world.WorldUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.concurrent.CopyOnWriteArrayList;

public class DamageParticles extends Module {

    private final NumberSetting count = new NumberSetting("settings.damageparticles.count", 30f, 10f, 50f, 1f);
    private final NumberSetting size = new NumberSetting("settings.damageparticles.size", 30f, 10f, 50f, 1f);
    private final NumberSetting maxParticles = new NumberSetting("settings.damageparticles.maxparticles", 500f, 100f, 2000f, 50f);
    private final EnumSetting<ParticleTexture> particleTexture = new EnumSetting<>("settings.damageparticles.texture", ParticleTexture.Star);

    public DamageParticles() {
        super("DamageParticles", Category.Render);
    }

    public enum ParticleTexture implements fun.motherhack.modules.settings.api.Nameable {
        Glow("settings.damageparticles.texture.glow"),
        Star("settings.damageparticles.texture.star"),
        Feather("settings.damageparticles.texture.feather"),
        Moon("settings.damageparticles.texture.moon"),
        Spark("settings.damageparticles.texture.spark"),
        Triangle("settings.damageparticles.texture.triangle"),
        Cube("settings.damageparticles.texture.cube"),
        Cross("settings.damageparticles.texture.cross"),
        Arrow("settings.damageparticles.texture.arrow"),
        Firefly("settings.damageparticles.texture.firefly"),
        Marker("settings.damageparticles.texture.marker");

        private final String name;

        ParticleTexture(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private final CopyOnWriteArrayList<Particle> particles = new CopyOnWriteArrayList<>();

    private boolean isInView(Vec3d pos) {
        return ((IWorldRenderer) mc.worldRenderer).getFrustum().isVisible(new Box(pos.add(-0.2, -0.2, -0.2), pos.add(0.2, 0.2, 0.2)));
    }

    @EventHandler
    private void onAttackEntity(EventAttackEntity e) {
        if (fullNullCheck()) return;

        if (e.getTarget() == mc.player) return;
        if (!(e.getTarget() instanceof LivingEntity entity)) return;
        if (!entity.isAlive()) return;
        
        // Проверка лимита частиц
        int particlesToAdd = count.getValue().intValue();
        int currentParticles = particles.size();
        int maxAllowed = maxParticles.getValue().intValue();
        
        if (currentParticles >= maxAllowed) return;
        
        // Добавляем только столько частиц, сколько можем без превышения лимита
        int actualCount = Math.min(particlesToAdd, maxAllowed - currentParticles);
        
        for (int i = 0; i < actualCount; i++) {
            particles.add(new Particle(entity.getPos().add(0, entity.getHeight() / 2f, 0)));
        }
    }

    @EventHandler
    private void onRender2D(EventRender2D e) {
        if (fullNullCheck()) return;

        // Получаем цвет из UI
        UI uiModule = MotherHack.getInstance().getModuleManager().getModule(UI.class);
        Color uiColor = uiModule != null ? uiModule.getTheme().getAccentColor() : new Color(255, 0, 0);

        //RenderSystem.enableBlend();
        //RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE_MINUS_DST_COLOR, GlStateManager.DstFactor.ONE);
        //RenderSystem.enableDepthTest();
        //RenderSystem.depthMask(false);
        
        for (Particle particle : particles) {
            if (System.currentTimeMillis() - particle.time > 7000 || particle.alpha <= 0) particles.remove(particle);
            else if (mc.player.getPos().distanceTo(particle.pos) > 100) particles.remove(particle);
            else if (isInView(particle.pos)) {
                particle.update();
                Vec3d position = WorldUtils.getPosition(particle.pos);
                float f = 1 - ((System.currentTimeMillis() - particle.time) / 7000f);
                
                // Используем цвет из UI с альфой частицы
                Color particleColor = new Color(
                    uiColor.getRed(),
                    uiColor.getGreen(),
                    uiColor.getBlue(),
                    (int) (255 * particle.alpha)
                );
                
                Render2D.drawTexture(e.getContext().getMatrices(),
                		(float) position.getX(),
                		(float) position.getY(),
                		size.getValue() * f,
                		size.getValue() * f,
                		0f,
                		particle.texture,
                		particleColor
                );
            } else particles.remove(particle);
        }
        
        //RenderSystem.depthMask(true);
        //RenderSystem.defaultBlendFunc();
        //RenderSystem.disableBlend();
    }

    private class Particle {
        private Vec3d pos, velocity;
        private final long time;
        private final Identifier texture;
        private long collisionTime = -1;
        private float alpha;

        public Particle(Vec3d pos) {
            this.pos = pos;
            this.velocity = new Vec3d(
                    MathUtils.randomFloat(-0.07f, 0.07f),
                    MathUtils.randomFloat(0f, 0.07f),
                    MathUtils.randomFloat(-0.07f, 0.07f)
            );
            this.time = System.currentTimeMillis();
            this.texture = getTexture();
            this.alpha = 0.8f;
        }

        public void update() {
            if (collisionTime != -1) {
                long timeSinceCollision = System.currentTimeMillis() - collisionTime;
                alpha = Math.max(0, 0.8f - (timeSinceCollision / 3000f));
            }

            velocity = velocity.subtract(0, 0.001, 0);

            if (!mc.world.getBlockState(new BlockPos((int) Math.floor(pos.x + velocity.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z))).isAir()) {
                velocity = new Vec3d(-velocity.x * 0.3f, velocity.y, velocity.z);
                if (collisionTime == -1) collisionTime = System.currentTimeMillis();
            }

            if (!mc.world.getBlockState(new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y + velocity.y), (int) Math.floor(pos.z))).isAir()) {
                velocity = new Vec3d(velocity.x, -velocity.y * 0.5f, velocity.z);
                if (collisionTime == -1) collisionTime = System.currentTimeMillis();
            }

            if (!mc.world.getBlockState(new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z + velocity.z))).isAir()) {
                velocity = new Vec3d(velocity.x, velocity.y, -velocity.z * 0.3f);
                if (collisionTime == -1) collisionTime = System.currentTimeMillis();
            }

            pos = pos.add(velocity);
            velocity = velocity.multiply(0.995);
        }

        private Identifier getTexture() {
            ParticleTexture textureType = DamageParticles.this.particleTexture.getValue();
            return switch (textureType) {
                case Glow -> Identifier.of("motherhack", "hud/glow.png");
                case Star -> Identifier.of("motherhack", "hud/star.png");
                case Feather -> Identifier.of("motherhack", "hud/feather.png");
                case Moon -> Identifier.of("motherhack", "hud/moon.png");
                case Spark -> Identifier.of("motherhack", "hud/spark.png");
                case Triangle -> Identifier.of("motherhack", "hud/triangle.png");
                case Cube -> Identifier.of("motherhack", "hud/cube.png");
                case Cross -> Identifier.of("motherhack", "hud/mcross.png");
                case Arrow -> Identifier.of("motherhack", "hud/arrow.png");
                case Firefly -> Identifier.of("motherhack", "hud/firefly.png");
                case Marker -> Identifier.of("motherhack", "hud/marker.png");
            };
        }
    }
}