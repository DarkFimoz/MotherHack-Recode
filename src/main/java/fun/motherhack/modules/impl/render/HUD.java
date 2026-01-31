package fun.motherhack.modules.impl.render;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.hud.HudElement;
import fun.motherhack.hud.impl.*;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import meteordevelopment.orbit.EventHandler;

public class HUD extends Module {

    private final BooleanSetting watermark = new BooleanSetting("Watermark", true);
    private final BooleanSetting targetHud = new BooleanSetting("TargetHUD", true);
    private final BooleanSetting dynamicIsland = new BooleanSetting("DynamicIsland", true);
    private final BooleanSetting keyBinds = new BooleanSetting("KeyBinds", true);
    private final BooleanSetting potions = new BooleanSetting("Potions", true);
    private final BooleanSetting staffList = new BooleanSetting("StaffList", true);
    private final BooleanSetting desktop = new BooleanSetting("Desktop", true);

    private boolean lastWatermark = true;
    private boolean lastTargetHud = true;
    private boolean lastDynamicIsland = true;
    private boolean lastKeyBinds = true;
    private boolean lastPotions = true;
    private boolean lastStaffList = true;
    private boolean lastDesktop = true;

    public HUD() {
        super("HUD", Category.Render);
        getSettings().add(watermark);
        getSettings().add(targetHud);
        getSettings().add(dynamicIsland);
        getSettings().add(keyBinds);
        getSettings().add(potions);
        getSettings().add(staffList);
        getSettings().add(desktop);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        updateHudElements();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        // Выключаем все элементы когда модуль выключен
        for (HudElement element : MotherHack.getInstance().getHudManager().getHudElements()) {
            element.setToggled(false);
        }
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (fullNullCheck()) return;
        
        // Проверяем изменения настроек
        if (lastWatermark != watermark.getValue() || 
            lastTargetHud != targetHud.getValue() ||
            lastDynamicIsland != dynamicIsland.getValue() ||
            lastKeyBinds != keyBinds.getValue() ||
            lastPotions != potions.getValue() ||
            lastStaffList != staffList.getValue() ||
            lastDesktop != desktop.getValue()) {
            
            updateHudElements();
            
            lastWatermark = watermark.getValue();
            lastTargetHud = targetHud.getValue();
            lastDynamicIsland = dynamicIsland.getValue();
            lastKeyBinds = keyBinds.getValue();
            lastPotions = potions.getValue();
            lastStaffList = staffList.getValue();
            lastDesktop = desktop.getValue();
        }
    }

    private void updateHudElements() {
        for (HudElement element : MotherHack.getInstance().getHudManager().getHudElements()) {
            if (element instanceof Watermark) {
                element.setToggled(toggled && watermark.getValue());
            } else if (element instanceof TargetHud) {
                element.setToggled(toggled && targetHud.getValue());
            } else if (element instanceof DynamicIsland) {
                element.setToggled(toggled && dynamicIsland.getValue());
            } else if (element instanceof KeyBinds) {
                element.setToggled(toggled && keyBinds.getValue());
            } else if (element instanceof Potions) {
                element.setToggled(toggled && potions.getValue());
            } else if (element instanceof StaffList) {
                element.setToggled(toggled && staffList.getValue());
            } else if (element instanceof Desktop) {
                element.setToggled(toggled && desktop.getValue());
            }
        }
    }
}
