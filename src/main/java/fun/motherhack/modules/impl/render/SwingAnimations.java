package fun.motherhack.modules.impl.render;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.combat.Aura;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import lombok.AllArgsConstructor;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class SwingAnimations extends Module {

    public EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.Smooth);
    public BooleanSetting onlyAura = new BooleanSetting("OnlyAura", false);
    public BooleanSetting slowAnimation = new BooleanSetting("SlowAnimation", true);
    public NumberSetting slowAnimationVal = new NumberSetting("SlowValue", 12, 1, 50, 1);
    public NumberSetting swingPower = new NumberSetting("SwingPower", 5.0f, 1.0f, 10.0f, 1.0f);
    public NumberSetting angle = new NumberSetting("Angle", 0.0f, 0.0f, 360.0f, 1.0f);

    public boolean flip = false;

    @AllArgsConstructor
    @Getter
    public enum Mode implements Nameable {
        Smooth("Smooth"),
        Self("Self"),
        Self2("Self2"),
        Down("Down"),
        Forward("Forward"),
        Touch("Touch"),
        Pander("Pander"),
        Curt("Curt"),
        BlockHit("BlockHit");

        private final String name;
    }

    public SwingAnimations() {
        super("SwingAnimations", Category.Render);
    }

    public boolean shouldAnimate() {
        Aura aura = MotherHack.getInstance().getModuleManager().getModule(Aura.class);
        return isToggled() 
            && (!onlyAura.getValue() || (aura != null && aura.isToggled() && aura.getTarget() != null));
    }

    public boolean shouldChangeAnimationDuration() {
        Aura aura = MotherHack.getInstance().getModuleManager().getModule(Aura.class);
        return isToggled() 
            && (!onlyAura.getValue() || (aura != null && aura.isToggled() && aura.getTarget() != null));
    }

    @EventHandler
    public void onPacketSend(EventPacket.Send e) {
        if (e.getPacket() instanceof HandSwingC2SPacket) {
            flip = !flip;
        }
    }

    public void renderSwordAnimation(MatrixStack matrices, float f, float swingProgress, float equipProgress, Arm arm) {
        float anim = (float) Math.sin(swingProgress * Math.PI / 2.0 * 2.0);
        float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);

        switch (mode.getValue()) {
            case Smooth -> {
                float power = swingPower.getValue().floatValue() * 10.0F;
                float g = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F + g * (-power / 4.0F)));
                float sinExtra = MathHelper.sin(MathHelper.sqrt(swingProgress * swingProgress) * (float) Math.PI);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sinExtra * -(power / 4.0F)));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sinExtra * -power));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45.0F));
            }
            case Self2 -> {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-30.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-angle.getValue().floatValue() - swingPower.getValue().floatValue() * 10.0F * anim));
            }
            case Forward -> {
                float power = 35.0F;
                matrices.translate(0.0, 0.0, -0.3 * sin2);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -power));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * power));
            }
            case Self -> {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-60.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-angle.getValue().floatValue() - swingPower.getValue().floatValue() * 10.0F * anim));
            }
            case Down -> {
                matrices.translate(0.0F, -anim * swingPower.getValue().floatValue() / 24.0F, 0.0F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-30.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
            }
            case Touch -> {
                matrices.scale(1.0F, 1.0F, 1.0F + anim * swingPower.getValue().floatValue() / 4.0F);
                matrices.translate(0.0F, 0.0F, -0.265F);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0F));
            }
            case Curt -> {
                float sqrtProgress = MathHelper.sqrt(swingProgress);
                float g = MathHelper.sin(sqrtProgress * (float) Math.PI);
                float sinExtra = MathHelper.sin(swingProgress * (float) Math.PI);
                matrices.translate(0.4F - g * 0.2F, -0.2F + g * 0.3F, -0.5F - sinExtra * 0.2F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(91.0F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F + g * -100.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F));
            }
            case Pander -> {
                matrices.scale(0.8F, 0.8F, 0.8F);
                matrices.translate(0.3 - anim * 0.15F, 0.2F - equipProgress * 0.12F, -0.15F - anim * 0.13F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76.0F - 10.0F * anim));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-16.0F - 8.0F * anim));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-83.0F - 26.0F * anim));
            }
            case BlockHit -> {
                float sinSquared = MathHelper.sin((float) (swingProgress * swingProgress * Math.PI));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F));
                float g = MathHelper.sin((float) (MathHelper.sqrt(swingProgress) * Math.PI));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sinSquared * -20.0F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(g * -20.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
                matrices.translate(0.4F, 0.2F, 0.2F);
                matrices.translate(-0.5F, 0.08F, 0.0F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
            }
        }
    }
}
