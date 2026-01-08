package fun.motherhack.modules.impl.movement;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import fun.motherhack.api.events.impl.EventKeyboardInput;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.utils.movement.MoveUtils;
import fun.motherhack.utils.network.NetworkUtils;
import lombok.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;

public class GuiMove extends Module {
	
	public final BooleanSetting funtime = new BooleanSetting("Funtime", false);

    public GuiMove() {
        super("GuiMove", Category.Movement);
    }
    
    @Getter @Setter private int ticks = 0;
    private final List<Packet<?>> packets = new CopyOnWriteArrayList<>();

    @EventHandler
    public void onPlayerTick(EventPlayerTick e) {
        if (fullNullCheck()) return;
        if (mc.currentScreen == null
        		|| mc.currentScreen instanceof ChatScreen 
        		|| mc.currentScreen instanceof SignEditScreen 
        		|| mc.currentScreen instanceof AnvilScreen
        		|| (funtime.getValue() && mc.currentScreen instanceof GenericContainerScreen)
        ) return;

        for (KeyBinding binding : new KeyBinding[]{mc.options.forwardKey, mc.options.backKey, mc.options.rightKey, mc.options.leftKey, mc.options.jumpKey}) {
        	if (!InputUtil.isKeyPressed(mc.getWindow().getHandle(), binding.getDefaultKey().getCode())) continue;
            binding.setPressed(true);
        }
    }
    
    @EventHandler
    public void onPacketSend(EventPacket.Send e) {
    	if (fullNullCheck() || !funtime.getValue()) return;
    	
    	if (e.getPacket() instanceof ClickSlotC2SPacket && mc.currentScreen instanceof InventoryScreen && MoveUtils.isMoving()) {
    		packets.add(e.getPacket());
    		e.cancel();
    	} else if (e.getPacket() instanceof CloseHandledScreenC2SPacket && !packets.isEmpty() && mc.currentScreen instanceof InventoryScreen && MoveUtils.isMoving()) {
    		ticks = 8;
    		new Thread(() -> {
    			try {
					Thread.sleep(ticks * 50L);
				} catch (Exception ex) {}
            	resumePackets();
    		}).start();
            e.cancel();
    	}
    }
    
    @EventHandler
    public void onKeyboardInput(EventKeyboardInput e) {
    	if (fullNullCheck() || !funtime.getValue()) return;
    	
    	if (ticks > 0) {
    		e.setMovementForward(0);
    		e.setMovementSideways(0);
    		ticks--;
    	}
    }
    
    private void resumePackets() {
    	if (packets.isEmpty()) return;
    	for (Packet<?> packet : packets) NetworkUtils.sendSilentPacket(packet);
    	packets.clear();
    }
    
    @Override
    public void onEnable() {
    	super.onEnable();
    	ticks = 0;
    }
    
    @Override
    public void onDisable() {
    	super.onDisable();
    	ticks = 0;
    }
}