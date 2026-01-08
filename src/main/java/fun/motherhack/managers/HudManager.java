package fun.motherhack.managers;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventMouse;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.hud.HudElement;
import fun.motherhack.hud.windows.Window;
import fun.motherhack.hud.impl.*;
import fun.motherhack.modules.settings.Setting;
import fun.motherhack.modules.settings.impl.*;
import fun.motherhack.utils.Wrapper;
import fun.motherhack.utils.math.MathUtils;
import lombok.Getter;
import lombok.Setter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import fun.motherhack.modules.api.Module;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Getter
public class HudManager implements Wrapper {

    @Setter private HudElement currentDragging;
    private final List<HudElement> hudElements = new ArrayList<>();
    protected final ListSetting elements = new ListSetting("Elements",
            new BooleanSetting("Watermark", false),
            new BooleanSetting("TargetHud", true),
            new BooleanSetting("DynamicIsland", true),
            new BooleanSetting("KeyBinds", true),
            new BooleanSetting("Potions", true),
            new BooleanSetting("StaffList", true),
            new BooleanSetting("Desktop", true)
    );
    @Setter private Window window;

    public HudManager() {
        MotherHack.getInstance().getEventHandler().subscribe(this);

        addElements(
                new Watermark(),
                new TargetHud(),
                new DynamicIsland(),
                new KeyBinds(),
                new Potions(),
                new StaffList(),
                new Desktop()
        );

        for (HudElement element : hudElements) {
            try {
                for (Field field : element.getClass().getDeclaredFields()) {
                    if (!Setting.class.isAssignableFrom(field.getType())) continue;
                    field.setAccessible(true);
                    Setting<?> setting = (Setting<?>) field.get(element);
                    if (setting != null && !element.getSettings().contains(setting)) element.getSettings().add(setting);
                }
            } catch (Exception ignored) {}
        }
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (Module.fullNullCheck()) return;

        if (window != null) {
        	 if (!(mc.currentScreen instanceof ChatScreen)) window.reset();
             
             if (window.closed()) {
                 window = null;
                 return;
             }
             
             window.render(e.getContext(), mouseX(), mouseY());
        }
    }

    @EventHandler
    public void onMouse(EventMouse e) {
        if (!(mc.currentScreen instanceof ChatScreen) || Module.fullNullCheck()) return;

        if (e.getAction() == 1) {
            if (window != null) {
                if (MathUtils.isHovered(window.getX(), window.getY(), window.getWidth(), window.getFinalHeight(), mouseX(), mouseY())) {
                    window.mouseClicked(mouseX(), mouseY(), e.getButton());
                    return;
                } else window.reset();
            }

            if (e.getButton() == 1) {
            	for (HudElement element : hudElements) {
            		if (element.getWindow() == null) continue;
            		if (element.getSettings().size() == 1) return;
            		element.getWindow().reset();
            	}
            	
                window = new Window(mouseX() + 3, mouseY() + 3, 100, 12.5f, List.of(elements));
            }
        }
    }

    public int mouseX() {
        return (int) (mc.mouse.getX() / mc.getWindow().getScaleFactor());
    }

    public int mouseY() {
        return (int) (mc.mouse.getY() / mc.getWindow().getScaleFactor());
    }

    private void addElements(HudElement... element) {
        this.hudElements.addAll(List.of(element));
    }
}