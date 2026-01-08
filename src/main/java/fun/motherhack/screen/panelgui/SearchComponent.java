package fun.motherhack.screen.panelgui;

import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

/**
 * SearchComponent - компонент поиска для PanelGui
 */
public class SearchComponent {

    private float x = 0, y = 0;
    private final float width = 300f;
    private final float height = 24f;
    private String text = "";
    private boolean focused = false;

    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }

    public void init() {
        text = "";
        focused = false;
    }

    public void draw(DrawContext context, float mouseX, float mouseY, float partialTicks, float parentAlpha) {
        int rectAlpha = (int) (parentAlpha * 230);
        int borderAlpha = (int) (parentAlpha * 12);
        int textAlpha = (int) (parentAlpha * 255);
        Color bgColor = new Color(24, 18, 24, rectAlpha);
        Color borderColor = new Color(200, 200, 200, borderAlpha);
        Color textColor = new Color(180, 180, 180, textAlpha);

        Render2D.drawRoundedRect(context.getMatrices(), x, y, width, height, 6f, bgColor);
        Render2D.drawBorder(context.getMatrices(), x, y, width, height, 6f, 1f, 1f, borderColor);

        String display = text.isEmpty() && !focused ? "Поиск" : text;
        Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(7f), display, x + 10f, y + (height / 2f) - 3f, textColor);

        // Курсор
        if (focused && System.currentTimeMillis() % 1000 < 500) {
            float cursorX = x + 10f + Fonts.REGULAR.getWidth(text, 7f) + 1f;
            Render2D.drawRoundedRect(context.getMatrices(), cursorX, y + 7f, 1f, 10f, 0.5f, new Color(255, 255, 255, textAlpha));
        }
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            focused = true;
            return true;
        } else {
            focused = false;
        }
        return false;
    }

    public boolean mouseReleased(float mouseX, float mouseY, int button) { return false; }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;
        if (keyCode == 257 || keyCode == 335) { // Enter
            focused = false;
            return true;
        }
        if (keyCode == 259) { // Backspace
            if (!text.isEmpty()) text = text.substring(0, text.length() - 1);
            return true;
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (!focused) return false;
        if (Character.isISOControl(codePoint)) return false;
        text += codePoint;
        return true;
    }

    public String getText() { return text; }
}
