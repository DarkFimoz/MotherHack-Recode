package fun.motherhack.modules.impl.misc;

import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.api.mixins.accessors.IHandledScreen;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

public class ItemScroller extends Module {

    private final NumberSetting delay = new NumberSetting("Delay", 50, 0, 500, 10);
    
    private long lastClickTime = 0;
    private Slot lastHoveredSlot = null;

    public ItemScroller() {
        super("ItemScroller", Category.Misc);
        getSettings().add(delay);
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (fullNullCheck()) return;
        
        // Проверяем что открыт инвентарь или сундук (HandledScreen включает InventoryScreen)
        if (!(mc.currentScreen instanceof HandledScreen)) return;
        
        HandledScreen<?> screen = (HandledScreen<?>) mc.currentScreen;
        
        // Проверяем что зажаты ЛКМ и Shift
        boolean leftMousePressed = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean shiftPressed = GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS 
                            || GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        
        if (!leftMousePressed || !shiftPressed) {
            lastHoveredSlot = null;
            return;
        }
        
        // Получаем слот под курсором через accessor
        Slot hoveredSlot = ((IHandledScreen) screen).getFocusedSlot();
        
        // Проверяем что слот существует и в нём есть предмет
        if (hoveredSlot == null || !hoveredSlot.hasStack()) return;
        
        // Проверяем задержку
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < delay.getValue()) return;
        
        // Выполняем клик с Shift (быстрое перемещение)
        // Работает и для инвентаря (InventoryScreen), и для сундуков (GenericContainerScreen)
        mc.interactionManager.clickSlot(
            screen.getScreenHandler().syncId,
            hoveredSlot.id,
            0,
            SlotActionType.QUICK_MOVE,
            mc.player
        );
        
        lastClickTime = currentTime;
        lastHoveredSlot = hoveredSlot;
    }
}
