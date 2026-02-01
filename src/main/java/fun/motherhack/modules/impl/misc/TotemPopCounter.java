package fun.motherhack.modules.impl.misc;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import fun.motherhack.api.events.impl.EventPopTotem;
import fun.motherhack.api.events.impl.EventRender2D;
import net.minecraft.util.Formatting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;

public class TotemPopCounter extends Module {
    public final BooleanSetting notification = new BooleanSetting("Уведомление", true);
    public final BooleanSetting renderTotem = new BooleanSetting("Рендер тотема", true);

    private final Animation colorAnimation = new Animation(500, 1.0, false, Easing.EASE_OUT_CUBIC);
    private final Animation rotationAnimation = new Animation(400, 0.0, false, Easing.EASE_OUT_BACK);
    private boolean shouldAnimate = false;
    
    private final Map<String, Integer> popCounts = new HashMap<>();

    public TotemPopCounter() {
        super("TotemPopCounter", Category.Misc);
        getSettings().add(notification);
        getSettings().add(renderTotem);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        popCounts.clear();
    }

    @EventHandler
    public void onTotemPop(EventPopTotem event) {
        if (fullNullCheck()) return;

        PlayerEntity player = event.getPlayer();
        if (player == null) return;

        String name = player.getName().getString();
        int count = popCounts.getOrDefault(name, 0) + 1;
        popCounts.put(name, count);
        
        if (player == mc.player) {
            if (renderTotem.getValue()) {
                shouldAnimate = true;
                colorAnimation.reset();
                rotationAnimation.reset();
            }
        }
        
        if (notification.getValue()) {
            String totemWord;
            if (count == 1) {
                totemWord = "тотем";
            } else if (count >= 2 && count <= 4) {
                totemWord = "тотема";
            } else {
                totemWord = "тотемов";
            }
            
            String message = Formatting.GREEN + name + Formatting.WHITE + " попнул " + 
                            Formatting.AQUA + count + Formatting.WHITE + " " + totemWord + "!";
            if (mc.player != null) {
                mc.player.sendMessage(net.minecraft.text.Text.literal(message), false);
            }
        }
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        if (fullNullCheck() || !renderTotem.getValue()) return;

        int totemCount = 0;
        for (ItemStack stack : mc.player.getInventory().main) {
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                totemCount += stack.getCount();
            }
        }
        
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
            totemCount += mc.player.getOffHandStack().getCount();
        }

        if (totemCount == 0) return;

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        
        int baseX = screenWidth / 2 + 20;
        int baseY = screenHeight / 2 - 8;
        
        DrawContext context = event.getContext();
        MatrixStack matrices = context.getMatrices();
        
        RenderSystem.enableBlend();
        
        matrices.push();
        
        float rotation = 0f;
        float redProgress = 0f;
        
        if (shouldAnimate) {
            redProgress = 1.0f - (float) colorAnimation.getValue();
            rotation = (float) (rotationAnimation.getValue() * 15.0);
            
            if (colorAnimation.finished() && rotationAnimation.finished()) {
                shouldAnimate = false;
            }
        }
        
        matrices.translate(baseX + 8, baseY + 8, 0);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        matrices.translate(-(baseX + 8), -(baseY + 8), 0);
        
        float red = 1.0f;
        float green = 1.0f - redProgress;
        float blue = 1.0f - redProgress;
        
        RenderSystem.setShaderColor(red, green, blue, 1.0f);
        
        ItemStack totemStack = new ItemStack(Items.TOTEM_OF_UNDYING);
        context.drawItem(totemStack, baseX, baseY);
        
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        
        matrices.pop();
        
        String countText = String.valueOf(totemCount);
        int textX = baseX + 18;
        int textY = baseY + 4;
        
        context.drawText(mc.textRenderer, countText, textX, textY, 0xFFFFFF, true);
        
        RenderSystem.disableBlend();
    }
}
