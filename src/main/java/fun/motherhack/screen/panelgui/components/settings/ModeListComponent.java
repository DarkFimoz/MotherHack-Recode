package fun.motherhack.screen.panelgui.components.settings;

import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.ListSetting;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.List;

/**
 * ModeListComponent - компонент для ListSetting
 */
public class ModeListComponent extends SettingComponent<ListSetting> {

    private static final float PADDING_X = 10f;
    private static final float PADDING_Y = 1f;
    private static final float TEXT_HEIGHT = 10f;
    private static final float MODE_SPACING_X = PADDING_X * 0.8f;
    private static final float FONT_SIZE = 7f;
    private static final float MODE_FONT_SIZE = 6.5f;
    private static final float BACKGROUND_PADDING = 4f;

    public ModeListComponent(ListSetting setting) {
        super(setting);
        this.height = 15f + (setting.getValue().size() * 12f);
    }

    private List<BooleanSetting> getModes() {
        return setting.getValue();
    }

    private int getSelectedCount() {
        return (int) setting.getValue().stream().filter(BooleanSetting::getValue).count();
    }

    @Override
    public float getHeight() {
        int modeLines = calculateModeLines();
        float totalHeight = PADDING_Y + TEXT_HEIGHT + PADDING_Y;
        totalHeight += modeLines * TEXT_HEIGHT;
        totalHeight += (modeLines > 0 ? (modeLines - 1) * PADDING_Y : 0);
        totalHeight += PADDING_Y;
        return totalHeight;
    }

    private int calculateModeLines() {
        if (width <= 0) return 1;

        int lines = 1;
        float currentX = PADDING_X;
        List<BooleanSetting> modes = getModes();

        for (BooleanSetting mode : modes) {
            float modeWidth = Fonts.REGULAR.getWidth(mode.getName(), MODE_FONT_SIZE);
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
    public void draw(DrawContext context, float mouseX, float mouseY, float partialTicks, float parentAlpha) {
        int headerTextAlpha = (int) (parentAlpha * 255);
        int modeTextAlpha = (int) (parentAlpha * 180);

        String headerText = setting.getName();
        Color headerColor = new Color(255, 255, 255, headerTextAlpha);
        Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(FONT_SIZE), headerText, x + PADDING_X, y + PADDING_Y, headerColor);
        Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(FONT_SIZE), getSelectedCount() + " из " + getModes().size(), x + PADDING_X + 76, y + PADDING_Y, headerColor);

        List<BooleanSetting> modes = getModes();
        float currentX = x + PADDING_X;
        float currentY = y + TEXT_HEIGHT + PADDING_Y * 2;

        for (BooleanSetting mode : modes) {
            float modeWidth = Fonts.REGULAR.getWidth(mode.getName(), MODE_FONT_SIZE);
            float itemWidth = modeWidth + MODE_SPACING_X;

            if (currentX + itemWidth > x + width - PADDING_X) {
                currentX = x + PADDING_X;
                currentY += TEXT_HEIGHT + PADDING_Y;
            }

            Color textColor = mode.getValue()
                    ? new Color(255, 255, 255, modeTextAlpha)
                    : new Color(180, 180, 180, modeTextAlpha);

            if (mode.getValue()) {
                Color backgroundColor = new Color(90, 36, 60, (int) (parentAlpha * 255));
                Render2D.drawRoundedRect(context.getMatrices(), currentX - BACKGROUND_PADDING / 2, currentY - PADDING_Y + 2, modeWidth + BACKGROUND_PADDING + 2, TEXT_HEIGHT, 2, backgroundColor);
            }

            Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(MODE_FONT_SIZE), mode.getName(), currentX, currentY + 2, textColor);
            currentX += itemWidth;
        }
    }

    @Override
    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (isHovered(mouseX, mouseY, x, y, width, getHeight())) {
            float modesYStart = y + TEXT_HEIGHT + PADDING_Y;

            if (mouseY >= modesYStart) {
                List<BooleanSetting> modes = getModes();
                float currentX = x + PADDING_X;
                float currentY = modesYStart + PADDING_Y;

                for (BooleanSetting mode : modes) {
                    float modeWidth = Fonts.REGULAR.getWidth(mode.getName(), MODE_FONT_SIZE);
                    float itemWidth = modeWidth + MODE_SPACING_X;

                    if (currentX + itemWidth > x + width - PADDING_X) {
                        currentX = x + PADDING_X;
                        currentY += TEXT_HEIGHT + PADDING_Y;
                    }

                    if (isHovered(mouseX, mouseY, currentX, currentY - PADDING_Y + 2, modeWidth + BACKGROUND_PADDING, TEXT_HEIGHT)) {
                        mode.setValue(!mode.getValue());
                        return true;
                    }

                    currentX += itemWidth;
                }
            }
        }
        return false;
    }
}
