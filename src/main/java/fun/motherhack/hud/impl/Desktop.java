package fun.motherhack.hud.impl;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.hud.HudElement;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Font;
import fun.motherhack.utils.render.fonts.Fonts;
import meteordevelopment.orbit.EventHandler;

import java.awt.Color;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Desktop HUD element - информационная панель с FPS, координатами, скоростью и пингом
 */
public class Desktop extends HudElement {

    private final NumberSetting fontSize = new NumberSetting("Font Size", 9f, 6f, 14f, 0.5f);
    private final NumberSetting backgroundAlpha = new NumberSetting("Background Alpha", 80f, 50f, 255f, 5f);
    private final BooleanSetting showFps = new BooleanSetting("Show FPS", true);
    private final BooleanSetting showCoords = new BooleanSetting("Show Coords", true);
    private final BooleanSetting showSpeed = new BooleanSetting("Show Speed", true);
    private final BooleanSetting showPing = new BooleanSetting("Show Ping", true);
    private final BooleanSetting showTime = new BooleanSetting("Show Time", true);

    // Анимированные значения для плавности
    private float animFps = 0f;
    private float animX = 0f;
    private float animY = 0f;
    private float animZ = 0f;
    private float animSpeed = 0f;
    private float animPing = 0f;
    private static final float ANIM_SPEED = 0.02f;

    public Desktop() {
        super("Desktop");
        getSettings().add(fontSize);
        getSettings().add(backgroundAlpha);
        getSettings().add(showFps);
        getSettings().add(showCoords);
        getSettings().add(showSpeed);
        getSettings().add(showPing);
        getSettings().add(showTime);
        getPosition().getValue().setX(0.005f);
        getPosition().getValue().setY(0.005f);
    }

    private float animate(float current, float target) {
        return current + (target - current) * ANIM_SPEED;
    }

    private int getPing() {
        if (mc.player == null || mc.getNetworkHandler() == null) return 0;
        var playerInfo = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return playerInfo != null ? playerInfo.getLatency() : 0;
    }

    private float getSpeed() {
        if (mc.player == null) return 0f;
        double dx = mc.player.getX() - mc.player.prevX;
        double dz = mc.player.getZ() - mc.player.prevZ;
        return (float) (Math.sqrt(dx * dx + dz * dz) * 20);
    }

    @EventHandler
    public void onRender2DX2(EventRender2D e) {
        if (fullNullCheck()) return;
        BooleanSetting setting = MotherHack.getInstance().getHudManager().getElements().getName("Desktop");
        if (setting != null) {
            toggledAnimation.update(setting.getValue());
        } else {
            toggledAnimation.update(true);
        }
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;
        if (Fonts.SEMIBOLD == null || Fonts.REGULAR == null) return;

        // Обновляем анимированные значения
        animFps = animate(animFps, mc.getCurrentFps());
        if (mc.player != null) {
            animX = animate(animX, (float) mc.player.getX());
            animY = animate(animY, (float) mc.player.getY());
            animZ = animate(animZ, (float) mc.player.getZ());
            animSpeed = animate(animSpeed, getSpeed());
            animPing = animate(animPing, getPing());
        }

        float x = getX();
        float y = getY();
        float currentFontSize = fontSize.getValue();
        Font font = Fonts.SEMIBOLD;
        Font bodyFont = Fonts.REGULAR;
        int bgAlpha = backgroundAlpha.getValue().intValue();
        Color textColor = new Color(255, 255, 255, 255);
        Color grayColor = new Color(170, 170, 170, 255);

        e.getContext().getMatrices().push();
        e.getContext().getMatrices().translate(x, y, 0f);
        e.getContext().getMatrices().scale(toggledAnimation.getValue(), toggledAnimation.getValue(), 0);
        e.getContext().getMatrices().translate(-x, -y, 0f);

        float currentX = x;
        float currentY = y;
        float panelHeight = 15f;
        float padding = 5f;

        // Логотип MH
        String logoText = "MH";
        float logoWidth = font.getWidth(logoText, currentFontSize + 2f) + padding * 2;
        Render2D.drawBlurredRect(e.getContext().getMatrices(), currentX, currentY, logoWidth, panelHeight, 5f, 10f, new Color(255, 255, 255, (int)(bgAlpha * 0.3f)));
        Render2D.drawRoundedRect(e.getContext().getMatrices(), currentX, currentY, logoWidth, panelHeight, 5f, new Color(0, 0, 0, bgAlpha));
        Render2D.drawFont(e.getContext().getMatrices(), font.getFont(currentFontSize + 2f), logoText, currentX + padding, currentY + 2.5f, textColor);
        currentX += logoWidth + 2;

        // Название клиента
        String clientName = "MotherHack";
        float nameWidth = bodyFont.getWidth(clientName, currentFontSize) + padding * 2;
        Render2D.drawBlurredRect(e.getContext().getMatrices(), currentX, currentY, nameWidth, panelHeight, 5f, 10f, new Color(255, 255, 255, (int)(bgAlpha * 0.3f)));
        Render2D.drawRoundedRect(e.getContext().getMatrices(), currentX, currentY, nameWidth, panelHeight, 5f, new Color(0, 0, 0, bgAlpha));
        Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), clientName, currentX + padding, currentY + 3f, textColor);
        currentX += nameWidth + 2;

        // FPS
        if (showFps.getValue()) {
            String fpsText = Math.round(animFps) + " fps";
            float fpsWidth = bodyFont.getWidth(fpsText, currentFontSize) + padding * 2;
            Render2D.drawBlurredRect(e.getContext().getMatrices(), currentX, currentY, fpsWidth, panelHeight, 5f, 10f, new Color(255, 255, 255, (int)(bgAlpha * 0.3f)));
            Render2D.drawRoundedRect(e.getContext().getMatrices(), currentX, currentY, fpsWidth, panelHeight, 5f, new Color(0, 0, 0, bgAlpha));
            Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), fpsText, currentX + padding, currentY + 3f, textColor);
            currentX += fpsWidth + 2;
        }

        // Время
        if (showTime.getValue()) {
            LocalTime now = LocalTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String timeText = now.format(formatter);
            float timeWidth = bodyFont.getWidth(timeText, currentFontSize) + padding * 2;
            Render2D.drawBlurredRect(e.getContext().getMatrices(), x, currentY + panelHeight + 2, timeWidth, panelHeight, 5f, 10f, new Color(255, 255, 255, (int)(bgAlpha * 0.3f)));
            Render2D.drawRoundedRect(e.getContext().getMatrices(), x, currentY + panelHeight + 2, timeWidth, panelHeight, 5f, new Color(0, 0, 0, bgAlpha));
            Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), timeText, x + padding, currentY + panelHeight + 5f, textColor);
        }

        // Нижняя информация
        int scHeight = mc.getWindow().getScaledHeight();
        float bottomY = scHeight - 20f;

        // Координаты
        if (showCoords.getValue() && mc.player != null) {
            String xyzText = String.format("XYZ: %.1f / %.1f / %.1f", animX, animY, animZ);
            Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), xyzText, 2, bottomY, grayColor);
            bottomY -= 10f;
        }

        // Скорость
        if (showSpeed.getValue()) {
            String speedText = String.format("Speed: %.1f b/s", animSpeed);
            Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), speedText, 2, bottomY, grayColor);
        }

        // Пинг (справа)
        if (showPing.getValue()) {
            String pingText = "Ping: " + Math.round(animPing) + "ms";
            float pingWidth = bodyFont.getWidth(pingText, currentFontSize);
            int scWidth = mc.getWindow().getScaledWidth();
            Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), pingText, scWidth - pingWidth - 3f, scHeight - 20f, grayColor);
        }

        e.getContext().getMatrices().pop();

        // Устанавливаем bounds
        float totalWidth = currentX - x;
        float totalHeight = panelHeight * 2 + 4;
        setBounds(getX(), getY(), totalWidth, totalHeight);
        super.onRender2D(e);
    }
}
