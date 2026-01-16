package fun.motherhack.modules.impl.render.models;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.motherhack.MotherHack;
import fun.motherhack.modules.impl.render.Models;
import fun.motherhack.utils.Wrapper;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.awt.*;

public class ModelsRenderer implements Wrapper {

    public static void renderModel(PlayerEntityRenderState state, MatrixStack matrices, Models models) {
        matrices.push();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float bodyYaw = state.bodyYaw;
        float limbPos = state.limbFrequency;
        float limbSpeed = state.limbAmplitudeMultiplier;

        Color body = models.bodyColor.getColor();
        Color eye = models.eyeColor.getColor();
        Color legs = models.legsColor.getColor();

        if (models.friendHighlight.getValue() && MotherHack.getInstance().getFriendManager().isFriend(state.name)) {
            body = Color.GREEN;
            eye = Color.WHITE;
            legs = Color.GREEN;
        }

        switch (models.mode.getValue()) {
            case Amogus -> renderAmogus(matrices, bodyYaw, limbPos, limbSpeed, body, eye, legs);
            case Rabbit -> renderRabbit(matrices, bodyYaw, limbPos, limbSpeed);
            case Freddy -> renderFreddy(matrices, bodyYaw, limbPos, limbSpeed);
            case TunTunSahur -> renderTunTunSahur(matrices, bodyYaw, limbPos, limbSpeed);
            case Custom -> {} // Custom mode is handled in mixin - just scales default Steve model
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private static void renderAmogus(MatrixStack matrices, float yaw, float limbPos, float limbSpeed,
                                      Color bodyColor, Color eyeColor, Color legsColor) {
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 - yaw));

        float scale = 0.9375f;
        matrices.scale(scale, scale, scale);

        BufferBuilder buffer;
        Matrix4f matrix;

        // === BODY ===
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        matrix = matrices.peek().getPositionMatrix();
        addBox(buffer, matrix, -0.25f, 0.4f, -0.15f, 0.5f, 0.8f, 0.3f, bodyColor);
        addBox(buffer, matrix, -0.15f, 0.5f, 0.15f, 0.3f, 0.5f, 0.15f, bodyColor);
        addBox(buffer, matrix, -0.2f, 1.2f, -0.12f, 0.4f, 0.15f, 0.24f, bodyColor);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // === VISOR ===
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        matrix = matrices.peek().getPositionMatrix();
        addBox(buffer, matrix, -0.18f, 0.9f, -0.16f, 0.36f, 0.25f, 0.02f, eyeColor);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // === LEGS ===
        float legAngle = (float) Math.sin(limbPos * 0.6662) * 45f * limbSpeed;

        matrices.push();
        matrices.translate(-0.1f, 0.4f, 0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(legAngle));
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.1f, -0.4f, -0.08f, 0.15f, 0.4f, 0.16f, legsColor);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();

        matrices.push();
        matrices.translate(0.1f, 0.4f, 0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-legAngle));
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.05f, -0.4f, -0.08f, 0.15f, 0.4f, 0.16f, legsColor);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();

        matrices.pop();
    }

    private static void renderRabbit(MatrixStack matrices, float yaw, float limbPos, float limbSpeed) {
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 - yaw));

        Color white = new Color(240, 240, 240);
        Color pink = new Color(255, 180, 180);
        Color black = Color.BLACK;

        BufferBuilder buffer;
        Matrix4f matrix;

        // === BODY ===
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        matrix = matrices.peek().getPositionMatrix();
        addBox(buffer, matrix, -0.2f, 0.4f, -0.15f, 0.4f, 0.5f, 0.3f, white);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // === HEAD ===
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        matrix = matrices.peek().getPositionMatrix();
        addBox(buffer, matrix, -0.18f, 0.9f, -0.12f, 0.36f, 0.35f, 0.28f, white);
        addBox(buffer, matrix, -0.08f, 0.9f, -0.18f, 0.16f, 0.15f, 0.06f, white);
        addBox(buffer, matrix, -0.03f, 0.98f, -0.19f, 0.06f, 0.06f, 0.02f, pink);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // === EARS ===
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        matrix = matrices.peek().getPositionMatrix();
        addBox(buffer, matrix, -0.14f, 1.25f, -0.02f, 0.08f, 0.45f, 0.04f, white);
        addBox(buffer, matrix, 0.06f, 1.25f, -0.02f, 0.08f, 0.45f, 0.04f, white);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        matrix = matrices.peek().getPositionMatrix();
        addBox(buffer, matrix, -0.12f, 1.28f, -0.025f, 0.04f, 0.35f, 0.02f, pink);
        addBox(buffer, matrix, 0.08f, 1.28f, -0.025f, 0.04f, 0.35f, 0.02f, pink);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // === EYES ===
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        matrix = matrices.peek().getPositionMatrix();
        addBox(buffer, matrix, -0.12f, 1.05f, -0.13f, 0.06f, 0.08f, 0.02f, black);
        addBox(buffer, matrix, 0.06f, 1.05f, -0.13f, 0.06f, 0.08f, 0.02f, black);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // === TAIL ===
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        matrix = matrices.peek().getPositionMatrix();
        addBox(buffer, matrix, -0.08f, 0.45f, 0.15f, 0.16f, 0.15f, 0.12f, white);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // === FRONT PAWS ===
        float armAngle = (float) Math.sin(limbPos * 0.6662) * 30f * limbSpeed;
        
        matrices.push();
        matrices.translate(-0.2f, 0.7f, -0.05f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(armAngle));
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.06f, -0.3f, -0.05f, 0.1f, 0.3f, 0.1f, white);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();

        matrices.push();
        matrices.translate(0.2f, 0.7f, -0.05f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-armAngle));
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.04f, -0.3f, -0.05f, 0.1f, 0.3f, 0.1f, white);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();

        // === BACK LEGS ===
        float legAngle = (float) Math.sin(limbPos * 0.6662) * 40f * limbSpeed;

        matrices.push();
        matrices.translate(-0.12f, 0.4f, 0.05f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(legAngle));
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.08f, -0.4f, -0.08f, 0.14f, 0.4f, 0.16f, white);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();

        matrices.push();
        matrices.translate(0.12f, 0.4f, 0.05f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-legAngle));
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.06f, -0.4f, -0.08f, 0.14f, 0.4f, 0.16f, white);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();

        matrices.pop();
    }


    private static void renderFreddy(MatrixStack matrices, float yaw, float limbPos, float limbSpeed) {
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 - yaw));

        Color brown = new Color(139, 90, 43);
        Color darkBrown = new Color(100, 60, 30);
        Color lightBrown = new Color(180, 130, 80);
        Color black = Color.BLACK;
        Color white = Color.WHITE;

        BufferBuilder buffer;
        Matrix4f matrix;

        // === TORSO ===
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        matrix = matrices.peek().getPositionMatrix();
        addBox(buffer, matrix, -0.25f, 0.5f, -0.12f, 0.5f, 0.6f, 0.24f, brown);
        addBox(buffer, matrix, -0.18f, 0.52f, -0.13f, 0.36f, 0.4f, 0.02f, lightBrown);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // === HEAD ===
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        matrix = matrices.peek().getPositionMatrix();
        addBox(buffer, matrix, -0.22f, 1.1f, -0.15f, 0.44f, 0.4f, 0.32f, brown);
        addBox(buffer, matrix, -0.12f, 1.1f, -0.22f, 0.24f, 0.2f, 0.08f, lightBrown);
        addBox(buffer, matrix, -0.04f, 1.22f, -0.24f, 0.08f, 0.08f, 0.03f, black);
        addBox(buffer, matrix, -0.1f, 1.05f, -0.2f, 0.2f, 0.08f, 0.06f, darkBrown);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // === EARS ===
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        matrix = matrices.peek().getPositionMatrix();
        addBox(buffer, matrix, -0.2f, 1.5f, 0f, 0.12f, 0.12f, 0.08f, brown);
        addBox(buffer, matrix, 0.08f, 1.5f, 0f, 0.12f, 0.12f, 0.08f, brown);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // === EYES ===
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        matrix = matrices.peek().getPositionMatrix();
        addBox(buffer, matrix, -0.15f, 1.3f, -0.16f, 0.1f, 0.1f, 0.02f, white);
        addBox(buffer, matrix, 0.05f, 1.3f, -0.16f, 0.1f, 0.1f, 0.02f, white);
        addBox(buffer, matrix, -0.12f, 1.32f, -0.17f, 0.04f, 0.06f, 0.02f, black);
        addBox(buffer, matrix, 0.08f, 1.32f, -0.17f, 0.04f, 0.06f, 0.02f, black);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // === TOP HAT ===
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        matrix = matrices.peek().getPositionMatrix();
        addBox(buffer, matrix, -0.18f, 1.5f, -0.12f, 0.36f, 0.04f, 0.24f, black);
        addBox(buffer, matrix, -0.12f, 1.54f, -0.08f, 0.24f, 0.2f, 0.16f, black);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // === BOW TIE ===
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        matrix = matrices.peek().getPositionMatrix();
        addBox(buffer, matrix, -0.1f, 1.02f, -0.13f, 0.2f, 0.08f, 0.02f, black);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // === ARMS ===
        float armAngle = (float) Math.sin(limbPos * 0.6662) * 25f * limbSpeed;

        matrices.push();
        matrices.translate(-0.3f, 0.95f, 0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-armAngle));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(8));
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.08f, -0.45f, -0.06f, 0.12f, 0.45f, 0.12f, brown);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();

        matrices.push();
        matrices.translate(0.3f, 0.95f, 0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(armAngle));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-8));
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.04f, -0.45f, -0.06f, 0.12f, 0.45f, 0.12f, brown);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();

        // === LEGS ===
        float legAngle = (float) Math.sin(limbPos * 0.6662) * 35f * limbSpeed;

        matrices.push();
        matrices.translate(-0.12f, 0.5f, 0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(legAngle));
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.1f, -0.5f, -0.08f, 0.15f, 0.5f, 0.16f, brown);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();

        matrices.push();
        matrices.translate(0.12f, 0.5f, 0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-legAngle));
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.05f, -0.5f, -0.08f, 0.15f, 0.5f, 0.16f, brown);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();

        matrices.pop();
    }

    private static void renderTunTunSahur(MatrixStack matrices, float yaw, float limbPos, float limbSpeed) {
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 - yaw));
        
        // Scale up the model for better visibility
        float scale = 1.4f;
        matrices.scale(scale, scale, scale);

        // Wooden figure colors (like a wooden drum/log - brainrot style)
        Color wood = new Color(165, 100, 55);
        Color woodDark = new Color(125, 75, 40);
        Color woodLight = new Color(195, 135, 80);
        Color woodHighlight = new Color(215, 165, 105);
        Color black = Color.BLACK;
        Color white = Color.WHITE;
        Color eyeBlue = new Color(120, 190, 255);
        Color batWood = new Color(210, 170, 110);
        Color batDark = new Color(160, 115, 65);

        BufferBuilder buffer;
        Matrix4f matrix;

        // === WOODEN CYLINDRICAL BODY (main drum-like body) ===
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        matrix = matrices.peek().getPositionMatrix();
        // Main wooden body
        addBox(buffer, matrix, -0.22f, 0.38f, -0.17f, 0.44f, 1.05f, 0.34f, wood);
        // Wood grain stripes (vertical)
        addBox(buffer, matrix, -0.1f, 0.4f, -0.175f, 0.025f, 1.0f, 0.012f, woodLight);
        addBox(buffer, matrix, 0.075f, 0.4f, -0.175f, 0.025f, 1.0f, 0.012f, woodLight);
        addBox(buffer, matrix, -0.015f, 0.4f, -0.175f, 0.03f, 1.0f, 0.012f, woodHighlight);
        // Back stripes
        addBox(buffer, matrix, -0.06f, 0.4f, 0.163f, 0.025f, 1.0f, 0.012f, woodLight);
        addBox(buffer, matrix, 0.035f, 0.4f, 0.163f, 0.025f, 1.0f, 0.012f, woodLight);
        // Rounded top cap
        addBox(buffer, matrix, -0.19f, 1.41f, -0.14f, 0.38f, 0.08f, 0.28f, wood);
        addBox(buffer, matrix, -0.16f, 1.47f, -0.11f, 0.32f, 0.05f, 0.22f, woodDark);
        // Rounded bottom
        addBox(buffer, matrix, -0.19f, 0.32f, -0.14f, 0.38f, 0.08f, 0.28f, wood);
        // Side wood ring details
        addBox(buffer, matrix, -0.235f, 0.55f, -0.1f, 0.025f, 0.75f, 0.2f, woodDark);
        addBox(buffer, matrix, 0.21f, 0.55f, -0.1f, 0.025f, 0.75f, 0.2f, woodDark);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // === EYEBROWS (thick, black, expressive) ===
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        matrix = matrices.peek().getPositionMatrix();
        addBox(buffer, matrix, -0.16f, 1.22f, -0.175f, 0.13f, 0.035f, 0.02f, black);
        addBox(buffer, matrix, 0.03f, 1.22f, -0.175f, 0.13f, 0.035f, 0.02f, black);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // === EYES (big, bright, glowing - brainrot style) ===
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        matrix = matrices.peek().getPositionMatrix();
        // White outer
        addBox(buffer, matrix, -0.15f, 1.02f, -0.175f, 0.13f, 0.17f, 0.02f, white);
        addBox(buffer, matrix, 0.02f, 1.02f, -0.175f, 0.13f, 0.17f, 0.02f, white);
        // Blue iris
        addBox(buffer, matrix, -0.12f, 1.05f, -0.18f, 0.08f, 0.11f, 0.02f, eyeBlue);
        addBox(buffer, matrix, 0.04f, 1.05f, -0.18f, 0.08f, 0.11f, 0.02f, eyeBlue);
        // Black pupils
        addBox(buffer, matrix, -0.1f, 1.07f, -0.185f, 0.045f, 0.065f, 0.02f, black);
        addBox(buffer, matrix, 0.06f, 1.07f, -0.185f, 0.045f, 0.065f, 0.02f, black);
        // Eye shine (glint)
        addBox(buffer, matrix, -0.13f, 1.14f, -0.185f, 0.025f, 0.025f, 0.01f, white);
        addBox(buffer, matrix, 0.045f, 1.14f, -0.185f, 0.025f, 0.025f, 0.01f, white);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // === NOSE ===
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        matrix = matrices.peek().getPositionMatrix();
        addBox(buffer, matrix, -0.035f, 0.9f, -0.2f, 0.07f, 0.1f, 0.045f, woodDark);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // === MOUTH (big smile with teeth) ===
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        matrix = matrices.peek().getPositionMatrix();
        // Mouth opening (dark)
        addBox(buffer, matrix, -0.13f, 0.68f, -0.175f, 0.26f, 0.2f, 0.02f, woodDark);
        // Upper teeth
        addBox(buffer, matrix, -0.11f, 0.8f, -0.18f, 0.22f, 0.06f, 0.02f, white);
        // Lower teeth
        addBox(buffer, matrix, -0.09f, 0.69f, -0.18f, 0.18f, 0.05f, 0.02f, white);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // === LEFT ARM WITH BAT (pointing LEFT horizontally) ===
        matrices.push();
        matrices.translate(-0.24f, 1.0f, 0);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90)); // Arm pointing LEFT
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10)); // Slightly forward
        
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        // Arm (one piece, horizontal)
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.045f, -0.5f, -0.045f, 0.09f, 0.5f, 0.09f, wood);
        // Hand gripping bat
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.055f, -0.58f, -0.055f, 0.11f, 0.1f, 0.11f, wood);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        // Baseball bat (pointing FORWARD from hand)
        matrices.translate(0, -0.55f, 0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90)); // Bat pointing forward
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        // Bat handle
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.03f, -0.1f, -0.03f, 0.06f, 0.35f, 0.06f, batWood);
        // Bat handle grip
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.035f, -0.15f, -0.035f, 0.07f, 0.1f, 0.07f, batDark);
        // Bat barrel (thick part)
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.045f, 0.2f, -0.045f, 0.09f, 0.25f, 0.09f, batWood);
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.055f, 0.4f, -0.055f, 0.11f, 0.15f, 0.11f, batWood);
        // Bat tip
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.045f, 0.53f, -0.045f, 0.09f, 0.06f, 0.09f, batDark);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();

        // === RIGHT ARM (pointing RIGHT horizontally) ===
        float armSwing = (float) Math.sin(limbPos * 0.6662) * 5f * limbSpeed;
        
        matrices.push();
        matrices.translate(0.24f, 1.0f, 0);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90)); // Arm pointing RIGHT
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-10 + armSwing)); // Slightly forward
        
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        // Arm (one piece, horizontal)
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.045f, -0.5f, -0.045f, 0.09f, 0.5f, 0.09f, wood);
        // Hand
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.05f, -0.57f, -0.05f, 0.1f, 0.09f, 0.1f, wood);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();

        // === THIN STICK LEGS WITH BIG FEET ===
        float legAngle = (float) Math.sin(limbPos * 0.6662) * 45f * limbSpeed;

        // Left leg
        matrices.push();
        matrices.translate(-0.09f, 0.38f, 0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(legAngle));
        
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        // Thin wooden leg
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.04f, -0.35f, -0.04f, 0.08f, 0.35f, 0.08f, wood);
        // Big foot
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.07f, -0.43f, -0.14f, 0.14f, 0.09f, 0.2f, wood);
        // Foot detail
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.06f, -0.44f, -0.13f, 0.12f, 0.035f, 0.1f, woodDark);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();

        // Right leg
        matrices.push();
        matrices.translate(0.09f, 0.38f, 0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-legAngle));
        
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        // Thin wooden leg
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.04f, -0.35f, -0.04f, 0.08f, 0.35f, 0.08f, wood);
        // Big foot
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.07f, -0.43f, -0.14f, 0.14f, 0.09f, 0.2f, wood);
        // Foot detail
        addBox(buffer, matrices.peek().getPositionMatrix(), -0.06f, -0.44f, -0.13f, 0.12f, 0.035f, 0.1f, woodDark);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();

        matrices.pop();
    }


    private static void addBox(BufferBuilder buffer, Matrix4f matrix,
                                float x, float y, float z, float w, float h, float d,
                                Color color) {
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = 1.0f;

        float x2 = x + w;
        float y2 = y + h;
        float z2 = z + d;

        // Front face
        float f = 0.9f;
        buffer.vertex(matrix, x, y, z).color(r*f, g*f, b*f, a);
        buffer.vertex(matrix, x, y2, z).color(r*f, g*f, b*f, a);
        buffer.vertex(matrix, x2, y2, z).color(r*f, g*f, b*f, a);
        buffer.vertex(matrix, x2, y, z).color(r*f, g*f, b*f, a);

        // Back face
        f = 0.75f;
        buffer.vertex(matrix, x2, y, z2).color(r*f, g*f, b*f, a);
        buffer.vertex(matrix, x2, y2, z2).color(r*f, g*f, b*f, a);
        buffer.vertex(matrix, x, y2, z2).color(r*f, g*f, b*f, a);
        buffer.vertex(matrix, x, y, z2).color(r*f, g*f, b*f, a);

        // Top face
        f = 1.0f;
        buffer.vertex(matrix, x, y2, z).color(r*f, g*f, b*f, a);
        buffer.vertex(matrix, x, y2, z2).color(r*f, g*f, b*f, a);
        buffer.vertex(matrix, x2, y2, z2).color(r*f, g*f, b*f, a);
        buffer.vertex(matrix, x2, y2, z).color(r*f, g*f, b*f, a);

        // Bottom face
        f = 0.6f;
        buffer.vertex(matrix, x, y, z2).color(r*f, g*f, b*f, a);
        buffer.vertex(matrix, x, y, z).color(r*f, g*f, b*f, a);
        buffer.vertex(matrix, x2, y, z).color(r*f, g*f, b*f, a);
        buffer.vertex(matrix, x2, y, z2).color(r*f, g*f, b*f, a);

        // Right face
        f = 0.8f;
        buffer.vertex(matrix, x2, y, z).color(r*f, g*f, b*f, a);
        buffer.vertex(matrix, x2, y2, z).color(r*f, g*f, b*f, a);
        buffer.vertex(matrix, x2, y2, z2).color(r*f, g*f, b*f, a);
        buffer.vertex(matrix, x2, y, z2).color(r*f, g*f, b*f, a);

        // Left face
        f = 0.8f;
        buffer.vertex(matrix, x, y, z2).color(r*f, g*f, b*f, a);
        buffer.vertex(matrix, x, y2, z2).color(r*f, g*f, b*f, a);
        buffer.vertex(matrix, x, y2, z).color(r*f, g*f, b*f, a);
        buffer.vertex(matrix, x, y, z).color(r*f, g*f, b*f, a);
    }
}
