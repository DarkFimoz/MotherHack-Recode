package fun.motherhack.screen.clickgui.components.impl;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.screen.clickgui.components.Component;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.render.fonts.Fonts;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.sound.GuiSoundHelper;
import fun.motherhack.modules.impl.client.UI;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;

import java.awt.*;
import java.util.*;

public class EnumComponent extends Component {

    private final EnumSetting<?> setting;
    private final Animation openAnimation = new Animation(300, 1f, false, Easing.BOTH_SINE);
    private final Map<Enum<?>, Animation> pickAnimations = new HashMap<>();
    private boolean open;

    public EnumComponent(EnumSetting<?> setting) {
        super(setting.getName());
        this.setting = setting;
        for (Enum<?> enums : setting.getValue().getClass().getEnumConstants()) pickAnimations.put(enums, new Animation(300, 1f, false, Easing.BOTH_SINE));
        this.addHeight = () -> openAnimation.getValue() > 0 ? ((setting.getValue().getClass().getEnumConstants().length * 14f)) * openAnimation.getValue() : 0;
        this.visible = setting::isVisible;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        UI.ClickGuiTheme theme = MotherHack.getInstance().getClickGui().getTheme();
        openAnimation.update(open);

        Render2D.drawFont(context.getMatrices(),
                Fonts.REGULAR.getFont(7.5f),
                I18n.translate(setting.getName()) + ": " + I18n.translate(setting.currentEnumName()),
                x + 4f,
                y + 3f,
                theme.getTextColor()
        );

        if (openAnimation.getValue() > 0) {
            float yOffset = height;
            for (Enum<?> enums : setting.getValue().getClass().getEnumConstants()) {
                Render2D.startScissor(context, x, y + yOffset, width, 14f);
                Animation anim = pickAnimations.get(enums);
                anim.update(enums == setting.getValue());
                Render2D.drawFont(context.getMatrices(), Fonts.ICONS.getFont(10f), "D", x + width - 14f, y + yOffset + 2f, new Color(theme.getTextColor().getRed(), theme.getTextColor().getGreen(), theme.getTextColor().getBlue(), (int) (255 * anim.getValue())));
                Render2D.drawFont(context.getMatrices(),
                        Fonts.REGULAR.getFont(7.5f),
                        I18n.translate(((Nameable) enums).getName()),
                        x + 6f,
                        y + yOffset + 2f,
                        theme.getTextColor()
                );
                yOffset += 14f;
                Render2D.stopScissor(context);
            }
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (MathUtils.isHovered(x, y, width, height, (float) mouseX, (float) mouseY)) {
            if (button == 0) {
                setting.increaseEnum();
                GuiSoundHelper.playToggleSound(true);
            }
            else if (button == 1) {
                open = !open;
                GuiSoundHelper.playExpandSound();
            }
        }

        if (open && button == 0) {
            float yOffset = height;
            for (Enum<?> enums : setting.getValue().getClass().getEnumConstants()) {
                if (MathUtils.isHovered(x, y + yOffset, width, 14f, (float) mouseX, (float) mouseY)) {
                    setting.setEnumValue(((Nameable) enums).getName());
                    GuiSoundHelper.playToggleSound(true);
                    break;
                }

                yOffset += 14f;
            }
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