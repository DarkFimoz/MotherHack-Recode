package fun.motherhack.api.render.builders;

import fun.motherhack.api.render.builders.impl.BlurBuilder;
import fun.motherhack.api.render.builders.impl.BorderBuilder;
import fun.motherhack.api.render.builders.impl.RectangleBuilder;
import fun.motherhack.api.render.builders.impl.TextureBuilder;
import fun.motherhack.api.render.builders.impl.TextBuilder;;

public final class Builder {

    private static final RectangleBuilder RECTANGLE_BUILDER = new RectangleBuilder();
    private static final BorderBuilder BORDER_BUILDER = new BorderBuilder();
    private static final TextureBuilder TEXTURE_BUILDER = new TextureBuilder();
    private static final TextBuilder TEXT_BUILDER = new TextBuilder();
    private static final BlurBuilder BLUR_BUILDER = new BlurBuilder();

    public static RectangleBuilder rectangle() {
        return RECTANGLE_BUILDER;
    }

    public static BorderBuilder border() {
        return BORDER_BUILDER;
    }

    public static TextureBuilder texture() {
        return TEXTURE_BUILDER;
    }

    public static TextBuilder text() {
        return TEXT_BUILDER;
    }

    public static BlurBuilder blur() {
        return BLUR_BUILDER;
    }
}