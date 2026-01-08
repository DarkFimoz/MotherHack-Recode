package fun.motherhack.screen.clickgui.components.impl;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.screen.clickgui.components.Component;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.render.ColorUtils;
import fun.motherhack.utils.render.fonts.Fonts;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.sound.GuiSoundHelper;
import fun.motherhack.modules.impl.client.UI;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;

import java.awt.*;

public class BooleanComponent extends Component {

    private final BooleanSetting setting;
    private final Animation toggleAnimation = new Animation(300, 1f, true, Easing.MotherHack);

    public BooleanComponent(BooleanSetting setting) {
        super(setting.getName());
        this.setting = setting;
        this.visible = setting::isVisible;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        UI.ClickGuiTheme theme = MotherHack.getInstance().getClickGui().getTheme();
        toggleAnimation.update(setting.getValue());
        Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(7.5f), I18n.translate(setting.getName()), x + 4f, y + 3f, theme.getTextColor());
        Render2D.drawRoundedRect(context.getMatrices(), x + width - 20f, y + 3.5f, 16f * toggleAnimation.getValue(), 8f, 2.5f, ColorUtils.getGlobalColor((int) (255 * toggleAnimation.getLinear())));
        Render2D.drawRoundedRect(context.getMatrices(), x + width - 4f - (16 * toggleAnimation.getReversedValue()), y + 3.5f, 16f * toggleAnimation.getReversedValue(), 8f, 2.5f, new Color(theme.getBackgroundColor().getRed(), theme.getBackgroundColor().getGreen(), theme.getBackgroundColor().getBlue(), 100));
        Render2D.drawRoundedRect(context.getMatrices(), x + width - 19.5f + (8f * toggleAnimation.getValue()), y + 4f, 7f, 7f, 2.5f, theme.getTextColor());
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (MathUtils.isHovered(x + width - 20f, y + 3.5f, 16f, 8f, (float) mouseX, (float) mouseY) && button == 0) {
            setting.setValue(!setting.getValue());
            GuiSoundHelper.playToggleSound(setting.getValue());
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