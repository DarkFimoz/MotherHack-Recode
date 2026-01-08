package fun.motherhack.screen.clickgui.components.impl;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.screen.clickgui.components.Component;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.animations.infinity.InfinityAnimation;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.render.ColorUtils;
import fun.motherhack.utils.render.fonts.Fonts;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.modules.impl.client.UI;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class SliderComponent extends Component {

    private final NumberSetting setting;
    private final InfinityAnimation animation = new InfinityAnimation(Easing.LINEAR);
    private boolean drag;

    public SliderComponent(NumberSetting setting) {
        super(setting.getName());
        this.setting = setting;
        this.addHeight = () -> 3f;
        this.visible = setting::isVisible;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        UI.ClickGuiTheme theme = MotherHack.getInstance().getClickGui().getTheme();
        if (drag) {
            float value = MathHelper.clamp(
                    MathUtils.round((mouseX - x - 5f) / (width - 12f) * (setting.getMax() - setting.getMin()) + setting.getMin(), setting.getIncrement()),
                    setting.getMin(),
                    setting.getMax()
            );
            setting.setValue(value);
        }

        Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(7.5f), I18n.translate(setting.getName()), x + 4f, y + 3f, theme.getTextColor());
        Render2D.drawRoundedRect(context.getMatrices(), x + 4f, y + 13f,
                width - 8f, 4f, 0.5f, new Color(theme.getBackgroundColor().getRed(), theme.getBackgroundColor().getGreen(), theme.getBackgroundColor().getBlue(), 100));
        Render2D.drawRoundedRect(context.getMatrices(), x + 4f, y + 13f,
                animation.animate((width - 8f) * ((setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin())), 100),
                4f, 0.5f, ColorUtils.getGlobalColor());
        Render2D.drawRoundedRect(context.getMatrices(),
                x + 1f + animation.animate((width - 8f) * ((setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin())), 100),
                y + 12f, 6f, 6f, 3f, theme.getTextColor());
        Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(6f), setting.getValue() + "",
                x + width - Fonts.REGULAR.getWidth(setting.getValue() + "", 6.5f) - 4.5f, y + 5f, theme.getTextColor());
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (MathUtils.isHovered(x + 4f, y + 12f, width - 8f, 6f, (float) mouseX, (float) mouseY) && button == 0) drag = !drag;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) drag = false;
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