package fun.motherhack.api.render.renderers.impl;

import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

import fun.motherhack.api.render.builders.states.QuadColorState;
import fun.motherhack.api.render.builders.states.QuadRadiusState;
import fun.motherhack.api.render.builders.states.SizeState;
import fun.motherhack.api.render.providers.ResourceProvider;
import fun.motherhack.api.render.renderers.IRenderer;

public record BuiltTexture(
        SizeState size,
        QuadRadiusState radius,
        QuadColorState color,
        float smoothness,
        float u, float v,
        float texWidth, float texHeight,
        int textureId
    ) implements IRenderer {

    private static final ShaderProgramKey TEXTURE_SHADER_KEY = new ShaderProgramKey(ResourceProvider.getShaderIdentifier("texture"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);
    
    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, this.textureId);

        float width = this.size.width(), height = this.size.height();

        ShaderProgram shader = RenderSystem.setShader(TEXTURE_SHADER_KEY);
        shader.getUniform("Size").set(width, height);
        shader.getUniform("Radius").set(this.radius.radius1(), this.radius.radius2(), this.radius.radius3(), this.radius.radius4());
        shader.getUniform("Smoothness").set(this.smoothness);

        BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        builder.vertex(matrix, x, y, z).texture(this.u, this.v).color(this.color.color1());
        builder.vertex(matrix, x, y + height, z).texture(this.u, this.v + this.texHeight).color(this.color.color2());
        builder.vertex(matrix, x + width, y + height, z).texture(this.u + this.texWidth, this.v + this.texHeight).color(this.color.color3());
        builder.vertex(matrix, x + width, y, z).texture(this.u + this.texWidth, this.v).color(this.color.color4());
        BufferRenderer.drawWithGlobalProgram(builder.end());
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}