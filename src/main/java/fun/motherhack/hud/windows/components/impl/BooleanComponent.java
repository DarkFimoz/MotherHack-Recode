package fun.motherhack.hud.windows.components.impl;

import java.awt.Color;

import fun.motherhack.hud.windows.components.WindowComponent;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.render.ColorUtils;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;

public class BooleanComponent extends WindowComponent {
	
	private final BooleanSetting setting;
	private final Animation toggleAnimation = new Animation(300, 1, false, Easing.MotherHack);
	
	public BooleanComponent(String name, BooleanSetting setting) {
		super(name);
		this.setting = setting;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		toggleAnimation.update(setting.getValue());
		Render2D.drawFont(context.getMatrices(), Fonts.BOLD.getFont(8f), getName(), x + 5f, y + 4f, new Color(255, 255, 255, (int) (255 * animation.getValue())));
        Render2D.drawRoundedRect(context.getMatrices(), x + width - 20f, y + 4.5f, 16f * toggleAnimation.getValue(), 8f, 2.5f, ColorUtils.getGlobalColor((int) (255 * toggleAnimation.getLinear())));
        Render2D.drawRoundedRect(context.getMatrices(), x + width - 4f - (16 * toggleAnimation.getReversedValue()), y + 4.5f, 16f * toggleAnimation.getReversedValue(), 8f, 2.5f, new Color(23, 23, 23, 100));
        Render2D.drawRoundedRect(context.getMatrices(), x + width - 19.5f + (8f * toggleAnimation.getValue()), y + 5f, 7f, 7f, 2.5f, Color.WHITE);
	}

	@Override
	public void mouseClicked(double mouseX, double mouseY, int button) {
        if (MathUtils.isHovered(x + width - 20f, y + 3.5f, 16f, 8f, (float) mouseX, (float) mouseY) && button == 0) setting.setValue(!setting.getValue());
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