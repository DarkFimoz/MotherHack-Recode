package fun.motherhack.modules.impl.misc;

import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.*;
import fun.motherhack.utils.auction.ab.impl.KrushItems;
import fun.motherhack.utils.math.TimerUtils;
import fun.motherhack.utils.auction.AuctionUtils;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import meteordevelopment.orbit.EventHandler;

public class AutoBuy extends Module {

    private final NumberSetting timer1 = new NumberSetting("Таймер открытия", 1000, 0, 5000, 25);
    private final NumberSetting timer2 = new NumberSetting("Таймер обновления", 500, 0, 5000, 25);
    private final NumberSetting timer3 = new NumberSetting("Таймер покупки", 500, 0, 5000, 25);
    private final BooleanSetting krushHelmet = new BooleanSetting("Шлем крушителя", true);
    private final StringSetting krushHelmetPrice = new StringSetting("Цена шлема круш.", "3000000", krushHelmet::getValue, true);
    private final BooleanSetting krushChestplate = new BooleanSetting("Нагрудник крушителя", true);
    private final StringSetting krushChestplatePrice = new StringSetting("Цена нагрудника круш.", "3000000", krushChestplate::getValue, true);
    private final BooleanSetting krushLeggings = new BooleanSetting("Поножи крушителя", true);
    private final StringSetting krushLeggingsPrice = new StringSetting("Цена поножей круш.", "3000000", krushLeggings::getValue, true);
    private final BooleanSetting krushBoots = new BooleanSetting("Ботинки крушителя", true);
    private final StringSetting krushBootsPrice = new StringSetting("Цена ботинок круш.", "3000000", krushBoots::getValue, true);
    private final BooleanSetting krushSword = new BooleanSetting("Меч крушителя", true);
    private final StringSetting krushSwordPrice = new StringSetting("Цена меча круш.", "3000000", krushSword::getValue, true);
    private final BooleanSetting krushPickaxe = new BooleanSetting("Кирка крушителя", true);
    private final StringSetting krushPickaxePrice = new StringSetting("Цена кирки круш.", "3000000", krushPickaxe::getValue, true);
    private final BooleanSetting krushTrident = new BooleanSetting("Трезубец крушителя", true);
    private final StringSetting krushTridentPrice = new StringSetting("Цена трезубца круш.", "3000000", krushTrident::getValue, true);
    private final BooleanSetting krushCrossbow = new BooleanSetting("Арбалет крушителя", true);
    private final StringSetting krushCrossbowPrice = new StringSetting("Цена арбалета круш.", "3000000", krushCrossbow::getValue, true);

    public AutoBuy() {
        super("AutoBuy", Category.Misc);
    }

    private final TimerUtils openTimer = new TimerUtils();
    private final TimerUtils updateTimer = new TimerUtils();
    private final TimerUtils buyTimer = new TimerUtils();
    private boolean open = false;

    @EventHandler
    public void onTick(EventTick e) {
        if (fullNullCheck()) return;

        if (mc.currentScreen instanceof GenericContainerScreen screen) {
            String title = screen.getTitle().getString();
            int syncId = screen.getScreenHandler().syncId;

            if (title.contains("Аукцион") || title.contains("Поиск")) {
                if (!open) {
                    open = true;
                    openTimer.reset();
                    updateTimer.reset();
                    buyTimer.reset();
                    return;
                }

                if (openTimer.passed(timer1.getValue().longValue())) {
                    if (buyTimer.passed(timer3.getValue().longValue()) && updateTimer.passed(timer2.getValue().longValue())) {
                        mc.interactionManager.clickSlot(syncId, 49, 0, SlotActionType.QUICK_MOVE, mc.player);
                        updateTimer.reset();
                    }

                    for (Slot slot : screen.getScreenHandler().slots) {
                        if (slot.getStack().isEmpty()) continue;
                        if (slot.id > 44) continue;
                        NbtComponent nbt = slot.getStack().get(DataComponentTypes.CUSTOM_DATA);

                        if (isBuying(slot.getStack(), nbt) && buyTimer.passed(timer3.getValue().longValue())) {
                            mc.interactionManager.clickSlot(syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                            buyTimer.reset();
                            updateTimer.reset();
                            break;
                        }
                    }
                }
            } else if (title.contains("Подтверждение")) {
                openTimer.reset();
                buyTimer.reset();
                mc.interactionManager.clickSlot(syncId, 0, 0, SlotActionType.QUICK_MOVE, mc.player);
            } else open = false;
        } else open = false;
    }

    private boolean isBuying(ItemStack stack, NbtComponent nbt) {
        if (AuctionUtils.compareEnchantments(stack, KrushItems.getHelmet()) && krushHelmet.getValue() && AuctionUtils.getPrice(nbt) <= Integer.parseInt(krushHelmetPrice.getValue())) return true;
        else if (AuctionUtils.compareEnchantments(stack, KrushItems.getChestplate()) && krushChestplate.getValue() && AuctionUtils.getPrice(nbt) <= Integer.parseInt(krushChestplatePrice.getValue())) return true;
        else if (AuctionUtils.compareEnchantments(stack, KrushItems.getLeggings()) && krushLeggings.getValue() && AuctionUtils.getPrice(nbt) <= Integer.parseInt(krushLeggingsPrice.getValue())) return true;
        else if (AuctionUtils.compareEnchantments(stack, KrushItems.getBoots()) && krushBoots.getValue() && AuctionUtils.getPrice(nbt) <= Integer.parseInt(krushBootsPrice.getValue())) return true;
        else if (AuctionUtils.compareEnchantments(stack, KrushItems.getSword()) && krushSword.getValue() && AuctionUtils.getPrice(nbt) <= Integer.parseInt(krushSwordPrice.getValue())) return true;
        else if (AuctionUtils.compareEnchantments(stack, KrushItems.getPickaxe()) && krushPickaxe.getValue() && AuctionUtils.getPrice(nbt) <= Integer.parseInt(krushPickaxePrice.getValue())) return true;
        else if (AuctionUtils.compareEnchantments(stack, KrushItems.getTrident()) && krushTrident.getValue() && AuctionUtils.getPrice(nbt) <= Integer.parseInt(krushTridentPrice.getValue())) return true;
        return AuctionUtils.compareEnchantments(stack, KrushItems.getCrossbow()) && krushCrossbow.getValue() && AuctionUtils.getPrice(nbt) <= Integer.parseInt(krushCrossbowPrice.getValue());
    }
}