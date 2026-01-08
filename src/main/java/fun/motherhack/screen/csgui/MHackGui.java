package fun.motherhack.screen.csgui;

import lombok.Getter;
import lombok.Setter;
import fun.motherhack.MotherHack;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.screen.csgui.elements.CategoryElement;
import fun.motherhack.screen.csgui.elements.ModuleElement;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Fonts;
import fun.motherhack.utils.sound.GuiSoundHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static fun.motherhack.utils.Wrapper.mc;

@Setter
@Getter
public class MHackGui extends Screen {
    private Category selectedCategory = Category.Combat;
    private final CategoryElement categoryElement = new CategoryElement();
    private final List<ModuleElement> moduleElements = new ArrayList<>();
    private float scrollOffset = 0;
    private float scrollSpeed = 10f;
    private static MHackGui INSTANCE;

    // Draggable functionality
    private boolean guiDragging = false;
    private double dragX, dragY;
    private float guiX = -1, guiY = -1; // -1 means center (default)
    
    // Search functionality
    @Getter private String searchQuery = "";
    @Getter private boolean searchFocused = false;

    public float getGuiX() { return guiX; }
    public float getGuiY() { return guiY; }
    public void setGuiX(float guiX) { this.guiX = guiX; }
    public void setGuiY(float guiY) { this.guiY = guiY; }

    static {
        INSTANCE = new MHackGui();
    }

    public MHackGui() {
        super(Text.of("mhackgui"));
        for (Module module : MotherHack.getInstance().getModuleManager().getModules()) {
            moduleElements.add(new ModuleElement(module));
        }
    }

    public static MHackGui getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MHackGui();
        }
        return INSTANCE;
    }

    public static MHackGui getGui() {
        return getInstance();
    }

    public Category getSelectedCategory() {
        return selectedCategory;
    }
    
    private void resetSearch() {
        searchQuery = "";
        searchFocused = false;
        scrollOffset = 0;
    }
    
    @Override
    protected void init() {
        super.init();
        GuiSoundHelper.playOpenSound();
    }
    
    @Override
    public void close() {
        resetSearch();
        GuiSoundHelper.playCloseSound();
        super.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        int rectWidth = 480;
        int rectHeight = 285;

        // Use saved position or center as default
        int x = guiX >= 0 ? (int) guiX : (screenWidth / 2) - (rectWidth / 2);
        int y = guiY >= 0 ? (int) guiY : (screenHeight / 2) - (rectHeight / 2);

        // Get theme colors from UI module
        fun.motherhack.modules.impl.client.UI uiModule = MotherHack.getInstance().getModuleManager().getModule(fun.motherhack.modules.impl.client.UI.class);
        Color backgroundColor = uiModule != null ? uiModule.getTheme().getBackgroundColor() : new Color(0xFF3A3A3A, true);
        Color accentColor = uiModule != null ? uiModule.getTheme().getAccentColor() : new Color(255, 255, 255, 25);

        Render2D.drawStyledRect(context.getMatrices(),
                x, y, rectWidth, rectHeight, 15, backgroundColor, 255);


        Render2D.drawStyledRect(context.getMatrices(),
                x + 5, y + 5, 102, 275, 10, new Color(backgroundColor.getRed(), backgroundColor.getGreen(), backgroundColor.getBlue(), 113), 255);



        Render2D.drawFont(
                context.getMatrices(),
                Fonts.BOLD.getFont(30f),
                "MH",
                x + 20f,
                y + 13f,
                new Color(255, 255, 255)
        );


        categoryElement.setX(x + 10);
        categoryElement.setY(y + 20);
        categoryElement.setWidth(90);
        categoryElement.setHeight(250);
        categoryElement.render(context, mouseX, mouseY, delta);

        Category selected = categoryElement.getSelectedCategory();
        String selectedName = searchQuery.isEmpty() ? selected.name() : "Search: " + searchQuery;

        Render2D.drawFont(
                context.getMatrices(),
                Fonts.BOLD.getFont(20f),
                selectedName,
                x + 130f,
                y + 13f,
                new Color(255, 255, 255)
        );
        
        // Render search bar
        renderSearchBar(context, x, y, mouseX, mouseY);

        List<ModuleElement> filteredModules;
        if (!searchQuery.isEmpty()) {
            // Search across all modules
            filteredModules = moduleElements.stream()
                    .filter(m -> m.getModule().getName().toLowerCase().contains(searchQuery.toLowerCase()))
                    .toList();
        } else {
            filteredModules = moduleElements.stream()
                    .filter(m -> m.getModule().getCategory() == selected)
                    .toList();
        }

        float startX = x + 120; // Relative to GUI window
        float startY = y + 45;  // Relative to GUI window
        float moduleWidth = 160;
        float moduleHeight = 30;
        float spacingX = 10;
        float spacingY = 2;
        int col = 0;
        float[] columnYOffset = new float[2];

        // Calculate total height needed for all modules including expanded settings
        float totalHeight = 0;
        for (ModuleElement moduleElement : filteredModules) {
            totalHeight += moduleElement.getAnimatedHeight() + spacingY;
        }
        float scissorHeight = Math.min(totalHeight, rectHeight - 80); // Use available GUI height minus header/footer space
        Render2D.startScissor(context, startX - 5, startY - 5, 2 * moduleWidth + spacingX + 10, scissorHeight + 25);

        for (ModuleElement moduleElement : filteredModules) {
            int currentCol = col % 2;

            float actualY = startY + columnYOffset[currentCol] - scrollOffset;
            moduleElement.setX(startX + currentCol * (moduleWidth + spacingX));
            moduleElement.setY(actualY);
            moduleElement.setWidth(moduleWidth);
            moduleElement.setHeight(moduleHeight);
            moduleElement.render(context, mouseX, mouseY, delta);

            columnYOffset[currentCol] += moduleElement.getAnimatedHeight() + spacingY;

            col++;
        }

        Render2D.stopScissor(context);
        
        // Render background image
        renderBackgroundImage(context);
    }
    
    private void renderBackgroundImage(DrawContext context) {
        // Render background image in bottom left corner if enabled
        fun.motherhack.modules.impl.client.MHACKGUI mhackguiModule = MotherHack.getInstance().getModuleManager().getModule(fun.motherhack.modules.impl.client.MHACKGUI.class);
        if (mhackguiModule != null && mhackguiModule.isShowBackground()) {
            int screenHeight = mc.getWindow().getScaledHeight();
            int imageSize = 150; // Size of the image
            float imageX = 10; // 10px padding from left edge
            float imageY = screenHeight - imageSize - 10; // 10px padding from bottom edge
            
            Render2D.drawTexture(
                context.getMatrices(),
                imageX,
                imageY,
                imageSize,
                imageSize,
                10f, // radius for rounded corners
                MotherHack.id("sexy.png"),
                Color.WHITE
            );
        }
    }
    
    private void renderSearchBar(DrawContext context, int guiX, int guiY, int mouseX, int mouseY) {
        float searchWidth = 200f;
        float searchHeight = 16f;
        float searchX = guiX + 260f;
        float searchY = guiY + 13f;
        
        // Background
        Color bgColor = searchFocused ? new Color(60, 60, 60, 220) : new Color(40, 40, 40, 180);
        Render2D.drawRoundedRect(context.getMatrices(), searchX, searchY, searchWidth, searchHeight, 5f, bgColor);
        
        // Border when focused
        if (searchFocused) {
            Render2D.drawBorder(context.getMatrices(), searchX, searchY, searchWidth, searchHeight, 5f, 1f, 1f, new Color(176, 115, 255));
        }
        
        // Search icon
        Render2D.drawFont(context.getMatrices(), Fonts.ICONS.getFont(8f), "B", searchX + 5f, searchY + 4f, new Color(150, 150, 150));
        
        // Text or placeholder
        String displayText = searchQuery.isEmpty() ? "Search..." : searchQuery;
        Color textColor = searchQuery.isEmpty() ? new Color(100, 100, 100) : new Color(255, 255, 255);
        Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(8f), displayText, searchX + 18f, searchY + 4f, textColor);
        
        // Cursor
        if (searchFocused && System.currentTimeMillis() % 1000 < 500) {
            float cursorX = searchX + 18f + Fonts.REGULAR.getWidth(searchQuery, 8f);
            Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(8f), "|", cursorX, searchY + 3.5f, new Color(255, 255, 255));
        }
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        int rectWidth = 480;
        int rectHeight = 285;
        int x = guiX >= 0 ? (int) guiX : (screenWidth / 2) - (rectWidth / 2);
        int y = guiY >= 0 ? (int) guiY : (screenHeight / 2) - (rectHeight / 2);
        
        // Check search bar click
        float searchX = x + 260f;
        float searchY = y + 13f;
        if (mouseX >= searchX && mouseX <= searchX + 200f && mouseY >= searchY && mouseY <= searchY + 16f) {
            searchFocused = true;
            return true;
        } else {
            searchFocused = false;
        }

        // Check if clicking on the top bar for dragging
        if (button == 0 && mouseX >= x && mouseX <= x + rectWidth && mouseY >= y && mouseY <= y + 50) {
            guiDragging = true;
            dragX = mouseX - x;
            dragY = mouseY - y;
            return true;
        }

        categoryElement.mouseClicked(mouseX, mouseY, button);
        if (categoryElement.categoryChanged()) {
            scrollOffset = 0; // Reset scroll when category changes
            searchQuery = ""; // Clear search when changing category
        }
        Category selected = categoryElement.getSelectedCategory();

        for (ModuleElement moduleElement : moduleElements) {
            if (!searchQuery.isEmpty()) {
                // When searching, check all modules that match
                if (!moduleElement.getModule().getName().toLowerCase().contains(searchQuery.toLowerCase())) continue;
            } else {
                if (moduleElement.getModule().getCategory() != selected) continue;
            }
            moduleElement.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            guiDragging = false;
        }
        categoryElement.mouseReleased(mouseX, mouseY, button);
        for (ModuleElement moduleElement : moduleElements) {
            moduleElement.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (guiDragging) {
            guiX = (float) (mouseX - dragX);
            guiY = (float) (mouseY - dragY);

            // Keep GUI within screen bounds
            int screenWidth = mc.getWindow().getScaledWidth();
            int screenHeight = mc.getWindow().getScaledHeight();
            int rectWidth = 480;
            int rectHeight = 285;

            if (guiX < 0) guiX = 0;
            if (guiY < 0) guiY = 0;
            if (guiX + rectWidth > screenWidth) guiX = screenWidth - rectWidth;
            if (guiY + rectHeight > screenHeight) guiY = screenHeight - rectHeight;
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                searchQuery = "";
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                scrollOffset = 0;
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_V && Screen.hasControlDown()) {
                String clipboard = GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
                if (clipboard != null) {
                    searchQuery += clipboard;
                    scrollOffset = 0;
                }
                return true;
            }
            return true;
        }
        
        for (ModuleElement moduleElement : moduleElements) {
            moduleElement.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused) {
            searchQuery += chr;
            scrollOffset = 0;
            return true;
        }
        
        for (ModuleElement moduleElement : moduleElements) {
            moduleElement.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scrollOffset -= vertical * 10f;
        if (scrollOffset < 0) scrollOffset = 0;
        return true;
    }
}