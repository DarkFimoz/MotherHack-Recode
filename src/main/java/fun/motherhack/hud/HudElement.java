package fun.motherhack.hud;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventMouse;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.hud.windows.Window;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.Setting;
import fun.motherhack.modules.settings.api.Position;
import fun.motherhack.modules.settings.impl.*;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.notify.Notify;
import fun.motherhack.utils.notify.NotifyIcons;
import fun.motherhack.utils.render.Render2D;
import lombok.Getter;
import lombok.Setter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public abstract class HudElement extends Module {

    private final PositionSetting position = new PositionSetting("Position", new Position(0, 0));
    private final Animation hoverAnimation = new Animation(300, 1f, false, Easing.SMOOTH_STEP);
    protected final Animation toggledAnimation = new Animation(300, 1f, false, Easing.BOTH_SINE);
    private float dragX, dragY, width, height;
    private boolean dragging, button;
    private final List<Setting<?>> settings = new ArrayList<>();
    private Window window;	

    public HudElement(String name) {
        super(name, Category.Hud);
        settings.add(position);
        setToggled(true);
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck()) return;

        hoverAnimation.update(MathUtils.isHovered(getX(), getY(), getWidth(), getHeight(), mouseX(), mouseY()) && window == null || button || dragging);

        if (button) {
            if (!dragging && MathUtils.isHovered(getX(), getY(), getWidth(), getHeight(), mouseX(), mouseY())) {
                dragX = mouseX() - getX();
                dragY = mouseY() - getY();
                dragging = true;
                MotherHack.getInstance().getHudManager().setCurrentDragging(this);
            }

            if (dragging) {
                float finalX = Math.min(Math.max(mouseX() - dragX, 0), mc.getWindow().getScaledWidth() - width);
                float finalY = Math.min(Math.max(mouseY() - dragY, 0), mc.getWindow().getScaledHeight() - height);
                position.getValue().setX(finalX / mc.getWindow().getScaledWidth());
                position.getValue().setY(finalY / mc.getWindow().getScaledHeight());
            }
        } else dragging = false;

        if (mc.currentScreen instanceof ChatScreen) Render2D.drawBorder(
                e.getContext().getMatrices(),
                getX() - 3,
                getY() - 3,
                getWidth() + 6,
                getHeight() + 6,
                3.5f + (3.5f / 2f),
                1.25f,
                1.25f,
                new Color(255, 255, 255, (int) (255 * hoverAnimation.getValue()))
        );

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
    public void onRender2DX2(EventRender2D e) {
        if (fullNullCheck()) return;

        // Ищем настройку по имени элемента
        String elementName = getName();
        BooleanSetting setting = null;
        
        // Перебираем все настройки и ищем нужную по имени (без учёта регистра)
        for (BooleanSetting bs : MotherHack.getInstance().getHudManager().getElements().getValue()) {
            if (bs.getName().equalsIgnoreCase(elementName)) {
                setting = bs;
                break;
            }
        }
        
        if (setting != null) {
            toggledAnimation.update(setting.getValue());
        } else {
            toggledAnimation.update(true); // По умолчанию включен
        }
    }

    @EventHandler
    public void onMouse(EventMouse e) {
        if (!(mc.currentScreen instanceof ChatScreen) || fullNullCheck()) return;

        if (e.getAction() == 0) {
            button = false;
            dragging = false;
            MotherHack.getInstance().getHudManager().setCurrentDragging(null);
        } else if (e.getAction() == 1) {
            if (e.getButton() == 0 && MathUtils.isHovered(getX(), getY(), getWidth(), getHeight(), mouseX(), mouseY()) && MotherHack.getInstance().getHudManager().getCurrentDragging() == null)
                button = true;

            if (window != null) {
                if (MathUtils.isHovered(window.getX(), window.getY(), window.getWidth(), window.getFinalHeight(), mouseX(), mouseY())) {
                    window.mouseClicked(mouseX(), mouseY(), e.getButton());
                    return;
                } else window.reset();
            }

            if (e.getButton() == 1) {
                if (MathUtils.isHovered(getX(), getY(), getWidth(), getHeight(), mouseX(), mouseY())) {

                    if (settings.size() == 1) {
                        if (MotherHack.getInstance().getHudManager().getWindow() != null) MotherHack.getInstance().getHudManager().setWindow(null);
                        MotherHack.getInstance().getNotifyManager().add(new Notify(NotifyIcons.failIcon, "У этого элемента нету настроек!", 1000));
                    } else {
                        if (MotherHack.getInstance().getHudManager().getWindow() != null)
                            MotherHack.getInstance().getHudManager().getWindow().reset();
                        for (HudElement element : MotherHack.getInstance().getHudManager().getHudElements()) {
                            if (element.getWindow() == null) continue;
                            element.getWindow().reset();
                        }
                        window = new Window(mouseX() + 3, mouseY() + 3, 100, 12.5f, settings);
                    }
                }
            }
        }
    }

    public float getX() {
        return mc.getWindow().getScaledWidth() * position.getValue().getX();
    }

    public float getY() {
        return mc.getWindow().getScaledHeight() * position.getValue().getY();
    }

    public int mouseX() {
        return (int) (mc.mouse.getX() / mc.getWindow().getScaleFactor());
    }

    public int mouseY() {
        return (int) (mc.mouse.getY() / mc.getWindow().getScaleFactor());
    }

    public void setBounds(float x, float y, float width, float height) {
        this.width = width;
        this.height = height;
        position.getValue().setX(x / mc.getWindow().getScaledWidth());
        position.getValue().setY(y / mc.getWindow().getScaledHeight());
    }
    
    protected boolean closed() {
    	return toggledAnimation.getValue() <= 0f;
    }

    @Override
    public void onEnable() {
    	super.onEnable();
    }

    @Override
    public void onDisable() {
    	super.onDisable();
    }
}