package fun.motherhack.utils.mediaplayer;

import dev.redstones.mediaplayerinfo.*;
import fun.motherhack.utils.render.Render2D;
import lombok.*;
import net.minecraft.client.texture.AbstractTexture;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Getter 
@Setter
public class MediaPlayer {
    private static final boolean DISABLE_MEDIA_PLAYER = Boolean.getBoolean("mediaplayer.disable");
    private static final AtomicBoolean isInitialized = new AtomicBoolean(false);
    
    private BufferedImage image;
    private AbstractTexture texture;
    private String title = "", artist = "", owner = "", lastTitle = "";
    private long duration = 0, position = 0;
    private boolean changeTrack;
    private IMediaSession session;
    private List<IMediaSession> sessions = Collections.emptyList();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private volatile boolean isActive = true;
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 500; // 0.5 second between updates
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);
    
    // Кеширование для оптимизации
    private final AtomicReference<CompletableFuture<AbstractTexture>> pendingTextureUpdate = new AtomicReference<>();
    private BufferedImage cachedArtwork;
    private AbstractTexture cachedTexture;
    private String cachedTitle = "", cachedArtist = "";

    public void onTick() {
        if (DISABLE_MEDIA_PLAYER || !isActive) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL) {
            return;
        }
        
        // Пропускаем если уже идёт обновление
        if (!isUpdating.compareAndSet(false, true)) {
            return;
        }
        
        lastUpdateTime = currentTime;

        CompletableFuture.runAsync(() -> {
            try {
                if (!isActive) return;

                if (!isInitialized.get()) {
                    initializeMediaPlayer();
                    if (!isInitialized.get()) {
                        return; // Initialization failed
                    }
                }

                updateMediaSession();
            } catch (Throwable e) {
                handleError(e);
            } finally {
                isUpdating.set(false);
            }
        }, executor);
    }

    private void initializeMediaPlayer() {
        try {
            // Test if media player is available
            MediaPlayerInfo.Instance.getMediaSessions();
            isInitialized.set(true);
        } catch (Throwable e) {
            System.err.println("[MediaPlayer] Failed to initialize media player: " + e.getMessage());
            isActive = false;
        }
    }

    private void updateMediaSession() {
        try {
            sessions = MediaPlayerInfo.Instance.getMediaSessions();
            session = sessions.stream()
                    .filter(s -> s != null && 
                              (s.getMedia() != null) && 
                              (s.getMedia().getArtist() != null || s.getMedia().getTitle() != null) &&
                              (!s.getMedia().getArtist().isEmpty() || !s.getMedia().getTitle().isEmpty()))
                    .findFirst()
                    .orElse(null);

            if (session == null) {
                resetMediaState();
                return;
            }

            MediaInfo info = session.getMedia();
            if (info == null) {
                resetMediaState();
                return;
            }

            updateTrackInfo(info);
            updateArtwork(info);
            
        } catch (Throwable e) {
            handleError(e);
        }
    }

    private void updateTrackInfo(MediaInfo info) {
        String newTitle = info.getTitle() != null ? info.getTitle() : "";
        String newArtist = info.getArtist() != null ? info.getArtist() : "";
        
        if (!newTitle.equals(title) || !newArtist.equals(artist)) {
            title = newTitle;
            artist = newArtist;
            duration = info.getDuration();
            position = info.getPosition();
            owner = session.getOwner() != null ? session.getOwner() : "";
            changeTrack = true;
            lastTitle = title;
        }
    }

    private void updateArtwork(MediaInfo info) {
        try {
            if (changeTrack) {
                BufferedImage newImage = info.getArtwork();
                if (newImage != null) {
                    // Проверяем, нужно ли обновлять кеш
                    if (!newImage.equals(cachedArtwork) || !title.equals(cachedTitle) || !artist.equals(cachedArtist)) {
                        // Отменяем предыдущее обновление, если оно есть
                        CompletableFuture<AbstractTexture> pending = pendingTextureUpdate.get();
                        if (pending != null && !pending.isDone()) {
                            pending.cancel(true);
                        }
                        
                        // Асинхронно обновляем текстуру в отдельном потоке
                        CompletableFuture<AbstractTexture> textureFuture = CompletableFuture.supplyAsync(() -> {
                            try {
                                return Render2D.convert(newImage);
                            } catch (Exception e) {
                                System.err.println("[MediaPlayer] Error converting artwork: " + e.getMessage());
                                return null;
                            }
                        }, executor);
                        
                        pendingTextureUpdate.set(textureFuture);
                        
                        // Обновляем текстуру асинхронно без блокировки
                        textureFuture.thenAcceptAsync(newTexture -> {
                            if (newTexture != null && isActive) {
                                // Закрываем старую текстуру
                                if (texture != null && texture != cachedTexture) {
                                    try {
                                        texture.close();
                                    } catch (Exception ignored) {}
                                }
                                if (cachedTexture != null && cachedTexture != newTexture) {
                                    try {
                                        cachedTexture.close();
                                    } catch (Exception ignored) {}
                                }
                                
                                // Обновляем кеш
                                image = newImage;
                                texture = newTexture;
                                cachedArtwork = newImage;
                                cachedTexture = newTexture;
                                cachedTitle = title;
                                cachedArtist = artist;
                            }
                        }, executor);
                    }
                } else {
                    // Нет обложки - сбрасываем
                    resetArtwork();
                }
                changeTrack = false;
            }
        } catch (Exception e) {
            System.err.println("[MediaPlayer] Error updating artwork: " + e.getMessage());
            resetArtwork();
        }
    }
    
    private void resetArtwork() {
        CompletableFuture.runAsync(() -> {
            if (texture != null) {
                try {
                    texture.close();
                } catch (Exception ignored) {}
                texture = null;
            }
            if (cachedTexture != null && cachedTexture != texture) {
                try {
                    cachedTexture.close();
                } catch (Exception ignored) {}
                cachedTexture = null;
            }
            image = null;
            cachedArtwork = null;
        }, executor);
    }

    private void resetMediaState() {
        title = "";
        lastTitle = "";
        artist = "";
        owner = "";
        image = null;
        resetArtwork();
        session = null;
        
        // Очищаем кеш
        cachedArtwork = null;
        cachedTitle = "";
        cachedArtist = "";
        
        // Отменяем pending обновления
        CompletableFuture<AbstractTexture> pending = pendingTextureUpdate.get();
        if (pending != null && !pending.isDone()) {
            pending.cancel(true);
            pendingTextureUpdate.set(null);
        }
    }

    private void handleError(Throwable e) {
        System.err.println("[MediaPlayer] Error: " + e.getMessage());
        resetMediaState();
        // Disable media player after multiple errors to prevent spam
        isActive = false;
    }

    public boolean fullNullCheck() {
        return session == null || lastTitle == null || lastTitle.isEmpty();
    }
    
    public boolean hasTexture() {
        return texture != null;
    }

    public void shutdown() {
        isActive = false;
        
        // Отменяем все pending обновления
        CompletableFuture<AbstractTexture> pending = pendingTextureUpdate.get();
        if (pending != null && !pending.isDone()) {
            pending.cancel(true);
        }
        
        // Даём время на завершение текущих задач
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        resetArtwork();
    }
}