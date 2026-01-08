package fun.motherhack.screen.panelgui.components.settings;

import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

/**
 * BooleanComponent - компонент для BooleanSetting
 */
public class BooleanComponent extends SettingComponent<BooleanSetting> {

    public BooleanComponent(BooleanSetting setting) {
        super(setting);
    }

    @Override
    public void draw(DrawContext context, float mouseX, float mouseY, float partialTicks, float parentAlpha) {
        float checkboxSize = 8f;
        float checkboxX = x + width - 20f;
        float checkboxY = y + (height - checkboxSize) / 2f;

        int textAlpha = (int) (parentAlpha * 255);
        int borderAlpha = (int) (parentAlpha * 45);
        Color textColor = new Color(255, 255, 255, textAlpha);
        Color borderColor = new Color(255, 255, 255, borderAlpha);
        Color iconOnColor = new Color(0, 255, 0, textAlpha);
        Color iconOffColor = new Color(255, 0, 0, textAlpha);

        String name = setting.getName();
        Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(7f), name, x + 10f, y + 3f, textColor);

        // Checkbox border
        Render2D.drawBorder(context.getMatrices(), checkboxX, checkboxY - 1, checkboxSize + 2, checkboxSize + 2, 3f, 0.1f, 0.1f, borderColor);

        // Checkbox state
        if (setting.getValue()) {
            Render2D.drawFont(context.getMatrices(), Fonts.ICONS.getFont(6f), "E", checkboxX + 1.5f, checkboxY + 0.3f, iconOnColor);
        } else {
            Render2D.drawFont(context.getMatrices(), Fonts.ICONS.getFont(5f), "B", checkboxX + 2.1f, checkboxY + 1f, iconOffColor);
        }
    }

    @Override
    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (isHovered(mouseX, mouseY, x, y, width, height) && button == 0) {
            setting.setValue(!setting.getValue());
            return true;
        }
        return false;
    }
}
