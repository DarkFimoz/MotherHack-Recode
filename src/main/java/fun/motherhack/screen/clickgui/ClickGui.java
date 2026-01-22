package fun.motherhack.screen.clickgui;

import lombok.Getter;
import lombok.Setter;
import fun.motherhack.MotherHack;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.utils.Wrapper;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.render.fonts.Fonts;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.sound.GuiSoundHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ClickGui extends Screen implements Wrapper {

    private final List<Panel> panels = new ArrayList<>();
    
    private final Animation openAnimation = new Animation(300, 1f, true, Easing.BOTH_SINE);
    private final Animation closeAnimation = new Animation(300, 1f, false, Easing.BOTH_SINE);
    private final Animation hoverAnimation = new Animation(300, 1f, true, Easing.BOTH_SINE);
    private final Animation searchFocusAnimation = new Animation(200, 1f, false, Easing.BOTH_SINE);
    private final Animation clearButtonAnimation = new Animation(150, 1f, false, Easing.BOTH_SINE);
    
    @Setter private String description = "";
    private boolean close = false;
    @Setter private boolean fromMainMenu = false;
    
    @Getter private String searchQuery = "";
    @Getter private boolean searchFocused = false;
    private boolean clearButtonHovered = false;
    
    private static final float SEARCH_WIDTH = 220f;
    private static final float SEARCH_HEIGHT = 20f;
    
    public void setSearchQuery(String query) {
        if (!query.equals(this.searchQuery)) {
            this.searchQuery = query;
        }
    }

    public ClickGui() {
        super(Text.of("motherhack-clickgui"));
        for (Category category : MotherHack.getInstance().getModuleManager().getCategories()) {
            if (category == Category.Hud) continue;
            panels.add(new Panel(-999, -999, 110, 20, category));
        }
    }

    public UI.ClickGuiTheme getTheme() {
        return MotherHack.getInstance().getModuleManager().getModule(UI.class).getTheme();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        openAnimation.update(!close);
        closeAnimation.update(close);
        searchFocusAnimation.update(searchFocused);
        clearButtonAnimation.update(clearButtonHovered && !searchQuery.isEmpty());
        
        if (close && closeAnimation.getValue() >= 0.99f) {
            resetSearch();
            MotherHack.getInstance().getModuleManager().getModule(UI.class).setToggled(false);
            if (fromMainMenu) {
                fromMainMenu = false;
                mc.setScreen(new net.minecraft.client.gui.screen.TitleScreen());
            } else {
                super.close();
            }
            return;
        }

        context.getMatrices().push();
        context.getMatrices().translate(mc.getWindow().getScaledWidth() / 2f, mc.getWindow().getScaledHeight() / 2f, 1f);
        float scale = close ? (1f - closeAnimation.getValue()) : openAnimation.getValue();
        context.getMatrices().scale(scale, scale, 1f);
        context.getMatrices().translate(-mc.getWindow().getScaledWidth() / 2f, -mc.getWindow().getScaledHeight() / 2f, 0f);
        
        renderClassicStyle(context, mouseX, mouseY, delta);
        
        context.getMatrices().pop();
    }
    
    private void renderClassicStyle(DrawContext context, int mouseX, int mouseY, float delta) {
        renderSearchBar(context, mouseX, mouseY);
        for (Panel panel : panels) panel.render(context, mouseX, mouseY, delta);
        
        UI uiModule = MotherHack.getInstance().getModuleManager().getModule(UI.class);
        if (uiModule != null && uiModule.isShowBackground()) {
            int imageSize = 150;
            Render2D.drawTexture(context.getMatrices(), 10, mc.getWindow().getScaledHeight() - imageSize - 10, imageSize, imageSize, 10f, MotherHack.id("sexy.png"), Color.WHITE);
        }
        renderDescription(context);
    }
    
    private void renderSearchBar(DrawContext context, int mouseX, int mouseY) {
        UI uiModule = MotherHack.getInstance().getModuleManager().getModule(UI.class);
        UI.ClickGuiTheme theme = getTheme();
        float searchX = getSearchBarX();
        float searchY = getSearchBarY();
        
        // Get blur and alpha settings
        boolean enableBlur = uiModule != null && uiModule.isEnableBlur();
        float blurRadius = uiModule != null ? uiModule.getBlurRadius() : 20f;
        float blurAlpha = uiModule != null ? uiModule.getBlurAlpha() : 150f;
        float bgAlpha = uiModule != null ? uiModule.getBackgroundAlpha() : 180f;
        
        int bgAlphaInt = (int) (bgAlpha + 40 * searchFocusAnimation.getValue());
        Color bgColor = new Color(theme.getBackgroundColor().getRed(), theme.getBackgroundColor().getGreen(), theme.getBackgroundColor().getBlue(), bgAlphaInt);
        
        // Render with blur if enabled
        if (enableBlur) {
            Render2D.drawBlurredRect(context.getMatrices(), searchX, searchY, SEARCH_WIDTH, SEARCH_HEIGHT, 6f, blurRadius, new Color(255, 255, 255, (int)blurAlpha));
            Render2D.drawRoundedRect(context.getMatrices(), searchX, searchY, SEARCH_WIDTH, SEARCH_HEIGHT, 6f, bgColor);
        } else {
            Render2D.drawStyledRect(context.getMatrices(), searchX, searchY, SEARCH_WIDTH, SEARCH_HEIGHT, 6f, bgColor, 200);
        }
        
        if (searchFocusAnimation.getValue() > 0) {
            Color borderColor = new Color(theme.getAccentColor().getRed(), theme.getAccentColor().getGreen(), theme.getAccentColor().getBlue(), (int) (255 * searchFocusAnimation.getValue()));
            Render2D.drawBorder(context.getMatrices(), searchX, searchY, SEARCH_WIDTH, SEARCH_HEIGHT, 6f, 1f, 1f, borderColor);
        }
        
        float iconOffset = searchFocusAnimation.getValue() * 2f;
        Color iconColor = new Color((int) (120 + 80 * searchFocusAnimation.getValue()), (int) (120 + 80 * searchFocusAnimation.getValue()), (int) (120 + 80 * searchFocusAnimation.getValue()));
        Render2D.drawFont(context.getMatrices(), Fonts.ICONS.getFont(10f), "B", searchX + 7f + iconOffset, searchY + 5f, iconColor);
        
        float textX = searchX + 22f + iconOffset;
        String displayText = searchQuery.isEmpty() ? "Search modules..." : searchQuery;
        Color textColor = searchQuery.isEmpty() ? new Color(100, 100, 100) : theme.getTextColor();
        Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(9f), displayText, textX, searchY + 5.5f, textColor);
        
        if (searchFocused && System.currentTimeMillis() % 1000 < 500) {
            float cursorX = textX + Fonts.REGULAR.getWidth(searchQuery, 9f) + 1f;
            Render2D.drawRoundedRect(context.getMatrices(), cursorX, searchY + 5f, 1f, 10f, 0.5f, theme.getAccentColor());
        }
        
        if (!searchQuery.isEmpty()) {
            float clearX = searchX + SEARCH_WIDTH - 18f;
            float clearY = searchY + 5f;
            clearButtonHovered = MathUtils.isHovered(clearX - 2f, clearY - 2f, 14f, 14f, mouseX, mouseY);
            Color clearColor = clearButtonHovered ? new Color(255, 100, 100, (int) (200 + 55 * clearButtonAnimation.getValue())) : new Color(150, 150, 150);
            float clearScale = 1f + 0.1f * clearButtonAnimation.getValue();
            Render2D.drawFont(context.getMatrices(), Fonts.ICONS.getFont(9f * clearScale), "C", clearX, clearY, clearColor);
        }
    }
    
    private void renderDescription(DrawContext context) {
        hoverAnimation.update(!description.isEmpty());
        if (!description.isEmpty()) {
            UI uiModule = MotherHack.getInstance().getModuleManager().getModule(UI.class);
            UI.ClickGuiTheme theme = getTheme();
            
            // Get blur and alpha settings
            boolean enableBlur = uiModule != null && uiModule.isEnableBlur();
            float blurRadius = uiModule != null ? uiModule.getBlurRadius() : 20f;
            float blurAlpha = uiModule != null ? uiModule.getBlurAlpha() : 150f;
            float bgAlpha = uiModule != null ? uiModule.getBackgroundAlpha() : 180f;
            
            float width = Fonts.MEDIUM.getWidth(description, 9f);
            float x = mc.getWindow().getScaledWidth() / 2f - width / 2f;
            float y = mc.getWindow().getScaledHeight() / 2f + 145f;
            float descWidth = width + 16f;
            float descHeight = 16f;
            
            int bgAlphaInt = (int) (bgAlpha * hoverAnimation.getValue());
            Color bgColor = new Color(theme.getBackgroundColor().getRed(), theme.getBackgroundColor().getGreen(), theme.getBackgroundColor().getBlue(), bgAlphaInt);
            
            // Render with blur if enabled
            if (enableBlur) {
                int blurAlphaInt = (int) (blurAlpha * hoverAnimation.getValue());
                Render2D.drawBlurredRect(context.getMatrices(), x - 8f, y - 2f, descWidth, descHeight, 4f, blurRadius, new Color(255, 255, 255, blurAlphaInt));
                Render2D.drawRoundedRect(context.getMatrices(), x - 8f, y - 2f, descWidth, descHeight, 4f, bgColor);
            } else {
                Render2D.drawStyledRect(context.getMatrices(), x - 8f, y - 2f, descWidth, descHeight, 4f, bgColor, bgAlphaInt);
            }
            
            Render2D.drawFont(context.getMatrices(), Fonts.MEDIUM.getFont(9f), description, x, y + 1f, new Color(255, 255, 255, (int) (255 * hoverAnimation.getValue())));
            description = "";
        }
    }
    
    private float getSearchBarX() { return mc.getWindow().getScaledWidth() / 2f - SEARCH_WIDTH / 2f; }
    private float getSearchBarY() { return mc.getWindow().getScaledHeight() / 2f - 155f; }
    public boolean isSearchActive() { return !searchQuery.isEmpty(); }
    
    public void resetSearch() {
        setSearchQuery("");
        searchFocused = false;
        for (Panel panel : panels) panel.resetSearchState();
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (close) return false;
        
        float searchX = getSearchBarX();
        float searchY = getSearchBarY();
        
        if (!searchQuery.isEmpty()) {
            float clearX = searchX + SEARCH_WIDTH - 18f;
            float clearY = searchY + 5f;
            if (MathUtils.isHovered(clearX - 2f, clearY - 2f, 14f, 14f, (float) mouseX, (float) mouseY)) {
                setSearchQuery("");
                for (Panel panel : panels) panel.resetSearchState();
                return true;
            }
        }
        
        if (MathUtils.isHovered(searchX, searchY, SEARCH_WIDTH, SEARCH_HEIGHT, (float) mouseX, (float) mouseY)) {
            searchFocused = true;
            return true;
        } else {
            searchFocused = false;
        }
        
        for (Panel panel : panels) panel.mouseClicked(mouseX, mouseY, button);
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (close) return false;
        for (Panel panel : panels) panel.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (close) return false;
        for (Panel panel : panels) panel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (close) return false;
        
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (!searchQuery.isEmpty()) {
                    setSearchQuery("");
                    for (Panel panel : panels) panel.resetSearchState();
                } else searchFocused = false;
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                setSearchQuery(searchQuery.substring(0, searchQuery.length() - 1));
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_V && Screen.hasControlDown()) {
                String clipboard = GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
                if (clipboard != null) setSearchQuery(searchQuery + clipboard);
                return true;
            }
            return true;
        }
        
        for (Panel panel : panels) panel.keyPressed(keyCode, scanCode, modifiers);
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (close) return false;
        for (Panel panel : panels) panel.keyReleased(keyCode, scanCode, modifiers);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (close) return false;
        if (searchFocused) {
            setSearchQuery(searchQuery + chr);
            return true;
        }
        for (Panel panel : panels) panel.charTyped(chr, modifiers);
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void init() {
        openAnimation.update(true);
        close = false;
        resetSearch();
        GuiSoundHelper.playOpenSound();
    }

    @Override
    public void tick() {
        UI uiModule = MotherHack.getInstance().getModuleManager().getModule(UI.class);
        float panelWidth = uiModule != null ? uiModule.getPanelWidth() : 110f;
        float panelSpacing = uiModule != null ? uiModule.getPanelSpacing() : 4f;
        
        float x = (mc.getWindow().getScaledWidth() / 2f) - (panelWidth * ((Category.values().length - 1) / 2f)) - (panelSpacing * 1.5f);
        float y = (mc.getWindow().getScaledHeight() / 2f) - 115f;
        for (Panel panel : panels) {
            panel.setX(x);
            panel.setY(y);
            panel.setWidth(panelWidth);
            x += panelWidth + panelSpacing;
        }
    }

    @Override
    public void close() {
        if (!close) {
            close = true;
            closeAnimation.reset();
            resetSearch();
            GuiSoundHelper.playCloseSound();
        }
    }

    @Override
    public boolean shouldPause() { return false; }
}
