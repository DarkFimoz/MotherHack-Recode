package fun.motherhack.hud.windows.components;

import fun.motherhack.screen.clickgui.components.Component;
import fun.motherhack.utils.animations.Animation;
import lombok.*;

@Getter @Setter
public abstract class WindowComponent extends Component {
	protected Animation animation;

	public WindowComponent(String name) {
		super(name);
	}
}