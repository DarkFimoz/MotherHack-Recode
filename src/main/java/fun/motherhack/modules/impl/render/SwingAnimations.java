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

    public EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.Default);
    public BooleanSetting onlyAura = new BooleanSetting("OnlyAura", false);
    public BooleanSetting slowAnimation = new BooleanSetting("SlowAnimation", true);
    public NumberSetting slowAnimationVal = new NumberSetting("SlowValue", 12, 1, 50, 1);
    public BooleanSetting disableSwapMain = new BooleanSetting("DisableSwapMain", true);
    public BooleanSetting disableSwapOff = new BooleanSetting("DisableSwapOff", true);

    public boolean flip = false;

    @AllArgsConstructor
    @Getter
    public enum Mode implements Nameable {
        Normal("Normal"),
        Default("Default"),
        One("One"),
        Two("Two"),
        Three("Three"),
        Four("Four"),
        Five("Five"),
        Six("Six"),
        Seven("Seven"),
        Eight("Eight"),
        Nine("Nine"),
        Ten("Ten"),
        Eleven("Eleven"),
        Twelve("Twelve"),
        Thirteen("Thirteen"),
        Fourteen("Fourteen");

        private final String name;
    }

    public SwingAnimations() {
        super("SwingAnimations", Category.Render);
    }

    public boolean shouldAnimate() {
        Aura aura = MotherHack.getInstance().getModuleManager().getModule(Aura.class);
        return isToggled() 
            && (!onlyAura.getValue() || (aura != null && aura.isToggled() && aura.getTarget() != null))
            && mode.getValue() != Mode.Normal;
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
        ViewModel viewModel = MotherHack.getInstance().getModuleManager().getModule(ViewModel.class);
        
        if (arm == Arm.LEFT && (mode.getValue() == Mode.Eleven || mode.getValue() == Mode.Ten || 
            mode.getValue() == Mode.Nine || mode.getValue() == Mode.Three || 
            mode.getValue() == Mode.Thirteen || mode.getValue() == Mode.Fourteen)) {
            applyEquipOffset(matrices, arm, equipProgress);
            if (viewModel != null && viewModel.isToggled()) {
                matrices.translate(-viewModel.mainX.getValue(), viewModel.mainY.getValue(), viewModel.mainZ.getValue());
            }
            applySwingOffset(matrices, arm, swingProgress);
            if (viewModel != null && viewModel.isToggled()) {
                matrices.translate(viewModel.mainX.getValue(), -viewModel.mainY.getValue(), -viewModel.mainZ.getValue());
            }
            return;
        }

        switch (mode.getValue()) {
            case Default -> {
                applyEquipOffset(matrices, arm, equipProgress);
                translateToViewModelOff(matrices, viewModel);
                applySwingOffset(matrices, arm, swingProgress);
                translateBackOff(matrices, viewModel);
            }
            case One -> {
                float n = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
                applyEquipOffset(matrices, arm, n);
                int i = arm == Arm.RIGHT ? 1 : -1;
                translateToViewModel(matrices, viewModel);
                float f1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * (45.0F + f1 * -20.0F)));
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * g * -20.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * 0.0F));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * -45.0F));
                translateBack(matrices, viewModel);
            }
            case Two -> applyEquipOffset(matrices, arm, 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * 6.2831855F));
            case Three -> {
                float n = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
                applyEquipOffset(matrices, arm, n);
                int i = arm == Arm.RIGHT ? 1 : -1;
                translateToViewModel(matrices, viewModel);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * (45.0F + f * -20.0F)));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * g * -70.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * -45.0F));
                translateBack(matrices, viewModel);
            }
            case Four -> {
                applyEquipOffset(matrices, arm, 0);
                translateToViewModel(matrices, viewModel);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(swingProgress > 0 ? -MathHelper.sin(swingProgress * 13f) * 37f : 0));
                translateBack(matrices, viewModel);
            }
            case Five -> {
                applyEquipOffset(matrices, arm, 0);
                int i = arm == Arm.RIGHT ? 1 : -1;
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
                translateToViewModel(matrices, viewModel);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * g * -20.0F));
                translateBack(matrices, viewModel);
            }
            case Six -> {
                applyEquipOffset(matrices, arm, equipProgress);
                translateToViewModel(matrices, viewModel);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(swingProgress * (flip ? 360.0F : -360)));
                translateBack(matrices, viewModel);
            }
            case Seven -> {
                applyEquipOffset(matrices, arm, equipProgress);
                float a = -MathHelper.sin(swingProgress * 3f) / 2f + 1f;
                matrices.scale(a, a, a);
            }
            case Eight -> {
                applyEquipOffset(matrices, arm, equipProgress);
                translateToViewModel(matrices, viewModel);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(swingProgress * -360));
                translateBack(matrices, viewModel);
            }
            case Nine -> {
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
                applyEquipOffset(matrices, arm, 0);
                translateToViewModel(matrices, viewModel);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-30f * (1f - g) - 30f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110f));
                translateBack(matrices, viewModel);
            }
            case Ten -> {
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
                applyEquipOffset(matrices, arm, 0);
                translateToViewModel(matrices, viewModel);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-60f * g - 50));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110f));
                translateBack(matrices, viewModel);
            }
            case Eleven -> {
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
                applyEquipOffset(matrices, arm, 0);
                translateToViewModel(matrices, viewModel);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-60f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110f + 20f * g));
                translateBack(matrices, viewModel);
            }
            case Twelve -> {
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
                applyEquipOffset(matrices, arm, 0);
                matrices.translate(0, 0, -g / 4f);
                translateToViewModel(matrices, viewModel);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-120f));
                translateBack(matrices, viewModel);
            }
            case Thirteen -> {
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
                applyEquipOffset(matrices, arm, 0);
                translateToViewModel(matrices, viewModel);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-MathHelper.sin(swingProgress * 3f) * 60f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-60f * g));
                translateBack(matrices, viewModel);
            }
            case Fourteen -> {
                if (swingProgress > 0) {
                    float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                    matrices.translate(0.56F, equipProgress * -0.2f - 0.5F, -0.7F);
                    translateToViewModel(matrices, viewModel);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -85.0F));
                    if (viewModel != null && viewModel.isToggled()) {
                        matrices.translate(-0.1F * viewModel.mainScaleX.getValue(), 
                                         0.28F * viewModel.mainScaleY.getValue(), 
                                         0.2F * viewModel.mainScaleZ.getValue());
                    } else {
                        matrices.translate(-0.1F, 0.28F, 0.2F);
                    }
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-85.0F));
                    translateBack(matrices, viewModel);
                } else {
                    float n = -0.4f * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                    float m = 0.2f * MathHelper.sin(MathHelper.sqrt(swingProgress) * ((float) Math.PI * 2));
                    float f1 = -0.2f * MathHelper.sin(swingProgress * (float) Math.PI);
                    matrices.translate(n, m, f1);
                    applyEquipOffset(matrices, arm, equipProgress);
                    applySwingOffset(matrices, arm, swingProgress);
                }
            }
        }
    }

    private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate((float) i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }

    private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * (45.0F + f * -20.0F)));
        float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * g * -20.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * -45.0F));
    }

    private void translateToViewModel(MatrixStack matrices, ViewModel viewModel) {
        if (viewModel != null && viewModel.isToggled()) {
            matrices.translate(viewModel.mainX.getValue(), viewModel.mainY.getValue(), viewModel.mainZ.getValue());
        }
    }

    private void translateToViewModelOff(MatrixStack matrices, ViewModel viewModel) {
        if (viewModel != null && viewModel.isToggled()) {
            matrices.translate(-viewModel.mainX.getValue(), viewModel.mainY.getValue(), viewModel.mainZ.getValue());
        }
    }

    private void translateBack(MatrixStack matrices, ViewModel viewModel) {
        if (viewModel != null && viewModel.isToggled()) {
            matrices.translate(-viewModel.mainX.getValue(), -viewModel.mainY.getValue(), -viewModel.mainZ.getValue());
        }
    }

    private void translateBackOff(MatrixStack matrices, ViewModel viewModel) {
        if (viewModel != null && viewModel.isToggled()) {
            matrices.translate(viewModel.mainX.getValue(), -viewModel.mainY.getValue(), -viewModel.mainZ.getValue());
        }
    }
}
