package fun.motherhack.modules.api;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.settings.Setting;
import fun.motherhack.modules.settings.api.Bind;
import fun.motherhack.utils.Wrapper;
import fun.motherhack.utils.notify.Notify;
import fun.motherhack.utils.notify.NotifyIcons;
import fun.motherhack.utils.sound.GuiSoundHelper;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class Module implements Wrapper {
    private final String name, description;
    private final Category category;
    protected boolean toggled;
    @Setter private Bind bind = new Bind(-1, false);
    private final List<Setting<?>> settings = new ArrayList<>();

    public Module(String name, Category category) {
        this.name = name;
        this.category = category;
        this.description = "descriptions" + "." + category.name().toLowerCase() + "." + name.toLowerCase();
    }

    public void onEnable() {
        toggled = true;
        MotherHack.getInstance().getEventHandler().subscribe(this);
        if (!fullNullCheck()) {
            MotherHack.getInstance().getNotifyManager().add(new Notify(NotifyIcons.successIcon, name + " was enable", 800));
            GuiSoundHelper.playToggleSound(true);
        }
    }

    public void onDisable() {
        toggled = false;
        MotherHack.getInstance().getEventHandler().unsubscribe(this);
        if (!fullNullCheck()) {
            MotherHack.getInstance().getNotifyManager().add(new Notify(NotifyIcons.failIcon, name + " was disable", 800));
            GuiSoundHelper.playToggleSound(false);
        }
    }

    public void setToggled(boolean toggled) {
        if (this.toggled == toggled) return; // предотвращаем повторное включение/выключение
        if (toggled) onEnable();
        else onDisable();
    }

    public void toggle() {
        setToggled(!toggled);
    }

    public static boolean fullNullCheck() {
        return mc.player == null || mc.world == null;
    }
}