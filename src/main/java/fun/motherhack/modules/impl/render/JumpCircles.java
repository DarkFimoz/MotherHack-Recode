package fun.motherhack.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.motherhack.api.events.impl.EventRender3D;
import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.api.render.providers.ResourceProvider;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.math.TimerUtils;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.MotherHack;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class JumpCircles extends Module {

    private static final ShaderProgramKey TEXTURE_SHADER_KEY = new ShaderProgramKey(ResourceProvider.getShaderIdentifier("texture"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

    private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.Default);
    private final BooleanSetting easeOut = new BooleanSetting("EaseOut", true);
    private final NumberSetting rotateSpeed = new NumberSetting("RotateSpeed", 2f, 0.5f, 5f, 0.1f);
    private final NumberSetting circleScale = new NumberSetting("CircleScale", 1f, 0.5f, 5f, 0.1f);
    private final BooleanSetting onlySelf = new BooleanSetting("OnlySelf", false);

    private final List<Circle> circles = new CopyOnWriteArrayList<>();
    private final Map<PlayerEntity, Boolean> previousOnGround = new HashMap<>();

    public JumpCircles() {
        super("JumpCircles", Category.Render);
        getSettings().add(mode);
        getSettings().add(easeOut);
        getSettings().add(rotateSpeed);
        getSettings().add(circleScale);
        getSettings().add(onlySelf);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        // Очищаем все круги и состояния при отключении модуля
        circles.clear();
        previousOnGround.clear();
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (!toggled) return; // Не обновляем круги если модуль выключен
        if (fullNullCheck()) return;

        for (PlayerEntity pl : mc.world.getPlayers()) {
            if (mc.player == pl || !onlySelf.getValue()) {
                Boolean wasOnGround = previousOnGround.get(pl);
                boolean isOnGround = pl.isOnGround();

                if (wasOnGround != null && wasOnGround && !isOnGround && pl.getVelocity().y > 0) {
                    // Player just jumped (not falling)
                    circles.add(new Circle(new Vec3d(pl.getX(), (int) Math.floor(pl.getY()) + 0.001f, pl.getZ()), new TimerUtils()));
                }

                previousOnGround.put(pl, isOnGround);
            }
        }

        // Optimize circle removal - only check every 10 ticks to reduce overhead
        if (mc.world.getTime() % 10 == 0) {
            circles.removeIf(c -> c.timer.getElapsed() >= (easeOut.getValue() ? 5000L : 6000L));
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (!toggled) return; // Не рендерим круги если модуль выключен
        if (fullNullCheck() || circles.isEmpty()) return;

        // Skip rendering if there are too many circles to prevent lag
        if (circles.size() > 100) {
            circles.clear();
            return;
        }

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        Identifier texture = switch (mode.getValue()) {
            case Portal -> Identifier.of("motherhack", "particles/dollar.png");
            case Default -> Identifier.of("motherhack", "particles/circle.png");
            case Custom -> Identifier.of("motherhack", "particles/circle.png");
        };

        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(TEXTURE_SHADER_KEY);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        // Pre-calculate values to reduce per-circle overhead
        float currentTime = mc.world.getTime();
        MatrixStack stack = e.getMatrixStack();

        for (Circle c : circles) {
            float elapsed = c.timer.getElapsed();
            float colorAnim = elapsed / 6000f;
            float sizeAnim = Math.max(0, circleScale.getValue() - (float) Math.pow(1 - ((elapsed * (easeOut.getValue() ? 2f : 1f)) / 5000f), 4));

            stack.push();
            stack.translate(c.pos.x - mc.getEntityRenderDispatcher().camera.getPos().x,
                           c.pos.y - mc.getEntityRenderDispatcher().camera.getPos().y,
                           c.pos.z - mc.getEntityRenderDispatcher().camera.getPos().z);
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(elapsed * rotateSpeed.getValue()));
            float scale = sizeAnim * 2f;

            // Optimized color calculation with UI color
            int alpha = (int) (255 * (1f - colorAnim));
            Color uiColor = MotherHack.getInstance().getModuleManager().getModule(UI.class).getTheme().getAccentColor();
            int color = (alpha << 24) | (uiColor.getRed() << 16) | (uiColor.getGreen() << 8) | uiColor.getBlue();

            // Optimized vertex rendering
            MatrixStack.Entry matrix = stack.peek();
            buffer.vertex(matrix.getPositionMatrix(), -sizeAnim, -sizeAnim + scale, 0).texture(0, 1).color(color);
            buffer.vertex(matrix.getPositionMatrix(), -sizeAnim + scale, -sizeAnim + scale, 0).texture(1, 1).color(color);
            buffer.vertex(matrix.getPositionMatrix(), -sizeAnim + scale, -sizeAnim, 0).texture(1, 0).color(color);
            buffer.vertex(matrix.getPositionMatrix(), -sizeAnim, -sizeAnim, 0).texture(0, 0).color(color);

            stack.pop();
        }

        try {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } catch (Exception ex) {
            // Only print stack trace if module is toggled to reduce console spam
            if (toggled) {
                ex.printStackTrace();
            }
        }

        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();
        Collections.reverse(circles);
    }

    public enum Mode implements Nameable {
        Default("Default"), Portal("Portal"), Custom("Custom");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public record Circle(Vec3d pos, TimerUtils timer) {
    }
}