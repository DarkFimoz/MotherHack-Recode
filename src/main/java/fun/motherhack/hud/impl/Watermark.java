package fun.motherhack.hud.impl;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.hud.HudElement;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.math.Counter;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.network.Server;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Font;
import fun.motherhack.utils.render.fonts.Fonts;

import java.awt.*;

public class Watermark extends HudElement {

    private final NumberSetting backgroundAlpha = new NumberSetting("Background Alpha", 80f, 10f, 100f, 5f);
    private final NumberSetting textAlpha = new NumberSetting("Text Alpha", 80f, 10f, 100f, 5f);
    private final BooleanSetting showIcon = new BooleanSetting("Show Icon", false);
    private final BooleanSetting showName = new BooleanSetting("Show Name", true);
    private final BooleanSetting showFps = new BooleanSetting("Show FPS", true);
    private final BooleanSetting showPing = new BooleanSetting("Show Ping", true);
    private final BooleanSetting showCoords = new BooleanSetting("Show Coordinates", false);
    private final BooleanSetting showTps = new BooleanSetting("Show Server TPS", true);
    private final BooleanSetting showTime = new BooleanSetting("Show Time", true);
    private final BooleanSetting showInternet = new BooleanSetting("Show Internet Icon", true);

    public Watermark() {
        super("Watermark");
        getSettings().add(backgroundAlpha);
        getSettings().add(textAlpha);
        getSettings().add(showIcon);
        getSettings().add(showName);
        getSettings().add(showFps);
        getSettings().add(showPing);
        getSettings().add(showCoords);
        getSettings().add(showTps);
        getSettings().add(showTime);
        getSettings().add(showInternet);
        // Set default position to top left
        getPosition().getValue().setX(0.01f);
        getPosition().getValue().setY(0.01f);
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;

        UI.ClickGuiTheme theme = MotherHack.getInstance().getModuleManager().getModule(UI.class).getTheme();

        float x = getX();
        float y = getY();
        float padding = 5f;
        float fontSize = 8f;
        String name = showName.getValue() ? "MotherHack Recode" : "";
        String ping = showPing.getValue() ? Server.getPing(mc.player) + "ms" : "";
        String fps = showFps.getValue() ? Counter.getCurrentFPS() + "fps" : "";
        String coords = showCoords.getValue() ? String.format("%.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ()) : "";
        String tps = showTps.getValue() ? String.format("%.1f tps", getServerTPS()) : "";
        String time = showTime.getValue() ? MathUtils.getCurrentTime() : "";
        boolean hasInternet = Server.getPing(mc.player) < 150;
        Font font = Fonts.BOLD;

        // Calculate widths only for enabled elements
        float width1 = showName.getValue() ? Fonts.BOLD.getWidth(name, fontSize) : 0;
        float width2 = showFps.getValue() ? font.getWidth(fps, fontSize) : 0;
        float width3 = showPing.getValue() ? font.getWidth(ping, fontSize) : 0;
        float width4 = showCoords.getValue() ? font.getWidth(coords, fontSize) : 0;
        float width5 = showTps.getValue() ? font.getWidth(tps, fontSize) : 0;
        float width6 = showTime.getValue() ? font.getWidth(time, fontSize) : 0;
        float internetIconWidth = showInternet.getValue() ? 10f : 0; // Space for internet icon

        // Calculate total width based on enabled elements - more compact
        float totalWidth = 0;
        if (showIcon.getValue()) totalWidth += padding * 2; // Icon space
        if (showName.getValue()) totalWidth += width1;
        if (showFps.getValue()) totalWidth += width2 + padding;
        if (showPing.getValue()) totalWidth += width3 + padding;
        if (showCoords.getValue()) totalWidth += width4 + padding;
        if (showTps.getValue()) totalWidth += width5 + padding;
        if (showInternet.getValue()) totalWidth += internetIconWidth + padding; // Internet icon on right
        // Time is on the left, doesn't add to total width

        // Add minimal padding at start and end
        totalWidth += padding * 2;

        float finalWidth = Math.max(totalWidth, 50f); // Minimum width
        float height = 17f;

        e.getContext().getMatrices().push();
        e.getContext().getMatrices().translate(x + finalWidth / 2, y + height / 2, 0f);
        e.getContext().getMatrices().scale(toggledAnimation.getValue(), toggledAnimation.getValue(), 0);
        e.getContext().getMatrices().translate(-(x + finalWidth / 2), -(y + height / 2), 0f);

        Color bgColor = new Color(theme.getBackgroundColor().getRed(), theme.getBackgroundColor().getGreen(), theme.getBackgroundColor().getBlue(), backgroundAlpha.getValue().intValue());
        Render2D.drawStyledRect(e.getContext().getMatrices(), x, y, finalWidth, height, 3.5f, bgColor, 255);

        Color textColor = new Color(theme.getTextColor().getRed(), theme.getTextColor().getGreen(), theme.getTextColor().getBlue(), textAlpha.getValue().intValue());

        float currentX = x;
        // Time on the left (like in DynamicIsland)
        if (showTime.getValue()) {
            Render2D.drawFont(e.getContext().getMatrices(), font.getFont(fontSize), time, currentX - (padding * 3f) - Fonts.BOLD.getWidth(time, fontSize), y + 3.5f, textColor);
        }

        if (showIcon.getValue()) {
            Render2D.drawFont(e.getContext().getMatrices(), Fonts.ICONS.getFont(11f), "R", currentX + padding / 2f, y + 3.5f, textColor);
            currentX += padding * 2;
        }

        if (showName.getValue()) {
            Render2D.drawFont(e.getContext().getMatrices(), font.getFont(fontSize), name, currentX + padding, y + 3.5f, textColor);
            currentX += width1 + padding * 2;
        }

        if (showFps.getValue()) {
            Render2D.drawFont(e.getContext().getMatrices(), font.getFont(fontSize), fps, currentX + padding, y + 4f, textColor);
            currentX += width2 + padding * 2;
        }

        if (showPing.getValue()) {
            Render2D.drawFont(e.getContext().getMatrices(), font.getFont(fontSize), ping, currentX + padding, y + 4f, textColor);
            currentX += width3 + padding * 2;
        }

        if (showCoords.getValue()) {
            Render2D.drawFont(e.getContext().getMatrices(), font.getFont(fontSize), coords, currentX + padding, y + 4f, textColor);
            currentX += width4 + padding * 2;
        }

        if (showTps.getValue()) {
            Render2D.drawFont(e.getContext().getMatrices(), font.getFont(fontSize), tps, currentX + padding, y + 4f, textColor);
        }

        // Internet icon on the right (closer to TPS)
        if (showInternet.getValue()) {
            Color internetColor = hasInternet ? textColor :
                new Color(theme.getBackgroundColor().getRed(), theme.getBackgroundColor().getGreen(),
                         theme.getBackgroundColor().getBlue(), textAlpha.getValue().intValue());
            Render2D.drawFont(e.getContext().getMatrices(), Fonts.ICONS.getFont(7f),
                hasInternet ? "Q" : "P", x + finalWidth + padding, y + 4f, internetColor);
        }

        e.getContext().getMatrices().pop();
        setBounds(getX(), getY(), finalWidth, height);
        super.onRender2D(e);
    }

    private float getServerTPS() {
        try {
            if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection() != null) {
                // Try to get TPS from server data
                // This is a simplified implementation - in a real scenario you'd need to track tick times
                return 20.0f; // Default to 20 TPS if we can't measure
            }
        } catch (Exception e) {
            // Fallback
        }
        return 20.0f;
    }
}