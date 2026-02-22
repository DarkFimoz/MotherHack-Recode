package fun.motherhack.hud.impl;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPopTotem;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.hud.HudElement;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Fonts;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.awt.*;

public class Companion extends HudElement {

    private final NumberSetting scale = new NumberSetting("Scale", 50f, 0f, 100f, 1f);
    private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.Boykisser);

    private static int currentFrame = 0;
    private String message = "";
    private long lastPopTime = 0;
    private long lastFrameTime = 0;
    private static final long FRAME_DELAY = 64; // milliseconds between frames
    private static final long MESSAGE_DURATION = 2000; // 2 seconds

    public Companion() {
        super("Companion");
        getSettings().add(scale);
        getSettings().add(mode);
        getPosition().getValue().setX(0.05f);
        getPosition().getValue().setY(0.05f);
        setToggled(true); // Enable by default
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;

        MatrixStack matrices = e.getContext().getMatrices();
        float x = getX();
        float y = getY();
        float scaleValue = scale.getValue() / 100f;

        // Update animation frame
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime >= FRAME_DELAY) {
            lastFrameTime = currentTime;
            currentFrame++;
            if (currentFrame > 52) currentFrame = 0;
        }

        matrices.push();
        matrices.translate((int) x + 100, (int) y + 100, 0);
        matrices.scale(scaleValue, scaleValue, 1);
        matrices.translate(-((int) x + 100), -((int) y + 100), 0);

        // Draw animated texture based on mode
        if (mode.getValue() == Mode.Boykisser) {
            // Calculate UV coordinates for animation frame
            float u = 0f;
            float v = (float) currentFrame / 53f; // 53 frames total (0-52)
            float textureWidth = 1f;
            float textureHeight = 1f / 53f; // Each frame is 1/53 of the texture
            
            // Use Render2D.drawTexture with UV coordinates
            Render2D.drawTexture(matrices, (int) x, (int) y, 130, 128, 0f, 
                u, v, textureWidth, textureHeight,
                Identifier.of("motherhack", "hud/boykisser.png"), Color.WHITE);
        }

        matrices.pop();

        // Draw message if recent pop
        if (currentTime - lastPopTime < MESSAGE_DURATION && !message.isEmpty()) {
            float w = Fonts.SEMIBOLD.getWidth(message, 8f) + 8;
            float factor = Math.min((currentTime - lastPopTime) / 500f, 1f);
            
            Color bgColor = new Color(252, 215, 221, 200);
            
            Render2D.drawRoundedRect(matrices, x + scale.getValue() / 3f, y + 70 - scale.getValue(), 
                factor * w, 10, 3f, bgColor);
            
            Render2D.drawFont(matrices, Fonts.SEMIBOLD.getFont(8f), message, 
                x + 2 + scale.getValue() / 3f, y + 72 - scale.getValue(), new Color(72, 72, 72));
        }

        setBounds(getX(), getY(), scale.getValue() * 3f, scale.getValue() * 3f);
        super.onRender2D(e);
    }

    @EventHandler
    public void onRender2DX2(EventRender2D e) {
        if (fullNullCheck()) return;
        BooleanSetting setting = MotherHack.getInstance().getHudManager().getElements().getName("Companion");
        if (setting != null) {
            toggledAnimation.update(setting.getValue());
        } else {
            toggledAnimation.update(true);
        }
    }

    @EventHandler
    public void onTotemPop(EventPopTotem event) {
        if (event.getPlayer() == mc.player) return;
        
        PlayerEntity player = event.getPlayer();
        int pops = getPopCount(player);
        
        boolean isRussian = MotherHack.getInstance().getModuleManager().getModule(UI.class) != null;
        
        if (isRussian) {
            message = player.getName().getString() + " попнул " + 
                (pops > 1 ? pops + " тотемов!" : "тотем!");
        } else {
            message = player.getName().getString() + " popped " + 
                (pops > 1 ? pops + " totems!" : "a totem!");
        }
        
        lastPopTime = System.currentTimeMillis();
    }

    private int getPopCount(PlayerEntity player) {
        // Simple pop counter - you may need to adjust this based on your combat manager
        return 1;
    }

    private enum Mode {
        Boykisser
    }
}
