package fun.motherhack.modules.impl.misc;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.utils.world.InventoryUtils;
import lombok.Getter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;

public class Teams extends Module {

    @Getter
    public enum TeamMode implements Nameable {
        ArmorColor("Armor Color"),      // По цвету брони
        Scoreboard("Scoreboard"),       // По скорборду
        NameColor("Name Color"),        // По цвету ника
        Both("Both");                   // Броня + скорборд

        private final String name;

        TeamMode(String name) {
            this.name = name;
        }
    }

    private final EnumSetting<TeamMode> mode = new EnumSetting<>("Mode", TeamMode.Both);

    public Teams() {
        super("Teams", Category.Misc);
    }

    public boolean isTeammate(PlayerEntity player) {
        if (player == mc.player) return true;

        switch (mode.getValue()) {
            case ArmorColor -> {
                return checkArmorColor(player);
            }
            case Scoreboard -> {
                return checkScoreboard(player);
            }
            case NameColor -> {
                return checkNameColor(player);
            }
            case Both -> {
                return checkArmorColor(player) || checkScoreboard(player);
            }
        }

        return false;
    }

    private boolean checkArmorColor(PlayerEntity player) {
        int playerColor = InventoryUtils.getArmorColor(player, 3);
        int myColor = InventoryUtils.getArmorColor(mc.player, 3);
        // Проверяем только если оба имеют окрашенную броню (не -1)
        return playerColor != -1 && myColor != -1 && playerColor == myColor;
    }

    private boolean checkScoreboard(PlayerEntity player) {
        if (mc.player.getScoreboardTeam() == null) return false;
        Team playerTeam = player.getScoreboardTeam();
        Team myTeam = mc.player.getScoreboardTeam();
        return playerTeam != null && playerTeam.equals(myTeam);
    }

    private boolean checkNameColor(PlayerEntity player) {
        // Проверяем цвет имени игрока
        String playerName = player.getDisplayName().getString();
        String myName = mc.player.getDisplayName().getString();
        
        // Извлекаем цветовой код из имени (§x)
        String playerColor = extractColorCode(playerName);
        String myColor = extractColorCode(myName);
        
        if (playerColor == null || myColor == null) return false;
        return playerColor.equals(myColor);
    }

    private String extractColorCode(String text) {
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) == '§') {
                return String.valueOf(text.charAt(i + 1));
            }
        }
        return null;
    }
}