package fun.motherhack.screen.clickgui;

import lombok.Getter;
import lombok.Setter;
import fun.motherhack.MotherHack;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.screen.clickgui.components.impl.ModuleComponent;
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
import java.util.*;
import java.util.List;

public class ClickGui extends Screen implements Wrapper {

    private final List<Panel> panels = new ArrayList<>();
    private final Map<Category, List<ModuleComponent>> columns = new EnumMap<>(Category.class);
    private final Map<Category, Float> targetScrollOffsets = new EnumMap<>(Category.class);
    private final Map<Category, Float> currentScrollOffsets = new EnumMap<>(Category.class);
    
    // Кэшированные значения размеров (применяются при открытии)
    private float cachedColumnWidth = 160f;
    private float cachedColumnHeight = 400f;
    private float cachedColumnY = 40f;
    
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
    private final float SCROLL_SPEED = 15f;
    private final float SCROLL_LERP_FACTOR = 0.15f;
    
    public void setSearchQuery(String query) {
        if (!query.equals(this.searchQuery)) {
            this.searchQuery = query;
            // Сбрасываем скролл при изменении поиска в Columns стиле
            if (getStyle() == UI.ClickGuiStyle.Columns) {
                resetColumnsScroll();
            }
        }
    }
    
    private void resetColumnsScroll() {
        for (Category c : columns.keySet()) {
            targetScrollOffsets.put(c, 0f);
            currentScrollOffsets.put(c, 0f);
        }
    }

    public ClickGui() {
        super(Text.of("motherhack-clickgui"));
        for (Category category : MotherHack.getInstance().getModuleManager().getCategories()) {
            if (category == Category.Hud) continue;
            panels.add(new Panel(-999, -999, 110, 20, category));
        }
        rebuildColumns();
    }
    
    private void rebuildColumns() {
        columns.clear();
        List<Module> modules = MotherHack.getInstance().getModuleManager().getModules();
        Category[] categories = {Category.Combat, Category.Movement, Category.Render, Category.Misc, Category.Client};
        for (Category c : categories) {
            List<ModuleComponent> list = new ArrayList<>();
            for (Module m : modules) {
                if (m.getCategory() == c) list.add(new ModuleComponent(m));
            }
            list.sort(Comparator.comparing(ModuleComponent::getName));
            columns.put(c, list);
            targetScrollOffsets.put(c, 0f);
            currentScrollOffsets.put(c, 0f);
        }
    }

    public UI.ClickGuiTheme getTheme() {
        return MotherHack.getInstance().getModuleManager().getModule(UI.class).getTheme();
    }
    
    private UI.ClickGuiStyle getStyle() {
        return MotherHack.getInstance().getModuleManager().getModule(UI.class).getStyle();
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
        
        if (getStyle() == UI.ClickGuiStyle.Columns) {
            renderColumnsStyle(context, mouseX, mouseY, delta);
        } else {
            renderClassicStyle(context, mouseX, mouseY, delta);
        }
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

    private void renderColumnsStyle(DrawContext context, int mouseX, int mouseY, float delta) {
        updateSmoothScrolling();
        UI uiModule = MotherHack.getInstance().getModuleManager().getModule(UI.class);
        UI.ClickGuiTheme theme = getTheme();
        boolean enableBlur = uiModule.isEnableBlur();
        float blurRadius = uiModule.getBlurRadius();
        float blurAlpha = uiModule.getBlurAlpha();
        float bgAlpha = uiModule.getBackgroundAlpha();
        
        // Используем кэшированные значения
        float colWidth = cachedColumnWidth;
        float colHeight = cachedColumnHeight;
        float startYPos = cachedColumnY;
        
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        Category[] cats = {Category.Combat, Category.Movement, Category.Render, Category.Misc, Category.Client};
        float gutter = 6f;
        float totalWidth = colWidth * cats.length + gutter * (cats.length - 1);
        float startX = (screenWidth - totalWidth) / 2f;
        float headerHeight = 22f;
        float searchHeight = 24f;
        float searchY = startYPos - searchHeight - 6f;
        
        // Рендерим поисковик
        renderColumnsSearchBar(context, mouseX, mouseY, startX, searchY, totalWidth, searchHeight, theme, enableBlur, blurRadius, blurAlpha, bgAlpha);
        
        float listHeight = Math.min(colHeight, screenHeight - startYPos - headerHeight - 60f);

        for (int i = 0; i < cats.length; i++) {
            Category c = cats[i];
            float colX = startX + i * (colWidth + gutter);
            float headerY = startYPos;
            float listY = headerY + headerHeight + 3f;
            float contentStart = listY + 3f;

            // Header
            if (enableBlur) {
                Render2D.drawBlurredRect(context.getMatrices(), colX, headerY, colWidth, headerHeight, 5f, blurRadius, new Color(255, 255, 255, (int)blurAlpha));
                Color transparentBg = new Color(
                    theme.getBackgroundColor().getRed(),
                    theme.getBackgroundColor().getGreen(),
                    theme.getBackgroundColor().getBlue(),
                    (int)bgAlpha
                );
                Render2D.drawRoundedRect(context.getMatrices(), colX, headerY, colWidth, headerHeight, 5f, transparentBg);
            } else {
                Render2D.drawRoundedRect(context.getMatrices(), colX, headerY, colWidth, headerHeight, 5f, theme.getBackgroundColor());
            }
            
            String catName = c.name();
            float nameWidth = Fonts.SEMIBOLD.getWidth(catName, 9f);
            Render2D.drawFont(context.getMatrices(), Fonts.SEMIBOLD.getFont(9f), catName, colX + colWidth / 2f - nameWidth / 2f, headerY + 6f, theme.getTextColor());

            // Module list background
            if (enableBlur) {
                Render2D.drawBlurredRect(context.getMatrices(), colX, listY, colWidth, listHeight, 5f, blurRadius, new Color(255, 255, 255, (int)blurAlpha));
                Color transparentBg = new Color(
                    theme.getBackgroundColor().getRed(),
                    theme.getBackgroundColor().getGreen(),
                    theme.getBackgroundColor().getBlue(),
                    (int)bgAlpha
                );
                Render2D.drawRoundedRect(context.getMatrices(), colX, listY, colWidth, listHeight, 5f, transparentBg);
            } else {
                Render2D.drawRoundedRect(context.getMatrices(), colX, listY, colWidth, listHeight, 5f, theme.getBackgroundColor());
            }

            List<ModuleComponent> comps = columns.getOrDefault(c, Collections.emptyList());
            float scrollOffset = currentScrollOffsets.getOrDefault(c, 0f);

            // Scissor - используем context.enableScissor
            context.enableScissor((int)colX, (int)listY, (int)(colX + colWidth), (int)(listY + listHeight));
            
            float curY = contentStart + scrollOffset;
            for (ModuleComponent comp : comps) {
                // Фильтрация по поиску
                if (!searchQuery.isEmpty() && !comp.getModule().getName().toLowerCase().contains(searchQuery.toLowerCase())) {
                    continue;
                }
                
                comp.setX(colX + 3f);
                comp.setY(curY);
                comp.setWidth(colWidth - 6f);
                comp.setHeight(18f);
                comp.render(context, mouseX, mouseY, delta);
                curY += comp.getHeight() + 2;
                
                // Render settings if module is open
                if (comp.getOpenAnimation().getValue() > 0f) {
                    float settingsHeight = 0;
                    for (fun.motherhack.screen.clickgui.components.Component sub : comp.getComponents()) {
                        if (!sub.getVisible().get()) continue;
                        settingsHeight += sub.getHeight() + sub.getAddHeight().get();
                    }
                    
                    float animatedSettingsHeight = settingsHeight * comp.getOpenAnimation().getValue();
                    float settingY = curY;
                    
                    for (fun.motherhack.screen.clickgui.components.Component sub : comp.getComponents()) {
                        if (!sub.getVisible().get()) continue;
                        sub.setX(colX + 5f);
                        sub.setY(settingY);
                        sub.setWidth(colWidth - 10f);
                        sub.setHeight(15f);
                        sub.render(context, mouseX, mouseY, delta);
                        settingY += (sub.getHeight() + sub.getAddHeight().get());
                    }
                    
                    curY += animatedSettingsHeight;
                }
            }
            
            context.disableScissor();
        }
        
        if (uiModule.isShowBackground()) {
            int imageSize = 150;
            Render2D.drawTexture(context.getMatrices(), 10, screenHeight - imageSize - 10, imageSize, imageSize, 10f, MotherHack.id("sexy.png"), Color.WHITE);
        }
    }
    
    private void updateSmoothScrolling() {
        for (Category c : columns.keySet()) {
            float target = targetScrollOffsets.getOrDefault(c, 0f);
            float current = currentScrollOffsets.getOrDefault(c, 0f);
            if (Math.abs(target - current) < 0.01f) current = target;
            else current += (target - current) * SCROLL_LERP_FACTOR;
            currentScrollOffsets.put(c, current);
        }
    }
    
    private void renderSearchBar(DrawContext context, int mouseX, int mouseY) {
        UI.ClickGuiTheme theme = getTheme();
        float searchX = getSearchBarX();
        float searchY = getSearchBarY();
        int bgAlpha = (int) (180 + 40 * searchFocusAnimation.getValue());
        Color bgColor = new Color(theme.getBackgroundColor().getRed(), theme.getBackgroundColor().getGreen(), theme.getBackgroundColor().getBlue(), bgAlpha);
        
        Render2D.drawStyledRect(context.getMatrices(), searchX, searchY, SEARCH_WIDTH, SEARCH_HEIGHT, 6f, bgColor, 200);
        
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
            UI.ClickGuiTheme theme = getTheme();
            float width = Fonts.MEDIUM.getWidth(description, 9f);
            float x = mc.getWindow().getScaledWidth() / 2f - width / 2f;
            float y = mc.getWindow().getScaledHeight() / 2f + 145f;
            Color bgColor = new Color(theme.getBackgroundColor().getRed(), theme.getBackgroundColor().getGreen(), theme.getBackgroundColor().getBlue(), (int) (200 * hoverAnimation.getValue()));
            Render2D.drawStyledRect(context.getMatrices(), x - 8f, y - 2f, width + 16f, 16f, 4f, bgColor, (int) (200 * hoverAnimation.getValue()));
            Render2D.drawFont(context.getMatrices(), Fonts.MEDIUM.getFont(9f), description, x, y + 1f, new Color(255, 255, 255, (int) (255 * hoverAnimation.getValue())));
            description = "";
        }
    }
    
    private void renderColumnsSearchBar(DrawContext context, int mouseX, int mouseY, float x, float y, float width, float height, UI.ClickGuiTheme theme, boolean enableBlur, float blurRadius, float blurAlpha, float bgAlpha) {
        int bgAlphaInt = (int) (bgAlpha + 40 * searchFocusAnimation.getValue());
        Color bgColor = new Color(theme.getBackgroundColor().getRed(), theme.getBackgroundColor().getGreen(), theme.getBackgroundColor().getBlue(), bgAlphaInt);
        
        if (enableBlur) {
            Render2D.drawBlurredRect(context.getMatrices(), x, y, width, height, 6f, blurRadius, new Color(255, 255, 255, (int)blurAlpha));
            Render2D.drawRoundedRect(context.getMatrices(), x, y, width, height, 6f, bgColor);
        } else {
            Render2D.drawRoundedRect(context.getMatrices(), x, y, width, height, 6f, bgColor);
        }
        
        if (searchFocusAnimation.getValue() > 0) {
            Color borderColor = new Color(theme.getAccentColor().getRed(), theme.getAccentColor().getGreen(), theme.getAccentColor().getBlue(), (int) (255 * searchFocusAnimation.getValue()));
            Render2D.drawBorder(context.getMatrices(), x, y, width, height, 6f, 1f, 1f, borderColor);
        }
        
        float iconOffset = searchFocusAnimation.getValue() * 2f;
        Color iconColor = new Color((int) (120 + 80 * searchFocusAnimation.getValue()), (int) (120 + 80 * searchFocusAnimation.getValue()), (int) (120 + 80 * searchFocusAnimation.getValue()));
        Render2D.drawFont(context.getMatrices(), Fonts.ICONS.getFont(10f), "B", x + 7f + iconOffset, y + 6f, iconColor);
        
        float textX = x + 22f + iconOffset;
        String displayText = searchQuery.isEmpty() ? "Search modules..." : searchQuery;
        Color textColor = searchQuery.isEmpty() ? new Color(100, 100, 100) : theme.getTextColor();
        Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(9f), displayText, textX, y + 7f, textColor);
        
        if (searchFocused && System.currentTimeMillis() % 1000 < 500) {
            float cursorX = textX + Fonts.REGULAR.getWidth(searchQuery, 9f) + 1f;
            Render2D.drawRoundedRect(context.getMatrices(), cursorX, y + 6f, 1f, 10f, 0.5f, theme.getAccentColor());
        }
        
        if (!searchQuery.isEmpty()) {
            float clearX = x + width - 18f;
            float clearY = y + 6f;
            clearButtonHovered = MathUtils.isHovered(clearX - 2f, clearY - 2f, 14f, 14f, mouseX, mouseY);
            Color clearColor = clearButtonHovered ? new Color(255, 100, 100, (int) (200 + 55 * clearButtonAnimation.getValue())) : new Color(150, 150, 150);
            float clearScale = 1f + 0.1f * clearButtonAnimation.getValue();
            Render2D.drawFont(context.getMatrices(), Fonts.ICONS.getFont(9f * clearScale), "C", clearX, clearY, clearColor);
        }
    }
    
    private float getSearchBarX() { return mc.getWindow().getScaledWidth() / 2f - SEARCH_WIDTH / 2f; }
    private float getSearchBarY() { return mc.getWindow().getScaledHeight() / 2f - 155f; }
    public boolean isSearchActive() { return !searchQuery.isEmpty(); }
    
    public void resetSearch() {
        setSearchQuery("");
        searchFocused = false;
        resetColumnsScroll();
        for (Panel panel : panels) panel.resetSearchState();
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (close) return false;
        
        if (getStyle() == UI.ClickGuiStyle.Classic) {
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
        } else {
            // Columns style
            int screenWidth = mc.getWindow().getScaledWidth();
            Category[] cats = {Category.Combat, Category.Movement, Category.Render, Category.Misc, Category.Client};
            float gutter = 6f;
            float totalWidth = cachedColumnWidth * cats.length + gutter * (cats.length - 1);
            float startX = (screenWidth - totalWidth) / 2f;
            float searchHeight = 24f;
            float searchY = cachedColumnY - searchHeight - 6f;
            
            // Проверка клика на кнопку очистки
            if (!searchQuery.isEmpty()) {
                float clearX = startX + totalWidth - 18f;
                float clearY = searchY + 6f;
                if (MathUtils.isHovered(clearX - 2f, clearY - 2f, 14f, 14f, (float) mouseX, (float) mouseY)) {
                    setSearchQuery("");
                    return true;
                }
            }
            
            // Проверка клика на поисковик
            if (MathUtils.isHovered(startX, searchY, totalWidth, searchHeight, (float) mouseX, (float) mouseY)) {
                searchFocused = true;
                return true;
            } else {
                searchFocused = false;
            }
            
            for (List<ModuleComponent> list : columns.values()) {
                for (ModuleComponent comp : list) {
                    // Фильтрация по поиску
                    if (!searchQuery.isEmpty() && !comp.getModule().getName().toLowerCase().contains(searchQuery.toLowerCase())) {
                        continue;
                    }
                    comp.mouseClicked(mouseX, mouseY, button);
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (close) return false;
        if (getStyle() == UI.ClickGuiStyle.Classic) {
            for (Panel panel : panels) panel.mouseReleased(mouseX, mouseY, button);
        } else {
            for (List<ModuleComponent> list : columns.values()) {
                for (ModuleComponent comp : list) {
                    // Фильтрация по поиску
                    if (!searchQuery.isEmpty() && !comp.getModule().getName().toLowerCase().contains(searchQuery.toLowerCase())) {
                        continue;
                    }
                    comp.mouseReleased(mouseX, mouseY, button);
                }
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (close) return false;
        if (getStyle() == UI.ClickGuiStyle.Classic) {
            for (Panel panel : panels) panel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        } else {
            Category[] cats = {Category.Combat, Category.Movement, Category.Render, Category.Misc, Category.Client};
            float gutter = 6f;
            int screenWidth = mc.getWindow().getScaledWidth();
            int screenHeight = mc.getWindow().getScaledHeight();
            
            float totalWidth = cachedColumnWidth * cats.length + gutter * (cats.length - 1);
            float startX = (screenWidth - totalWidth) / 2f;
            float headerHeight = 22f;
            float listHeight = Math.min(cachedColumnHeight, screenHeight - cachedColumnY - headerHeight - 60f);
            float listY = cachedColumnY + headerHeight + 3f;

            for (int i = 0; i < cats.length; i++) {
                Category c = cats[i];
                float colX_start = startX + i * (cachedColumnWidth + gutter);
                float colX_end = colX_start + cachedColumnWidth;

                if (mouseX >= colX_start && mouseX <= colX_end && mouseY >= listY && mouseY <= listY + listHeight) {
                    float targetOffset = targetScrollOffsets.getOrDefault(c, 0f);
                    targetOffset += verticalAmount * SCROLL_SPEED;
                    
                    // Считаем полную высоту контента включая настройки
                    float contentTotalHeight = 0;
                    for (ModuleComponent comp : columns.getOrDefault(c, Collections.emptyList())) {
                        // Фильтрация по поиску
                        if (!searchQuery.isEmpty() && !comp.getModule().getName().toLowerCase().contains(searchQuery.toLowerCase())) {
                            continue;
                        }
                        
                        contentTotalHeight += comp.getHeight() + 2;
                        if (comp.getOpenAnimation().getValue() > 0f) {
                            for (fun.motherhack.screen.clickgui.components.Component sub : comp.getComponents()) {
                                if (!sub.getVisible().get()) continue;
                                contentTotalHeight += (sub.getHeight() + sub.getAddHeight().get()) * comp.getOpenAnimation().getValue();
                            }
                        }
                    }
                    float maxScroll = Math.max(0, contentTotalHeight - listHeight + 6f);
                    targetOffset = Math.max(-maxScroll, Math.min(0, targetOffset));
                    targetScrollOffsets.put(c, targetOffset);
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (close) return false;
        
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (!searchQuery.isEmpty()) {
                    setSearchQuery("");
                    if (getStyle() == UI.ClickGuiStyle.Classic) {
                        for (Panel panel : panels) panel.resetSearchState();
                    }
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
        
        if (getStyle() == UI.ClickGuiStyle.Classic) {
            for (Panel panel : panels) panel.keyPressed(keyCode, scanCode, modifiers);
        } else {
            for (List<ModuleComponent> list : columns.values()) {
                for (ModuleComponent comp : list) {
                    // Фильтрация по поиску
                    if (!searchQuery.isEmpty() && !comp.getModule().getName().toLowerCase().contains(searchQuery.toLowerCase())) {
                        continue;
                    }
                    comp.keyPressed(keyCode, scanCode, modifiers);
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (close) return false;
        if (getStyle() == UI.ClickGuiStyle.Classic) {
            for (Panel panel : panels) panel.keyReleased(keyCode, scanCode, modifiers);
        } else {
            for (List<ModuleComponent> list : columns.values()) {
                for (ModuleComponent comp : list) {
                    // Фильтрация по поиску
                    if (!searchQuery.isEmpty() && !comp.getModule().getName().toLowerCase().contains(searchQuery.toLowerCase())) {
                        continue;
                    }
                    comp.keyReleased(keyCode, scanCode, modifiers);
                }
            }
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (close) return false;
        if (searchFocused) {
            setSearchQuery(searchQuery + chr);
            return true;
        }
        if (getStyle() == UI.ClickGuiStyle.Classic) {
            for (Panel panel : panels) panel.charTyped(chr, modifiers);
        } else {
            for (List<ModuleComponent> list : columns.values()) {
                for (ModuleComponent comp : list) {
                    // Фильтрация по поиску
                    if (!searchQuery.isEmpty() && !comp.getModule().getName().toLowerCase().contains(searchQuery.toLowerCase())) {
                        continue;
                    }
                    comp.charTyped(chr, modifiers);
                }
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void init() {
        openAnimation.update(true);
        close = false;
        resetSearch();
        GuiSoundHelper.playOpenSound();
        
        // Кэшируем размеры при открытии
        UI uiModule = MotherHack.getInstance().getModuleManager().getModule(UI.class);
        if (uiModule != null) {
            cachedColumnWidth = uiModule.getColumnWidth();
            cachedColumnHeight = uiModule.getColumnHeight();
            cachedColumnY = uiModule.getColumnY();
        } else {
            // Значения по умолчанию если модуль не найден
            cachedColumnWidth = 160f;
            cachedColumnHeight = 400f;
            cachedColumnY = 40f;
        }
    }

    @Override
    public void tick() {
        if (getStyle() == UI.ClickGuiStyle.Classic) {
            float x = (mc.getWindow().getScaledWidth() / 2f) - (110 * ((Category.values().length - 1) / 2f)) - (4f * 1.5f);
            float y = (mc.getWindow().getScaledHeight() / 2f) - 115f;
            for (Panel panel : panels) {
                panel.setX(x);
                panel.setY(y);
                x += 110f + 4f;
            }
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
