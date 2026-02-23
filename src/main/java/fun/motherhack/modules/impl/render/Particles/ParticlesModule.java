package fun.motherhack.modules.impl.render.Particles;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.Setting;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

public class ParticlesModule extends Module {
    @Getter private static final ParticlesModule instance = new ParticlesModule();

    private final WorldParticles worldParticles = new WorldParticles();

    public ParticlesModule() {
        super("Particles", Category.Render);
        
        for (Setting<?> setting : worldParticles.getSettings()) {
            getSettings().add(setting);
        }
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        worldParticles.onEvent();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        worldParticles.toggle();
    }

    @Getter
    @Accessors(fluent = true)
    public static class BaseSettings {
        private final EnumSetting<TextureMode> textureMode;
        private final NumberSetting count;
        private final NumberSetting size;
        private final NumberSetting lifeTime;
        private final NumberSetting spawnDuration;
        private final NumberSetting dyingDuration;
        private final BooleanSetting rotate;
        private final BooleanSetting trail;
        private final NumberSetting trailLength;

        private final List<Setting<?>> settings = new ArrayList<>();

        public BaseSettings() {
            textureMode = new EnumSetting<>("Texture", TextureMode.Spark);
            count = new NumberSetting("Count", 25f, 10f, 100f, 1f);
            size = new NumberSetting("Size", 0.2f, 0.1f, 0.4f, 0.05f);
            lifeTime = new NumberSetting("Life time", 10f, 2f, 100f, 1f);
            spawnDuration = new NumberSetting("Spawn duration", 15f, 0f, 40f, 1f);
            dyingDuration = new NumberSetting("Dying duration", 15f, 0f, 40f, 1f);
            rotate = new BooleanSetting("Rotate", true);
            trail = new BooleanSetting("Trail", false);
            trailLength = new NumberSetting("Trail length", 5f, 1f, 20f, 1f);

            settings.add(textureMode);
            settings.add(count);
            settings.add(size);
            settings.add(lifeTime);
            settings.add(spawnDuration);
            settings.add(dyingDuration);
            settings.add(rotate);
            settings.add(trail);
            settings.add(trailLength);
        }

        public List<Setting<?>> getSettings() {
            return settings;
        }
    }

    public enum TextureMode implements fun.motherhack.modules.settings.api.Nameable {
        Spark, Star, Heart, Dollar, Snowflake, Glow, Firefly;

        @Override
        public String getName() {
            return name();
        }
    }
}
