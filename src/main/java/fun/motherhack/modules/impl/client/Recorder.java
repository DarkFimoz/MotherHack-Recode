package fun.motherhack.modules.impl.client;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventKey;
import fun.motherhack.api.events.impl.EventMouse;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Bind;
import fun.motherhack.modules.settings.impl.BindSetting;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.ButtonSetting;
import fun.motherhack.utils.mediaplayer.MediaPlayer;
import fun.motherhack.utils.network.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.ScreenshotRecorder;

import java.io.File;

public class Recorder extends Module {

    // Music control buttons
    private final ButtonSetting playPauseButton = new ButtonSetting("settings.recorder.playpausebtn", this::togglePlayPause);
    private final ButtonSetting nextTrackButton = new ButtonSetting("settings.recorder.nexttrackbtn", this::nextTrack);
    private final ButtonSetting prevTrackButton = new ButtonSetting("settings.recorder.prevtrackbtn", this::previousTrack);
    private final ButtonSetting stopMusicButton = new ButtonSetting("settings.recorder.stopbtn", this::stopMusic);
    
    // Music control binds
    private final BindSetting playPauseBind = new BindSetting("settings.recorder.playpause", new Bind(-1, false));
    private final BindSetting nextTrackBind = new BindSetting("settings.recorder.nexttrack", new Bind(-1, false));
    private final BindSetting prevTrackBind = new BindSetting("settings.recorder.prevtrack", new Bind(-1, false));
    private final BindSetting stopBind = new BindSetting("settings.recorder.stop", new Bind(-1, false));
    
    // Screenshot button and bind
    private final ButtonSetting screenshotButton = new ButtonSetting("settings.recorder.screenshotbtn", this::takeScreenshot);
    private final BindSetting screenshotBind = new BindSetting("settings.recorder.screenshot", new Bind(-1, false));
    private final BooleanSetting screenshotNotify = new BooleanSetting("settings.recorder.screenshotnotify", true);
    
    public Recorder() {
        super("Recorder", Category.Client);
    }

    @EventHandler
    public void onKey(EventKey e) {
        if (fullNullCheck() || mc.currentScreen != null) return;
        
        if (e.getAction() == 1) {
            int key = e.getKey();
            
            if (key == playPauseBind.getValue().getKey() && !playPauseBind.getValue().isMouse() && key != -1) {
                togglePlayPause();
            } else if (key == nextTrackBind.getValue().getKey() && !nextTrackBind.getValue().isMouse() && key != -1) {
                nextTrack();
            } else if (key == prevTrackBind.getValue().getKey() && !prevTrackBind.getValue().isMouse() && key != -1) {
                previousTrack();
            } else if (key == stopBind.getValue().getKey() && !stopBind.getValue().isMouse() && key != -1) {
                stopMusic();
            } else if (key == screenshotBind.getValue().getKey() && !screenshotBind.getValue().isMouse() && key != -1) {
                takeScreenshot();
            }
        }
    }

    @EventHandler
    public void onMouse(EventMouse e) {
        if (fullNullCheck() || mc.currentScreen != null) return;
        
        if (e.getAction() == 1) {
            int button = e.getButton();
            
            if (button == playPauseBind.getValue().getKey() && playPauseBind.getValue().isMouse() && button != -1) {
                togglePlayPause();
            } else if (button == nextTrackBind.getValue().getKey() && nextTrackBind.getValue().isMouse() && button != -1) {
                nextTrack();
            } else if (button == prevTrackBind.getValue().getKey() && prevTrackBind.getValue().isMouse() && button != -1) {
                previousTrack();
            } else if (button == stopBind.getValue().getKey() && stopBind.getValue().isMouse() && button != -1) {
                stopMusic();
            } else if (button == screenshotBind.getValue().getKey() && screenshotBind.getValue().isMouse() && button != -1) {
                takeScreenshot();
            }
        }
    }

    // Music control methods
    private void togglePlayPause() {
        MediaPlayer mediaPlayer = MotherHack.getInstance().getMediaPlayer();
        if (mediaPlayer != null && mediaPlayer.getSession() != null) {
            try {
                mediaPlayer.getSession().playPause();
                ChatUtils.sendMessage("§7[§aRecorder§7] §fToggled play/pause");
            } catch (Exception e) {
                ChatUtils.sendMessage("§7[§cRecorder§7] §fFailed to toggle play/pause");
            }
        } else {
            ChatUtils.sendMessage("§7[§cRecorder§7] §fNo media session found");
        }
    }

    private void nextTrack() {
        MediaPlayer mediaPlayer = MotherHack.getInstance().getMediaPlayer();
        if (mediaPlayer != null && mediaPlayer.getSession() != null) {
            try {
                mediaPlayer.getSession().next();
                ChatUtils.sendMessage("§7[§aRecorder§7] §fNext track");
            } catch (Exception e) {
                ChatUtils.sendMessage("§7[§cRecorder§7] §fFailed to skip track");
            }
        } else {
            ChatUtils.sendMessage("§7[§cRecorder§7] §fNo media session found");
        }
    }

    private void previousTrack() {
        MediaPlayer mediaPlayer = MotherHack.getInstance().getMediaPlayer();
        if (mediaPlayer != null && mediaPlayer.getSession() != null) {
            try {
                mediaPlayer.getSession().previous();
                ChatUtils.sendMessage("§7[§aRecorder§7] §fPrevious track");
            } catch (Exception e) {
                ChatUtils.sendMessage("§7[§cRecorder§7] §fFailed to go to previous track");
            }
        } else {
            ChatUtils.sendMessage("§7[§cRecorder§7] §fNo media session found");
        }
    }

    private void stopMusic() {
        MediaPlayer mediaPlayer = MotherHack.getInstance().getMediaPlayer();
        if (mediaPlayer != null && mediaPlayer.getSession() != null) {
            try {
                mediaPlayer.getSession().stop();
                ChatUtils.sendMessage("§7[§aRecorder§7] §fStopped music");
            } catch (Exception e) {
                ChatUtils.sendMessage("§7[§cRecorder§7] §fFailed to stop music");
            }
        } else {
            ChatUtils.sendMessage("§7[§cRecorder§7] §fNo media session found");
        }
    }

    // Screenshot method
    private void takeScreenshot() {
        if (mc == null || mc.getFramebuffer() == null) return;
        
        File screenshotDir = new File(mc.runDirectory, "screenshots");
        if (!screenshotDir.exists()) {
            screenshotDir.mkdirs();
        }
        
        ScreenshotRecorder.saveScreenshot(
            mc.runDirectory,
            mc.getFramebuffer(),
            (text) -> {
                if (screenshotNotify.getValue()) {
                    mc.execute(() -> ChatUtils.sendMessage("§7[§aRecorder§7] §f" + text.getString()));
                }
            }
        );
    }
}
