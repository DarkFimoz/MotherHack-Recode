package fun.motherhack.modules.impl.movement;

import fun.motherhack.api.events.impl.EventKeyboardInput;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.network.NetworkUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.AllArgsConstructor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

public class NoSlow extends Module {

    private final EnumSetting<Mode> mode = new EnumSetting<>("settings.mode", Mode.GrimV3);
    private final NumberSetting ticks = new NumberSetting("settings.noslow.ticks", 3f, 1f, 5f, 1f, () -> mode.getValue() == Mode.GrimV3);

    public NoSlow() {
        super("NoSlow", Category.Movement);
    }

    @EventHandler
    public void onKeyboardInput(EventKeyboardInput e) {
        if (fullNullCheck()) return;

        if (mode.getValue() == Mode.GrimV3) {
            if (mc.player.isUsingItem()) {
                if (mc.player.getItemUseTime() <= ticks.getValue()) {
                    NetworkUtils.sendSilentPacket(new ClickSlotC2SPacket(
                            mc.player.currentScreenHandler.syncId,
                            0,
                            1,
                            0,
                            SlotActionType.PICKUP,
                            ItemStack.EMPTY,
                            new Int2ObjectOpenHashMap<>())
                    );
                } else {
                    e.setMovementForward(e.getMovementForward() * 5f);
                    e.setMovementSideways(e.getMovementSideways() * 5f);
                }
            }
        }
    }

    @AllArgsConstructor
    private enum Mode implements Nameable {
        GrimV3("settings.noslow.mode.grimv3");

        private final String name;

        @Override
        public String getName() {
            return name;
        }
    }
}