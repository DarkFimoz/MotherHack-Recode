package fun.motherhack.screen.panelgui.components;

import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.Setting;
import fun.motherhack.modules.settings.api.Bind;
import fun.motherhack.modules.settings.impl.*;
import fun.motherhack.screen.panelgui.components.settings.*;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static fun.motherhack.utils.Wrapper.mc;

/**
 * ModuleComponent - компонент модуля для PanelGui
 */
public class ModuleComponent {

    private final Module module;
    private final List<SettingComponent<?>> settingComponents = new ArrayList<>();
    private float x, y, width;
    private final float height = 18f;
    private boolean binding = false;

    private boolean expanded = false;
    private float animatedHeight = height;

    public ModuleComponent(Module m) {
        this.module = m;
        List<Setting<?>> settings = m.getSettings();
        for (Setting<?> setting : settings) {
            if (setting instanceof BooleanSetting bs) {
                settingComponents.add(new BooleanComponent(bs));
            } else if (setting instanceof NumberSetting ns) {
                settingComponents.add(new NumberComponent(ns));
            } else if (setting instanceof EnumSetting<?> es) {
                settingComponents.add(new ModeComponent(es));
            } else if (setting instanceof ListSetting ls) {
                settingComponents.add(new ModeListComponent(ls));
            }
        }
    }

    public void draw(DrawContext context, float mouseX, float mouseY, float partialTicks, float parentAlpha) {
        float targetHeight = getTargetHeight();
        animatedHeight += (targetHeight - animatedHeight) * 0.1f * (1.0f - partialTicks);
        if (Math.abs(targetHeight - animatedHeight) < 0.1f) {
            animatedHeight = targetHeight;
        }

        int rectAlpha = (int) (parentAlpha * 170);
        int borderAlpha = (int) (parentAlpha * 45);
        int textAlpha = (int) (parentAlpha * 255);

        float animFactor = Math.min(1.0f, (animatedHeight - height) / (targetHeight - height));
        if (targetHeight == height) {
            animFactor = 0.0f;
        }

        float settingsAlpha = parentAlpha * animFactor;

        Color baseBg = new Color(28, 22, 28, rectAlpha);
        Color toggledBg = new Color(60, 36, 60, rectAlpha);
        Color borderColor = new Color(255, 255, 255, borderAlpha);
        Color textColor = new Color(220, 220, 220, textAlpha);
        Color dotsColor = new Color(180, 180, 180, textAlpha);
        Color bg = module.isToggled() ? toggledBg : baseBg;

        // Рамка и фон
        Render2D.drawBorder(context.getMatrices(), x + 6, y, width - 12, animatedHeight, 4f, 0.1f, 0.1f, borderColor);
        Render2D.drawBlurredRect(context.getMatrices(), x + 6, y, width - 12, animatedHeight, 4f, 10f, new Color(255, 255, 255, (int)(rectAlpha * 0.3f)));
        Render2D.drawRoundedRect(context.getMatrices(), x + 6, y, width - 12, animatedHeight, 4f, bg);

        // Название модуля
        Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(8f), module.getName(), x + 10f, y + 4f, textColor);

        // Точки если есть настройки
        if (!settingComponents.isEmpty()) {
            Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(10f), "...", x + 100, y + 6f, dotsColor);
        }

        // Настройки
        if (animatedHeight > height + 1) {
            float settingY = y + height - 2;
            for (SettingComponent<?> comp : settingComponents) {
                comp.setX(x);
                comp.setY(settingY);
                comp.setWidth(width);
                comp.draw(context, mouseX, mouseY, partialTicks, settingsAlpha);
                settingY += comp.getHeight();
            }
        }

        // Режим биндинга
        if (binding) {
            int overlayAlpha = (int) (parentAlpha * 120);
            Render2D.drawRoundedRect(context.getMatrices(), 0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), 0f, new Color(0, 0, 0, overlayAlpha));
            String text = "Press any key to bind...";
            float textWidth = Fonts.SEMIBOLD.getWidth(text, 12f);
            float centerX = (mc.getWindow().getScaledWidth() - textWidth) / 2f;
            float centerY = (mc.getWindow().getScaledHeight() - 12f) / 2f;

            Color bindTextColor = new Color(255, 255, 255, textAlpha);
            Render2D.drawFont(context.getMatrices(), Fonts.SEMIBOLD.getFont(12f), text, centerX, centerY, bindTextColor);
        }
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        boolean hovered = isHovered(mouseX, mouseY, x, y, width, height);

        if (binding) return true;

        if (hovered) {
            if (button == 0) {
                module.toggle();
                return true;
            } else if (button == 1) {
                if (!settingComponents.isEmpty()) {
                    expanded = !expanded;
                }
                return true;
            } else if (button == 2) {
                binding = true;
                return true;
            }
        }

        if (expanded) {
            for (SettingComponent<?> comp : settingComponents) {
                if (comp.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void mouseDragged(double mouseX, double mouseY, int button) {
        if (expanded) {
            for (SettingComponent<?> comp : settingComponents) {
                comp.mouseDragged(mouseX, mouseY, button);
            }
        }
    }

    public boolean mouseReleased(float mouseX, float mouseY, int button) {
        if (expanded) {
            for (SettingComponent<?> comp : settingComponents) {
                comp.mouseReleased(mouseX, mouseY, button);
            }
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            module.setBind(new Bind(keyCode, false));
            binding = false;
            return true;
        }
        return false;
    }

    private float getTargetHeight() {
        float totalHeight = height;
        if (expanded) {
            for (SettingComponent<?> comp : settingComponents) {
                totalHeight += comp.getHeight();
            }
        }
        return totalHeight;
    }

    private boolean isHovered(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public Module getModule() {
        return module;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setWidth(float w) {
        this.width = w;
    }

    public float getAnimatedHeight() {
        return animatedHeight;
    }
}
