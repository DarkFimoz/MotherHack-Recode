package fun.motherhack.modules.impl.render;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.combat.Aura;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.animations.Animation;
import fun.motherhack.utils.animations.Easing;
import fun.motherhack.utils.render.ColorUtils;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.world.WorldUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

public class TargetEsp extends Module {

    private final NumberSetting size = new NumberSetting(I18n.translate("settings.targetesp.size"), 15f, 10f, 25f, 1f);

    public TargetEsp() {
        super("TargetEsp", Category.Render);
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
    }
}
