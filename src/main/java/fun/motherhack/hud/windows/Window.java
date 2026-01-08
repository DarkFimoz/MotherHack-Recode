package fun.motherhack.hud.windows;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import fun.motherhack.hud.windows.components.WindowComponent;
import fun.motherhack.hud.windows.components.impl.BooleanComponent;
import fun.motherhack.hud.windows.components.impl.ListComponent;
import fun.motherhack.hud.windows.components.impl.NumberComponent;
import fun.motherhack.modules.settings.Setting;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.render.Render2D;
import net.minecraft.client.gui.DrawContext;
import fun.motherhack.modules.settings.impl.*;
import lombok.*;

@Getter @Setter
public class Window {
	private float x, y, width, height;
	private final List<Setting<?>> settings;
	private final List<WindowComponent> components = new ArrayList<>();
	private final Animation animation = new Animation(300, 1f, true, Easing.BOTH_SINE);
	
	public Window(float x, float y, float width, float height, List<Setting<?>> settings) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.settings = settings;
		for (Setting<?> setting : settings) {
			if (setting instanceof BooleanSetting) components.add(new BooleanComponent(setting.getName(), ((BooleanSetting) setting)));
			else if (setting instanceof ListSetting) components.add(new ListComponent(setting.getName(), ((ListSetting) setting)));
			else if (setting instanceof NumberSetting) components.add(new NumberComponent(setting.getName(), ((NumberSetting) setting)));
		}
	}
	
	public void render(DrawContext context, int mouseX, int mouseY) {
		Render2D.startScissor(
				context,
				getX() + (getWidth() - getWidth() * animation.getValue()) / 2,
				getY() + (getFinalHeight() - getFinalHeight() * animation.getValue()) / 2,
				getWidth() * animation.getValue(),
				getFinalHeight() * animation.getValue()
		);

		Render2D.drawStyledRect(
				context.getMatrices(),
				getX() + (getWidth() - getWidth() * animation.getValue()) / 2,
				getY() + (getFinalHeight() - getFinalHeight() * animation.getValue()) / 2,
				getWidth() * animation.getValue(),
				getFinalHeight() * animation.getValue(),
				3.5f, new Color(0, 0, 0, (int) (200 * animation.getValue())), (int) (255 * animation.getValue())
		);

		float finalY = y;

		for (WindowComponent component : components) {
			component.setX(x);
			component.setY(finalY);
			component.setWidth(width);
			component.setHeight(height);
			component.setAnimation(animation);
			component.render(context, mouseX, mouseY, 0);
			finalY += component.getHeight() + 4.5f;
		}

		Render2D.stopScissor(context);
	}

	public void reset() {
		animation.update(false);
	}

	public boolean closed() {
		return animation.finished(false) && animation.getValue() <= 0.01f;
	}
	
	public void mouseClicked(double mouseX, double mouseY, int button) {
		for (WindowComponent component : components) component.mouseClicked(mouseX, mouseY, button);
	}
	
	public float getFinalHeight() {
		float height = 0;
		for (WindowComponent component : components) {
			if (!component.getVisible().get()) continue;
			height += component.getHeight() + component.getAddHeight().get() + 4.5f;
		}
		
		return height;
	}
}