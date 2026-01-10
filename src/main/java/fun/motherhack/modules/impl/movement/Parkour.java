package fun.motherhack.modules.impl.movement;

import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.Random;

public class Parkour extends Module {

    private final BooleanSetting randomPixel = new BooleanSetting("settings.parkour.randompixel", false);
    private final NumberSetting minOffset = new NumberSetting("settings.parkour.minoffset", 0.01f, 0.001f, 0.1f, 0.001f, () -> randomPixel.getValue());
    private final NumberSetting maxOffset = new NumberSetting("settings.parkour.maxoffset", 0.05f, 0.001f, 0.1f, 0.001f, () -> randomPixel.getValue());

    private final Random random = new Random();

    public Parkour() {
        super("Parkour", Category.Movement);
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (fullNullCheck()) return;
        if (!mc.player.isOnGround() || mc.player.isSneaking()) return;

        double expandOffset = randomPixel.getValue() 
            ? -(minOffset.getValue() + random.nextDouble() * (maxOffset.getValue() - minOffset.getValue()))
            : -0.001;

        Box box = mc.player.getBoundingBox().offset(0, -0.5, 0).expand(expandOffset, 0, expandOffset);

        BlockPos.Mutable pos = new BlockPos.Mutable();
        
        for (int x = (int) Math.floor(box.minX); x <= Math.floor(box.maxX); x++) {
            for (int z = (int) Math.floor(box.minZ); z <= Math.floor(box.maxZ); z++) {
                pos.set(x, (int) Math.floor(box.minY), z);
                if (mc.world.getBlockState(pos).getBlock() != Blocks.AIR) {
                    return;
                }
            }
        }

        mc.player.jump();
    }
}
