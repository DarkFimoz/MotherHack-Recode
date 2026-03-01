package fun.motherhack.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.motherhack.api.events.impl.EventRender3D;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.ColorSetting;
import fun.motherhack.utils.render.Render3D;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.*;
import net.minecraft.util.math.Box;

import java.awt.*;

public class BaseFinder extends Module {

    @Getter
    private static BaseFinder instance;

    private final BooleanSetting showChests = new BooleanSetting("Сундуки", true);
    private final BooleanSetting showEnderChests = new BooleanSetting("Эндер сундуки", true);
    private final BooleanSetting showShulkers = new BooleanSetting("Шалкеры", true);
    private final ColorSetting chestColor = new ColorSetting("Цвет сундуков", new Color(255, 200, 0, 150));
    private final ColorSetting enderColor = new ColorSetting("Цвет эндер сундуков", new Color(170, 0, 170, 150));
    private final ColorSetting shulkerColor = new ColorSetting("Цвет шалкеров", new Color(200, 100, 255, 150));

    public BaseFinder() {
        super("BaseFinder", Category.Render);
        instance = this;
        getSettings().add(showChests);
        getSettings().add(showEnderChests);
        getSettings().add(showShulkers);
        getSettings().add(chestColor);
        getSettings().add(enderColor);
        getSettings().add(shulkerColor);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        fun.motherhack.utils.world.BlockUtils.getLoadedBlockEntities().forEach(blockEntity -> {
            Color color = getColorForBlockEntity(blockEntity);
            if (color != null) {
                Box box = new Box(blockEntity.getPos());
                Render3D.renderBox(event.getMatrixStack(), box, color);
                Render3D.renderBoxOutline(event.getMatrixStack(), box, 
                    new Color(color.getRed(), color.getGreen(), color.getBlue(), 255));
            }
        });

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private Color getColorForBlockEntity(BlockEntity blockEntity) {
        if (showChests.getValue() && (blockEntity instanceof ChestBlockEntity || blockEntity instanceof TrappedChestBlockEntity)) {
            return chestColor.getValue();
        }
        if (showEnderChests.getValue() && blockEntity instanceof EnderChestBlockEntity) {
            return enderColor.getValue();
        }
        if (showShulkers.getValue() && blockEntity instanceof ShulkerBoxBlockEntity) {
            return shulkerColor.getValue();
        }
        return null;
    }
}
