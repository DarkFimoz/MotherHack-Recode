package fun.motherhack.modules.impl.combat;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.world.InventoryUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

public class AutoShield extends Module {

    private final NumberSetting health = new NumberSetting("settings.autoshield.health", 10f, 0f, 36f, 0.5f);
    private final BooleanSetting calcAbsorption = new BooleanSetting("settings.autoshield.calcabsorption", true);
    private final BooleanSetting onProjectile = new BooleanSetting("settings.autoshield.onprojectile", true);
    private final NumberSetting projectileRange = new NumberSetting("settings.autoshield.projectilerange", 8.0f, 3.0f, 15.0f, 0.5f, onProjectile::getValue);
    private final BooleanSetting fall = new BooleanSetting("settings.autoshield.fall", true);
    private final NumberSetting fallDistance = new NumberSetting("settings.autoshield.falldistance", 15f, 5f, 50f, 1f, fall::getValue);
    private final BooleanSetting elytra = new BooleanSetting("settings.autoshield.elytra", true);
    private final BooleanSetting onCrystal = new BooleanSetting("settings.autoshield.oncrystal", true);
    private final BooleanSetting onCreeper = new BooleanSetting("settings.autoshield.oncreeper", true);
    private final BooleanSetting onAnchor = new BooleanSetting("settings.autoshield.onanchor", true);
    private final BooleanSetting onTnt = new BooleanSetting("settings.autoshield.ontnt", true);
    private final BooleanSetting onMinecartTnt = new BooleanSetting("settings.autoshield.onminecarttnt", true);
    private final NumberSetting explosionRange = new NumberSetting("settings.autoshield.explosionrange", 6.0f, 3.0f, 12.0f, 0.5f);
    private final NumberSetting swapDelay = new NumberSetting("settings.autoshield.swapdelay", 200, 50, 500, 10);
    private final BooleanSetting autoBlock = new BooleanSetting("settings.autoshield.autoblock", true);
    
    private long lastSwapTime = 0;
    private boolean isSwapping = false;

    public AutoShield() {
        super("AutoShield", Category.Combat);
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick e) {
        if (fullNullCheck()) return;

        boolean hasShieldInOffhand = mc.player.getOffHandStack().getItem() == Items.SHIELD;
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastSwap = currentTime - lastSwapTime;
        
        if (timeSinceLastSwap < swapDelay.getValue().longValue()) {
            return;
        }
        
        if (isSwapping) {
            return;
        }
        
        boolean needShield = false;
        
        // Check health
        float currentHealth = calcAbsorption.getValue() 
            ? mc.player.getHealth() + mc.player.getAbsorptionAmount() 
            : mc.player.getHealth();
            
        if (currentHealth <= health.getValue()) {
            needShield = true;
        }
        
        // Check for fall damage
        if (fall.getValue() && !needShield) {
            if (MotherHack.getInstance().getServerManager().getFallDistance() >= fallDistance.getValue()) {
                needShield = true;
            }
        }
        
        // Check for elytra flight
        if (elytra.getValue() && !needShield) {
            if (mc.player.getInventory().getArmorStack(2).getItem() == Items.ELYTRA && mc.player.isGliding()) {
                needShield = true;
            }
        }
        
        // Check for projectiles
        if (onProjectile.getValue() && !needShield) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof ProjectileEntity || entity instanceof ArrowEntity) {
                    double distance = mc.player.getPos().squaredDistanceTo(entity.getPos());
                    if (distance <= projectileRange.getValue() * projectileRange.getValue()) {
                        needShield = true;
                        break;
                    }
                }
            }
        }
        
        // Check for end crystals
        if (onCrystal.getValue() && !needShield) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EndCrystalEntity) {
                    double distance = mc.player.getPos().squaredDistanceTo(entity.getPos());
                    if (distance <= explosionRange.getValue() * explosionRange.getValue()) {
                        needShield = true;
                        break;
                    }
                }
            }
        }
        
        // Check for creepers
        if (onCreeper.getValue() && !needShield) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof CreeperEntity) {
                    double distance = mc.player.getPos().squaredDistanceTo(entity.getPos());
                    if (distance <= explosionRange.getValue() * explosionRange.getValue()) {
                        needShield = true;
                        break;
                    }
                }
            }
        }
        
        // Check for TNT
        if (onTnt.getValue() && !needShield) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof TntEntity) {
                    double distance = mc.player.getPos().squaredDistanceTo(entity.getPos());
                    if (distance <= explosionRange.getValue() * explosionRange.getValue()) {
                        needShield = true;
                        break;
                    }
                }
            }
        }
        
        // Check for TNT minecart
        if (onMinecartTnt.getValue() && !needShield) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof TntMinecartEntity) {
                    double distance = mc.player.getPos().squaredDistanceTo(entity.getPos());
                    if (distance <= explosionRange.getValue() * explosionRange.getValue()) {
                        needShield = true;
                        break;
                    }
                }
            }
        }
        
        // Check for respawn anchors
        if (onAnchor.getValue() && !needShield) {
            int range = (int) explosionRange.getValue().floatValue();
            BlockPos playerPos = mc.player.getBlockPos();
            for (int x = -range; x <= range; x++) {
                for (int y = -range; y <= range; y++) {
                    for (int z = -range; z <= range; z++) {
                        BlockPos pos = playerPos.add(x, y, z);
                        if (mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                            needShield = true;
                            break;
                        }
                    }
                    if (needShield) break;
                }
                if (needShield) break;
            }
        }
        
        if (needShield && !hasShieldInOffhand) {
            int shieldSlot = InventoryUtils.find(Items.SHIELD);
            if (shieldSlot != -1) {
                isSwapping = true;
                lastSwapTime = currentTime;
                performShieldSwap(shieldSlot, 45);
            }
        }
        
        // Auto block with shield
        if (autoBlock.getValue() && hasShieldInOffhand && needShield) {
            if (!mc.player.isUsingItem()) {
                mc.options.useKey.setPressed(true);
            }
        } else if (autoBlock.getValue() && hasShieldInOffhand && !needShield) {
            mc.options.useKey.setPressed(false);
        }
    }
    
    private void performShieldSwap(int slot, int targetSlot) {
        if (slot == -1 || targetSlot == -1) return;
        
        new Thread(() -> {
            try {
                Thread.sleep(50 + (long)(Math.random() * 50));
                
                mc.interactionManager.clickSlot(
                    mc.player.playerScreenHandler.syncId, 
                    InventoryUtils.indexToSlot(slot), 
                    0, 
                    SlotActionType.PICKUP, 
                    mc.player
                );
                
                Thread.sleep(80 + (long)(Math.random() * 70));
                
                mc.interactionManager.clickSlot(
                    mc.player.playerScreenHandler.syncId, 
                    targetSlot, 
                    0, 
                    SlotActionType.PICKUP, 
                    mc.player
                );
                
                Thread.sleep(80 + (long)(Math.random() * 70));
                
                mc.interactionManager.clickSlot(
                    mc.player.playerScreenHandler.syncId, 
                    InventoryUtils.indexToSlot(slot), 
                    0, 
                    SlotActionType.PICKUP, 
                    mc.player
                );
                
                Thread.sleep(50 + (long)(Math.random() * 50));
                
                isSwapping = false;
                
            } catch (Exception ex) {
                ex.printStackTrace();
                isSwapping = false;
            }
        }).start();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        isSwapping = false;
        mc.options.useKey.setPressed(false);
    }
}
