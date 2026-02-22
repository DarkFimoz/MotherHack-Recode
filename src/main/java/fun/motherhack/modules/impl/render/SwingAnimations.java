package fun.motherhack.modules.impl.render;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.combat.Aura;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.modules.settings.api.Nameable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class SwingAnimations extends Module {

    @Getter
    private static SwingAnimations instance;

    public final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.Mode1);
    private final BooleanSetting auraOnly = new BooleanSetting("Only with aura", false);
    public final NumberSetting strength = new NumberSetting("Strength", 20f, 20f, 75f, 0.1f, 
            () -> mode.getValue() != Mode.Mode1 && mode.getValue() != Mode.Mode5);
    public final BooleanSetting slow = new BooleanSetting("Slow", false);
    public final NumberSetting speed = new NumberSetting("Speed", 12f, 1f, 50f, 1f, 
            () -> slow.getValue());
    
    // Custom XYZ settings
    private final BooleanSetting customXYZ = new BooleanSetting("Custom XYZ", false);
    public final NumberSetting rightX = new NumberSetting("Right X", 0f, -2f, 2f, 0.01f, 
            () -> customXYZ.getValue());
    public final NumberSetting rightY = new NumberSetting("Right Y", 0f, -2f, 2f, 0.01f, 
            () -> customXYZ.getValue());
    public final NumberSetting rightZ = new NumberSetting("Right Z", 0f, -2f, 2f, 0.01f, 
            () -> customXYZ.getValue());
    public final NumberSetting leftX = new NumberSetting("Left X", 0f, -2f, 2f, 0.01f, 
            () -> customXYZ.getValue());
    public final NumberSetting leftY = new NumberSetting("Left Y", 0f, -2f, 2f, 0.01f, 
            () -> customXYZ.getValue());
    public final NumberSetting leftZ = new NumberSetting("Left Z", 0f, -2f, 2f, 0.01f, 
            () -> customXYZ.getValue());
    
    // Custom Eat Animation
    private final BooleanSetting customEatAnimation = new BooleanSetting("Custom Eat Animation", false);

    @AllArgsConstructor
    @Getter
    public enum Mode implements Nameable {
        Mode1("Mode 1"),
        Mode2("Mode 2"),
        Mode3("Mode 3"),
        Mode4("Mode 4"),
        Mode5("Mode 5");

        private final String name;
    }

    public SwingAnimations() {
        super("SwingAnimations", Category.Render);
        instance = this;
    }

    private void handleSwordAnim(MatrixStack matrices, float swingProgress, Arm arm) {
        float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        float anim = (float) Math.sin(swingProgress * (Math.PI / 2) * 2);
        float isLeft = arm == Arm.LEFT ? -1f : 1f;

        switch (mode.getValue()) {
            case Mode1 -> {
                applyEquipOffset(matrices, arm, 0);
                applySwingOffset(matrices, arm, swingProgress);
            }
            case Mode2 -> {
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(isLeft * -60f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(isLeft * (110f + strength.getValue() * g)));
            }
            case Mode3 -> {
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(isLeft * (-30f * (1f - g) - 30f + (strength.getValue() - 20f) * g)));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(isLeft * 110f));
            }
            case Mode4 -> {
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(isLeft * 90f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(isLeft * -30f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90f - strength.getValue() * anim + 10f));
            }
            case Mode5 -> {
                float rotation = swingProgress * -360f;
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotation));
            }
        }
    }

    public void handleRenderItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!player.isUsingSpyglass()) {
            boolean bl = hand == Hand.MAIN_HAND;
            Arm arm = bl ? player.getMainArm() : player.getMainArm().getOpposite();
            
            matrices.push();
            
            // Применяем кастомное позиционирование в самом начале (до всех трансформаций)
            // Это гарантирует что оси X,Y,Z работают как ожидается
            if (customXYZ.getValue() && !item.isEmpty()) {
                if (arm == Arm.RIGHT) {
                    matrices.translate(rightX.getValue().doubleValue(), rightY.getValue().doubleValue(), rightZ.getValue().doubleValue());
                } else {
                    matrices.translate(leftX.getValue().doubleValue(), leftY.getValue().doubleValue(), leftZ.getValue().doubleValue());
                }
            }

            if (item.isOf(Items.CROSSBOW)) {
                boolean bl2 = CrossbowItem.isCharged(item);
                boolean bl3 = arm == Arm.RIGHT;
                int i = bl3 ? 1 : -1;

                if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                    this.applyEquipOffset(matrices, arm, equipProgress);
                    matrices.translate((float) i * -0.4785682F, -0.094387F, 0.05731531F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-11.935F));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * 65.3F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * -9.785F));
                    float f = (float) item.getMaxUseTime(mc.player) - ((float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F);
                    float gx = f / (float) CrossbowItem.getPullTime(item, mc.player);
                    if (gx > 1.0F) {
                        gx = 1.0F;
                    }
                    if (gx > 0.1F) {
                        float h = MathHelper.sin((f - 0.1F) * 1.3F);
                        float j = gx - 0.1F;
                        float k = h * j;
                        matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                    }
                    matrices.translate(gx * 0.0F, gx * 0.0F, gx * 0.04F);
                    matrices.scale(1.0F, 1.0F, 1.0F + gx * 0.2F);
                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) i * 45.0F));
                } else {
                    float fx = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                    float gx = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
                    float h = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
                    matrices.translate((float) i * fx, gx, h);
                    this.applyEquipOffset(matrices, arm, equipProgress);
                    this.applySwingOffset(matrices, arm, swingProgress);
                    if (bl2 && swingProgress < 0.001F && bl) {
                        matrices.translate((float) i * -0.641864F, 0.0F, 0.0F);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * 10.0F));
                    }
                }
                
                this.renderItem(player, item, bl3 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl3, matrices, vertexConsumers, light);
            } else {
                boolean bl2 = arm == Arm.RIGHT;

                if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                    int l = bl2 ? 1 : -1;
                    switch (item.getUseAction()) {
                        case NONE, BLOCK, CROSSBOW, SPYGLASS, BUNDLE, TOOT_HORN:
                            this.applyEquipOffset(matrices, arm, equipProgress);
                            break;
                        case EAT:
                        case DRINK:
                            this.applyEatOrDrinkTransformation(matrices, tickDelta, arm, item);
                            this.applyEquipOffset(matrices, arm, equipProgress);
                            break;
                        case BOW:
                            this.applyEquipOffset(matrices, arm, equipProgress);
                            matrices.translate((float) l * -0.2785682F, 0.18344387F, 0.15731531F);
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-13.935F));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) l * 35.3F));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) l * -9.785F));
                            float mx = (float) item.getMaxUseTime(mc.player) - ((float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F);
                            float fxx = mx / 20.0F;
                            fxx = (fxx * fxx + fxx * 2.0F) / 3.0F;
                            if (fxx > 1.0F) {
                                fxx = 1.0F;
                            }
                            if (fxx > 0.1F) {
                                float gx = MathHelper.sin((mx - 0.1F) * 1.3F);
                                float h = fxx - 0.1F;
                                float j = gx * h;
                                matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                            }
                            matrices.translate(fxx * 0.0F, fxx * 0.0F, fxx * 0.04F);
                            matrices.scale(1.0F, 1.0F, 1.0F + fxx * 0.2F);
                            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) l * 45.0F));
                            break;
                        case SPEAR:
                            this.applyEquipOffset(matrices, arm, equipProgress);
                            matrices.translate((float) l * -0.5F, 0.7F, 0.1F);
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-55.0F));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) l * 35.3F));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) l * -9.785F));
                            float m = (float) item.getMaxUseTime(mc.player) - ((float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F);
                            float fx = m / 10.0F;
                            if (fx > 1.0F) {
                                fx = 1.0F;
                            }
                            if (fx > 0.1F) {
                                float gx = MathHelper.sin((m - 0.1F) * 1.3F);
                                float h = fx - 0.1F;
                                float j = gx * h;
                                matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                            }
                            matrices.translate(0.0F, 0.0F, fx * 0.2F);
                            matrices.scale(1.0F, 1.0F, 1.0F + fx * 0.2F);
                            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) l * 45.0F));
                            break;
                        case BRUSH:
                            this.applyBrushTransformation(matrices, tickDelta, arm, item, equipProgress);
                    }
                    
                } else if (player.isUsingRiptide()) {
                    this.applyEquipOffset(matrices, arm, equipProgress);
                    int l = bl2 ? 1 : -1;
                    matrices.translate((float) l * -0.4F, 0.8F, 0.3F);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) l * 65.0F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) l * -85.0F));
                } else {
                    if (isToggled() && auraCheck()) {
                        handleSwordAnim(matrices, swingProgress, arm);
                    } else {
                        float n = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                        float mxx = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
                        float fxxx = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
                        int o = bl2 ? 1 : -1;
                        matrices.translate((float) o * n, mxx, fxxx);
                        this.applyEquipOffset(matrices, arm, equipProgress);
                        this.applySwingOffset(matrices, arm, swingProgress);
                    }
                }
                
                this.renderItem(player, item, bl2 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl2, matrices, vertexConsumers, light);
            }
            matrices.pop();
        }
    }

    private void applyBrushTransformation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack, float equipProgress) {
        this.applyEquipOffset(matrices, arm, equipProgress);
        float f = (float) (mc.player.getItemUseTimeLeft() % 10);
        float g = f - tickDelta + 1.0F;
        float h = 1.0F - g / 10.0F;
        float n = -15.0F + 75.0F * MathHelper.cos(h * 2.0F * (float) Math.PI);
        if (arm != Arm.RIGHT) {
            matrices.translate(0.1, 0.83, 0.35);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(n));
            matrices.translate(-0.3, 0.22, 0.35);
        } else {
            matrices.translate(-0.25, 0.22, 0.35);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(0.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(n));
        }
    }

    private void applyEatOrDrinkTransformation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack) {
        if (customEatAnimation.getValue()) {
            // Кастомная анимация еды - минимальная тряска, предмет остается на месте
            float f = (float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F;
            float g = f / (float) stack.getMaxUseTime(mc.player);
            
            // Очень маленькая тряска (в 5 раз меньше оригинала)
            if (g < 0.8F) {
                float h = MathHelper.abs(MathHelper.cos(f / 4.0F * (float) Math.PI) * 0.02F);
                matrices.translate(0.0F, h, 0.0F);
            }
            
            // Минимальное движение (в 10 раз меньше оригинала)
            float h = 1.0F - (float) Math.pow(g, 27.0);
            int i = arm == Arm.RIGHT ? 1 : -1;
            matrices.translate(h * 0.06F * (float) i, h * -0.05F, h * 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * h * 9.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * 1.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * h * 3.0F));
        } else {
            // Оригинальная анимация еды
            float f = (float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F;
            float g = f / (float) stack.getMaxUseTime(mc.player);
            if (g < 0.8F) {
                float h = MathHelper.abs(MathHelper.cos(f / 4.0F * (float) Math.PI) * 0.1F);
                matrices.translate(0.0F, h, 0.0F);
            }
            float h = 1.0F - (float) Math.pow(g, 27.0);
            int i = arm == Arm.RIGHT ? 1 : -1;
            matrices.translate(h * 0.6F * (float) i, h * -0.5F, h * 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * h * 90.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * 10.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * h * 30.0F));
        }
    }

    private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate((float) i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }

    private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * (45.0F + f * -20.0F)));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * g * -20.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * -45.0F));
    }

    public void renderItem(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!stack.isEmpty()) {
            mc.getItemRenderer().renderItem(entity, stack, renderMode, leftHanded, matrices, vertexConsumers, entity.getWorld(), light, OverlayTexture.DEFAULT_UV, entity.getId() + renderMode.ordinal());
        }
    }

    public boolean auraCheck() {
        Aura aura = MotherHack.getInstance().getModuleManager().getModule(Aura.class);
        return !auraOnly.getValue() || (aura != null && aura.getTarget() != null);
    }
}
