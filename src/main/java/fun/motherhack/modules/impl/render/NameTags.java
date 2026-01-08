package fun.motherhack.modules.impl.render;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.network.Server;
import fun.motherhack.utils.render.fonts.Fonts;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.world.WorldUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

public class NameTags extends Module {

    private final NumberSetting rectRounding = new NumberSetting("settings.nametags.rectrounding", 1.5f, 0f, 5f, 0.5f);
    private final BooleanSetting items = new BooleanSetting("settings.nametags.items", true);
    private final BooleanSetting border = new BooleanSetting("settings.nametags.border", true);
    private final NumberSetting borderRounding = new NumberSetting("settings.nametags.borderrounding", 2f, 0f, 5f, 0.5f, border::getValue);
    private final BooleanSetting box = new BooleanSetting("settings.nametags.box", true);
    private final NumberSetting boxRounding = new NumberSetting("settings.nametags.boxrounding", 2f, 0f, 5f, 0.5f, box::getValue);
    private final BooleanSetting blur = new BooleanSetting("settings.nametags.blur", true);

    public NameTags() {
        super("NameTags", Category.Render);
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck()) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == null) continue;
            if (!(entity instanceof PlayerEntity player)) continue;
            if (entity == mc.player && mc.options.getPerspective() == Perspective.FIRST_PERSON) continue;
            if (Server.isBot(player)) continue;
            double x = MathHelper.lerp(e.getTickCounter().getTickDelta(true), player.prevX, player.getX());
            double y = MathHelper.lerp(e.getTickCounter().getTickDelta(true), player.prevY, player.getY()) + player.getHeight() * 1.3f;
            double z = MathHelper.lerp(e.getTickCounter().getTickDelta(true), player.prevZ, player.getZ());
            Vec3d position = WorldUtils.getPosition(new Vec3d(x, y, z));

            String name = format(player.getDisplayName().getString());
            String health = MathUtils.round(Server.getHealth(player, true)) + "HP";
            String ping = Server.getPing(player) + "ms";
            float nameWidth = Fonts.REGULAR.getWidth(name, 9f);
            float healthWidth = Fonts.REGULAR.getWidth(health, 9f);
            float pingWidth = Fonts.REGULAR.getWidth(ping, 9f);
            float totalWidth = pingWidth + 4f + nameWidth + healthWidth + 8f;
            float rectWidth = (float) ((totalWidth + 8f));
            float height = 15f;
            float centerX = (float) position.getX();
            float centerY = (float) position.getY();
            if (!(position.z > 0) || !(position.z < 1)) continue;

            if (box.getValue()) renderBox(e, player);
            if (items.getValue()) renderItems(e.getContext(), player, centerX, centerY - 25f);

            if (blur.getValue()) {
                Render2D.drawStyledRect(e.getContext().getMatrices(),
                        centerX - rectWidth / 2f,
                        centerY,
                        rectWidth,
                        height,
                        rectRounding.getValue(),
                        entity == mc.player || MotherHack.getInstance().getFriendManager().isFriend(entity.getName().getString()) ? new Color(0, 255, 255, 150) : new Color(0, 0, 0, 150),
                        255
                );
            } else {
                Render2D.drawRoundedRect(e.getContext().getMatrices(),
                        centerX - rectWidth / 2f,
                        centerY,
                        rectWidth,
                        height,
                        rectRounding.getValue(),
                        entity == mc.player || MotherHack.getInstance().getFriendManager().isFriend(entity.getName().getString()) ? new Color(0, 255, 255, 150) : new Color(0, 0, 0, 175)
                );
            }

            if (border.getValue()) Render2D.drawBorder(e.getContext().getMatrices(),
                    centerX - rectWidth / 2f,
                    centerY,
                    rectWidth,
                    height,
                    borderRounding.getValue(),
                    0.8f,
                    0.8f,
                    new Color(200, 200, 200, 200)
            );

            Render2D.drawFont(e.getContext().getMatrices(),
                    Fonts.REGULAR.getFont(9f),
                    ping,
                    centerX - totalWidth / 2f,
                    centerY + 2f,
                    getPingColor(Server.getPing(player))
            );

            if (!name.isEmpty()) Render2D.drawFont(e.getContext().getMatrices(),
                    Fonts.REGULAR.getFont(9f),
                    name,
                    centerX - totalWidth / 2f + pingWidth + 4f,
                    centerY + 2f,
                    Color.WHITE
            );

            Render2D.drawFont(e.getContext().getMatrices(),
                    Fonts.REGULAR.getFont(9f),
                    health,
                    centerX - totalWidth / 2f + pingWidth + 4f + nameWidth + 8f,
                    centerY + 2f,
                    getHealthColor(player)
            );
        }
    }

    private void renderBox(EventRender2D event, PlayerEntity entity) {
        double minX = entity.getBoundingBox().minX;
        double minY = entity.getBoundingBox().minY;
        double minZ = entity.getBoundingBox().minZ;
        double maxX = entity.getBoundingBox().maxX;
        double maxY = entity.getBoundingBox().maxY;
        double maxZ = entity.getBoundingBox().maxZ;

        double interpX = MathHelper.lerp(event.getTickCounter().getTickDelta(true), entity.prevX, entity.getX());
        double interpY = MathHelper.lerp(event.getTickCounter().getTickDelta(true), entity.prevY, entity.getY());
        double interpZ = MathHelper.lerp(event.getTickCounter().getTickDelta(true), entity.prevZ, entity.getZ());

        double offsetX = interpX - entity.getX();
        double offsetY = interpY - entity.getY();
        double offsetZ = interpZ - entity.getZ();

        minX += offsetX;
        maxX += offsetX;
        minY += offsetY;
        maxY += offsetY;
        minZ += offsetZ;
        maxZ += offsetZ;

        Vec3d[] corners = {
                WorldUtils.getPosition(new Vec3d(minX, minY, minZ)),
                WorldUtils.getPosition(new Vec3d(maxX, minY, minZ)),
                WorldUtils.getPosition(new Vec3d(maxX, maxY, minZ)),
                WorldUtils.getPosition(new Vec3d(minX, maxY, minZ)),
                WorldUtils.getPosition(new Vec3d(minX, minY, maxZ)),
                WorldUtils.getPosition(new Vec3d(maxX, minY, maxZ)),
                WorldUtils.getPosition(new Vec3d(maxX, maxY, maxZ)),
                WorldUtils.getPosition(new Vec3d(minX, maxY, maxZ))
        };

        float minScreenX = Float.MAX_VALUE;
        float maxScreenX = Float.MIN_VALUE;
        float minScreenY = Float.MAX_VALUE;
        float maxScreenY = Float.MIN_VALUE;

        for (Vec3d corner : corners) {
            if (corner.z > 0 && corner.z < 1) {
                minScreenX = Math.min(minScreenX, (float) corner.x);
                maxScreenX = Math.max(maxScreenX, (float) corner.x);
                minScreenY = Math.min(minScreenY, (float) corner.y);
                maxScreenY = Math.max(maxScreenY, (float) corner.y);
            }
        }

        if (minScreenX != Float.MAX_VALUE) {
            float width = maxScreenX - minScreenX;
            float height = maxScreenY - minScreenY;

            Render2D.drawBorder(event.getContext().getMatrices(),
                    minScreenX - 2f,
                    minScreenY - 2f,
                    width + 4f,
                    height + 4f,
                    boxRounding.getValue(),
                    0.8f,
                    0.8f,
                    getBorderColor(entity)
            );
        }
    }

    private void renderItems(DrawContext context, PlayerEntity player, float x, float y) {
        ItemStack[] items = {
                player.getMainHandStack(),
                player.getInventory().getArmorStack(3),
                player.getInventory().getArmorStack(2),
                player.getInventory().getArmorStack(1),
                player.getInventory().getArmorStack(0),
                player.getOffHandStack()
        };

        int count = 0;
        for (ItemStack item : items) if (!item.isEmpty()) count++;

        if (count == 0) return;

        float armorWidth = count * 18f;
        float startX = x - armorWidth / 2f;

        int index = 0;
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                if (blur.getValue()) {
                    Render2D.drawStyledRect(context.getMatrices(),
                            startX + index * 18f - 1f,
                            y - 1f,
                            18,
                            item.isDamageable() ? 25 : 20,
                            1.5f,
                            new Color(45, 45, 45, 150),
                            255
                    );
                } else {
                    Render2D.drawRoundedRect(context.getMatrices(),
                            startX + index * 18f - 1f,
                            y - 1f,
                            18,
                            item.isDamageable() ? 25 : 20,
                            1.5f,
                            new Color(0, 0, 0, 175)
                    );
                }

                context.drawItem(item, (int) (startX + index * 18f), (int) y);

                if (item.isStackable()) {
                    Render2D.drawFont(context.getMatrices(),
                            Fonts.REGULAR.getFont(7f),
                            item.getCount() + "",
                            startX + index * 18f + 11.5f - Fonts.REGULAR.getWidth(item.getCount() + "", 7f) / 2f,
                            y + 10f,
                            Color.WHITE
                    );
                }

                if (item.isDamageable()) {
                    float percent = (float) (item.getMaxDamage() - item.getDamage()) / item.getMaxDamage();

                    Color color;
                    if (percent > 0.6f) color = new Color(85, 255, 85);
                    else if (percent > 0.3f) color = new Color(255, 255, 85);
                    else color = new Color(255, 85, 85);

                    Render2D.drawRoundedRect(context.getMatrices(),
                            startX + index * 18f + 1f,
                            y + 17.5f,
                            14,
                            5f,
                            0.5f,
                            new Color(25, 25, 25)
                    );

                    Render2D.drawRoundedRect(context.getMatrices(),
                            startX + index * 18f + 1.5f,
                            y + 18f,
                            13f * percent,
                            4f,
                            0.5f,
                            color
                    );
                }

                index++;
            }
        }
    }

    private Color getHealthColor(LivingEntity entity) {
        float health = MathUtils.round(Server.getHealth(entity, true));

        if (health >= 15) return new Color(0, 255, 0);
        else if (health >= 5) return new Color(255, 150, 0);
        else return new Color(255, 0, 0);
    }

    private Color getBorderColor(LivingEntity entity) {
        float health = MathUtils.round(Server.getHealth(entity, true));

        if (entity == mc.player || MotherHack.getInstance().getFriendManager().getFriends().contains(entity.getName().getString())) return new Color(0, 255, 255);
        else if (health >= 15) return new Color(0, 255, 0);
        else if (health >= 5) return new Color(255, 150, 0);
        else return new Color(255, 0, 0);
    }

    private String format(String text) {
        StringBuilder builder = new StringBuilder();
        boolean next = false;

        for (char c : text.toCharArray()) {
            if (next) {
                next = false;
                continue;
            }

            if (c == '§') {
                next = true;
                continue;
            }

            builder.append(c);
        }

        return builder.toString();
    }

    private Color getPingColor(int ping) {
        if (ping <= 50) return new Color(0, 255, 0);
        else if (ping <= 100) return new Color(255, 255, 0);
        else if (ping <= 150) return new Color(255, 165, 0);
        else return new Color(255, 0, 0);
    }
}
