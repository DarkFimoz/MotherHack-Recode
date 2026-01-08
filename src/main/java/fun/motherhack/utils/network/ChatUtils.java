package fun.motherhack.utils.network;

import java.awt.Color;

import fun.motherhack.utils.Wrapper;
import fun.motherhack.utils.render.ColorUtils;
import lombok.experimental.UtilityClass;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

@UtilityClass
public class ChatUtils implements Wrapper {

    public void sendMessage(String message) {
        MutableText text = Text.literal("");
        for (int i = 0; i < "MotherHack".length(); i++) {
            text.append(Text.literal("MotherHack".charAt(i) + "")
                    .setStyle(Style.EMPTY
                            .withBold(true)
                            .withColor(TextColor.fromRgb(ColorUtils.gradient(ColorUtils.getGlobalColor(), Color.WHITE, (float) i / "MotherHack".length()).getRGB()))
                    )
            );
        }

        text.append(Text.literal(" ⇨ ")
                .setStyle(Style.EMPTY
                        .withBold(false)
                        .withColor(TextColor.fromRgb(new Color(200, 200, 200).getRGB()))
                )
        );

        text.append(Text.literal(message)
                .setStyle(Style.EMPTY
                        .withBold(false)
                        .withColor(TextColor.fromRgb(new Color(200, 200, 200).getRGB()))
                )
        );

        mc.player.sendMessage(text, false);
    }
}