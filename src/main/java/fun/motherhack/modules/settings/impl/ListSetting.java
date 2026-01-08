package fun.motherhack.modules.settings.impl;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import fun.motherhack.modules.settings.Setting;

public class ListSetting extends Setting<List<BooleanSetting>> {

	public ListSetting(String name, BooleanSetting... values) {
		super(name, Arrays.asList(values));
	}
	
	public ListSetting(String name, Supplier<Boolean> visible, BooleanSetting... values) {
		super(name, Arrays.asList(values), visible);
	}
	
    public BooleanSetting getName(String name) {
        return getValue().stream().filter(setting -> setting.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

	public List<BooleanSetting> getToggled() {
		return getValue().stream().filter(Setting::getValue).toList();
	}
}