package fun.motherhack.modules.impl.client;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.api.Nameable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;

public class Cape extends Module {

    @AllArgsConstructor
    @Getter
    public enum CapeType implements Nameable {
        None("None"),
        Dev("Dev"),
        Tester("Tester"),
        Star("Star"),
        BKGroup("BK Group"),
        FBGroup("FB Group");

        private final String name;
    }

    private final EnumSetting<CapeType> capeType = new EnumSetting<>("Cape Type", CapeType.None);
    private CapeType lastCapeType = CapeType.None;

    public Cape() {
        super("Cape", Category.Client);
        getSettings().add(capeType);
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (capeType.getValue() != lastCapeType) {
            lastCapeType = capeType.getValue();
            updateCape();
        }
    }

    public CapeType getCapeType() {
        return capeType.getValue();
    }

    public boolean isEnabled() {
        return isToggled() && capeType.getValue() != CapeType.None;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        updateCape();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        MotherHack.getInstance().getCapeManager().setSelectedCape((fun.motherhack.managers.CapeManager.Cape) null);
    }

    private void updateCape() {
        if (capeType.getValue() == CapeType.None) {
            MotherHack.getInstance().getCapeManager().setSelectedCape((fun.motherhack.managers.CapeManager.Cape) null);
        } else {
            MotherHack.getInstance().getCapeManager().setSelectedCape(capeType.getValue().getName());
        }
    }
}
