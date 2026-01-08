package fun.motherhack.screen.panelgui.components.settings;

import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * NumberComponent - компонент для NumberSetting
 */
public class NumberComponent extends SettingComponent<NumberSetting> {

    private boolean dragging = false;
    private float targetValue;
    private final float ANIMATION_SPEED = 0.8f;
    private final float SLIDER_HEIGHT = 4f;
    private final float CIRCLE_RADIUS = 4f;

    public NumberComponent(NumberSetting setting) {
        super(setting);
        this.height = 20f;
        this.targetValue = setting.getValue();
    }

    @Override
    public void draw(DrawContext context, float mouseX, float mouseY, float partialTicks, float parentAlpha) {
        float currentValue = setting.getValue();
        float interpolatedValue = MathHelper.lerp(ANIMATION_SPEED * partialTicks, currentValue, targetValue);
        setting.setValue(interpolatedValue);
        float currentValueForRender = setting.getValue();

        int sliderBgAlpha = (int) (parentAlpha * 45);
        int accentAlpha = (int) (parentAlpha * 255);
        int textAlpha = (int) (parentAlpha * 255);
        int valueAlpha = (int) (parentAlpha * 170);

        float sliderX = x + 10;
        float sliderWidth = width - 20;
        float sliderY = y + 10;
        final float textHeight = 7f;

        String valueStr = new BigDecimal(currentValueForRender).setScale(2, RoundingMode.HALF_UP).toString();

        float valueWidth = Fonts.REGULAR.getWidth(valueStr, textHeight);
        float valueX = x + width - valueWidth - 10;
        float textY = y - 1f;

        Color nameColor = new Color(255, 255, 255, textAlpha);
        Color valueColor = new Color(180, 180, 180, textAlpha);
        Color filledColor1 = new Color(90, 36, 60, valueAlpha);

        final float padding = 2f;

        Render2D.drawRoundedRect(context.getMatrices(), valueX - padding, textY - padding + 1, valueWidth + 2 * padding, textHeight + 3, 1f, filledColor1);

        Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(textHeight), setting.getName(), x + 10f, textY, nameColor);
        Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(textHeight), valueStr, valueX, textY, valueColor);

        Color bgColor = new Color(255, 255, 255, sliderBgAlpha);
        Render2D.drawBorder(context.getMatrices(), sliderX, sliderY - 0.5f, sliderWidth, SLIDER_HEIGHT + 1, 1f, 0.1f, 0.1f, bgColor);

        float range = setting.getMax() - setting.getMin();
        float percentage = (currentValueForRender - setting.getMin()) / range;
        float filledWidth = sliderWidth * percentage;
        Color filledColor = new Color(90, 36, 60, accentAlpha);
        Render2D.drawRoundedRect(context.getMatrices(), sliderX, sliderY, filledWidth, SLIDER_HEIGHT, 1f, filledColor);

        float circleCenterX = sliderX + filledWidth - 4;
        float circleCenterY = sliderY + SLIDER_HEIGHT / 2f - 4;

        Color circleColor = new Color(255, 255, 255, (int)(parentAlpha * 255));
        Render2D.drawRoundedRect(context.getMatrices(), circleCenterX, circleCenterY, 8, 8, 3, circleColor);
    }

    @Override
    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        float hitAreaX = x + 10 - CIRCLE_RADIUS;
        float hitAreaWidth = width - 20 + 2 * CIRCLE_RADIUS;
        float hitAreaY = y;
        float hitAreaHeight = height;

        if (button == 0 && isHovered(mouseX, mouseY, hitAreaX, hitAreaY, hitAreaWidth, hitAreaHeight)) {
            dragging = true;
            updateValue(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public void mouseDragged(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            updateValue((float) mouseX);
        }
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
    }

    private void updateValue(float mouseX) {
        float sliderX = x + 10;
        float sliderWidth = width - 20;
        float min = setting.getMin();
        float max = setting.getMax();
        float percentage = MathHelper.clamp((mouseX - sliderX) / sliderWidth, 0f, 1f);
        float calculatedValue = min + (max - min) * percentage;
        float increment = setting.getIncrement();

        calculatedValue = Math.round(calculatedValue / increment) * increment;
        calculatedValue = MathHelper.clamp(calculatedValue, min, max);

        targetValue = calculatedValue;
    }
}
