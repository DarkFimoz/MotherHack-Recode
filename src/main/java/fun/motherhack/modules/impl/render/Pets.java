package fun.motherhack.modules.impl.render;

import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.api.interfaces.IPlayerEntity;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.EnumSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;

public class Pets extends Module {

    public enum ParrotVariant implements Nameable {
        RED_BLUE("Red Blue", 0),
        BLUE("Blue", 1),
        GREEN("Green", 2),
        YELLOW_BLUE("Yellow Blue", 3),
        GRAY("Gray", 4);

        private final String displayName;
        private final int variantId;

        ParrotVariant(String displayName, int variantId) {
            this.displayName = displayName;
            this.variantId = variantId;
        }

        public int getVariantId() {
            return variantId;
        }

        @Override
        public String getName() {
            return displayName;
        }
    }

    public enum Shoulder implements Nameable {
        RIGHT("Right"), 
        LEFT("Left"), 
        BOTH("Both");
        
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

    private NbtCompound savedLeftShoulder = null;
    private NbtCompound savedRightShoulder = null;

    public Pets() {
        super("Pets", Category.Render);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player != null) {
            // Сохраняем оригинальные данные плеч
            savedLeftShoulder = mc.player.getShoulderEntityLeft().copy();
            savedRightShoulder = mc.player.getShoulderEntityRight().copy();
            
            // Устанавливаем попугая на плечо
            updateParrotOnShoulder();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player != null) {
            // Восстанавливаем оригинальные данные плеч
            ((IPlayerEntity) mc.player).setShoulderEntityLeft(savedLeftShoulder != null ? savedLeftShoulder : new NbtCompound());
            ((IPlayerEntity) mc.player).setShoulderEntityRight(savedRightShoulder != null ? savedRightShoulder : new NbtCompound());
        }
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (mc.player == null) return;
        
        // Постоянно обновляем попугая на плече, чтобы он не слетал
        updateParrotOnShoulder();
    }

    private void updateParrotOnShoulder() {
        NbtCompound parrotNbt = createParrotNbt();
        
        Shoulder shoulderSetting = shoulder.getValue();
        
        IPlayerEntity player = (IPlayerEntity) mc.player;
        
        switch (shoulderSetting) {
            case LEFT:
                player.setShoulderEntityLeft(parrotNbt);
                player.setShoulderEntityRight(new NbtCompound());
                break;
            case RIGHT:
                player.setShoulderEntityLeft(new NbtCompound());
                player.setShoulderEntityRight(parrotNbt);
                break;
            case BOTH:
                player.setShoulderEntityLeft(parrotNbt.copy());
                player.setShoulderEntityRight(parrotNbt.copy());
                break;
        }
    }

    private NbtCompound createParrotNbt() {
        NbtCompound nbt = new NbtCompound();
        
        // Устанавливаем тип сущности - попугай
        nbt.putString("id", EntityType.getId(EntityType.PARROT).toString());
        
        // Устанавливаем вариант попугая (цвет)
        nbt.putInt("Variant", variant.getValue().getVariantId());
        
        return nbt;
    }
}
