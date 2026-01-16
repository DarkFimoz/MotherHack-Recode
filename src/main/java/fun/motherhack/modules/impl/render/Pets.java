package fun.motherhack.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.motherhack.api.events.impl.EventRender3D;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.ParrotEntityModel;
import net.minecraft.client.render.entity.state.ParrotEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public class Pets extends Module {

    public enum ParrotVariant implements Nameable {
        RED_BLUE("Red Blue", "parrot_red_blue"),
        BLUE("Blue", "parrot_blue"),
        GREEN("Green", "parrot_green"),
        YELLOW_BLUE("Yellow Blue", "parrot_yellow_blue"),
        GRAY("Gray", "parrot_grey");

        private final String displayName;
        private final Identifier texture;

        ParrotVariant(String displayName, String textureName) {
            this.displayName = displayName;
            this.texture = Identifier.ofVanilla("textures/entity/parrot/" + textureName + ".png");
        }

        public Identifier getTexture() {
            return texture;
        }

        @Override
        public String getName() {
            return displayName;
        }
    }

    public enum Shoulder implements Nameable {
        RIGHT("Right"), LEFT("Left");
        
        private final String name;
        
        Shoulder(String name) {
            this.name = name;
        }
        
        @Override
        public String getName() {
            return name;
        }
    }

    public EnumSetting<ParrotVariant> variant = new EnumSetting<>("Variant", ParrotVariant.RED_BLUE);
    public EnumSetting<Shoulder> shoulder = new EnumSetting<>("Shoulder", Shoulder.RIGHT);
    public BooleanSetting renderOnSelf = new BooleanSetting("RenderOnSelf", true);
    public BooleanSetting onlyFirstPerson = new BooleanSetting("OnlyFirstPerson", false);

    private ParrotEntityModel parrotModel;
    private ParrotEntityRenderState parrotState;

    public Pets() {
        super("Pets", Category.Render);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        initModel();
    }

    private void initModel() {
        try {
            if (mc.getBakedModelManager() == null) return;
            var supplier = mc.getBakedModelManager().getEntityModelsSupplier();
            if (supplier == null) return;
            var entityModels = supplier.get();
            if (entityModels != null) {
                parrotModel = new ParrotEntityModel(entityModels.getModelPart(EntityModelLayers.PARROT));
                parrotState = new ParrotEntityRenderState();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (mc.player == null || mc.world == null) return;
        
        if (parrotModel == null || parrotState == null) {
            initModel();
            if (parrotModel == null) return;
        }

        if (onlyFirstPerson.getValue() && !mc.options.getPerspective().isFirstPerson()) {
            return;
        }

        MatrixStack matrices = event.getMatrixStack();
        Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();
        float tickDelta = event.getTickCounter().getTickDelta(true);
        
        // Получаем буфер для рендеринга
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        
        if (renderOnSelf.getValue()) {
            renderParrotOnPlayer(matrices, mc.player, cameraPos, immediate, tickDelta);
        }
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (player.getShoulderEntityLeft().isEmpty() && player.getShoulderEntityRight().isEmpty()) {
                renderParrotOnPlayer(matrices, player, cameraPos, immediate, tickDelta);
            }
        }
        
        // Важно: рисуем буфер
        immediate.draw();
    }

    private void renderParrotOnPlayer(MatrixStack matrices, PlayerEntity player, Vec3d cameraPos, 
                                       VertexConsumerProvider vertexConsumers, float tickDelta) {
        // Интерполяция позиции игрока
        double x = player.prevX + (player.getX() - player.prevX) * tickDelta;
        double y = player.prevY + (player.getY() - player.prevY) * tickDelta;
        double z = player.prevZ + (player.getZ() - player.prevZ) * tickDelta;
        
        // Интерполяция поворота тела
        float bodyYaw = player.prevBodyYaw + (player.bodyYaw - player.prevBodyYaw) * tickDelta;
        
        matrices.push();
        
        // Настройка рендер-состояния
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Позиция относительно камеры (центр игрока на уровне пояса, как в vanilla renderer)
        // Vanilla PlayerEntityRenderer рендерит игрока с pivot point на уровне ног + 0.9375 (15/16 блока)
        matrices.translate(x - cameraPos.x, y - cameraPos.y + 0.9375, z - cameraPos.z);
        
        // Поворот по направлению тела игрока
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));
        
        // Vanilla координаты плеча относительно pivot point модели игрока
        boolean leftShoulder = shoulder.getValue() == Shoulder.LEFT;
        float shoulderX = leftShoulder ? 0.4f : -0.4f;
        float shoulderY = player.isInSneakingPose() ? -1.3f : -1.5f;
        matrices.translate(shoulderX, shoulderY, 0.0f);
        
        // Поворот модели (стандартно для Minecraft)
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
        
        // Масштаб попугая (vanilla = 0.5)
        matrices.scale(0.5f, 0.5f, 0.5f);
        
        // Настройка состояния попугая
        parrotState.age = player.age + tickDelta;
        parrotState.flapAngle = 0.0f;
        parrotState.parrotPose = ParrotEntityModel.Pose.ON_SHOULDER;
        
        // Получаем текстуру и рендер слой
        Identifier texture = variant.getValue().getTexture();
        RenderLayer renderLayer = RenderLayer.getEntityCutoutNoCull(texture);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
        
        // Устанавливаем углы модели
        parrotModel.setAngles(parrotState);
        
        // Рендерим модель
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        parrotModel.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
        
        RenderSystem.disableBlend();
        
        matrices.pop();
    }
}
