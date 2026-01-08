package fun.motherhack.hud.impl;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.hud.HudElement;
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

/**
 * Potions HUD element - красивый стиль с blur и анимациями
 */
public class Potions extends HudElement {

    private final NumberSetting fontSize = new NumberSetting("Font Size", 8f, 4f, 16f, 0.5f);
    private final NumberSetting backgroundAlpha = new NumberSetting("Background Alpha", 80f, 50f, 255f, 5f);

    // Анимация появления
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

        List<StatusEffectInstance> potions = getActivePotions();
        boolean isInChatScreen = mc.currentScreen instanceof ChatScreen;
        boolean shouldBeVisible = !potions.isEmpty() || (potions.isEmpty() && isInChatScreen);

        // Анимация появления/исчезновения
        if (shouldBeVisible != isVisible) {
            isVisible = shouldBeVisible;
            alphaAnimation.update(isVisible);
        }
        alphaAnimation.update(isVisible);

        float alpha = alphaAnimation.getValue();
        if (alpha < 0.01f && !isVisible) return;

        float x = getX();
        float y = getY();
        float padding = 3f;
        float currentFontSize = fontSize.getValue();
        Font headerFont = Fonts.SEMIBOLD;
        Font bodyFont = Fonts.REGULAR;

        float headerHeight = currentFontSize + 2f + padding * 2;
        int bgAlpha = (int) (backgroundAlpha.getValue() * alpha);
        int textAlpha = (int) (255 * alpha);
        Color textColor = new Color(255, 255, 255, textAlpha);
        Color levelColor = new Color(235, 85, 105, textAlpha);

        // Расчёт ширины
        String headerText = "Active Potions";
        float headerTextWidth = headerFont.getWidth(headerText, currentFontSize + 2f);
        float maxWidth = headerTextWidth + 30f;

        if (potions.isEmpty() && isInChatScreen) {
            // Превью
            String potionName = "Preview";
            String level = "10";
            String duration = "**:**";
            float lineWidth = bodyFont.getWidth(potionName, currentFontSize) + bodyFont.getWidth(level, currentFontSize) + bodyFont.getWidth(duration, currentFontSize) + 35;
            maxWidth = Math.max(maxWidth, lineWidth);
        } else {
            for (StatusEffectInstance potion : potions) {
                String potionName = potion.getEffectType().value().getName().getString();
                int level = potion.getAmplifier() + 1;
                String duration = formatDuration(potion.getDuration());
                float lineWidth = bodyFont.getWidth(potionName, currentFontSize) + bodyFont.getWidth("" + level, currentFontSize) + bodyFont.getWidth(duration, currentFontSize) + 30;
                maxWidth = Math.max(maxWidth, lineWidth);
            }
        }

        float width = maxWidth;

        e.getContext().getMatrices().push();
        e.getContext().getMatrices().translate(x + width / 2, y + headerHeight / 2, 0f);
        e.getContext().getMatrices().scale(toggledAnimation.getValue(), toggledAnimation.getValue(), 0);
        e.getContext().getMatrices().translate(-(x + width / 2), -(y + headerHeight / 2), 0f);

        // Заголовок с blur
        Render2D.drawBlurredRect(e.getContext().getMatrices(), x, y, width, headerHeight, 5f, 10f, new Color(255, 255, 255, (int)(bgAlpha * 0.3f)));
        Render2D.drawRoundedRect(e.getContext().getMatrices(), x, y, width, headerHeight + 1, 5f, new Color(0, 0, 0, bgAlpha));

        // Иконка и текст заголовка
        Render2D.drawFont(e.getContext().getMatrices(), Fonts.ICONS.getFont(currentFontSize + 2f), "E", x + 4, y + padding + 0.5f, textColor);
        Render2D.drawFont(e.getContext().getMatrices(), headerFont.getFont(currentFontSize), headerText, x + 20, y + padding + 0.5f, textColor);

        float currentY = y + headerHeight + padding + 0.7f;
        float rowHeight = currentFontSize + padding * 2 - 1;

        if (potions.isEmpty() && isInChatScreen) {
            // Превью
            String potionName = "Preview";
            String level = "10";
            String duration = "**:**";

            Render2D.drawBlurredRect(e.getContext().getMatrices(), x, currentY - padding, width, rowHeight, 5f, 10f, new Color(255, 255, 255, (int)(bgAlpha * 0.3f)));
            Render2D.drawRoundedRect(e.getContext().getMatrices(), x, currentY - padding, width, rowHeight, 5f, new Color(0, 0, 0, bgAlpha));
            Render2D.drawRoundedRect(e.getContext().getMatrices(), x + 15, currentY - padding + 1.5f, 1, 11, 1f, new Color(255, 255, 255, textAlpha));

            float textOffsetX = x + padding + 16;
            Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), potionName, textOffsetX, currentY, textColor);
            textOffsetX += bodyFont.getWidth(potionName, currentFontSize) + 4;
            Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), level, textOffsetX, currentY, levelColor);

            float durationWidth = bodyFont.getWidth(duration, currentFontSize);
            float durationX = x + width - durationWidth - padding;
            Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), duration, durationX, currentY + 0.5f, textColor);

            currentY += rowHeight + 2;
        } else {
            // Реальные зелья
            for (StatusEffectInstance potion : potions) {
                String potionName = potion.getEffectType().value().getName().getString();
                int level = potion.getAmplifier() + 1;
                String duration = formatDuration(potion.getDuration());

                Render2D.drawBlurredRect(e.getContext().getMatrices(), x, currentY - padding, width, rowHeight, 5f, 10f, new Color(255, 255, 255, (int)(bgAlpha * 0.3f)));
                Render2D.drawRoundedRect(e.getContext().getMatrices(), x, currentY - padding, width, rowHeight, 5f, new Color(0, 0, 0, bgAlpha));
                Render2D.drawRoundedRect(e.getContext().getMatrices(), x + 15, currentY - padding + 1.5f, 1, 11, 1f, new Color(255, 255, 255, textAlpha));

                float textOffsetX = x + padding + 16;
                Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), potionName, textOffsetX, currentY, textColor);
                textOffsetX += bodyFont.getWidth(potionName, currentFontSize) + 4;
                Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), "" + level, textOffsetX, currentY, levelColor);

                float durationWidth = bodyFont.getWidth(duration, currentFontSize);
                float durationX = x + width - durationWidth - padding;
                Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), duration, durationX, currentY - 0.2f, textColor);

                currentY += rowHeight + 2;
            }
        }

        e.getContext().getMatrices().pop();

        float totalHeight = currentY - y + padding;
        setBounds(getX(), getY(), width, totalHeight);
        super.onRender2D(e);
    }
}
