package fun.motherhack.modules.impl.render;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.ColorSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;

import java.awt.*;

public class Models extends Module {

    public EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.Amogus);
    public BooleanSetting onlySelf = new BooleanSetting("OnlySelf", false);
    public BooleanSetting friends = new BooleanSetting("Friends", false);
    public BooleanSetting friendHighlight = new BooleanSetting("FriendHighlight", false, () -> mode.getValue() == Mode.Amogus);
    
    public ColorSetting bodyColor = new ColorSetting("BodyColor", new Color(255, 0, 0), () -> mode.getValue() == Mode.Amogus);
    public ColorSetting eyeColor = new ColorSetting("EyeColor", new Color(150, 200, 255), () -> mode.getValue() == Mode.Amogus);
    public ColorSetting legsColor = new ColorSetting("LegsColor", new Color(255, 0, 0), () -> mode.getValue() == Mode.Amogus);
    
    public NumberSetting customScale = new NumberSetting("Scale", 1.0f, 0.1f, 5.0f, 0.1f, () -> mode.getValue() == Mode.Custom);

    public Models() {
        super("Models", Category.Render);
    }

    public enum Mode implements Nameable {
        Amogus("Amogus"),
        Rabbit("Rabbit"),
        Freddy("Freddy"),
        TunTunSahur("TunTunSahur"),
        Custom("Custom");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
