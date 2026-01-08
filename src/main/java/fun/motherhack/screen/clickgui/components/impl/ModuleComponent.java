package fun.motherhack.screen.clickgui.components.impl;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.settings.impl.*;
import fun.motherhack.screen.clickgui.components.Component;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.Setting;
import fun.motherhack.modules.settings.api.Bind;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.render.fonts.Fonts;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.sound.GuiSoundHelper;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ModuleComponent extends Component {

    @Getter private final Module module;
    private boolean open, binding;
    @Getter private final List<Component> components = new ArrayList<>();
    private final Animation alphaAnimation = new Animation(300, 1f, false, Easing.BOTH_SINE);
    private final Animation toggleAnimation = new Animation(150, 1f, false, Easing.BOTH_SINE);
    private final Animation showingAnimation = new Animation(300, 1f, false, Easing.BOTH_SINE);
    @Getter private final Animation openAnimation = new Animation(300, 1f, false, Easing.BOTH_SINE);
    
    // Shake effect for search highlight
    @Getter private final Animation shakeAnimation = new Animation(400, 1f, false, Easing.BOTH_SINE);
    private long shakeStartTime = 0;
    private boolean shaking = false;

    public ModuleComponent(Module module) {
        super(module.getName());
        this.module = module;
        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting) components.add(new BooleanComponent((BooleanSetting) setting));
            else if (setting instanceof NumberSetting) components.add(new SliderComponent((NumberSetting) setting));
            else if (setting instanceof EnumSetting) components.add(new EnumComponent((EnumSetting<?>) setting));
            else if (setting instanceof StringSetting) components.add(new StringComponent((StringSetting) setting));
            else if (setting instanceof ListSetting) components.add(new ListComponent((ListSetting) setting));
            else if (setting instanceof BindSetting) components.add(new BindComponent((BindSetting) setting));
            else if (setting instanceof ButtonSetting) components.add(new ButtonComponent((ButtonSetting) setting));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        UI.ClickGuiTheme theme = MotherHack.getInstance().getClickGui().getTheme();
        boolean showing = Screen.hasAltDown() && !module.getBind().isEmpty();
        boolean hovered = MathUtils.isHovered(x, y, width, height, mouseX, mouseY);

        if (!showing) {
            if (hovered) alphaAnimation.update(true);
            else alphaAnimation.update(module.isToggled());
            toggleAnimation.update(module.isToggled());
        }

        openAnimation.update(open);
        showingAnimation.update(showing);
        
        // Calculate shake offset
        float shakeOffset = 0f;
        if (shaking) {
            long elapsed = System.currentTimeMillis() - shakeStartTime;
            if (elapsed < 400) {
                float progress = elapsed / 400f;
                float intensity = (1f - progress) * 3f;
                shakeOffset = (float) Math.sin(elapsed * 0.05) * intensity;
            } else {
                shaking = false;
            }
        }
        
        float renderX = x + shakeOffset;

        if (binding) Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(8f), "Key: " + module.getBind().toString().replace("_", " "), renderX + 7f, y + 5f, new Color(theme.getTextColor().getRed(), theme.getTextColor().getGreen(), theme.getTextColor().getBlue(), (int) (200 + (55 * alphaAnimation.getValue()))));
        else {
            Render2D.drawFont(context.getMatrices(), Fonts.ICONS.getFont(11f), "D", renderX + width - 18f, y + 5f, new Color(theme.getTextColor().getRed(), theme.getTextColor().getGreen(), theme.getTextColor().getBlue(), (int) (255 * toggleAnimation.getValue())));
            Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(8f), module.getName(), renderX + 7f + (3f * toggleAnimation.getValue()), y + 5f, showingAnimation.getValue() > 0.2f ? new Color(theme.getTextColor().getRed(), theme.getTextColor().getGreen(), theme.getTextColor().getBlue(), (int) (255 * showingAnimation.getReversedValue())) : new Color(theme.getTextColor().getRed(), theme.getTextColor().getGreen(), theme.getTextColor().getBlue(), (int) (200 + (55 * alphaAnimation.getValue()))));
        }

        Render2D.drawFont(context.getMatrices(),
                Fonts.REGULAR.getFont(8f),
                "Key: " + module.getBind().toString().replace("_", " "),
                renderX + 7f - ((Fonts.REGULAR.getWidth("Key: " + module.getBind().toString().replace("_", " "), 8f) + 10f) * showingAnimation.getReversedValue()),
                y + 5f,
                theme.getTextColor()
        );

        Render2D.drawStyledRect(context.getMatrices(), renderX, y + height - 1, width, 1, 1f, theme.getAccentColor(), 255);
        if (hovered) MotherHack.getInstance().getClickGui().setDescription(I18n.translate(module.getDescription()));
    }
    
    public void triggerShake() {
        shaking = true;
        shakeStartTime = System.currentTimeMillis();
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (MathUtils.isHovered(x, y, width, height, (float) mouseX, (float) mouseY) ) {
            if (button == 0 && !binding) {
                module.toggle();
            }
            else if (button == 1 && !components.isEmpty() && !binding) {
                open = !open;
                GuiSoundHelper.playExpandSound();
            }
            else if (button == 2 && !binding) {
                binding = true;
                return;
            }
        }

        if (binding) {
            module.setBind(new Bind(button, true));
            binding = false;
        }

        for (Component component : components) if (openAnimation.getValue() > 0) component.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        for (Component component : components) if (openAnimation.getValue() > 0) component.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) module.setBind(new Bind(-1, false));
            else module.setBind(new Bind(keyCode, false));
            binding = false;
        }

        for (Component component : components) if (openAnimation.getValue() > 0) component.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void keyReleased(int keyCode, int scanCode, int modifiers) {
        for (Component component : components) if (openAnimation.getValue() > 0) component.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        for (Component component : components) if (openAnimation.getValue() > 0) component.charTyped(chr, modifiers);
    }
    
    // Метод для нового ClickGui с поддержкой parentAlpha
    public void render(DrawContext context, int mouseX, int mouseY, float delta, float parentAlpha) {
        render(context, mouseX, mouseY, delta);
    }
    
    // Метод для получения анимированной высоты модуля
    public float getAnimatedHeight() {
        float baseHeight = 18f;
        if (!open) return baseHeight;
        
        float totalHeight = baseHeight;
        for (Component comp : components) {
            if (!comp.getVisible().get()) continue;
            totalHeight += comp.getHeight();
        }
        return baseHeight + (totalHeight - baseHeight) * openAnimation.getValue();
    }
}