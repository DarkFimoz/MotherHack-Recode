package fun.motherhack.screen.csgui.elements;

import fun.motherhack.modules.api.Category;
import fun.motherhack.screen.csgui.components.Component;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;
import java.util.Map;
import java.util.List;


public class CategoryElement extends Component {

    private Category selectedCategory = Category.Combat;
    private Category previousCategory = Category.Combat;
    private final float buttonWidth = 85;
    private final float buttonHeight = 20;
    private final float buttonSpacing = 5;

    private final Map<Category, List<String>> modules = Map.of(
            Category.Combat, List.of("KillAura", "AutoClicker"),
            Category.Movement, List.of("Speed", "Fly"),
            Category.Misc, List.of("Spammer"),
            Category.Render, List.of("ESP", "Chams"),
            Category.Client, List.of("Configs"),
            Category.Hud, List.of("Watermark", "Media")
    );

    private final String name;

    public CategoryElement() {
        super("Category");
        this.name = "Category";
    }

    public CategoryElement(String name) {
        super(name);
        this.name = name;
    }

    public Category getSelectedCategory() {
        return selectedCategory;
    }

    public boolean categoryChanged() {
        if (selectedCategory != previousCategory) {
            previousCategory = selectedCategory;
            return true;
        }
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();
        float offsetY = y + 35f;

        Category[] categories = Category.values();

        for (int i = 0; i < categories.length; i++) {
            Category category = categories[i];

            float btnX = x + 2;
            float btnY = offsetY;
            float btnW = buttonWidth;
            float btnH = buttonHeight;

            boolean isLast = (i == categories.length - 1);

            if (isLast) {
                // включаем scissor для последней категории
                Render2D.startScissor(context, btnX, btnY, btnW, btnH - 25);
            }

            // фон
            if (category == selectedCategory) {
                Render2D.drawStyledRect(
                        matrices,
                        btnX, btnY, btnW, btnH,
                        5,
                        new Color(0x1DFFFFFF, true),
                        255
                );
            }

            // текст
            Render2D.drawFont(
                    matrices,
                    Fonts.MEDIUM.getFont(10f),
                    category.name(),
                    btnX + 8f,
                    btnY + (btnH / 2f - 6),
                    new Color(255, 255, 255, 255)
            );

            if (isLast) {
                // выключаем scissor
                Render2D.stopScissor(context);
            }

            offsetY += btnH + buttonSpacing;
        }
    }




    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return; // ЛКМ только

        Category[] categories = Category.values();
        float offsetY = y + 35f;

        for (int i = 0; i < categories.length; i++) {
            Category category = categories[i];
            float btnX = x + 2;
            float btnY = offsetY;
            float btnW = buttonWidth;
            float btnH = buttonHeight;

            boolean isLast = (i == categories.length - 1);

            // если последняя категория — пропускаем
            if (isLast) {
                offsetY += buttonHeight + buttonSpacing;
                continue;
            }

            if (mouseX >= btnX && mouseX <= btnX + btnW
                    && mouseY >= btnY && mouseY <= btnY + btnH) {
                selectedCategory = category;
                break;
            }

            offsetY += buttonHeight + buttonSpacing;
        }
    }


    // Остальные методы пустые
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void keyReleased(int keyCode, int scanCode, int modifiers) {}
    @Override public void charTyped(char chr, int modifiers) {}
}