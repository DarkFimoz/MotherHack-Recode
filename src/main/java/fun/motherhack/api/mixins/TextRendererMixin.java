package fun.motherhack.api.mixins;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.impl.misc.NameProtect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextRenderer.class)
public class TextRendererMixin {
    
    @ModifyVariable(method = "draw(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String modifyDrawString(String text) {
        if (text == null) return text;
        NameProtect nameProtect = MotherHack.getInstance().getModuleManager().getModule(NameProtect.class);
        if (nameProtect != null && nameProtect.isToggled()) {
            return nameProtect.replaceName(text);
        }
        return text;
    }
    
    @ModifyVariable(method = "draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Text modifyDrawText(Text text) {
        if (text == null) return text;
        NameProtect nameProtect = MotherHack.getInstance().getModuleManager().getModule(NameProtect.class);
        if (nameProtect != null && nameProtect.isToggled()) {
            return nameProtect.replaceNameDeep(text);
        }
        return text;
    }
}
