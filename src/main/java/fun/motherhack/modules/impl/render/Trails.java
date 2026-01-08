package fun.motherhack.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender3D;
import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.ColorSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.utils.render.ColorUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Trails extends Module {

    @AllArgsConstructor @Getter
    public enum ColorMode implements Nameable {
        Rainbow("Rainbow"),
        UIColor("UIColor");

        private final String name;
    }

    private final BooleanSetting showFriends = new BooleanSetting("settings.trails.showfriends", true);
    private final BooleanSetting showSelf = new BooleanSetting("settings.trails.showself", true);
    private final BooleanSetting showPlayers = new BooleanSetting("settings.trails.showplayers", true);
    private final EnumSetting<ColorMode> colorMode = new EnumSetting<>("Color Mode", ColorMode.UIColor);
    private final ColorSetting uiColor = new ColorSetting(new Color(0x64007CFF, true));

    private final long trailLifetimeMs = 250L;
    private final double minDistance = 0.01;

    private final Map<PlayerEntity, List<Trail>> trailsMap = new HashMap<>();

    public Trails() {
        super("Trails", Category.Render);
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        // Очищаем все следы при отключении модуля
        trailsMap.clear();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (!toggled) return; // Не обновляем следы если модуль выключен
        if (fullNullCheck()) return;
        long now = System.currentTimeMillis();
        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (!shouldRenderTrails(entity)) continue;
            List<Trail> trails = trailsMap.computeIfAbsent(entity, k -> new ArrayList<>());
            trails.removeIf(t -> t.isExpired(now));
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (!toggled) return; // Не рендерим следы если модуль выключен
        if (fullNullCheck()) return;
        float tickDelta = event.getTickCounter().getTickDelta(true);
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();
        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (!shouldRenderTrails(entity)) continue;
            Vec3d interp = interpolateEntityPosition(entity, tickDelta);
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
}