package fun.motherhack.hud.impl;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.hud.HudElement;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Font;
import fun.motherhack.utils.render.fonts.Fonts;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Potions extends HudElement {

    private final NumberSetting fontSize = new NumberSetting("Font Size", 8f, 4f, 16f, 0.5f);
    private final NumberSetting backgroundAlpha = new NumberSetting("Background Alpha", 80f, 50f, 255f, 5f);

    private final Animation alphaAnimation = new Animation(300, 1f, false, Easing.SMOOTH_STEP);
    private boolean isVisible = false;

    public Potions() {
        super("Potions");
        getSettings().add(fontSize);
        getSettings().add(backgroundAlpha);
        getPosition().getValue().setX(0.01f);
        getPosition().getValue().setY(0.5f);
    }

    private List<StatusEffectInstance> getActivePotions() {
        List<StatusEffectInstance> potions = new ArrayList<>();
        if (mc.player != null) {
            potions.addAll(mc.player.getStatusEffects());
            potions.sort(Comparator.comparing(p -> p.getEffectType().value().getName().getString().toLowerCase()));
        }
        return potions;
    }

    private String formatDuration(int ticks) {
        int seconds = ticks / 20;
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d", m, s);
    }

    private String getRomanNumeral(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(number);
        };
    }

    @EventHandler
    public void onRender2DX2(EventRender2D e) {
        if (fullNullCheck()) return;
        BooleanSetting setting = MotherHack.getInstance().getHudManager().getElements().getName("Potions");
        if (setting != null) {
            toggledAnimation.update(setting.getValue());
        } else {
            toggledAnimation.update(true);
        }
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;
        if (Fonts.SEMIBOLD == null || Fonts.REGULAR == null || Fonts.ICONS == null) return;

        UI.ClickGuiTheme theme = MotherHack.getInstance().getModuleManager().getModule(UI.class).getTheme();

        List<StatusEffectInstance> potions = getActivePotions();
        boolean isInChatScreen = mc.currentScreen instanceof ChatScreen;
        boolean shouldBeVisible = !potions.isEmpty() || isInChatScreen;

        if (shouldBeVisible != isVisible) {
            isVisible = shouldBeVisible;
            alphaAnimation.update(isVisible);
        }
        alphaAnimation.update(isVisible);

        float alpha = alphaAnimation.getValue();
        if (alpha < 0.01f && !isVisible) return;

        float x = getX();
        float y = getY();
        float padding = 5f;
        float currentFontSize = fontSize.getValue();
        Font headerFont = Fonts.SEMIBOLD;
        Font bodyFont = Fonts.REGULAR;

        float headerHeight = currentFontSize + 4f + padding * 2;
        int bgAlpha = (int) (backgroundAlpha.getValue() * alpha);
        int textAlpha = (int) (255 * alpha);
        
        Color textColor = new Color(255, 255, 255, textAlpha);
        Color levelColor = new Color(255, 100, 120, textAlpha);
        Color durationColor = new Color(200, 200, 200, textAlpha);
        Color bgColor = new Color(theme.getBackgroundColor().getRed(), theme.getBackgroundColor().getGreen(), theme.getBackgroundColor().getBlue(), bgAlpha);
        Color blurColor = new Color(theme.getBackgroundColor().getRed(), theme.getBackgroundColor().getGreen(), theme.getBackgroundColor().getBlue(), (int)(bgAlpha * 0.3f));

        String headerText = "Potions";
        float headerTextWidth = headerFont.getWidth(headerText, currentFontSize + 1f);
        float maxWidth = headerTextWidth + 40f;

        if (potions.isEmpty() && isInChatScreen) {
            String potionName = "Speed";
            String level = "II";
            String duration = "5:00";
            float lineWidth = bodyFont.getWidth(potionName, currentFontSize) + bodyFont.getWidth(level, currentFontSize) + bodyFont.getWidth(duration, currentFontSize) + padding * 8;
            maxWidth = Math.max(maxWidth, lineWidth);
        } else {
            for (StatusEffectInstance potion : potions) {
                String potionName = potion.getEffectType().value().getName().getString();
                String level = getRomanNumeral(potion.getAmplifier() + 1);
                String duration = formatDuration(potion.getDuration());
                float lineWidth = bodyFont.getWidth(potionName, currentFontSize) + bodyFont.getWidth(level, currentFontSize) + bodyFont.getWidth(duration, currentFontSize) + padding * 8;
                maxWidth = Math.max(maxWidth, lineWidth);
            }
        }

        float width = maxWidth;

        e.getContext().getMatrices().push();
        e.getContext().getMatrices().translate(x + width / 2, y + headerHeight / 2, 0f);
        e.getContext().getMatrices().scale(toggledAnimation.getValue(), toggledAnimation.getValue(), 0);
        e.getContext().getMatrices().translate(-(x + width / 2), -(y + headerHeight / 2), 0f);

        Render2D.drawBlurredRect(e.getContext().getMatrices(), x, y, width, headerHeight, 6f, 12f, blurColor);
        Render2D.drawRoundedRect(e.getContext().getMatrices(), x, y, width, headerHeight, 6f, bgColor);

        Render2D.drawFont(e.getContext().getMatrices(), Fonts.ICONS.getFont(currentFontSize + 3f), "E", x + padding, y + padding + 1f, levelColor);
        Render2D.drawFont(e.getContext().getMatrices(), headerFont.getFont(currentFontSize + 1f), headerText, x + padding + 18, y + padding + 1f, textColor);

        float currentY = y + headerHeight + 3f;
        float rowHeight = currentFontSize + padding * 2;

        if (potions.isEmpty() && isInChatScreen) {
            String potionName = "Speed";
            String level = "II";
            String duration = "5:00";

            Render2D.drawBlurredRect(e.getContext().getMatrices(), x, currentY, width, rowHeight, 6f, 12f, blurColor);
            Render2D.drawRoundedRect(e.getContext().getMatrices(), x, currentY, width, rowHeight, 6f, bgColor);

            Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), potionName, x + padding, currentY + padding, textColor);
            
            float levelX = x + padding + bodyFont.getWidth(potionName, currentFontSize) + 4;
            Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), level, levelX, currentY + padding, levelColor);

            float durationWidth = bodyFont.getWidth(duration, currentFontSize);
            float durationX = x + width - durationWidth - padding;
            Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), duration, durationX, currentY + padding, durationColor);

            currentY += rowHeight + 3f;
        } else {
            for (StatusEffectInstance potion : potions) {
                String potionName = potion.getEffectType().value().getName().getString();
                String level = getRomanNumeral(potion.getAmplifier() + 1);
                String duration = formatDuration(potion.getDuration());

                Render2D.drawBlurredRect(e.getContext().getMatrices(), x, currentY, width, rowHeight, 6f, 12f, blurColor);
                Render2D.drawRoundedRect(e.getContext().getMatrices(), x, currentY, width, rowHeight, 6f, bgColor);

                Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), potionName, x + padding, currentY + padding, textColor);
                
                float levelX = x + padding + bodyFont.getWidth(potionName, currentFontSize) + 4;
                Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), level, levelX, currentY + padding, levelColor);

                float durationWidth = bodyFont.getWidth(duration, currentFontSize);
                float durationX = x + width - durationWidth - padding;
                Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), duration, durationX, currentY + padding, durationColor);

                currentY += rowHeight + 3f;
            }
        }

        e.getContext().getMatrices().pop();

        float totalHeight = currentY - y;
        setBounds(getX(), getY(), width, totalHeight);
        super.onRender2D(e);
    }
}
