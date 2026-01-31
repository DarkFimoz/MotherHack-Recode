package fun.motherhack.hud.impl;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.hud.HudElement;
import fun.motherhack.modules.impl.combat.Aura;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.animations.infinity.InfinityAnimation;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.network.Server;
import fun.motherhack.utils.render.ColorUtils;
import fun.motherhack.utils.render.fonts.Fonts;
import fun.motherhack.utils.render.Render2D;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.awt.Color;
import java.util.List;

public class TargetHud extends HudElement {
	
    private final NumberSetting backgroundAlpha = new NumberSetting("Background Alpha", 80f, 10f, 100f, 5f);
    private final NumberSetting textAlpha = new NumberSetting("Text Alpha", 80f, 10f, 100f, 5f);
    private final BooleanSetting killSounds = new BooleanSetting("Kill Sounds", true);

    public TargetHud() {
        super("TargetHud");
        getSettings().add(backgroundAlpha);
        getSettings().add(textAlpha);
        getSettings().add(killSounds);
        // Set default position to bottom center
        getPosition().getValue().setX(0.42f);
        getPosition().getValue().setY(0.75f);
    }

    private final InfinityAnimation healthAnimation = new InfinityAnimation(Easing.LINEAR);
    private final InfinityAnimation gappleAnimation = new InfinityAnimation(Easing.LINEAR);
    
    // Kill animation
    private String killText = "";
    private long killAnimationStart = 0;
    private boolean wasTargetAlive = true;
    private LivingEntity lastTarget = null;
    private boolean killSoundPlayed = false;
    private static final long ANIMATION_DURATION = 900; // 9 frames * 100ms

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;

        UI.ClickGuiTheme theme = MotherHack.getInstance().getModuleManager().getModule(UI.class).getTheme();

        Aura aura = MotherHack.getInstance().getModuleManager().getModule(Aura.class);
        LivingEntity target = mc.currentScreen instanceof ChatScreen ? mc.player : aura.getTarget();
        
        // Check for kill - исправленная система отслеживания
        if (target != null) {
            // Если это новая цель
            if (lastTarget != target) {
                // Проверяем, была ли предыдущая цель убита
                if (lastTarget != null && wasTargetAlive && !killSoundPlayed && lastTarget.getHealth() <= 0) {
                    long currentTime = System.currentTimeMillis();
                    if (killAnimationStart == 0 || (currentTime - killAnimationStart) > ANIMATION_DURATION) {
                        killAnimationStart = currentTime;
                        killSoundPlayed = true;
                        if (killSounds.getValue()) {
                            fun.motherhack.utils.sound.KillSoundHelper.playRandomKillSound();
                        }
                    }
                }
                lastTarget = target;
                wasTargetAlive = target.getHealth() > 0;
                killSoundPlayed = false; // Сбрасываем флаг для новой цели
            } else {
                // Та же цель - проверяем смерть только один раз
                if (wasTargetAlive && !killSoundPlayed && target.getHealth() <= 0) {
                    long currentTime = System.currentTimeMillis();
                    if (killAnimationStart == 0 || (currentTime - killAnimationStart) > ANIMATION_DURATION) {
                        killAnimationStart = currentTime;
                        killSoundPlayed = true;
                        if (killSounds.getValue()) {
                            fun.motherhack.utils.sound.KillSoundHelper.playRandomKillSound();
                        }
                    }
                    wasTargetAlive = false;
                }
            }
        } else {
            // Нет цели - проверяем, была ли предыдущая цель убита (только один раз)
            if (lastTarget != null && wasTargetAlive && !killSoundPlayed && lastTarget.getHealth() <= 0) {
                long currentTime = System.currentTimeMillis();
                if (killAnimationStart == 0 || (currentTime - killAnimationStart) > ANIMATION_DURATION) {
                    killAnimationStart = currentTime;
                    killSoundPlayed = true;
                    if (killSounds.getValue()) {
                        fun.motherhack.utils.sound.KillSoundHelper.playRandomKillSound();
                    }
                }
            }
        }
        
        // Проверяем активность анимации убийства
        boolean isKillAnimationActive = killAnimationStart > 0 && 
            (System.currentTimeMillis() - killAnimationStart) < ANIMATION_DURATION;
        
        // Если нет цели и анимация не активна - выходим
        if (target == null && !isKillAnimationActive) {
            lastTarget = null;
            wasTargetAlive = true;
            return;
        }
        
        // Если анимация активна, но нет цели - используем последнюю цель
        if (target == null && isKillAnimationActive) {
            target = lastTarget;
        }
        
        if (target == null) return;
        float posX = getX();
        float posY = getY();

        float width = 140f;
        float height = 50f;
        float fontSize = 9f;
        float headSize = 25f;
        float padding = 5f;

        float hp = MathUtils.round(Server.getHealth(target, false));
        float maxHp = MathUtils.round(target.getMaxHealth());
        float gappleHp = MathUtils.round(target.getAbsorptionAmount());
        float healthPercent = hp / maxHp;
        float gapplePercent = gappleHp / maxHp;
        float healthWidth = healthAnimation.animate((width - padding * 2) * healthPercent, 200);
        float gappleWidth = gappleAnimation.animate((width - padding * 2) * gapplePercent, 200);
        float barWidth = (width - padding * 2);
        e.getContext().getMatrices().push();
        e.getContext().getMatrices().translate(posX + width / 2, posY + height / 2, 0f);
        e.getContext().getMatrices().scale(toggledAnimation.getValue(), toggledAnimation.getValue(), 0);
        e.getContext().getMatrices().translate(-(posX + width / 2), -(posY + height / 2), 0f);
        if (target instanceof PlayerEntity player) {
            float offset = 0f;
            List<ItemStack> armor = player.getInventory().armor;
            for (ItemStack stack : new ItemStack[]{armor.get(3), armor.get(2), armor.get(1), armor.get(0), player.getOffHandStack(), player.getMainHandStack()}) {
                if (stack.isEmpty()) continue;
                e.getContext().getMatrices().push();
                e.getContext().getMatrices().scale(0.75f, 0.75f, 0.75f);
                e.getContext().drawItem(stack, (int) ((posX + width - offset - padding * 2.75f) / 0.75f), (int) ((posY - padding * 2.75f) / 0.75f));
                e.getContext().getMatrices().pop();
                offset += 12f;
            }
        }

        Render2D.startScissor(e.getContext(), posX, posY, width, height);
        Color bgColor = new Color(theme.getBackgroundColor().getRed(), theme.getBackgroundColor().getGreen(), theme.getBackgroundColor().getBlue(), backgroundAlpha.getValue().intValue());
        Render2D.drawStyledRect(
                e.getContext().getMatrices(),
                posX,
                posY,
                width,
                height,
                3.5f,
                bgColor,
                255
        );

        float headX = posX + padding;
        float headY = posY + padding;

        if (target instanceof PlayerEntity)
            Render2D.drawTexture(
                    e.getContext().getMatrices(),
                    headX,
                    headY,
                    headSize,
                    headSize,
                    2f,
                    0.125f,
                    0.125f,
                    0.125f,
                    0.125f,
                    ((AbstractClientPlayerEntity) target).getSkinTextures().texture(),
                    Color.WHITE
            );
        else {
            Render2D.drawRoundedRect(
                    e.getContext().getMatrices(),
                    headX,
                    headY,
                    headSize,
                    headSize,
                    2f,
                    new Color(20, 20, 20)
            );

            Render2D.drawFont(
                    e.getContext().getMatrices(),
                    Fonts.BOLD.getFont(9f),
                    "?",
                    headX + (headSize / 2f) - Fonts.BOLD.getWidth("?", 9f) / 2f,
                    headY + (headSize / 2f) - Fonts.BOLD.getHeight(9f) / 2f,
                    Color.RED
            );
        }

        if (!target.getName().getString().isEmpty()) {
            Color nameColor = new Color(theme.getTextColor().getRed(), theme.getTextColor().getGreen(), theme.getTextColor().getBlue(), textAlpha.getValue().intValue());
            Render2D.drawFont(e.getContext().getMatrices(),
                    Fonts.MEDIUM.getFont(fontSize),
                    target.getName().getString(),
                    headX + headSize + padding,
                       posY + padding * 2f - padding / 2f,
                    nameColor
            );
        }

        Color hpColor = new Color(theme.getTextColor().getRed(), theme.getTextColor().getGreen(), theme.getTextColor().getBlue(), textAlpha.getValue().intValue());
        Render2D.drawFont(e.getContext().getMatrices(),
                Fonts.MEDIUM.getFont(fontSize),
                "HP: " + hp + (gappleHp > 0 ? " (%s)".formatted(gappleHp) : ""),
                headX + headSize + padding,
                posY + height - 20f - padding * 2,
                hpColor
        );

        Render2D.drawRoundedRect(e.getContext().getMatrices(),
                posX + padding,
                  posY + height - 10f - padding,
                barWidth,
                10f,
                2f,
                new Color(theme.getBackgroundColor().getRed(), theme.getBackgroundColor().getGreen(), theme.getBackgroundColor().getBlue(), 100)
        );

        Render2D.drawRoundedRect(e.getContext().getMatrices(),
                posX + padding,
                posY + height - 10f - padding,
                healthWidth,
                10f,
                2f,
                ColorUtils.getGlobalColor()
        );

        Render2D.drawRoundedRect(e.getContext().getMatrices(),
                posX + padding,
                posY + height - 10f - padding,
                gappleWidth,
                10f,
                2f,
                new Color(255, 200, 0)
        );

        Render2D.stopScissor(e.getContext());
        
        // Render kill animation
        if (killAnimationStart > 0) {
            long elapsed = System.currentTimeMillis() - killAnimationStart;
            long frameDuration = 100; // 100ms per frame
            int totalFrames = 9;
            long totalDuration = frameDuration * totalFrames;
            
            if (elapsed < totalDuration) {
                int frame = (int) (elapsed / frameDuration);
                String[] frames = {"Ez", "Ezz", "Ezz", "Ezzz", "Ezzzz", "Ezzzzz", "Ezzzz", "Ezzz", "Ezz"};
                killText = frames[Math.min(frame, frames.length - 1)];
                
                float killTextX = posX + width / 2f - Fonts.BOLD.getWidth(killText, 14f) / 2f;
                float killTextY = posY - 20f;
                
                Render2D.drawFont(
                    e.getContext().getMatrices(),
                    Fonts.BOLD.getFont(14f),
                    killText,
                    killTextX,
                    killTextY,
                    ColorUtils.getGlobalColor()
                );
            } else {
                killAnimationStart = 0;
                killText = "";
            }
        }
        
        e.getContext().getMatrices().pop();
        setBounds(getX(), getY(), width, height);
        super.onRender2D(e);
    }
}