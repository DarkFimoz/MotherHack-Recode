package fun.motherhack.hud.windows.components.impl;

import java.awt.Color;

import fun.motherhack.hud.windows.components.WindowComponent;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;

public class NumberComponent extends WindowComponent {

	private final NumberSetting setting;
	private final Animation hoverAnimation = new Animation(300, 1, false, Easing.MotherHack);

	public NumberComponent(String name, NumberSetting setting) {
		super(name);
		this.setting = setting;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		boolean hovered = MathUtils.isHovered(x, y, width, height, mouseX, mouseY);
		hoverAnimation.update(hovered);

		Render2D.drawFont(context.getMatrices(), Fonts.BOLD.getFont(8f), getName(), x + 5f, y + 4f, new Color(255, 255, 255, (int) (255 * animation.getValue())));
		String valueText = String.format("%.1f", setting.getValue());
		float valueWidth = Fonts.BOLD.getWidth(valueText, 8f);
		Render2D.drawFont(context.getMatrices(), Fonts.BOLD.getFont(8f), valueText, x + width - valueWidth - 5f, y + 4f, new Color(255, 255, 255, (int) (255 * animation.getValue())));

		// Draw hover effect
		if (hovered) {
			float centerX = x + width / 2;
			if (mouseX < centerX) {
				// Left half - decrease indicator
				Render2D.drawRoundedRect(context.getMatrices(), x, y, width / 2, height, 0, new Color(255, 0, 0, (int) (30 * animation.getValue())));
			} else {
				// Right half - increase indicator
				Render2D.drawRoundedRect(context.getMatrices(), x + width / 2, y, width / 2, height, 0, new Color(0, 255, 0, (int) (30 * animation.getValue())));
			}
		}
	}

	@Override
	public void mouseClicked(double mouseX, double mouseY, int button) {
		if (!MathUtils.isHovered(x, y, width, height, (float) mouseX, (float) mouseY) || button != 0) return;

		float current = setting.getValue();
		float centerX = x + width / 2;
		if (mouseX < centerX) { // Left half - decrease
			setting.setValue(Math.max(current - setting.getIncrement(), setting.getMin()));
		} else { // Right half - increase
			setting.setValue(Math.min(current + setting.getIncrement(), setting.getMax()));
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