package fun.motherhack.screen.csgui.elements;

import lombok.Getter;
import lombok.Setter;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Bind;
import fun.motherhack.modules.settings.impl.*;
import fun.motherhack.modules.settings.Setting;
import fun.motherhack.screen.clickgui.components.impl.*;
import fun.motherhack.screen.csgui.components.Component;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Fonts;
import fun.motherhack.utils.sound.GuiSoundHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ModuleElement extends Component {

    private final Module module;
    @Getter private boolean open, binding;
    private final Animation toggleAnimation = new Animation(300, 1f, true, Easing.BOTH_SINE);
    private final Animation descriptionScrollAnimation = new Animation(2000, 0f, false, Easing.LINEAR);
    private final Animation expandAnimation = new Animation(200, 1f, false, Easing.EASE_OUT_CUBIC);
    private float animatedHeight = 30f;
    public Module getModule() { return module; }
    @Setter
    private String description = "";
    @Getter
    private final List<fun.motherhack.screen.clickgui.components.Component> components = new ArrayList<>();

    public ModuleElement(Module module) {
        super(module.getName());
        this.module = module;

        // генерируем элементы настроек
        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting) components.add(new BooleanComponent((BooleanSetting) setting));
            else if (setting instanceof NumberSetting) components.add(new SliderComponent((NumberSetting) setting));
            else if (setting instanceof EnumSetting) components.add(new EnumComponent((EnumSetting<?>) setting));
            else if (setting instanceof StringSetting) components.add(new StringComponent((StringSetting) setting));
            else if (setting instanceof ListSetting) components.add(new ListComponent((ListSetting) setting));
            else if (setting instanceof BindSetting) components.add(new BindComponent((BindSetting) setting));
        }
    }


    public float getTotalHeight() {
        float height = 30;
        if (open) {
            for (int i = 0; i < components.size(); i++) {
                fun.motherhack.screen.clickgui.components.Component comp = components.get(i);
                if (!comp.getVisible().get()) continue;
                height += comp.getHeight();

                boolean hasNextVisible = false;
                for (int j = i + 1; j < components.size(); j++) {
                    if (components.get(j).getVisible().get()) {
                        hasNextVisible = true;
                        break;
                    }
                }
                if (hasNextVisible) height += 2;
            }
        }
        return height;
    }
    
    public float getAnimatedHeight() {
        return animatedHeight;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Обновляем анимацию раскрытия
        expandAnimation.update(open);
        
        // Плавная анимация высоты
        float targetHeight = getTotalHeight();
        animatedHeight += (targetHeight - animatedHeight) * 0.2f;
        if (Math.abs(targetHeight - animatedHeight) < 0.5f) {
            animatedHeight = targetHeight;
        }

        Render2D.drawStyledRect(context.getMatrices(),
                x, y, width, animatedHeight,
                10,
                new Color(0x6F000000, true),
                255);


        Render2D.drawFont(
                context.getMatrices(),
                Fonts.MEDIUM.getFont(9f),
                module.getName(),
                x + 10, y + 5,
                new Color(255, 255, 255, 255)
        );

        if (!module.getSettings().isEmpty()) {
            Render2D.drawFont(
                    context.getMatrices(),
                    Fonts.ICONS.getFont(7f),
                    "A",
                    x + 10 + Fonts.MEDIUM.getFont(9f).getWidth(module.getName()) + 2, // +2 для небольшого отступа
                    y + 7,
                    new Color(200, 200, 200, 255) // чуть светлее, чтобы выделять
            );
        }


        String descriptionText = I18n.translate(module.getDescription());
        float availableWidth = width - 20; // Available width for text
        float textWidth = Fonts.MEDIUM.getWidth(descriptionText, 7f);
        
        boolean isHovered = fun.motherhack.utils.math.MathUtils.isHovered(x, y, width, 30, (float) mouseX, (float) mouseY);

        if (textWidth > availableWidth) {
            // Text is too long, implement scrolling on hover
            descriptionScrollAnimation.update(isHovered);
            float scrollOffset = descriptionScrollAnimation.getValue() * (textWidth - availableWidth + 20);

            Render2D.startScissor(context, x + 10, y + 16.5f - 2, availableWidth, 10);
            Render2D.drawFont(context.getMatrices(), Fonts.MEDIUM.getFont(7f), descriptionText, x + 10 - scrollOffset, y + 16.5f, new Color(131, 131, 131, 255));
            Render2D.stopScissor(context);

            // Reset animation when it reaches the end
            if (descriptionScrollAnimation.getValue() >= 1.0f) {
                descriptionScrollAnimation.setValue(0f);
            }
        } else {
            // Text fits, render normally
            Render2D.drawFont(context.getMatrices(), Fonts.MEDIUM.getFont(7f), descriptionText, x + 10, y + 16.5f, new Color(131, 131, 131, 255));
        }


        toggleAnimation.update(module.isToggled());
        Render2D.drawRoundedRect(context.getMatrices(), x + width - 30, y + 9.5f, 18f * toggleAnimation.getValue(), 10f, 4f, new Color(176, 115, 255,(int) (255 * toggleAnimation.getLinear())));
        Render2D.drawRoundedRect(context.getMatrices(), x + width - 31 + (9f * toggleAnimation.getValue()), y + 9.5f, 19f * toggleAnimation.getReversedValue(), 10f, 4, new Color(23, 23, 23, 100));
        Render2D.drawRoundedRect(context.getMatrices(), x + width - 30 + (9f * toggleAnimation.getValue()), y + 10.5f, 8f, 8f, 3f, Color.WHITE);

        // Рендерим настройки с scissor для плавного появления
        if (animatedHeight > 30.5f) {
            float offsetY = y + 30;
            
            // Scissor обрезает контент по анимированной высоте
            Render2D.startScissor(context, (int)x, (int)(y + 30), (int)(x + width), (int)(y + animatedHeight));
            
            for (int i = 0; i < components.size(); i++) {
                fun.motherhack.screen.clickgui.components.Component comp = components.get(i);
                if (!comp.getVisible().get()) continue;

                comp.setX(x + 5);
                comp.setY(offsetY);
                comp.setWidth(width - 10);
                comp.setHeight(20);

                // Рендерим только если компонент попадает в видимую область
                if (offsetY < y + animatedHeight) {
                    comp.render(context, mouseX, mouseY, delta);
                }

                boolean hasNextVisible = false;
                for (int j = i + 1; j < components.size(); j++) {
                    if (components.get(j).getVisible().get()) {
                        hasNextVisible = true;
                        break;
                    }
                }
                if (hasNextVisible) offsetY += comp.getHeight() + 2;
                else offsetY += comp.getHeight();
            }
            
            Render2D.stopScissor(context);
        }
    }



    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (MathUtils.isHovered(x, y, width, 30, (float) mouseX, (float) mouseY)) {
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

        if (open) {
            for (fun.motherhack.screen.clickgui.components.Component comp : components) comp.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (open) for (fun.motherhack.screen.clickgui.components.Component comp : components) comp.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) module.setBind(new Bind(-1, false));
            else module.setBind(new Bind(keyCode, false));
            binding = false;
        }

        if (open) for (fun.motherhack.screen.clickgui.components.Component comp : components) comp.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void keyReleased(int keyCode, int scanCode, int modifiers) {
        if (open) for (fun.motherhack.screen.clickgui.components.Component comp : components) comp.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        if (open) for (fun.motherhack.screen.clickgui.components.Component comp : components) comp.charTyped(chr, modifiers);
    }
}