package fun.motherhack.modules.impl.render.motionblur;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.motherhack.MotherHack;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import fun.motherhack.api.events.impl.EventRender3D;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class MotionBlur extends Module {
    @Getter
    private static MotionBlur instance;

    public final NumberSetting strength = new NumberSetting("Strength", 0.5f, 0.1f, 0.9f, 0.05f);
    public final BooleanSetting useRefreshRateScaling = new BooleanSetting("Use RR Scaling", false);

    private Framebuffer accumBuffer = null;
    private boolean firstFrame = true;
    private long lastNano = System.nanoTime();
    private float currentFPS = 60f;

    public MotionBlur() {
        super("MotionBlur", Category.Render);
        instance = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        firstFrame = true;
        MotherHack.getInstance().getEventHandler().subscribe(this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        MotherHack.getInstance().getEventHandler().unsubscribe(this);
        if (accumBuffer != null) {
            accumBuffer.delete();
            accumBuffer = null;
        }
    }

    @EventHandler
    public void onRender(EventRender3D.Game event) {
        if (mc.player == null || mc.world == null) return;
        
        // Update FPS
        long now = System.nanoTime();
        float delta = (now - lastNano) / 1_000_000_000f;
        lastNano = now;
        if (delta > 0 && delta < 1f) {
            currentFPS = 1f / delta;
        }

        Framebuffer main = mc.getFramebuffer();
        int w = main.textureWidth;
        int h = main.textureHeight;

        // Create/resize accumulation buffer
        if (accumBuffer == null || accumBuffer.textureWidth != w || accumBuffer.textureHeight != h) {
            if (accumBuffer != null) accumBuffer.delete();
            accumBuffer = new SimpleFramebuffer(w, h, false);
            firstFrame = true;
        }

        if (firstFrame) {
            // Copy current frame to accum buffer
            blit(main, accumBuffer);
            firstFrame = false;
            return;
        }

        float blend = strength.getValue();
        if (useRefreshRateScaling.getValue()) {
            MonitorInfoProvider.updateDisplayInfo();
            int rr = MonitorInfoProvider.getRefreshRate();
            if (rr > 0 && currentFPS > rr) {
                blend = Math.min(0.9f, blend * (currentFPS / rr));
            }
        }

        // Draw accumulated frame over current with alpha blending
        main.beginWrite(false);
        
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        RenderSystem.setShaderTexture(0, accumBuffer.getColorAttachment());
        RenderSystem.setShaderColor(1f, 1f, 1f, blend);
        
        // Draw fullscreen quad with previous frame
        accumBuffer.draw(w, h);
        
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
        
        // Copy result back to accum for next frame
        blit(main, accumBuffer);
    }

    private void blit(Framebuffer src, Framebuffer dst) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, src.fbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dst.fbo);
        GL30.glBlitFramebuffer(
            0, 0, src.textureWidth, src.textureHeight,
            0, 0, dst.textureWidth, dst.textureHeight,
            GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST
        );
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, src.fbo);
    }

    public enum BlurAlgorithm {
        BACKWARDS, CENTERED
    }
}
