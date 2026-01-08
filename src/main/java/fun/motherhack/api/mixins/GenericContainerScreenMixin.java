package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.impl.misc.AuctionHelper;
import fun.motherhack.utils.auction.AuctionUtils;
import fun.motherhack.utils.render.ColorUtils;
import fun.motherhack.utils.render.Render2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(GenericContainerScreen.class)
public abstract class GenericContainerScreenMixin extends HandledScreen<GenericContainerScreenHandler> {

    public GenericContainerScreenMixin(GenericContainerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/GenericContainerScreen;drawMouseoverTooltip(Lnet/minecraft/client/gui/DrawContext;II)V"))
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {

        if (MotherHack.getInstance().getModuleManager().getModule(AuctionHelper.class).isToggled() && (getTitle().getString().contains("Аукцион") || getTitle().getString().contains("Поиск"))) {
            int minPrice = Integer.MAX_VALUE;
            int maxPrice = Integer.MIN_VALUE;
            int totalPrice = 0;
            int priceCount = 0;
            float avgPrice;

            Slot minSlot = null;
            Slot maxSlot = null;
            Slot avgSlot = null;

            for (Slot slot : handler.slots) {
                if (slot.hasStack()) {
                    NbtComponent component = slot.getStack().get(DataComponentTypes.CUSTOM_DATA);
                    int price = AuctionUtils.getPrice(component);

                    if (price >= 0) {
                        if (price < minPrice) {
                            minPrice = price;
                            minSlot = slot;
                        }

                        if (price > maxPrice) {
                            maxPrice = price;
                            maxSlot = slot;
                        }

                        totalPrice += price;
                        priceCount++;
                    }
                }
            }

            if (priceCount > 0) {
                avgPrice = totalPrice / (float) priceCount;
                float closestDiff = Float.MAX_VALUE;

                for (Slot slot : handler.slots) {
                    if (slot.hasStack()) {
                        NbtComponent component = slot.getStack().get(DataComponentTypes.CUSTOM_DATA);
                        int price = AuctionUtils.getPrice(component);

                        if (price >= 0) {
                            float diff = Math.abs(price - avgPrice);

                            if (diff < closestDiff) {
                                closestDiff = diff;
                                avgSlot = slot;
                            }
                        }
                    }
                }
            }

            if (minSlot != null) {
                Render2D.drawRoundedRect(
                        context.getMatrices(),
                        minSlot.x + (width / 2f) - backgroundWidth / 2f,
                        minSlot.y + (height / 2f) - backgroundHeight / 2f,
                        16f,
                        16f,
                        0f,
                        ColorUtils.pulse(new Color(0, 255, 0, 200), 20)
                );
            }

            if (maxSlot != null) {
                Render2D.drawRoundedRect(
                        context.getMatrices(),
                        maxSlot.x + (width / 2f) - backgroundWidth / 2f,
                        maxSlot.y + (height / 2f) - backgroundHeight / 2f,
                        16f,
                        16f,
                        0f,
                        ColorUtils.pulse(new Color(255, 0, 0, 200), 20)
                );
            }

            if (avgSlot != null) {
                Render2D.drawRoundedRect(
                        context.getMatrices(),
                        avgSlot.x + (width / 2f) - backgroundWidth / 2f,
                        avgSlot.y + (height / 2f) - backgroundHeight / 2f,
                        16f,
                        16f,
                        0f,
                        ColorUtils.pulse(new Color(0, 100, 255, 200), 20)
                );
            }
        }
    }
}