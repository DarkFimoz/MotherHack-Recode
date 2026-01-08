package fun.motherhack.api.events.impl;

import fun.motherhack.api.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class EventRender3D extends Event {
    @AllArgsConstructor @Getter
    public static class Game extends Event {
        private final RenderTickCounter tickCounter;
        private final MatrixStack matrixStack;
    }

    @AllArgsConstructor @Getter
    public static class World extends Event {
        private final Camera camera;
        private final Matrix4f positionMatrix;
        private final RenderTickCounter tickCounter;
    }
}