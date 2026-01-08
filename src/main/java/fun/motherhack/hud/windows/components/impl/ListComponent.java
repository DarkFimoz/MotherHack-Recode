package fun.motherhack.hud.windows.components.impl;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import fun.motherhack.hud.windows.components.WindowComponent;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.ListSetting;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;

public class ListComponent extends WindowComponent {
	
	private final ListSetting setting;
	private final Map<BooleanSetting, Animation> pickAnimations = new HashMap<>();

	public ListComponent(String name, ListSetting setting) {
		super(name);
		this.setting = setting;
		for (BooleanSetting setting1 : setting.getValue()) pickAnimations.put(setting1, new Animation(300, 1f, false, Easing.BOTH_SINE));
		this.addHeight = () -> setting.getValue().size() * 14f;
		this.visible = setting::isVisible;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		Render2D.drawFont(context.getMatrices(),
				Fonts.REGULAR.getFont(7.5f),
				I18n.translate(setting.getName()),
				x + 5f,
				y + 3.5f,
				Color.WHITE
		);
		
		Render2D.drawFont(context.getMatrices(),
				Fonts.REGULAR.getFont(7.5f),
	            "(" + setting.getToggled().size() + "/" + setting.getValue().size() + ")",
	            x + width - Fonts.REGULAR.getWidth("(" + setting.getToggled().size() + "/" + setting.getValue().size() + ")", 7.5f) - 5f,
				y + 3.5f,
				Color.WHITE
		);
		
		float yOffset = height;
		for (BooleanSetting setting : setting.getValue()) {
			Animation anim = pickAnimations.get(setting);
			anim.update(setting.getValue());
			Render2D.drawFont(context.getMatrices(), Fonts.ICONS.getFont(10f), "D", x + width - 14f, y + yOffset + 3.5f, new Color(255, 255, 255, (int) (255 * anim.getValue())));
			Render2D.drawFont(context.getMatrices(),
					Fonts.REGULAR.getFont(7.5f),
					I18n.translate(setting.getName()),
					x + 6f,
					y + yOffset + 3.5f,
					Color.WHITE
			);
			yOffset += 14f;
		}
	}

	@Override
	public void mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
            float yOffset = height;
            for (BooleanSetting setting : setting.getValue()) {
                if (MathUtils.isHovered(x, y + yOffset, width, 14f, (float) mouseX, (float) mouseY)) {
                    setting.setValue(!setting.getValue());
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