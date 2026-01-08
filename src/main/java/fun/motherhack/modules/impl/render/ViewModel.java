package fun.motherhack.modules.impl.render;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.NumberSetting;

public class ViewModel extends Module {
	
    public NumberSetting mainX = new NumberSetting("MainX", 0f, -2f, 2f, 0.05f);
    public NumberSetting mainY = new NumberSetting("MainY", 0f, -2f, 2f, 0.05f);
    public NumberSetting mainZ = new NumberSetting("MainZ", 0f, -2f, 2f, 0.05f);
    public NumberSetting mainScaleX = new NumberSetting("MainScaleX", 1f, 0f, 3f, 0.05f);
    public NumberSetting mainScaleY = new NumberSetting("MainScaleY", 1f, 0f, 3f, 0.05f);
    public NumberSetting mainScaleZ = new NumberSetting("MainScaleZ", 1f, 0f, 3f, 0.05f);
    public NumberSetting offX = new NumberSetting("OffX", 0f, -2f, 2f, 0.05f);
    public NumberSetting offY = new NumberSetting("OffY", 0f, -2f, 2f, 0.05f);
    public NumberSetting offZ = new NumberSetting("OffZ", 0f, -2f, 2f, 0.05f);
    public NumberSetting offScaleX = new NumberSetting("OffScaleX", 1f, 0f, 3f, 0.05f);
    public NumberSetting offScaleY = new NumberSetting("OffScaleY", 1f, 0f, 3f, 0.05f);
    public NumberSetting offScaleZ = new NumberSetting("OffScaleZ", 1f, 0f, 3f, 0.05f);

	public ViewModel() {
		super("ViewModel", Category.Render);
	}
}