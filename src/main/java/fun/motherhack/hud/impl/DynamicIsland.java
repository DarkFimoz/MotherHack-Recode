package fun.motherhack.hud.impl;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.hud.HudElement;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.animations.infinity.InfinityAnimation;
import fun.motherhack.utils.mediaplayer.MediaPlayer;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Fonts;
import meteordevelopment.orbit.EventHandler;

import java.awt.*;

public class DynamicIsland extends HudElement {
    private final Animation mediaAnimation = new Animation(300, 1f, false, Easing.BOTH_SINE);
    private final NumberSetting backgroundAlpha = new NumberSetting("Background Alpha", 80f, 10f, 100f, 5f);
    private final NumberSetting textAlpha = new NumberSetting("Text Alpha", 80f, 10f, 100f, 5f);
    
    private String lastTrack = "";
    private boolean wasPlaying = false;

    public DynamicIsland() {
        super("DynamicIsland");
        getSettings().add(backgroundAlpha);
        getSettings().add(textAlpha);
        // Set default position to top center
        getPosition().getValue().setX(0.45f);
        getPosition().getValue().setY(0.02f);
    }

    @EventHandler
    public void onTick(EventTick e) {
        try {
            if (Module.fullNullCheck() || MotherHack.getInstance().isPanic()) return;
            
            MediaPlayer mediaPlayer = MotherHack.getInstance().getMediaPlayer();
            if (mediaPlayer == null) {
                mediaAnimation.update(false);
                return;
            }
            
            mediaPlayer.onTick();
            
            boolean isPlaying = !mediaPlayer.fullNullCheck() && 
                              mediaPlayer.getTitle() != null && 
                              !mediaPlayer.getTitle().isEmpty();
            
            mediaAnimation.update(isPlaying);
            
            String currentTrack = (mediaPlayer.getTitle() != null ? mediaPlayer.getTitle() : "") + 
                                 (mediaPlayer.getArtist() != null && !mediaPlayer.getArtist().isEmpty() ? 
                                 " - " + mediaPlayer.getArtist() : "");
            
            if (isPlaying && !currentTrack.equals(lastTrack)) {
                lastTrack = currentTrack;
                System.out.println("[DynamicIsland] Сейчас играет: " + currentTrack);
                wasPlaying = true;
            } else if (!isPlaying && wasPlaying) {
                System.out.println("[DynamicIsland] Воспроизведение остановлено");
                lastTrack = "";
                wasPlaying = false;
            }
        } catch (Exception ex) {
            System.err.println("[DynamicIsland] Ошибка в onTick: " + ex.getMessage());
            mediaAnimation.update(false);
            wasPlaying = false;
        }
    }

    @Override
    public void onRender2D(EventRender2D e) {
        try {
            if (fullNullCheck() || closed()) return;

            MediaPlayer mediaPlayer = MotherHack.getInstance().getMediaPlayer();
            if (mediaPlayer == null || mediaPlayer.fullNullCheck()) {
                return;
            }

            UI.ClickGuiTheme theme = MotherHack.getInstance().getModuleManager().getModule(UI.class).getTheme();
            if (theme == null) {
                return;
            }

            String title = mediaPlayer.getTitle() != null ? mediaPlayer.getTitle() : "";
            String artist = mediaPlayer.getArtist() != null ? mediaPlayer.getArtist() : "";
            String track = title + (artist.isEmpty() ? "" : " - " + artist);
            
            mediaAnimation.update(true);
            
            float padding = 2f;
            float round = 6f;
            float imageSize = 15f;
            float textWidth = Fonts.BOLD.getWidth(track, 7f);
            float width = 15 + textWidth + padding * 2;
            float height = 15f;
            float x = getX();
            float y = getY();
            
            // Фон
            Color bgColor = new Color(
                theme.getBackgroundColor().getRed(), 
                theme.getBackgroundColor().getGreen(), 
                theme.getBackgroundColor().getBlue(), 
                backgroundAlpha.getValue().intValue()
            );
            
            Render2D.startScissor(e.getContext(), x, y, width + 1f, height);
            
            Render2D.drawStyledRect(
                e.getContext().getMatrices(),
                x, y, width, height, round, bgColor, 255
            );

            // Обложка трека (если есть)
            if (mediaPlayer.hasTexture()) {
                Render2D.drawTexture(
                    e.getContext().getMatrices(),
                    x + padding,
                    y + padding,
                    imageSize - padding * 2,
                    imageSize - padding * 2,
                    4f,
                    mediaPlayer.getTexture(),
                    Color.WHITE
                );
            }

            // Текст трека
            Color textColor = new Color(
                theme.getTextColor().getRed(), 
                theme.getTextColor().getGreen(), 
                theme.getTextColor().getBlue(), 
                textAlpha.getValue().intValue()
            );
            
            Render2D.drawFont(
                e.getContext().getMatrices(),
                Fonts.BOLD.getFont(7f),
                track,
                x + imageSize + padding,
                y - (padding / 2f) + (Fonts.BOLD.getHeight(7f) / 2f),
                textColor
            );

            Render2D.stopScissor(e.getContext());
            setBounds(x, y, width, height);
            super.onRender2D(e);
        } catch (Exception ex) {
            System.err.println("[DynamicIsland] Ошибка в onRender2D: " + ex.getMessage());
        }
    }
}