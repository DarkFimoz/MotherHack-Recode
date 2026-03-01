package fun.motherhack.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.api.events.impl.EventRender3D;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.world.WorldUtils;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class Trajectories extends Module {
    
    private final BooleanSetting showLine;
    private final BooleanSetting showHitMarker;
    private final BooleanSetting showParticles;
    private final NumberSetting lineWidth;
    private final NumberSetting particleCount;
    private final NumberSetting particleSize;
    private final EnumSetting<ParticleTextureType> particleTexture;
    
    private final CopyOnWriteArrayList<TrajectoryParticle> particles;
    
    public enum ParticleTextureType implements fun.motherhack.modules.settings.api.Nameable {
        Glow("settings.trajectories.texture.glow"),
        Star("settings.trajectories.texture.star"),
        Feather("settings.trajectories.texture.feather"),
        Moon("settings.trajectories.texture.moon"),
        Spark("settings.trajectories.texture.spark"),
        Triangle("settings.trajectories.texture.triangle"),
        Cube("settings.trajectories.texture.cube"),
        Cross("settings.trajectories.texture.cross"),
        Arrow("settings.trajectories.texture.arrow"),
        Firefly("settings.trajectories.texture.firefly"),
        Marker("settings.trajectories.texture.marker");

        private final String name;

        ParticleTextureType(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
    
    public Trajectories() {
        super("Trajectories", Category.Render);
        
        this.showLine = new BooleanSetting("settings.trajectories.showline", true);
        this.showHitMarker = new BooleanSetting("settings.trajectories.showhitmarker", true);
        this.showParticles = new BooleanSetting("settings.trajectories.showparticles", true);
        this.lineWidth = new NumberSetting("settings.trajectories.linewidth", 2.0f, 1.0f, 5.0f, 0.5f);
        this.particleCount = new NumberSetting("settings.trajectories.particlecount", 5.0f, 1.0f, 20.0f, 1.0f);
        this.particleSize = new NumberSetting("settings.trajectories.particlesize", 8.0f, 4.0f, 16.0f, 1.0f);
        this.particleTexture = new EnumSetting<>("settings.trajectories.texture", ParticleTextureType.Star);
        
        this.particles = new CopyOnWriteArrayList<>();
    }
    
    @meteordevelopment.orbit.EventHandler
    public void onRender3D(EventRender3D.Game event) {
            if (fullNullCheck()) {
                return;
            }

            ItemStack mainHandStack = mc.player.getMainHandStack();
            ItemStack offHandStack = mc.player.getOffHandStack();

            ItemStack throwableStack = null;

            if (isThrowable(mainHandStack)) {
                throwableStack = mainHandStack;
            } else if (isThrowable(offHandStack)) {
                throwableStack = offHandStack;
            }

            if (throwableStack == null) {
                return;
            }

            TrajectoryResult result = calculateTrajectoryWithHit();

            if (result.points.isEmpty()) {
                return;
            }

            MatrixStack matrices = event.getMatrixStack();
            matrices.push();

            Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
            matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.lineWidth(this.lineWidth.getValue());
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();

            if (this.showHitMarker.getValue() && result.hitNormal != null && !result.points.isEmpty()) {
                Vec3d hitPos = result.points.get(result.points.size() - 1);

                if (result.hitEntity != null) {
                    drawAnimatedCircle(matrices, hitPos, result.hitNormal, true);
                } else {
                    drawHitMarker(matrices, hitPos, result.hitNormal);
                }
            }

            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            matrices.pop();
        }

    
    private void drawTrajectoryLine(MatrixStack matrices, List<Vec3d> points, Vec3d cameraPos) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        BufferBuilder buffer = Tessellator.getInstance()
            .begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        // Получаем цвет из UI
        UI uiModule = MotherHack.getInstance().getModuleManager().getModule(UI.class);
        Color uiColor = uiModule != null ? uiModule.getTheme().getAccentColor() : Color.WHITE;
        
        for (int i = 0; i < points.size() - 1; i++) {
            Vec3d p1 = points.get(i);
            Vec3d p2 = points.get(i + 1);
            
            float progress1 = (float) i / points.size();
            float progress2 = (float) (i + 1) / points.size();
            
            // Градиент от полной яркости к более тёмному
            Color color1 = new Color(
                uiColor.getRed(),
                uiColor.getGreen(),
                uiColor.getBlue(),
                (int) (255 * (1.0f - progress1 * 0.5f))
            );
            Color color2 = new Color(
                uiColor.getRed(),
                uiColor.getGreen(),
                uiColor.getBlue(),
                (int) (255 * (1.0f - progress2 * 0.5f))
            );
            
            buffer.vertex(matrix,
                (float) (p1.x - cameraPos.x),
                (float) (p1.y - cameraPos.y),
                (float) (p1.z - cameraPos.z))
                .color(color1.getRGB());
            
            buffer.vertex(matrix,
                (float) (p2.x - cameraPos.x),
                (float) (p2.y - cameraPos.y),
                (float) (p2.z - cameraPos.z))
                .color(color2.getRGB());
        }
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
    
    private void drawHitMarker(MatrixStack matrices, Vec3d hitPos, Vec3d normal) {
        long time = System.currentTimeMillis();
        float animProgress = (time % 2000) / 2000.0f;
        
        float radius = 0.5f;
        int segments = 48;
        double angleStep = Math.PI * 2 / segments;
        
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(3.0f);
        
        BufferBuilder buffer = Tessellator.getInstance()
            .begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        // Вычисляем правильные tangent и bitangent для ориентации круга
        Vec3d tangent;
        Vec3d bitangent;
        
        // Нормализуем нормаль
        normal = normal.normalize();
        
        // Выбираем вектор, который не параллелен нормали
        Vec3d up = Math.abs(normal.y) < 0.9 ? new Vec3d(0, 1, 0) : new Vec3d(1, 0, 0);
        
        // Вычисляем tangent и bitangent через cross product
        tangent = normal.crossProduct(up).normalize();
        bitangent = normal.crossProduct(tangent).normalize();
        
        // Зелёно-белый градиент для блоков
        Color startColor = Color.WHITE;
        Color endColor = new Color(0, 255, 0);
        
        for (int i = 0; i < segments; i++) {
            double angle1 = i * angleStep;
            double angle2 = (i + 1) * angleStep;
            
            Vec3d p1 = tangent.multiply(Math.cos(angle1) * radius)
                .add(bitangent.multiply(Math.sin(angle1) * radius));
            Vec3d p2 = tangent.multiply(Math.cos(angle2) * radius)
                .add(bitangent.multiply(Math.sin(angle2) * radius));
            
            // Смещаем немного по нормали, чтобы круг был виден
            Vec3d pos1 = hitPos.add(p1).add(normal.multiply(0.02));
            Vec3d pos2 = hitPos.add(p2).add(normal.multiply(0.02));
            
            // Анимированный градиент
            float progress1 = ((float) i / segments + animProgress) % 1.0f;
            float progress2 = ((float) (i + 1) / segments + animProgress) % 1.0f;
            
            Color color1 = interpolateColor(startColor, endColor, progress1);
            Color color2 = interpolateColor(startColor, endColor, progress2);
            
            buffer.vertex(matrix,
                (float) pos1.x,
                (float) pos1.y,
                (float) pos1.z)
                .color(color1.getRGB());
            
            buffer.vertex(matrix,
                (float) pos2.x,
                (float) pos2.y,
                (float) pos2.z)
                .color(color2.getRGB());
        }
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
    
    private void drawAnimatedCircle(MatrixStack matrices, Vec3d hitPos, Vec3d normal, boolean isEntity) {
        long time = System.currentTimeMillis();
        float animProgress = (time % 2000) / 2000.0f;
        
        // Для сущности делаем круг больше
        float radius = isEntity ? 1.0f : 0.5f;
        int segments = 48;
        double angleStep = Math.PI * 2 / segments;
        
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(3.0f);
        
        BufferBuilder buffer = Tessellator.getInstance()
            .begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        // Для сущности рисуем горизонтальный круг вокруг неё
        if (isEntity) {
            // Горизонтальный круг на уровне центра сущности
            for (int i = 0; i < segments; i++) {
                double angle1 = i * angleStep;
                double angle2 = (i + 1) * angleStep;
                
                // Круг в плоскости XZ (горизонтальный)
                Vec3d p1 = new Vec3d(
                    Math.cos(angle1) * radius,
                    0,
                    Math.sin(angle1) * radius
                );
                Vec3d p2 = new Vec3d(
                    Math.cos(angle2) * radius,
                    0,
                    Math.sin(angle2) * radius
                );
                
                Vec3d pos1 = hitPos.add(p1);
                Vec3d pos2 = hitPos.add(p2);
                
                // Анимированный градиент
                float progress1 = ((float) i / segments + animProgress) % 1.0f;
                float progress2 = ((float) (i + 1) / segments + animProgress) % 1.0f;
                
                // Чёрно-белый градиент для сущностей
                Color color1 = interpolateColor(Color.WHITE, Color.BLACK, progress1);
                Color color2 = interpolateColor(Color.WHITE, Color.BLACK, progress2);
                
                buffer.vertex(matrix,
                    (float) pos1.x,
                    (float) pos1.y,
                    (float) pos1.z)
                    .color(color1.getRGB());
                
                buffer.vertex(matrix,
                    (float) pos2.x,
                    (float) pos2.y,
                    (float) pos2.z)
                    .color(color2.getRGB());
            }
        } else {
            // Для блоков - круг перпендикулярный поверхности
            Vec3d tangent;
            Vec3d bitangent;
            
            // Нормализуем нормаль
            normal = normal.normalize();
            
            // Выбираем вектор, который не параллелен нормали
            Vec3d up = Math.abs(normal.y) < 0.9 ? new Vec3d(0, 1, 0) : new Vec3d(1, 0, 0);
            
            // Вычисляем tangent и bitangent через cross product
            tangent = normal.crossProduct(up).normalize();
            bitangent = normal.crossProduct(tangent).normalize();
            
            for (int i = 0; i < segments; i++) {
                double angle1 = i * angleStep;
                double angle2 = (i + 1) * angleStep;
                
                Vec3d p1 = tangent.multiply(Math.cos(angle1) * radius)
                    .add(bitangent.multiply(Math.sin(angle1) * radius));
                Vec3d p2 = tangent.multiply(Math.cos(angle2) * radius)
                    .add(bitangent.multiply(Math.sin(angle2) * radius));
                
                // Смещаем немного по нормали, чтобы круг был виден
                Vec3d pos1 = hitPos.add(p1).add(normal.multiply(0.02));
                Vec3d pos2 = hitPos.add(p2).add(normal.multiply(0.02));
                
                // Анимированный градиент
                float progress1 = ((float) i / segments + animProgress) % 1.0f;
                float progress2 = ((float) (i + 1) / segments + animProgress) % 1.0f;
                
                // Зелёно-белый градиент для блоков
                Color color1 = interpolateColor(Color.WHITE, new Color(0, 255, 0), progress1);
                Color color2 = interpolateColor(Color.WHITE, new Color(0, 255, 0), progress2);
                
                buffer.vertex(matrix,
                    (float) pos1.x,
                    (float) pos1.y,
                    (float) pos1.z)
                    .color(color1.getRGB());
                
                buffer.vertex(matrix,
                    (float) pos2.x,
                    (float) pos2.y,
                    (float) pos2.z)
                    .color(color2.getRGB());
            }
        }
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
    
    private Color interpolateColor(Color c1, Color c2, float progress) {
        int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * progress);
        int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * progress);
        int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * progress);
        int a = (int) (c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * progress);
        
        return new Color(r, g, b, a);
    }
    
    @meteordevelopment.orbit.EventHandler
    public void onRender2D(EventRender2D event) {
        if (fullNullCheck()) {
            return;
        }
        
        if (!this.showParticles.getValue()) {
            return;
        }
        
        for (Entity entity : mc.world.getEntities()) {
            if (isProjectile(entity)) {
                spawnParticlesForProjectile(entity);
            }
        }
        
        UI uiModule = MotherHack.getInstance().getModuleManager().getModule(UI.class);
        Color accentColor = uiModule != null 
            ? uiModule.getTheme().getAccentColor() 
            : new Color(255, 0, 0);
        
        for (TrajectoryParticle particle : this.particles) {
            if (System.currentTimeMillis() - particle.time > 2000 || particle.alpha <= 0) {
                this.particles.remove(particle);
                continue;
            }
            
            particle.update();
            
            Vec3d screenPos = WorldUtils.getPosition(particle.pos);
            
            if (screenPos.z > 0 && screenPos.z < 1) {
                float fadeProgress = 1.0f - (System.currentTimeMillis() - particle.time) / 2000.0f;
                
                Color color = new Color(
                    accentColor.getRed(),
                    accentColor.getGreen(),
                    accentColor.getBlue(),
                    (int) (255 * particle.alpha)
                );
                
                Render2D.drawTexture(
                    event.getContext().getMatrices(),
                    (float) screenPos.getX(),
                    (float) screenPos.getY(),
                    this.particleSize.getValue() * fadeProgress,
                    this.particleSize.getValue() * fadeProgress,
                    0.0f,
                    particle.texture,
                    color
                );
            }
        }
    }
    
    private void spawnParticlesForProjectile(Entity entity) {
        if (this.particles.size() >= this.particleCount.getValue() * 10) {
            return;
        }
        
        Vec3d velocity = entity.getVelocity();
        if (velocity.length() < 0.001) {
            return;
        }
        
        if (Math.random() < 0.3) {
            this.particles.add(new TrajectoryParticle(entity.getPos()));
        }
    }
    
    private boolean isThrowable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        Item item = stack.getItem();
        
        return item instanceof BowItem || item instanceof CrossbowItem ||
               item instanceof SnowballItem || item instanceof EggItem ||
               item instanceof EnderPearlItem || item instanceof ExperienceBottleItem ||
               item instanceof PotionItem || item instanceof TridentItem;
    }
    
    private boolean isProjectile(Entity entity) {
        return entity instanceof ProjectileEntity;
    }
    
    private TrajectoryResult calculateTrajectoryWithHit() {
        List<Vec3d> points = new ArrayList<>();
        
        ItemStack stack = mc.player.getMainHandStack();
        if (stack.isEmpty() || !isThrowable(stack)) {
            stack = mc.player.getOffHandStack();
        }
        
        if (stack.isEmpty() || !isThrowable(stack)) {
            return new TrajectoryResult(points, new Vec3d(0, 1, 0), null);
        }
        
        // Начальная позиция - глаза игрока
        Vec3d pos = mc.player.getEyePos();
        
        // Направление взгляда игрока
        Vec3d lookVec = mc.player.getRotationVec(1.0f);
        
        // Получаем параметры снаряда
        float power = getProjectilePower(stack);
        float gravity = getGravity(stack);
        float drag = getDrag(stack);
        
        // Начальная скорость
        Vec3d velocity = lookVec.multiply(power);
        
        Vec3d hitNormal = new Vec3d(0, 1, 0);
        Entity hitEntity = null;
        
        // Симуляция полёта снаряда
        for (int i = 0; i < 300; i++) {
            points.add(pos);
            
            // Следующая позиция
            Vec3d nextPos = pos.add(velocity);
            
            // Проверка столкновения с сущностями
            Entity entity = checkEntityCollision(pos, nextPos);
            if (entity != null) {
                // Используем центр сущности для окружности
                Vec3d entityCenter = entity.getPos().add(0, entity.getHeight() / 2.0, 0);
                points.add(entityCenter);
                hitEntity = entity;
                
                // Для сущности нормаль не важна, круг будет горизонтальным
                hitNormal = new Vec3d(0, 1, 0);
                
                break;
            }
            
            // Проверка столкновения с блоками
            var hitResult = mc.world.raycast(new RaycastContext(
                pos, nextPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
            ));
            
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                points.add(hitResult.getPos());
                hitNormal = Vec3d.of(hitResult.getSide().getVector());
                break;
            }
            
            // Обновляем позицию и скорость
            pos = nextPos;
            
            // Применяем гравитацию
            velocity = velocity.add(0, -gravity, 0);
            
            // Применяем сопротивление воздуха
            velocity = velocity.multiply(drag);
            
            // Проверка выхода за границы мира
            if (pos.y < mc.world.getBottomY() - 10 || pos.y > 320) {
                break;
            }
            
            // Если скорость слишком мала, останавливаем
            if (velocity.lengthSquared() < 0.0001) {
                break;
            }
        }
        
        return new TrajectoryResult(points, hitNormal, hitEntity);
    }
    
    private Entity checkEntityCollision(Vec3d start, Vec3d end) {
        Entity closestEntity = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) {
                continue;
            }
            
            if (!entity.isAttackable()) {
                continue;
            }
            
            if (entity.isSpectator()) {
                continue;
            }
            
            Box box = entity.getBoundingBox().expand(0.3);
            Optional<Vec3d> hit = box.raycast(start, end);
            
            if (hit.isPresent()) {
                double distance = start.distanceTo(hit.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEntity = entity;
                }
            }
        }
        
        return closestEntity;
    }
    
    private float getProjectilePower(ItemStack stack) {
        Item item = stack.getItem();
        
        if (item instanceof BowItem) {
            // Лук: скорость зависит от натяжения, максимум 3.0
            int useTicks = mc.player.getItemUseTime();
            float charge = BowItem.getPullProgress(useTicks);
            return charge * 3.0f;
        } else if (item instanceof CrossbowItem) {
            // Арбалет: фиксированная скорость 3.15
            return 3.15f;
        } else if (item instanceof TridentItem) {
            // Трезубец: 2.5
            return 2.5f;
        } else if (item instanceof SnowballItem || item instanceof EggItem) {
            // Снежки и яйца: 1.5
            return 1.5f;
        } else if (item instanceof EnderPearlItem) {
            // Жемчуг края: 1.5
            return 1.5f;
        } else if (item instanceof ExperienceBottleItem) {
            // Бутылка опыта: 0.7
            return 0.7f;
        } else if (item instanceof PotionItem) {
            // Зелье: 0.5
            return 0.5f;
        }
        
        return 1.5f;
    }
    
    private float getGravity(ItemStack stack) {
        Item item = stack.getItem();
        
        if (item instanceof BowItem || item instanceof CrossbowItem) {
            // Стрелы: 0.05
            return 0.05f;
        } else if (item instanceof TridentItem) {
            // Трезубец: 0.05
            return 0.05f;
        } else if (item instanceof PotionItem) {
            // Зелье: 0.05
            return 0.05f;
        } else if (item instanceof SnowballItem || item instanceof EggItem) {
            // Снежки и яйца: 0.03
            return 0.03f;
        } else if (item instanceof EnderPearlItem) {
            // Жемчуг края: 0.03
            return 0.03f;
        } else if (item instanceof ExperienceBottleItem) {
            // Бутылка опыта: 0.07
            return 0.07f;
        }
        
        return 0.03f;
    }
    
    private float getDrag(ItemStack stack) {
        Item item = stack.getItem();
        
        // Все снаряды имеют одинаковое сопротивление воздуха
        if (item instanceof BowItem || item instanceof CrossbowItem ||
            item instanceof TridentItem || item instanceof PotionItem ||
            item instanceof SnowballItem || item instanceof EggItem ||
            item instanceof EnderPearlItem || item instanceof ExperienceBottleItem) {
            return 0.99f;
        }
        
        return 0.99f;
    }
    
    static class TrajectoryResult {
        final List<Vec3d> points;
        final Vec3d hitNormal;
        final Entity hitEntity;
        
        TrajectoryResult(List<Vec3d> points, Vec3d hitNormal, Entity hitEntity) {
            this.points = points;
            this.hitNormal = hitNormal;
            this.hitEntity = hitEntity;
        }
    }
    
    class TrajectoryParticle {
        Vec3d pos;
        Vec3d velocity;
        long time;
        float alpha;
        Identifier texture;
        
        TrajectoryParticle(Vec3d pos) {
            this.pos = pos;
            this.velocity = new Vec3d(
                (Math.random() - 0.5) * 0.02,
                (Math.random() - 0.5) * 0.02,
                (Math.random() - 0.5) * 0.02
            );
            this.time = System.currentTimeMillis();
            this.alpha = 1.0f;
            this.texture = getParticleTexture();
        }
        
        void update() {
            this.pos = this.pos.add(this.velocity);
            this.velocity = this.velocity.multiply(0.98);
            
            long elapsed = System.currentTimeMillis() - this.time;
            this.alpha = 1.0f - (elapsed / 2000.0f);
        }
        
        private Identifier getParticleTexture() {
            ParticleTextureType textureType = Trajectories.this.particleTexture.getValue();
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
