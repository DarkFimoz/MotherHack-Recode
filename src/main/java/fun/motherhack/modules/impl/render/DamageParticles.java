package fun.motherhack.modules.impl.render;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventAttackEntity;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.api.mixins.accessors.IWorldRenderer;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
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

    public DamageParticles() {
        super("DamageParticles", Category.Render);
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
        for (int i = 0; i < count.getValue(); i++) particles.add(new Particle(entity.getPos().add(0, entity.getHeight() / 2f, 0)));
    }

    @EventHandler
    private void onRender2D(EventRender2D e) {
        if (fullNullCheck()) return; 	

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
                Render2D.drawTexture(e.getContext().getMatrices(),
                		(float) position.getX(),
                		(float) position.getY(),
                		size.getValue() * f,
                		size.getValue() * f,
                		0f,
                		particle.texture,
                		new Color(255, 0, 0, (int) (255 * particle.alpha))
                );
            } else particles.remove(particle);
        }
        
        //RenderSystem.depthMask(true);
        //RenderSystem.defaultBlendFunc();
        //RenderSystem.disableBlend();
    }

    private static class Particle {
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
            this.texture = randomTexture();
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

        private Identifier randomTexture() {
            int random = MathUtils.randomInt(1, 4);
            if (random == 1) return MotherHack.id("particles/dollar.png");
            if (random == 2) return MotherHack.id("particles/firefly.png");
            if (random == 3) return MotherHack.id("particles/snow.png");

            return MotherHack.id("particles/star.png");
        }
    }
}