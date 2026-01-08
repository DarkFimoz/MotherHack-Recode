package fun.motherhack.modules.impl.render;

import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.render.Render2D;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class CrossHair extends Module {

    private final EnumSetting<Mode> modeSetting = new EnumSetting<>("settings.crosshair.mode", Mode.Dot);
    private final NumberSetting attackSetting = new NumberSetting("settings.crosshair.attack", 6f, 0f, 20f, 1f);
    private final NumberSetting indentSetting = new NumberSetting("settings.crosshair.indent", 2f, 0f, 5f, 1f);
    private final NumberSetting size1Setting = new NumberSetting("settings.crosshair.size1", 6f, 2f, 10f, 1f);
    private final NumberSetting size2Setting = new NumberSetting("settings.crosshair.size2", 2f, 2f, 4f, 1f);

    public CrossHair() {
        super("CrossHair", Category.Render);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    private float red = 0;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRender2D(EventRender2D event) {
        if (fullNullCheck()) return;
        
        // Рисуем кастомный прицел
        if (modeSetting.getValue() == Mode.Dot) {
            renderDot(event);
        } else {
            renderCross(event);
        }
    }

    private void renderCross(EventRender2D event) {
        red = (float) MathUtils.interpolate(red, mc.crosshairTarget instanceof EntityHitResult ? 5 : 1, 0.1);
        int firstColor = multRed(Color.WHITE.getRGB(), red);
        int secondColor = Color.BLACK.getRGB();
        float x = mc.getWindow().getScaledWidth() / 2F;
        float y = mc.getWindow().getScaledHeight() / 2F;
        float cooldown = attackSetting.getValue() - (attackSetting.getValue() * mc.player.getAttackCooldownProgress(mc.getRenderTickCounter().getTickDelta(true)));
        float size = size1Setting.getValue();
        float size2 = size2Setting.getValue();
        float offset = size2 / 2;
        float indent = indentSetting.getValue() + cooldown;

        renderMain(event, x, y, size, size2, 1, indent, offset, secondColor);
        renderMain(event, x, y, size, size2, 0, indent, offset, firstColor);
    }

    private void renderDot(EventRender2D event) {
        if (mc.options.getPerspective() != Perspective.FIRST_PERSON) return;
        
        float x = mc.getWindow().getScaledWidth() / 2F;
        float y = mc.getWindow().getScaledHeight() / 2F;
        float size = size2Setting.getValue();
        Color color = mc.crosshairTarget instanceof EntityHitResult ? Color.RED : Color.WHITE;
        Render2D.drawRoundedRect(event.getContext().getMatrices(), x - size / 2, y - size / 2, size, size, size / 2, color);
    }

    private void renderMain(EventRender2D event, float x, float y, float size, float size2, float padding, float indent, float offset, int color) {
        if (mc.options.getPerspective() != Perspective.FIRST_PERSON) return;
        
        // Верхняя линия
        Render2D.drawRoundedRect(event.getContext().getMatrices(), 
            x - offset - padding/2, 
            y - size - indent - padding/2, 
            size2 + padding, 
            size + padding, 
            0, 
            new Color(color, true));
            
        // Нижняя линия
        Render2D.drawRoundedRect(event.getContext().getMatrices(), 
            x - offset - padding/2, 
            y + indent - padding/2, 
            size2 + padding, 
            size + padding, 
            0, 
            new Color(color, true));
            
        // Левая линия
        Render2D.drawRoundedRect(event.getContext().getMatrices(), 
            x - size - indent - padding/2, 
            y - offset - padding/2, 
            size + padding, 
            size2 + padding, 
            0, 
            new Color(color, true));
            
        // Правая линия
        Render2D.drawRoundedRect(event.getContext().getMatrices(), 
            x + indent - padding/2, 
            y - offset - padding/2, 
            size + padding, 
            size2 + padding, 
            0, 
            new Color(color, true));
    }

    private int multRed(int color, float factor) {
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        return (a << 24) | (Math.min(255, r) << 16) | (g << 8) | b;
    }

    public enum Mode implements Nameable {
        Cross("Cross"), Dot("Dot");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}