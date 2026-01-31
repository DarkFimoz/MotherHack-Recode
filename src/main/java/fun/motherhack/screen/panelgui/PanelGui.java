package fun.motherhack.screen.panelgui;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.screen.panelgui.components.ModuleComponent;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static fun.motherhack.utils.Wrapper.mc;

public class PanelGui extends Screen {

    private float x, y;
    private final float width = 820f;
    private final float height = 295f;
    private final Map<Category, List<ModuleComponent>> columns = new EnumMap<>(Category.class);
    private final SearchComponent search = new SearchComponent();
    private final Animation alphaAnimation = new Animation(200, 1f, false, Easing.SMOOTH_STEP);
    private final float SCROLL_SPEED = 15f;
    private final float SCROLLBAR_WIDTH = 2.0f;

    private final Map<Category, Float> targetScrollOffsets = new EnumMap<>(Category.class);
    private final Map<Category, Float> currentScrollOffsets = new EnumMap<>(Category.class);
    private final float SCROLL_LERP_FACTOR = 0.1f;

    private boolean closing = false;

    public PanelGui() {
        super(Text.of("panelgui"));
        rebuildColumns();
    }

    private void rebuildColumns() {
        columns.clear();
        targetScrollOffsets.clear();
        currentScrollOffsets.clear();
        List<Module> modules = MotherHack.getInstance().getModuleManager().getModules();
        Category[] categories = {Category.Combat, Category.Movement, Category.Render, Category.Misc, Category.Client};
        for (Category c : categories) {
            List<ModuleComponent> list = modules.stream()
                    .filter(m -> m.getCategory() == c)
                    .map(ModuleComponent::new)
                    .collect(Collectors.toList());
            columns.put(c, list);
            targetScrollOffsets.put(c, 0f);
            currentScrollOffsets.put(c, 0f);
        }
    }

    @Override
    protected void init() {
        super.init();
        this.x = (mc.getWindow().getScaledWidth() / 2f) - (width / 2f);
        this.y = (mc.getWindow().getScaledHeight() / 2f) - (height / 2f);
        search.setX(x + (width / 2f) - 150f);
        search.setY(y + height + 8f);
        alphaAnimation.update(true);
        closing = false;
        for (Category c : targetScrollOffsets.keySet()) {
            targetScrollOffsets.put(c, 0f);
            currentScrollOffsets.put(c, 0f);
        }
    }

    private void updateSmoothScrolling(float partialTicks) {
        for (Category c : columns.keySet()) {
            float target = targetScrollOffsets.getOrDefault(c, 0f);
            float current = currentScrollOffsets.getOrDefault(c, 0f);

            if (Math.abs(target - current) < 0.01f) {
                current = target;
            } else {
                current += (target - current) * SCROLL_LERP_FACTOR * (1.0f - partialTicks);
            }
            currentScrollOffsets.put(c, current);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateSmoothScrolling(delta);
        alphaAnimation.update(!closing);

        float currentAlpha = alphaAnimation.getValue();
        if (currentAlpha <= 0.01f && closing) {
            mc.setScreen(null);
            return;
        }

        this.x = (mc.getWindow().getScaledWidth() / 2f) - (width / 2f);
        this.y = (mc.getWindow().getScaledHeight() / 2f) - (height / 2f);

        int panelAlpha = (int) (currentAlpha * 230);
        int headerAlpha = (int) (currentAlpha * 255);
        int scrollbarColor = new Color(255, 255, 255, (int)(currentAlpha * 120)).getRGB();

        // Затемнение фона
        int bgAlpha = (int) (currentAlpha * 200);
        Render2D.drawRoundedRect(context.getMatrices(), 0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), 0f, new Color(0, 0, 0, bgAlpha));

        Category[] cats = {Category.Combat, Category.Movement, Category.Render, Category.Misc, Category.Client};
        float gutter = 5f;
        float colWidth = (width - gutter * (cats.length - 1) - 220) / cats.length;
        float startX = x + 20f;
        float headerY = y - 10f;

        for (int i = 0; i < cats.length; i++) {
            Category c = cats[i];
            float colX = startX + i * (colWidth + gutter);
            float listY = headerY + 22f;
            float listHeight = height - 70f;
            float contentStart = listY + 20f;
            float scrollAreaX1 = colX + 60f;
            float scrollAreaY1 = listY - 6f;
            float scrollAreaX2 = colX + 60f + colWidth;
            float scrollAreaY2 = listY - 6f + listHeight + 32f;
            float visibleModuleHeight = scrollAreaY2 - contentStart;

            // Фон колонки с blur
            Render2D.drawBlurredRect(context.getMatrices(), scrollAreaX1, scrollAreaY1, colWidth, listHeight + 32f, 6f, 10f, new Color(255, 255, 255, (int)(panelAlpha * 0.3f)));
            Render2D.drawRoundedRect(context.getMatrices(), scrollAreaX1, scrollAreaY1, colWidth, listHeight + 32f, 6f, new Color(20, 16, 22, panelAlpha));

            // Заголовок категории
            Render2D.drawFont(context.getMatrices(), Fonts.SEMIBOLD.getFont(11f), c.name(), colX + 115f - Fonts.SEMIBOLD.getWidth(c.name(), 11f) / 2f, headerY + 28, new Color(220, 220, 220, headerAlpha));

            List<ModuleComponent> comps = columns.getOrDefault(c, Collections.emptyList());
            float contentTotalHeight = 0;
            for (ModuleComponent comp : comps) {
                contentTotalHeight += comp.getAnimatedHeight() + 2;
            }

            float scrollOffset = currentScrollOffsets.getOrDefault(c, 0f);

            // Scissor для модулей
            Render2D.startScissor(context, scrollAreaX1, contentStart - 4, colWidth, scrollAreaY2 - contentStart + 4);
            float curY = contentStart + scrollOffset;
            for (ModuleComponent comp : comps) {
                comp.setX(colX + 60f);
                comp.setY(curY);
                comp.setWidth(colWidth);
                comp.draw(context, mouseX, mouseY, delta, currentAlpha);
                curY += comp.getAnimatedHeight() + 2;
            }
            Render2D.stopScissor(context);

            // Scrollbar
            float maxScroll = Math.max(0, contentTotalHeight - visibleModuleHeight);
            if (maxScroll > 0) {
                float trackHeight = visibleModuleHeight;
                float thumbHeight = Math.max(15.0f, trackHeight * (visibleModuleHeight / contentTotalHeight));
                float scrollPercentage = scrollOffset == 0 ? 0 : -scrollOffset / maxScroll;
                float thumbY = contentStart + (trackHeight - thumbHeight) * scrollPercentage;
                float scrollX = scrollAreaX2 - SCROLLBAR_WIDTH - 2f;

                Render2D.drawRoundedRect(context.getMatrices(), scrollX, thumbY, SCROLLBAR_WIDTH, thumbHeight, 1.5f, new Color(scrollbarColor));
            }
        }

        // Поиск
        search.setX(x + (width / 2f) - (search.getWidth() / 2f));
        search.setY(y + height + 8f);
        search.draw(context, mouseX, mouseY, delta, currentAlpha);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalDelta, double verticalDelta) {
        if (closing) return false;

        float listY_start = y - 10f + 22f - 6f;
        float listY_end = listY_start + height - 70f + 32f;

        if (mouseY >= listY_start && mouseY <= listY_end) {
            Category[] cats = {Category.Combat, Category.Movement, Category.Render, Category.Misc, Category.Client};
            float gutter = 5f;
            float colWidth = (width - gutter * (cats.length - 1) - 220) / cats.length;
            float startX = x + 20f;

            for (int i = 0; i < cats.length; i++) {
                Category c = cats[i];
                float colX_start = startX + i * (colWidth + gutter) + 60f;
                float colX_end = colX_start + colWidth;

                if (mouseX >= colX_start && mouseX <= colX_end) {
                    float targetOffset = targetScrollOffsets.getOrDefault(c, 0f);
                    targetOffset += (float) verticalDelta * SCROLL_SPEED;

                    float contentTotalHeight = 0;
                    for (ModuleComponent comp : columns.getOrDefault(c, Collections.emptyList())) {
                        contentTotalHeight += comp.getAnimatedHeight() + 2;
                    }
                    float contentStart = y - 10f + 22f + 20f;
                    float scrollAreaY2 = y - 10f + 22f - 6f + height - 70f + 32f;
                    float visibleModuleHeight = scrollAreaY2 - contentStart;
                    float maxScroll = Math.max(0, contentTotalHeight - visibleModuleHeight);

                    targetOffset = MathHelper.clamp(targetOffset, -maxScroll, 0);
                    targetScrollOffsets.put(c, targetOffset);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mxD, double myD, int button) {
        if (closing) return false;
        if (search.mouseClicked((float) mxD, (float) myD, button)) return true;
        for (List<ModuleComponent> list : columns.values()) {
            for (ModuleComponent comp : list) {
                if (comp.mouseClicked((float) mxD, (float) myD, button)) return true;
            }
        }
        return super.mouseClicked(mxD, myD, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (closing) return false;
        for (List<ModuleComponent> list : columns.values()) {
            for (ModuleComponent comp : list) {
                comp.mouseDragged(mouseX, mouseY, button);
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mxD, double myD, int button) {
        if (closing) return false;
        float mx = (float) mxD;
        float my = (float) myD;
        for (List<ModuleComponent> list : columns.values()) {
            for (ModuleComponent comp : list) {
                comp.mouseReleased(mx, my, button);
            }
        }
        search.mouseReleased(mx, my, button);
        return super.mouseReleased(mxD, myD, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closing = true;
            return true;
        }
        if (closing) return false;
        if (search.keyPressed(keyCode, scanCode, modifiers)) return true;
        for (List<ModuleComponent> list : columns.values()) {
            for (ModuleComponent comp : list) {
                if (comp.keyPressed(keyCode, scanCode, modifiers)) return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (closing) return false;
        if (search.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void close() {
        closing = true;
    }
}
