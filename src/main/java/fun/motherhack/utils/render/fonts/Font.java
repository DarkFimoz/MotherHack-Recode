package fun.motherhack.utils.render.fonts;

import fun.motherhack.api.render.msdf.MsdfFont;

public record Font(MsdfFont font) {
    public Instance getFont(float size) {
        return new Instance(font, size);
    }

    public float getWidth(String text, float size) {
        return font.getWidth(text, size);
    }

    public float getHeight(float size) {
        return font.getHeight(size);
    }
}