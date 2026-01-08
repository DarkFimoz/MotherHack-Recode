package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.render.ColorUtils;
import fun.motherhack.utils.render.fonts.Fonts;
import fun.motherhack.utils.render.Render2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(PressableWidget.class)
public abstract class PressableWidgetMixin extends ClickableWidget {

    @Unique private final Animation animation = new Animation(300, 1f, false, Easing.EASE_OUT_BACK);

    public PressableWidgetMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (MotherHack.getInstance().isPanic()) return;
        ci.cancel();
        animation.update(isHovered());
        Render2D.drawStyledRect(context.getMatrices(), getX(), getY(), getWidth(), getHeight(), 1.5f, new Color(0, 0, 0, active ? 150 : 0), 255);
        if (active) Render2D.drawRoundedRect(
                context.getMatrices(),
                getX() + (getWidth() - getWidth() * animation.getValue()) / 2,
                getY() + (getHeight() - getHeight() * animation.getValue()) / 2,
                getWidth() * animation.getValue(),
                getHeight() * animation.getValue(),
                1.5f,
                ColorUtils.getGlobalColor(MathHelper.clamp((int) (200 * animation.getValue()), 0, 255))
        );
        if (!(Fonts.REGULAR.getWidth(getMessage().getString(), 8f) > getWidth()) && !getMessage().getString().isEmpty()) {
            Render2D.drawFont(
                    context.getMatrices(),
                    Fonts.REGULAR.getFont(8f),
                    getMessage().getString(),
                    getX() - Fonts.REGULAR.getWidth(getMessage().getString(), 8f) / 2f + getWidth() / 2f,
                    getY() + 5f,
                    Color.WHITE
            );
        }
    }
}