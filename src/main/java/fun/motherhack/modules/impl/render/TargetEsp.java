package fun.motherhack.modules.impl.render;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.combat.Aura;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.render.ColorUtils;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.world.WorldUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

public class TargetEsp extends Module {

    public final EnumSetting<Mode> mode = new EnumSetting<>("settings.targetesp.mode", Mode.Client);
    public final NumberSetting size = new NumberSetting(I18n.translate("settings.targetesp.size"), 15f, 10f, 25f, 1f);
    public final NumberSetting speed = new NumberSetting(I18n.translate("settings.targetesp.speed"), 3f, 0.7f, 9f, 0.1f, () -> mode.getValue() == Mode.Ghosts);
    public final NumberSetting ghostSize = new NumberSetting(I18n.translate("settings.targetesp.ghostsize"), 30f, 5f, 140f, 1f, () -> mode.getValue() == Mode.Ghosts);
    public final NumberSetting brightness = new NumberSetting(I18n.translate("settings.targetesp.brightness"), 255f, 1f, 255f, 1f, () -> mode.getValue() == Mode.Ghosts);
    public final EnumSetting<ColorMode> colorMode = new EnumSetting<>("settings.targetesp.colormode", ColorMode.UIColor, () -> mode.getValue() == Mode.Ghosts);

    public TargetEsp() {
        super("TargetEsp", Category.Render);
    }
    
    @AllArgsConstructor @Getter
    public enum ColorMode implements Nameable {
        Rainbow("settings.targetesp.colormode.rainbow"),
        UIColor("settings.targetesp.colormode.uicolor");

        private final String name;

        @Override
        public String getName() {
            return I18n.translate(name);
        }
    }
    
    public enum Mode implements Nameable {
        Client("settings.targetesp.mode.client"),
        Ghosts("settings.targetesp.mode.ghosts");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return I18n.translate(name);
        }
    }

    private final Animation animation = new Animation(300, 1f, true, Easing.BOTH_SINE);
    private LivingEntity lastTarget = null;

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck()) return;
        
        Aura aura = MotherHack.getInstance().getModuleManager().getModule(Aura.class);
        if (aura.getTarget() != null) lastTarget = aura.getTarget();
        animation.update(aura.getTarget() != null);
        
        if (animation.getValue() > 0 && lastTarget != null) {
            if (lastTarget.isRemoved() || !lastTarget.isAlive()) {
                lastTarget = null;
                return;
            }

            if (mode.getValue() == Mode.Client) {
                renderClientMode(e);
            } else if (mode.getValue() == Mode.Ghosts) {
                renderGhostsMode(e);
            }
        }
    }

    private void renderClientMode(EventRender2D e) {
        double sin = Math.sin(System.currentTimeMillis() / 1000.0);
        double deltaX = lastTarget.getX() - mc.player.getX();
        double deltaZ = lastTarget.getZ() - mc.player.getZ();
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float maxSize = (float) WorldUtils.getScale(lastTarget.getPos(), size.getValue() * animation.getValue());
        float alpha = (float) Math.sin(lastTarget.hurtTime * (18F * 0.017453292519943295F));
        float finalSize = Math.max(maxSize - (float) distance, size.getValue());
        Vec3d interpolated = lastTarget.getLerpedPos(e.getTickCounter().getTickDelta(true));
        Vec3d pos = WorldUtils.getPosition(new Vec3d(interpolated.x, interpolated.y + lastTarget.getHeight() / 2f, interpolated.z));
        Color color = ColorUtils.fade(new Color(255, 255, 255, (int) (255 * animation.getValue())), new Color(255, 0, 0, (int) (255 * animation.getValue())), alpha);
        
        if (!(pos.z > 0) || !(pos.z < 1)) return;

        e.getContext().getMatrices().push();
        e.getContext().getMatrices().translate(pos.getX(), pos.getY(), 0);
        e.getContext().getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) sin * 360));
        Render2D.drawTexture(e.getContext().getMatrices(), -finalSize / 2f, -finalSize / 2f, finalSize, finalSize, 0f,
                MotherHack.id("hud/marker.png"), color);
        e.getContext().getMatrices().pop();
    }

    private void renderGhostsMode(EventRender2D e) {
        float speedValue = speed.getValue();
        float sizeValue = ghostSize.getValue();
        float brightnessValue = brightness.getValue();
        
        double speedTime = speedValue;
        double time = System.currentTimeMillis() / (500.0 / speedTime);
        
        Vec3d interpolated = lastTarget.getLerpedPos(e.getTickCounter().getTickDelta(true));
        Vec3d bodyPos = new Vec3d(interpolated.x, interpolated.y + lastTarget.getHeight() / 2f, interpolated.z);
        Vec3d legPos = new Vec3d(interpolated.x, interpolated.y, interpolated.z);
        
        Vec3d[] upperPositions = new Vec3d[]{bodyPos.add(0, 0.5, 0)};
        Vec3d[] lowerPositions = new Vec3d[]{legPos.add(0, 0.5, 0)};
        
        for (int j = 0; j < 40; j++) {
            float alpha = brightnessValue - j * 5;
            if (alpha < 0) alpha = 0;
            
            float trailSize = sizeValue * (1f - j * 0.02f);
            double trailTime = time - j * 0.1;
            double trailSin = Math.sin(trailTime);
            double trailCos = Math.cos(trailTime);
            float angleOffset = j * 7.2f;
            
            // Upper positions
            for (int i = 0; i < upperPositions.length; i++) {
                Vec3d pos3d = upperPositions[i].add(0, Math.sin(trailTime) * 0.26, 0);
                Vec3d screenPos = WorldUtils.getPosition(new Vec3d(
                    pos3d.x + trailCos * 0.5,
                    pos3d.y,
                    pos3d.z + trailSin * 0.5
                ));
                
                if (!(screenPos.z > 0) || !(screenPos.z < 1)) continue;
                
                Color color = getGhostColor((int) (trailSin * 360 + i * 180 + angleOffset), (int) alpha);
                
                e.getContext().getMatrices().push();
                e.getContext().getMatrices().translate(screenPos.x, screenPos.y, 0);
                e.getContext().getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) (trailSin * 360 + i * 180 + angleOffset)));
                Render2D.drawTexture(e.getContext().getMatrices(), -trailSize / 2f, -trailSize / 2f, trailSize, trailSize, 0f,
                        MotherHack.id("hud/glow.png"), color);
                e.getContext().getMatrices().pop();
            }
            
            // Lower positions
            for (int i = 0; i < lowerPositions.length; i++) {
                Vec3d pos3d = lowerPositions[i].add(0, Math.sin(trailTime) * 0.26, 0);
                Vec3d screenPos = WorldUtils.getPosition(new Vec3d(
                    pos3d.x - trailCos * 0.5,
                    pos3d.y,
                    pos3d.z - trailSin * 0.5
                ));
                
                if (!(screenPos.z > 0) || !(screenPos.z < 1)) continue;
                
                Color color = getGhostColor((int) (-trailSin * 360 + i * 180 + angleOffset), (int) alpha);
                
                e.getContext().getMatrices().push();
                e.getContext().getMatrices().translate(screenPos.x, screenPos.y, 0);
                e.getContext().getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) (-trailSin * 360 + i * 180 + angleOffset)));
                Render2D.drawTexture(e.getContext().getMatrices(), -trailSize / 2f, -trailSize / 2f, trailSize, trailSize, 0f,
                        MotherHack.id("hud/glow.png"), color);
                e.getContext().getMatrices().pop();
            }
        }
    }
    
    private Color getGhostColor(int offset, int alpha) {
        if (colorMode.getValue() == ColorMode.Rainbow) {
            // Rainbow режим
            float hue = (System.currentTimeMillis() % 10000) / 10000f;
            hue += offset / 360f;
            hue = hue % 1f;
            return new Color(Color.HSBtoRGB(hue, 0.7f, 1f) & 0xFFFFFF | (alpha << 24), true);
        } else {
            // UIColor режим - берем цвет из UI
            UI uiModule = MotherHack.getInstance().getModuleManager().getModule(UI.class);
            if (uiModule != null) {
                Color uiColor = uiModule.getTheme().getAccentColor();
                return new Color(uiColor.getRed(), uiColor.getGreen(), uiColor.getBlue(), alpha);
            }
            // Fallback на белый цвет
            return new Color(255, 255, 255, alpha);
        }
    }
}
