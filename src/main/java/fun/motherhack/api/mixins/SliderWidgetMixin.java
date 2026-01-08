package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.utils.render.fonts.Fonts;
import fun.motherhack.utils.render.Render2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(value = SliderWidget.class)
public abstract class SliderWidgetMixin extends ClickableWidget {

    @Shadow protected double value;

    public SliderWidgetMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (MotherHack.getInstance().isPanic()) return;
        ci.cancel();
        Render2D.drawStyledRect(context.getMatrices(), getX(), getY(), getWidth(), getHeight(), 1.5f, new Color(0, 0, 0, active ? 150 : 0), 255);
        Render2D.drawStyledRect(context.getMatrices(), (float) (getX() + (value * (getWidth() - 8))), getY(), 8f, getHeight(), 1.5f, new Color(0, 0, 0, active ? 175 : 0), 255);
        if (!getMessage().getString().isEmpty()) Render2D.drawFont(
                context.getMatrices(),
                Fonts.REGULAR.getFont(8f),
                getMessage().getString(),
                getX() - Fonts.REGULAR.getWidth(getMessage().getString(), 8f) / 2f + getWidth() / 2f,
                getY() + 5f,
                Color.WHITE
        );
    }
}