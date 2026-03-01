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
import net.minecraft.scoreboard.Team;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class StaffList extends HudElement {

    private final NumberSetting fontSize = new NumberSetting("Font Size", 8f, 4f, 16f, 0.5f);
    private final NumberSetting backgroundAlpha = new NumberSetting("Background Alpha", 80f, 50f, 255f, 5f);

    private final Animation alphaAnimation = new Animation(300, 1f, false, Easing.SMOOTH_STEP);
    private boolean isVisible = false;

    private static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");
    private static final Pattern PREFIX_MATCHES = Pattern.compile(".*(mod|der|adm|help|wne|хелп|адм|поддержка|кура|own|staf|curat|dev|supp|yt|гл\\.мод|мл\\.мод|мл\\.сотруд|ст\\.сотруд|стажёр|стажер|сотруд).*");

    private final List<Staff> staffPlayers = new ArrayList<>();

    public StaffList() {
        super("StaffList");
        getSettings().add(fontSize);
        getSettings().add(backgroundAlpha);
        getPosition().getValue().setX(0.85f);
        getPosition().getValue().setY(0.3f);
    }

    private void updateStaffList() {
        staffPlayers.clear();
        if (mc.player == null || mc.world == null) return;

        for (Team team : mc.world.getScoreboard().getTeams()) {
            for (String member : team.getPlayerList()) {
                String name = member;
                String prefix = team.getPrefix().getString();

                boolean vanish = true;
                if (mc.getNetworkHandler() != null) {
                    for (var playerInfo : mc.getNetworkHandler().getPlayerList()) {
                        if (playerInfo.getProfile().getName().equals(name)) {
                            vanish = false;
                            break;
                        }
                    }
                }

                if (NAME_PATTERN.matcher(name).matches() && !name.equals(mc.player.getName().getString())) {
                    if (!vanish) {
                        if (PREFIX_MATCHES.matcher(prefix.toLowerCase(Locale.ROOT)).matches()) {
                            staffPlayers.add(new Staff(prefix, name, false));
                        }
                    }
                    if (vanish && !prefix.isEmpty()) {
                        staffPlayers.add(new Staff(prefix, name, true));
                    }
                }
            }
        }

        staffPlayers.sort(Comparator.comparing(staff -> staff.name.toLowerCase(Locale.ROOT)));
    }

    @EventHandler
    public void onRender2DX2(EventRender2D e) {
        if (fullNullCheck()) return;
        BooleanSetting setting = MotherHack.getInstance().getHudManager().getElements().getName("StaffList");
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

        updateStaffList();

        boolean isInChatScreen = mc.currentScreen instanceof ChatScreen;
        boolean shouldBeVisible = !staffPlayers.isEmpty() || isInChatScreen;

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
        float circleRadius = 3f;
        float currentFontSize = fontSize.getValue();
        Font headerFont = Fonts.SEMIBOLD;
        Font bodyFont = Fonts.REGULAR;

        float headerHeight = currentFontSize + 4f + padding * 2;
        int bgAlpha = (int) (backgroundAlpha.getValue() * alpha);
        int textAlpha = (int) (255 * alpha);
        
        Color textColor = new Color(255, 255, 255, textAlpha);
        Color iconColor = new Color(255, 100, 120, textAlpha);
        Color bgColor = new Color(theme.getBackgroundColor().getRed(), theme.getBackgroundColor().getGreen(), theme.getBackgroundColor().getBlue(), bgAlpha);
        Color blurColor = new Color(theme.getBackgroundColor().getRed(), theme.getBackgroundColor().getGreen(), theme.getBackgroundColor().getBlue(), (int)(bgAlpha * 0.3f));

        String headerText = "Staff List";
        float headerTextWidth = headerFont.getWidth(headerText, currentFontSize + 1f);
        float maxWidth = headerTextWidth + 40f;

        if (staffPlayers.isEmpty() && isInChatScreen) {
            String prefixText = "DEVELOPER";
            String nameText = "DarkFimoz";
            float nameWidth = bodyFont.getWidth(prefixText, currentFontSize) + bodyFont.getWidth(nameText, currentFontSize) + padding * 8 + circleRadius * 2;
            maxWidth = Math.max(maxWidth, nameWidth);
        } else {
            for (Staff staff : staffPlayers) {
                String cleanPrefix = staff.prefix.replaceAll("§.", "");
                float nameWidth = (staff.prefix.isEmpty() ? 0 : bodyFont.getWidth(cleanPrefix, currentFontSize) + 4) +
                        bodyFont.getWidth(staff.name, currentFontSize) + padding * 8 + circleRadius * 2;
                maxWidth = Math.max(maxWidth, nameWidth);
            }
        }

        float width = maxWidth;

        e.getContext().getMatrices().push();
        e.getContext().getMatrices().translate(x + width / 2, y + headerHeight / 2, 0f);
        e.getContext().getMatrices().scale(toggledAnimation.getValue(), toggledAnimation.getValue(), 0);
        e.getContext().getMatrices().translate(-(x + width / 2), -(y + headerHeight / 2), 0f);

        Render2D.drawBlurredRect(e.getContext().getMatrices(), x, y, width, headerHeight, 6f, 12f, blurColor);
        Render2D.drawRoundedRect(e.getContext().getMatrices(), x, y, width, headerHeight, 6f, bgColor);

        Render2D.drawFont(e.getContext().getMatrices(), Fonts.ICONS.getFont(currentFontSize + 3f), "D", x + padding + 1, y + padding + 0.5f, iconColor);
        Render2D.drawFont(e.getContext().getMatrices(), headerFont.getFont(currentFontSize + 1f), headerText, x + padding + 18, y + padding + 1f, textColor);

        float currentY = y + headerHeight + 3f;
        float rowHeight = currentFontSize + padding * 2;

        if (staffPlayers.isEmpty() && isInChatScreen) {
            String nameText = "Player";

            Render2D.drawBlurredRect(e.getContext().getMatrices(), x, currentY, width, rowHeight, 6f, 12f, blurColor);
            Render2D.drawRoundedRect(e.getContext().getMatrices(), x, currentY, width, rowHeight, 6f, bgColor);

            long timeInSeconds = System.currentTimeMillis() / 800;
            Color circleColor = (timeInSeconds % 2 == 0) ? new Color(100, 255, 100, textAlpha) : new Color(255, 100, 100, textAlpha);
            Render2D.drawRoundedRect(e.getContext().getMatrices(), x + padding + 1, currentY + padding - 1, 6, 6, circleRadius, circleColor);

            float textOffsetX = x + padding + circleRadius * 2 + 6;
            Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), "ADMIN", textOffsetX, currentY + padding, new Color(255, 100, 100, textAlpha));
            textOffsetX += bodyFont.getWidth("ADMIN", currentFontSize) + 4;
            Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), nameText, textOffsetX, currentY + padding, textColor);

            currentY += rowHeight + 3f;
        } else {
            for (Staff staff : staffPlayers) {
                Render2D.drawBlurredRect(e.getContext().getMatrices(), x, currentY, width, rowHeight, 6f, 12f, blurColor);
                Render2D.drawRoundedRect(e.getContext().getMatrices(), x, currentY, width, rowHeight, 6f, bgColor);

                Color circleColor = staff.isSpec ? new Color(255, 100, 100, textAlpha) : new Color(100, 255, 100, textAlpha);
                Render2D.drawRoundedRect(e.getContext().getMatrices(), x + padding + 1, currentY + padding - 1, 6, 6, circleRadius, circleColor);

                float textOffsetX = x + padding + circleRadius * 2 + 6;

                if (!staff.prefix.isEmpty()) {
                    Color prefixColor = getMinecraftColor(staff.prefix, textAlpha);
                    String cleanPrefix = staff.prefix.replaceAll("§.", "");
                    Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), cleanPrefix, textOffsetX, currentY + padding, prefixColor);
                    textOffsetX += bodyFont.getWidth(cleanPrefix, currentFontSize) + 4;
                }

                Render2D.drawFont(e.getContext().getMatrices(), bodyFont.getFont(currentFontSize), staff.name, textOffsetX, currentY + padding, textColor);

                currentY += rowHeight + 3f;
            }
        }

        e.getContext().getMatrices().pop();

        float totalHeight = currentY - y;
        setBounds(getX(), getY(), width, totalHeight);
        super.onRender2D(e);
    }

    private Color getMinecraftColor(String text, int alpha) {
        if (text == null || text.isEmpty()) return new Color(255, 255, 255, alpha);

        int index = text.indexOf('§');
        if (index != -1 && index + 1 < text.length()) {
            char colorChar = text.charAt(index + 1);
            return switch (colorChar) {
                case '0' -> new Color(0, 0, 0, alpha);
                case '1' -> new Color(0, 0, 170, alpha);
                case '2' -> new Color(0, 170, 0, alpha);
                case '3' -> new Color(0, 170, 170, alpha);
                case '4' -> new Color(170, 0, 0, alpha);
                case '5' -> new Color(170, 0, 170, alpha);
                case '6' -> new Color(255, 170, 0, alpha);
                case '7' -> new Color(170, 170, 170, alpha);
                case '8' -> new Color(85, 85, 85, alpha);
                case '9' -> new Color(85, 85, 255, alpha);
                case 'a' -> new Color(85, 255, 85, alpha);
                case 'b' -> new Color(85, 255, 255, alpha);
                case 'c' -> new Color(255, 85, 85, alpha);
                case 'd' -> new Color(255, 85, 255, alpha);
                case 'e' -> new Color(255, 255, 85, alpha);
                case 'f' -> new Color(255, 255, 255, alpha);
                default -> new Color(255, 255, 255, alpha);
            };
        }
        return new Color(255, 255, 255, alpha);
    }

    private static class Staff {
        String prefix;
        String name;
        boolean isSpec;

        public Staff(String prefix, String name, boolean isSpec) {
            this.prefix = prefix;
            this.name = name;
            this.isSpec = isSpec;
        }
    }
}
