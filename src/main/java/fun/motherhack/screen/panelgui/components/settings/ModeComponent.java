package fun.motherhack.screen.panelgui.components.settings;

import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * ModeComponent - компонент для EnumSetting
 */
public class ModeComponent extends SettingComponent<EnumSetting<?>> {

    private static final float TEXT_HEIGHT = 10f;
    private static final float PADDING_X = 10f;
    private static final float PADDING_Y = 3f;
    private static final float MODE_SPACING_X = PADDING_X * 0.8f;
    private static final float FONT_SIZE = 7f;
    private static final float MODE_FONT_SIZE = 6.5f;
    private static final float BACKGROUND_PADDING = 4f;

    public ModeComponent(EnumSetting<?> setting) {
        super(setting);
    }

    private List<String> getModes() {
        return Arrays.stream((Enum<?>[]) setting.getValue().getClass().getEnumConstants())
                .map(Enum::name)
                .toList();
    }

    private String getCurrentMode() {
        return setting.getValue().name();
    }

    private int calculateModeLines() {
        if (width <= 0) return 1;

        int lines = 1;
        float currentX = PADDING_X;
        List<String> modes = getModes();

        for (String mode : modes) {
            float modeWidth = Fonts.REGULAR.getWidth(mode, MODE_FONT_SIZE);
            float itemWidth = modeWidth + MODE_SPACING_X;

            if (currentX + itemWidth > width - PADDING_X) {
                lines++;
                currentX = PADDING_X + itemWidth;
            } else {
                currentX += itemWidth;
            }
        }
        return lines;
    }

    @Override
    public float getHeight() {
        int modeLines = calculateModeLines();
        float totalHeight = PADDING_Y;
        totalHeight += TEXT_HEIGHT;
        totalHeight += PADDING_Y;
        totalHeight += modeLines * TEXT_HEIGHT;
        totalHeight += (modeLines > 0 ? (modeLines - 1) * PADDING_Y : 0);
        totalHeight += PADDING_Y;

        return totalHeight;
    }

    @Override
    public void draw(DrawContext context, float mouseX, float mouseY, float partialTicks, float parentAlpha) {
        int headerTextAlpha = (int) (parentAlpha * 255);
        int modeTextAlpha = (int) (parentAlpha * 180);
        int selectedBgAlpha = (int) (parentAlpha * 255);
        Color headerColor = new Color(255, 255, 255, headerTextAlpha);

        Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(FONT_SIZE), setting.getName(), x + PADDING_X, y + PADDING_Y, headerColor);

        List<String> modes = getModes();
        String currentMode = getCurrentMode();

        float currentX = x + PADDING_X;
        float currentY = y + TEXT_HEIGHT + PADDING_Y * 2;

        for (String mode : modes) {
            float modeWidth = Fonts.REGULAR.getWidth(mode, MODE_FONT_SIZE);
            float itemWidth = modeWidth + MODE_SPACING_X;

            if (currentX + itemWidth > x + width - PADDING_X) {
                currentX = x + PADDING_X;
                currentY += TEXT_HEIGHT + PADDING_Y;
            }

            Color textColor = new Color(180, 180, 180, modeTextAlpha);

            if (mode.equals(currentMode)) {
                textColor = new Color(255, 255, 255, modeTextAlpha);
                Color backgroundColor = new Color(90, 36, 60, selectedBgAlpha);

                float rectX = currentX - BACKGROUND_PADDING / 2;
                float rectY = currentY - PADDING_Y + 2;
                float rectW = modeWidth + BACKGROUND_PADDING;
                float rectH = TEXT_HEIGHT;

                Render2D.drawRoundedRect(context.getMatrices(), rectX, rectY, rectW, rectH, 2, backgroundColor);
            }

            Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(MODE_FONT_SIZE), mode, currentX, currentY, textColor);
            currentX += itemWidth;
        }
    }

    @Override
    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (isHovered(mouseX, mouseY, x, y, width, getHeight())) {
            float modesYStart = y + TEXT_HEIGHT + PADDING_Y;

            if (mouseY < modesYStart) {
                if (button == 0) {
                    setting.increaseEnum();
                    return true;
                }
            } else {
                List<String> modes = getModes();
                float currentX = x + PADDING_X;
                float currentY = modesYStart + PADDING_Y;

                for (String mode : modes) {
                    float modeWidth = Fonts.REGULAR.getWidth(mode, MODE_FONT_SIZE);
                    float itemWidth = modeWidth + MODE_SPACING_X;

                    if (currentX + itemWidth > x + width - PADDING_X) {
                        currentX = x + PADDING_X;
                        currentY += TEXT_HEIGHT + PADDING_Y;
                    }

                    if (isHovered(mouseX, mouseY, currentX, currentY - PADDING_Y + 2, modeWidth + BACKGROUND_PADDING, TEXT_HEIGHT)) {
                        setting.setEnumValue(mode);
                        return true;
                    }

                    currentX += itemWidth;
                }
            }
        }
        return false;
    }
}
