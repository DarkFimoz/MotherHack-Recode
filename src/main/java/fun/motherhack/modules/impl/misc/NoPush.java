package fun.motherhack.modules.impl.misc;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;

public class NoPush extends Module {

    public BooleanSetting players = new BooleanSetting("settings.nopush.players", true);
    public BooleanSetting blocks = new BooleanSetting("settings.nopush.blocks", true);
    public BooleanSetting water = new BooleanSetting("settings.nopush.water", true);

    public NoPush() {
        super("NoPush", Category.Misc);
    }
}