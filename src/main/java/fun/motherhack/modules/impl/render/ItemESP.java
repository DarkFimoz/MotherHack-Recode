package fun.motherhack.modules.impl.render;

import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.ColorSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Fonts;
import fun.motherhack.utils.world.WorldUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4d;

import java.awt.Color;
import java.util.*;

public class ItemESP extends Module {
    
    private final ColorSetting backgroundColor;
    private final ColorSetting textColor;
    private final NumberSetting range;
    
    private static final double GROUP_DISTANCE = 2.0;
    
    public ItemESP() {
        super("ItemESP", Category.Render);
        
        this.backgroundColor = new ColorSetting("settings.itemesp.backgroundcolor", 
            new Color(0, 0, 0, 80));
        this.textColor = new ColorSetting("settings.itemesp.textcolor", 
            new Color(255, 255, 255, 255));
        this.range = new NumberSetting("settings.itemesp.range", 100.0f, 10.0f, 5000.0f, 10.0f);
    }
    
    @meteordevelopment.orbit.EventHandler
    public void onRender2D(EventRender2D event) {
        if (fullNullCheck()) {
            return;
        }
        
        List<ItemEntity> itemEntities = new ArrayList<>();
        
        for (var entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity itemEntity)) {
                continue;
            }
            
            float distance = mc.player.distanceTo(entity);
            if (distance > this.range.getValue()) {
                continue;
            }
            
            itemEntities.add(itemEntity);
        }
        
        List<ItemGroup> groups = new ArrayList<>();
        Set<ItemEntity> processed = new HashSet<>();
        
        for (ItemEntity item : itemEntities) {
            if (processed.contains(item)) {
                continue;
            }
            
            ItemGroup group = new ItemGroup();
            group.addEntity(item);
            processed.add(item);
            
            for (ItemEntity other : itemEntities) {
                if (processed.contains(other)) {
                    continue;
                }
                
                double distance = item.getPos().distanceTo(other.getPos());
                if (distance <= GROUP_DISTANCE) {
                    group.addEntity(other);
                    processed.add(other);
                }
            }
            
            groups.add(group);
        }
        
        for (ItemGroup group : groups) {
            renderGroup(event, group);
        }
    }
    
    private void renderGroup(EventRender2D event, ItemGroup group) {
        Vector4d bounds = null;
        
        for (ItemEntity entity : group.entities) {
            Vec3d[] points = getPoints(entity, event.getTickCounter().getTickDelta(true));
            
            for (Vec3d point : points) {
                Vec3d screenPos = WorldUtils.getPosition(
                    new Vec3d(point.x, point.y, point.z));
                
                if (screenPos.z > 0 && screenPos.z < 1) {
                    if (bounds == null) {
                        bounds = new Vector4d(
                            screenPos.x, screenPos.y,
                            screenPos.x, screenPos.y
                        );
                    } else {
                        bounds.x = Math.min(screenPos.x, bounds.x);
                        bounds.y = Math.min(screenPos.y, bounds.y);
                        bounds.z = Math.max(screenPos.x, bounds.z);
                        bounds.w = Math.max(screenPos.y, bounds.w);
                    }
                }
            }
        }
        
        if (bounds == null) {
            return;
        }
        
        float x1 = (float) bounds.x;
        float y1 = (float) bounds.y;
        float x2 = (float) bounds.z;
        float centerX = (x1 + x2) / 2.0f;
        
        Map<String, Integer> itemCounts = new LinkedHashMap<>();
        
        for (ItemEntity entity : group.entities) {
            String itemName = entity.getStack().getName().getString();
            int count = entity.getStack().getCount();
            
            itemCounts.put(itemName, 
                itemCounts.getOrDefault(itemName, 0) + count);
        }
        
        List<String> displayLines = new ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            String line = entry.getValue() > 1 
                ? entry.getKey() + " " + entry.getValue()
                : entry.getKey();
            displayLines.add(line);
        }
        
        float maxWidth = 0;
        for (String line : displayLines) {
            float width = Fonts.BOLD.getWidth(line, 10.0f);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        
        float padding = 12.0f + (displayLines.size() > 1 ? 4.0f : 0.0f);
        float verticalPadding = 4.0f + (displayLines.size() > 1 ? 2.0f : 0.0f);
        float lineHeight = 12.0f;
        
        float boxWidth = maxWidth + padding * 2;
        float boxHeight = displayLines.size() * lineHeight + verticalPadding * 2;
        
        float boxX = centerX - boxWidth / 2.0f;
        float boxY = y1 - boxHeight - 2.0f;
        
        Render2D.drawRoundedRect(
            event.getContext().getMatrices(),
            boxX, boxY, boxWidth, boxHeight, 4.0f,
            this.backgroundColor.getValue()
        );
        
        float textY = boxY + verticalPadding;
        
        for (String line : displayLines) {
            float textWidth = Fonts.BOLD.getWidth(line, 10.0f);
            float textX = boxX + (boxWidth - textWidth) / 2.0f;
            
            Render2D.drawFont(
                event.getContext().getMatrices(),
                Fonts.BOLD.getFont(10.0f),
                line, textX, textY,
                this.textColor.getValue()
            );
            
            textY += lineHeight;
        }
    }
    
    private Vec3d[] getPoints(Entity entity, float partialTicks) {
        Box box = getBox(entity, partialTicks);
        
        return new Vec3d[] {
            new Vec3d(box.minX, box.minY, box.minZ),
            new Vec3d(box.minX, box.maxY, box.minZ),
            new Vec3d(box.maxX, box.minY, box.minZ),
            new Vec3d(box.maxX, box.maxY, box.minZ),
            new Vec3d(box.minX, box.minY, box.maxZ),
            new Vec3d(box.minX, box.maxY, box.maxZ),
            new Vec3d(box.maxX, box.minY, box.maxZ),
            new Vec3d(box.maxX, box.maxY, box.maxZ)
        };
    }
    
    private Box getBox(Entity entity, float partialTicks) {
        double x = MathHelper.lerp(partialTicks, entity.prevX, entity.getX());
        double y = MathHelper.lerp(partialTicks, entity.prevY, entity.getY());
        double z = MathHelper.lerp(partialTicks, entity.prevZ, entity.getZ());
        
        Box box = entity.getBoundingBox();
        
        return new Box(
            box.minX - entity.getX() + x - 0.05,
            box.minY - entity.getY() + y,
            box.minZ - entity.getZ() + z - 0.05,
            box.maxX - entity.getX() + x + 0.05,
            box.maxY - entity.getY() + y + 0.15,
            box.maxZ - entity.getZ() + z + 0.05
        );
    }
    
    static class ItemGroup {
        List<ItemEntity> entities = new ArrayList<>();
        
        void addEntity(ItemEntity entity) {
            entities.add(entity);
        }
    }
}
