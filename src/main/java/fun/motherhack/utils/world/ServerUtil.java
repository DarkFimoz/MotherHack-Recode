package fun.motherhack.utils.world;

import fun.motherhack.utils.Wrapper;
import fun.motherhack.utils.math.MathUtils;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.util.profiling.jfr.event.PacketEvent;

import java.util.ArrayDeque;
@UtilityClass
public class ServerUtil implements Wrapper {

    private long time;
    private long tickTime;
    private float tps;

    public boolean isFuntime() {
        return connectedTo("funtime");
    }

    public boolean isSpookyTime() {
        return connectedTo("spookytime");
    }

    public boolean isHolyWorld() {
        return connectedTo("holyworld");
    }
    public boolean isRw() {
        return connectedTo("reallyworld");
    }

    public boolean connectedTo(String ip) {
        return ((mc.getNetworkHandler() != null && mc.getNetworkHandler().getServerInfo() != null && mc.getNetworkHandler().getServerInfo().address.contains(ip)));
    }

    public float getHealth(PlayerEntity player) {
        if (isFuntime() || isSpookyTime()) {
            ScoreboardObjective scoreBoard = null;
            String resolvedHp = "";
            if ((player.getScoreboard()).getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME) != null) {
                scoreBoard = (player.getScoreboard()).getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
                if (scoreBoard != null) {
                    ReadableScoreboardScore readableScoreboardScore = player.getScoreboard().getScore(player, scoreBoard);
                    MutableText text2 = ReadableScoreboardScore.getFormattedScore(readableScoreboardScore, scoreBoard.getNumberFormatOr(StyledNumberFormat.EMPTY));
                    resolvedHp = text2.getString();
                }
            }
            float numValue = 0;
            try {
                numValue = Float.parseFloat(resolvedHp);
            } catch (NumberFormatException ignored) {
            }
            return numValue;
        } else {
            return (float) MathUtils.round(player.getHealth() + player.getAbsorptionAmount(),0.1F);
        }
    }

    public String getItemName(ItemStack stack) {
        String name;

        if (isHolyWorld()) {
            if (stack.getName().getString().equals("Зако") || stack.getName().getString().equals("Zako") || stack.getName().getString().equals("@")) {
                name = stack.getItem().getName().getString();
            } else {
                name = stack.getName().getString();
            }
        } else {
            name = stack.getName().getString();
        }

        return  name ;
    }


    public static boolean isBadEffect(StatusEffect effect) {
        return effect.getCategory() == StatusEffectCategory.HARMFUL;
    }

}
