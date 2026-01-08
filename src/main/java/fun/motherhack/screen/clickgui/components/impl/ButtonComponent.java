package fun.motherhack.screen.clickgui.components.impl;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.settings.impl.ButtonSetting;
import fun.motherhack.screen.clickgui.components.Component;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.render.fonts.Fonts;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.modules.impl.client.UI;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;

import java.awt.*;

public class ButtonComponent extends Component {

    private final ButtonSetting setting;
    private final Animation hoverAnimation = new Animation(200, 1f, true, Easing.BOTH_SINE);

    public ButtonComponent(ButtonSetting setting) {
        super(setting.getName());
        this.setting = setting;
        this.visible = setting::isVisible;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        UI.ClickGuiTheme theme = MotherHack.getInstance().getClickGui().getTheme();
        boolean hovered = MathUtils.isHovered(x, y, width, height, mouseX, mouseY);
        hoverAnimation.update(hovered);

        Color bgColor = new Color(theme.getAccentColor().getRed(), theme.getAccentColor().getGreen(), theme.getAccentColor().getBlue(), (int) (50 + 50 * hoverAnimation.getValue()));
        Render2D.drawRoundedRect(context.getMatrices(), x + 2f, y + 2f, width - 4f, height - 4f, 3f, bgColor);

        Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(7.5f), I18n.translate(setting.getName()), x + 4f, y + 3f, theme.getTextColor());
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (MathUtils.isHovered(x, y, width, height, (float) mouseX, (float) mouseY) && button == 0) {
            setting.run();
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {

    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {

    }

    @Override
    public void keyReleased(int keyCode, int scanCode, int modifiers) {

    }

    @Override
    public void charTyped(char chr, int modifiers) {

    }
}