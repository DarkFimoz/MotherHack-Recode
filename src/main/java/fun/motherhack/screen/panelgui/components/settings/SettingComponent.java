package fun.motherhack.screen.panelgui.components.settings;

import fun.motherhack.modules.settings.Setting;
import net.minecraft.client.gui.DrawContext;

/**
 * SettingComponent - базовый класс для компонентов настроек
 */
public abstract class SettingComponent<T extends Setting<?>> {

    protected final T setting;
    protected float x, y, width, height;

    public SettingComponent(T setting) {
        this.setting = setting;
        this.height = 15f;
    }

    public abstract void draw(DrawContext context, float mouseX, float mouseY, float partialTicks, float parentAlpha);

    public abstract boolean mouseClicked(float mouseX, float mouseY, int button);

    public void mouseDragged(double mouseX, double mouseY, int button) {}

    public void mouseReleased(float mouseX, float mouseY, int button) {}

    protected boolean isHovered(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public T getSetting() {
        return setting;
    }
}
