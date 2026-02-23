package fun.motherhack.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender3D;
import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.api.render.providers.ResourceProvider;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.modules.impl.render.Particles.ParticlesModule;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.ColorSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.render.ColorUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Trails extends Module {

    @AllArgsConstructor @Getter
    public enum ColorMode implements Nameable {
        Rainbow("Rainbow"),
        UIColor("UIColor");

        private final String name;
    }

    @AllArgsConstructor @Getter
    public enum TrailMode implements Nameable {
        Line("Line"),
        Particles("Particles");

        private final String name;
    }

    private final EnumSetting<TrailMode> mode = new EnumSetting<>("Mode", TrailMode.Line);
    private final BooleanSetting showFriends = new BooleanSetting("settings.trails.showfriends", true);
    private final BooleanSetting showSelf = new BooleanSetting("settings.trails.showself", true);
    private final BooleanSetting showPlayers = new BooleanSetting("settings.trails.showplayers", true);
    private final EnumSetting<ColorMode> colorMode = new EnumSetting<>("Color Mode", ColorMode.UIColor);
    private final ColorSetting uiColor = new ColorSetting(new Color(0x64007CFF, true));
    
    // Настройки для режима Particles
    private final EnumSetting<ParticlesModule.TextureMode> particleTexture = new EnumSetting<>("Particle Texture", ParticlesModule.TextureMode.Spark);
    private final NumberSetting particleSize = new NumberSetting("Particle Size", 0.15f, 0.05f, 1f, 0.05f);
    private final NumberSetting particleLifetime = new NumberSetting("Particle Lifetime", 1000f, 100f, 10000f, 100f);
    private final NumberSetting particleSpawnRate = new NumberSetting("Spawn Rate", 50f, 1f, 500f, 1f);
    private final NumberSetting particleAmount = new NumberSetting("Particle Amount", 1f, 1f, 20f, 1f);
    private final NumberSetting particleSpread = new NumberSetting("Particle Spread", 0.2f, 0f, 2f, 0.05f);

    private final long trailLifetimeMs = 250L;
    private final double minDistance = 0.01;

    private final Map<PlayerEntity, List<Trail>> trailsMap = new HashMap<>();
    private final Map<PlayerEntity, List<TrailParticle>> particlesMap = new HashMap<>();
    private final Map<PlayerEntity, Long> lastParticleSpawn = new HashMap<>();
    
    private static final Random RANDOM = new Random();
    private static final ShaderProgramKey TEXTURE_SHADER_KEY = new ShaderProgramKey(ResourceProvider.getShaderIdentifier("texture"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

    public Trails() {
        super("Trails", Category.Render);
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        // Очищаем все следы при отключении модуля
        trailsMap.clear();
        particlesMap.clear();
        lastParticleSpawn.clear();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (!toggled) return; // Не обновляем следы если модуль выключен
        if (fullNullCheck()) return;
        long now = System.currentTimeMillis();
        
        TrailMode currentMode = mode.getValue();
        
        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (!shouldRenderTrails(entity)) continue;
            
            if (currentMode == TrailMode.Line) {
                List<Trail> trails = trailsMap.computeIfAbsent(entity, k -> new ArrayList<>());
                trails.removeIf(t -> t.isExpired(now));
            } else if (currentMode == TrailMode.Particles) {
                List<TrailParticle> particles = particlesMap.computeIfAbsent(entity, k -> new ArrayList<>());
                particles.removeIf(p -> p.isExpired(now));
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (!toggled) return; // Не рендерим следы если модуль выключен
        if (fullNullCheck()) return;
        float tickDelta = event.getTickCounter().getTickDelta(true);
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();
        
        TrailMode currentMode = mode.getValue();
        
        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (!shouldRenderTrails(entity)) continue;
            Vec3d interp = interpolateEntityPosition(entity, tickDelta);
            
            if (currentMode == TrailMode.Line) {
                List<Trail> trails = trailsMap.computeIfAbsent(entity, k -> new ArrayList<>());
                if (trails.isEmpty()) {
                    trails.add(new Trail(interp, getTrailColor(entity), now));
                } else {
                    Trail last = trails.get(trails.size() - 1);
                    if (last.pos.distanceTo(interp) >= minDistance) {
                        trails.add(new Trail(interp, getTrailColor(entity), now));
                    }
                }
                render(event, entity, cameraPos, now);
            } else if (currentMode == TrailMode.Particles) {
                List<TrailParticle> particles = particlesMap.computeIfAbsent(entity, k -> new ArrayList<>());
                Long lastSpawn = lastParticleSpawn.get(entity);
                
                if (lastSpawn == null || now - lastSpawn >= particleSpawnRate.getValue()) {
                    int amount = particleAmount.getValue().intValue();
                    // Спавним несколько частиц за раз
                    for (int i = 0; i < amount; i++) {
                        Identifier texture = getParticleTexture();
                        // Спавним частицы на случайной высоте от ног до головы игрока
                        float heightOffset = RANDOM.nextFloat(0f, Math.max(0.01f, entity.getHeight()));
                        float spread = particleSpread.getValue();
                        // Добавляем случайное смещение по X и Z для более естественного эффекта
                        double offsetX = spread > 0 ? RANDOM.nextFloat(-spread, spread) : 0;
                        double offsetZ = spread > 0 ? RANDOM.nextFloat(-spread, spread) : 0;
                        Vec3d particlePos = interp.add(offsetX, heightOffset, offsetZ);
                        particles.add(new TrailParticle(particlePos, texture, particleSize.getValue(), getTrailColor(entity), now, particleLifetime.getValue().longValue()));
                    }
                    lastParticleSpawn.put(entity, now);
                }
                
                renderParticles(event, entity, cameraPos, now);
            }
        }
    }

    private int getTrailColor(PlayerEntity entity) {
        if (MotherHack.getInstance().getFriendManager().isFriend(entity.getName().getString())) {
            return new Color(0, 255, 0).getRGB();
        }
        
        // Всегда используем цвет из UI модуля
        UI uiModule = MotherHack.getInstance().getModuleManager().getModule(UI.class);
        if (uiModule != null) {
            UI.ClickGuiTheme theme = uiModule.getTheme();
            Color uiColor = theme.getAccentColor();
            
            ColorMode mode = colorMode.getValue();
            if (mode == ColorMode.Rainbow) {
                // Rainbow эффект с базовым цветом UI
                float hue = (System.currentTimeMillis() % 2000) / 2000f;
                return Color.HSBtoRGB(hue, 1f, 1f);
            } else {
                // UIColor - используем цвет из UI
                return uiColor.getRGB();
            }
        }
        
        // Fallback на белый если UI модуль недоступен
        return Color.WHITE.getRGB();
    }

    private boolean shouldRenderTrails(PlayerEntity entity) {
        if (entity == mc.player) {
            if (mc.options.getPerspective().isFirstPerson()) {
                return false;
            }
            return showSelf.getValue();
        }
        if (showFriends.getValue() && MotherHack.getInstance().getFriendManager().isFriend(entity.getName().getString())) {
            return true;
        }
        return showPlayers.getValue();
    }

    private Vec3d interpolateEntityPosition(PlayerEntity entity, float tickDelta) {
        double ix = entity.prevX + (entity.getX() - entity.prevX) * tickDelta;
        double iy = entity.prevY + (entity.getY() - entity.prevY) * tickDelta;
        double iz = entity.prevZ + (entity.getZ() - entity.prevZ) * tickDelta;
        return new Vec3d(ix, iy, iz);
    }

    private void render(EventRender3D.Game event, PlayerEntity entity, Vec3d cameraPos, long now) {
        List<Trail> trails = trailsMap.get(entity);
        if (trails == null || trails.isEmpty()) return;

        List<Trail> validTrails = trails.stream().filter(t -> !t.isExpired(now)).toList();
        if (validTrails.isEmpty()) return;

        float playerHeight = entity.getHeight();
        event.getMatrixStack().push();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        for (Trail p : validTrails) {
            float ageFrac = (float) (now - p.time) / (float) trailLifetimeMs;
            float alpha = 1f - Math.min(1f, ageFrac);
            alpha = Math.max(0.01f, alpha);
            int color = ColorUtils.alpha(new Color(p.color), (int) (alpha * 255)).getRGB();
            Vec3d posRel = p.pos.subtract(cameraPos);

            buffer.vertex(event.getMatrixStack().peek().getPositionMatrix(), (float) posRel.x, (float) (posRel.y + playerHeight), (float) posRel.z).color(color);
            buffer.vertex(event.getMatrixStack().peek().getPositionMatrix(), (float) posRel.x, (float) posRel.y, (float) posRel.z).color(color);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        event.getMatrixStack().pop();
    }

    private Identifier getParticleTexture() {
        ParticlesModule.TextureMode textureMode = particleTexture.getValue();
        return switch (textureMode) {
            case Spark -> MotherHack.id("particles/spark_" + RANDOM.nextInt(1, 5) + ".png");
            case Star -> MotherHack.id("particles/star.png");
            case Heart -> MotherHack.id("particles/heart.png");
            case Dollar -> MotherHack.id("particles/dollar.png");
            case Snowflake -> MotherHack.id("particles/snow.png");
            case Glow -> MotherHack.id("particles/glow.png");
            case Firefly -> MotherHack.id("particles/firefly.png");
        };
    }

    private void renderParticles(EventRender3D.Game event, PlayerEntity entity, Vec3d cameraPos, long now) {
        List<TrailParticle> particles = particlesMap.get(entity);
        if (particles == null || particles.isEmpty()) return;

        MatrixStack matrices = event.getMatrixStack();
        Camera camera = mc.gameRenderer.getCamera();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();

        for (TrailParticle particle : particles) {
            if (particle.isExpired(now)) continue;

            float alpha = particle.getAlpha(now);
            Color baseColor = new Color(particle.color);
            int alphaInt = Math.max(0, Math.min(255, (int) (255 * alpha)));
            Color particleColor = new Color(
                baseColor.getRed(),
                baseColor.getGreen(),
                baseColor.getBlue(),
                alphaInt
            );

            matrices.push();
            
            matrices.translate(
                particle.pos.x - cameraPos.x,
                particle.pos.y - cameraPos.y,
                particle.pos.z - cameraPos.z
            );
            
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

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

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static class Trail {
        public final Vec3d pos;
        public final int color;
        public final long time;

        public Trail(Vec3d pos, int color, long time) {
            this.pos = pos;
            this.color = color;
            this.time = time;
        }

        public boolean isExpired(long now) {
            return (now - time) > 250L;
        }
    }

    public static class TrailParticle {
        public final Vec3d pos;
        public final Identifier texture;
        public final float size;
        public final int color;
        public final long spawnTime;
        public final long lifetime;

        public TrailParticle(Vec3d pos, Identifier texture, float size, int color, long spawnTime, long lifetime) {
            this.pos = pos;
            this.texture = texture;
            this.size = size;
            this.color = color;
            this.spawnTime = spawnTime;
            this.lifetime = lifetime;
        }

        public boolean isExpired(long now) {
            return (now - spawnTime) > lifetime;
        }

        public float getAlpha(long now) {
            long elapsed = now - spawnTime;
            float progress = (float) elapsed / lifetime;
            
            if (progress < 0.2f) {
                return progress / 0.2f;
            } else if (progress > 0.8f) {
                return 1.0f - ((progress - 0.8f) / 0.2f);
            }
            return 1.0f;
        }
    }
}