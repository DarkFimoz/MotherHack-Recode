package fun.motherhack.modules.impl.render;

import fun.motherhack.MotherHack;
import fun.motherhack.api.mixins.accessors.IClientPlayerInteractionManager;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.ColorSetting;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.utils.render.Render3D;
import lombok.AllArgsConstructor;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import fun.motherhack.api.events.impl.EventRender3D;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import java.awt.*;

public class BreakHighLight extends Module {
    private final EnumSetting<Mode> mode = new EnumSetting<>("Режим", Mode.Shrink);
    private final BooleanSetting useUIColor = new BooleanSetting("Использовать цвет UI", true);
    private final ColorSetting color = new ColorSetting("Цвет", new Color(0x90FD0000, true));
    private final ColorSetting color2 = new ColorSetting("Цвет 2", new Color(0xFFFD0000, true));
    private final NumberSetting lineWidth = new NumberSetting("Ширина линии", 2f, 0f, 5f, 0.1f);
    private final BooleanSetting otherPlayer = new BooleanSetting("Другие игроки", true);

    @AllArgsConstructor
    @Getter
    public enum Mode implements Nameable {
        Grow("Рост"),
        Shrink("Сжатие"),
        Static("Статичный");

        private final String name;
    }

    public BreakHighLight() {
        super("BreakHighLight", Category.Render);
        getSettings().add(mode);
        getSettings().add(useUIColor);
        getSettings().add(color);
        getSettings().add(color2);
        getSettings().add(lineWidth);
        getSettings().add(otherPlayer);
        
        color.setVisible(() -> !useUIColor.getValue());
        color2.setVisible(() -> !useUIColor.getValue());
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;
        
        if (mc.interactionManager != null && mc.interactionManager.isBreakingBlock()) {
            IClientPlayerInteractionManager accessor = (IClientPlayerInteractionManager) mc.interactionManager;
            BlockPos pos = accessor.getCurrentBreakingPos();
            float progress = accessor.getCurrentBreakingProgress();
            
            if (pos != null && progress > 0) {
                renderBreakingBlock(event.getMatrixStack(), pos, progress);
            }
        }
    }

    private void renderBreakingBlock(net.minecraft.client.util.math.MatrixStack matrices, BlockPos pos, float progress) {
        Box box = new Box(pos);
        
        switch (mode.getValue()) {
            case Grow -> {
                double shrink = (1.0 - progress) * 0.5;
                box = box.contract(shrink, shrink, shrink);
            }
            case Shrink -> {
                double expand = progress * 0.5;
                box = box.expand(expand, expand, expand);
            }
            case Static -> {
            }
        }
        
        Color fillColor = getColor();
        Color outlineColor = getColor2();
        
        Render3D.renderBox(matrices, box, fillColor);
        Render3D.renderBoxOutline(matrices, box, outlineColor);
    }

    public Color getColor() {
        if (useUIColor.getValue()) {
            UI ui = MotherHack.getInstance().getModuleManager().getModule(UI.class);
            if (ui != null) {
                return ui.getTheme().getAccentColor();
            }
        }
        return color.getColor();
    }
    
    public Color getColor2() {
        if (useUIColor.getValue()) {
            UI ui = MotherHack.getInstance().getModuleManager().getModule(UI.class);
            if (ui != null) {
                Color accent = ui.getTheme().getAccentColor();
                return new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 150);
            }
        }
        return color2.getColor();
    }
}
