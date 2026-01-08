package fun.motherhack.modules.impl.combat;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.movement.GuiMove;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.world.InventoryUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

public class OffHand extends Module {

    public enum AntiCheatMode implements Nameable {
        Normal("Normal"),
        Matrix("Matrix"),
        Grim("Grim"),
        Hybrid("Hybrid");

        private final String name;

        AntiCheatMode(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private final NumberSetting health = new NumberSetting("settings.offhand.health", 5f, 0f, 36f, 0.05f);
    private final BooleanSetting calcAbsorption = new BooleanSetting("settings.offhand.calcabsorption", true);
    private final BooleanSetting elytra = new BooleanSetting("settings.offhand.elytra", false);
    private final NumberSetting elytraHealth = new NumberSetting("settings.offhand.elytrahealth", 10f, 0f, 36f, 0.05f, elytra::getValue);
    private final BooleanSetting fall = new BooleanSetting("settings.offhand.fall", false);
    private final NumberSetting fallDistance = new NumberSetting("settings.offhand.falldistance", 20f, 10f, 50f, 0.05f, fall::getValue);
    private final BooleanSetting onCrystal = new BooleanSetting("settings.offhand.oncrystal", true);
    private final BooleanSetting onObsidianPlace = new BooleanSetting("settings.offhand.onobsidianplace", false);
    private final BooleanSetting onCrystalInHand = new BooleanSetting("settings.offhand.oncrystalinhand", false);
    private final BooleanSetting onMinecartTnt = new BooleanSetting("settings.offhand.onminecarttnt", true);
    private final BooleanSetting onCreeper = new BooleanSetting("settings.offhand.oncreeper", true);
    private final BooleanSetting onAnchor = new BooleanSetting("settings.offhand.onanchor", true);
    private final BooleanSetting onTnt = new BooleanSetting("settings.offhand.ontnt", true);
    private final NumberSetting explosionRange = new NumberSetting("settings.offhand.explosionrange", 6.0f, 1.0f, 12.0f, 0.5f);
    private final BooleanSetting inContainer = new BooleanSetting("settings.offhand.incontainer", true);
    private final BooleanSetting sync = new BooleanSetting("settings.sync", false);
    private final EnumSetting<AntiCheatMode> antiCheatMode = new EnumSetting<>("settings.offhand.anticheat", AntiCheatMode.Normal);
    private final NumberSetting randomDelayMin = new NumberSetting("settings.offhand.randomdelaymin", 0, 0, 500, 10);
    private final NumberSetting randomDelayMax = new NumberSetting("settings.offhand.randomdelaymax", 100, 0, 500, 10);
    private final BooleanSetting matrixBypass = new BooleanSetting("settings.offhand.matrixbypass", false);
    private final BooleanSetting grimBypass = new BooleanSetting("settings.offhand.grimbypass", false);

    public OffHand() {
        super("OffHand", Category.Combat);
    }

    private int ticks;
    private Item previousItem = null;
    private long lastSwapTime = 0;
    private int swapCooldown = 0;

    @EventHandler
    public void onPlayerTick(EventPlayerTick e) {
        if (fullNullCheck()) return;
        if (mc.currentScreen != null && !inContainer.getValue()) return;
        
        // Anti-cheat cooldown management
        if (swapCooldown > 0) {
            swapCooldown--;
            return;
        }
        
        if (ticks > 0) {
        	ticks--;
            return;
        }

        Item currentOffhandItem = mc.player.getOffHandStack().isEmpty() ? null : mc.player.getOffHandStack().getItem();
        
        if (needTotems()) {
            if (currentOffhandItem != Items.TOTEM_OF_UNDYING) {
                int totemSlot = InventoryUtils.find(Items.TOTEM_OF_UNDYING);
                if (totemSlot != -1) {
                    performAntiCheatSwap(totemSlot, 45);
                    ticks = sync.getValue() ? MotherHack.getInstance().getModuleManager().getModule(GuiMove.class).getTicks() : 0;
                }
            }
        } else if (currentOffhandItem == Items.TOTEM_OF_UNDYING && previousItem != null) {
            int previousSlot = InventoryUtils.find(previousItem);
            if (previousSlot != -1) {
                performAntiCheatSwap(previousSlot, 45);
                ticks = sync.getValue() ? MotherHack.getInstance().getModuleManager().getModule(GuiMove.class).getTicks() : 0;
                previousItem = null;
            }
        }

        if (needTotems() && currentOffhandItem != Items.TOTEM_OF_UNDYING && currentOffhandItem != null) previousItem = currentOffhandItem;
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive e) {
        if (fullNullCheck()) return;
        
        // Handle crystal spawn packets
        if (e.getPacket() instanceof EntitySpawnS2CPacket spawn) {
            if (spawn.getEntityType() == EntityType.END_CRYSTAL) {
                if (onCrystal.getValue()) {
                    double distance = mc.player.getPos().squaredDistanceTo(spawn.getX(), spawn.getY(), spawn.getZ());
                    if (distance <= explosionRange.getValue() * explosionRange.getValue()) {
                        int totemSlot = InventoryUtils.find(Items.TOTEM_OF_UNDYING);
                        if (totemSlot != -1 && mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                            performAntiCheatSwap(totemSlot, 45);
                        }
                    }
                }
            }
        }
        
        // Handle obsidian place packets
        if (e.getPacket() instanceof BlockUpdateS2CPacket blockUpdate) {
            if (blockUpdate.getState().getBlock() == Blocks.OBSIDIAN && onObsidianPlace.getValue()) {
                double distance = mc.player.getPos().squaredDistanceTo(blockUpdate.getPos().toCenterPos());
                if (distance <= explosionRange.getValue() * explosionRange.getValue()) {
                    int totemSlot = InventoryUtils.find(Items.TOTEM_OF_UNDYING);
                    if (totemSlot != -1 && mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                        performAntiCheatSwap(totemSlot, 45);
                    }
                }
            }
        }
    }

    private boolean needTotems() {
        float currentHealth = calcAbsorption.getValue() 
            ? mc.player.getHealth() + mc.player.getAbsorptionAmount() 
            : mc.player.getHealth();
            
        if (currentHealth <= health.getValue()) return true;
        
        if (fall.getValue() && MotherHack.getInstance().getServerManager().getFallDistance() >= fallDistance.getValue()) return true;
        
        if (elytra.getValue() && mc.player.getInventory().getArmorStack(2).getItem() == Items.ELYTRA) {
            if (currentHealth <= elytraHealth.getValue()) return true;
        }
        
        // Check for crystal threats
        if (onCrystal.getValue()) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EndCrystalEntity) {
                    if (mc.player.getPos().squaredDistanceTo(entity.getPos()) <= explosionRange.getValue() * explosionRange.getValue()) {
                        return true;
                    }
                }
            }
        }
        
        // Check for crystal in hand of nearby players
        if (onCrystalInHand.getValue()) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player) continue;
                if (mc.player.getPos().squaredDistanceTo(player.getPos()) <= 36) {
                    Item mainHand = player.getMainHandStack().getItem();
                    Item offHand = player.getOffHandStack().getItem();
                    if (mainHand == Items.END_CRYSTAL || mainHand == Items.OBSIDIAN ||
                        offHand == Items.END_CRYSTAL || offHand == Items.OBSIDIAN) {
                        return true;
                    }
                }
            }
        }
        
        // Check for TNT
        if (onTnt.getValue()) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof TntEntity) {
                    if (mc.player.getPos().squaredDistanceTo(entity.getPos()) <= explosionRange.getValue() * explosionRange.getValue()) {
                        return true;
                    }
                }
            }
        }
        
        // Check for TNT minecart
        if (onMinecartTnt.getValue()) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof TntMinecartEntity) {
                    if (mc.player.getPos().squaredDistanceTo(entity.getPos()) <= explosionRange.getValue() * explosionRange.getValue()) {
                        return true;
                    }
                }
            }
        }
        
        // Check for creepers
        if (onCreeper.getValue()) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof CreeperEntity) {
                    if (mc.player.getPos().squaredDistanceTo(entity.getPos()) <= explosionRange.getValue() * explosionRange.getValue()) {
                        return true;
                    }
                }
            }
        }
        
        // Check for respawn anchors
        if (onAnchor.getValue()) {
            int range = (int) explosionRange.getValue().floatValue();
            BlockPos playerPos = mc.player.getBlockPos();
            for (int x = -range; x <= range; x++) {
                for (int y = -range; y <= range; y++) {
                    for (int z = -range; z <= range; z++) {
                        BlockPos pos = playerPos.add(x, y, z);
                        if (mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }

    private void performAntiCheatSwap(int slot, int targetSlot) {
        if (slot == -1 || targetSlot == -1) return;

        // Apply anti-cheat specific bypasses
        switch (antiCheatMode.getValue()) {
            case Matrix:
                performMatrixBypass(slot, targetSlot);
                break;
            case Grim:
                performGrimBypass(slot, targetSlot);
                break;
            case Hybrid:
                performHybridBypass(slot, targetSlot);
                break;
            case Normal:
            default:
                performNormalSwap(slot, targetSlot);
                break;
        }
    }

    private void performNormalSwap(int slot, int targetSlot) {
        if (sync.getValue()) {
            InventoryUtils.bypassSwap(slot, targetSlot);
        } else {
            InventoryUtils.swap(slot, targetSlot);
        }
    }

    private void performMatrixBypass(int slot, int targetSlot) {
        // Matrix anti-cheat detection prevention
        if (matrixBypass.getValue()) {
            // Add random delays to avoid pattern detection
            int randomDelay = randomDelayMin.getValue().intValue() + (int)(Math.random() * (randomDelayMax.getValue().intValue() - randomDelayMin.getValue().intValue() + 1));
            swapCooldown = randomDelay / 50; // Convert milliseconds to ticks
        }

        // Use alternative swap method with delays
        new Thread(() -> {
            try {
                if (matrixBypass.getValue()) Thread.sleep(randomDelayMin.getValue().longValue());
                
                // Perform swap with randomized timing
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, InventoryUtils.indexToSlot(slot), 0, SlotActionType.PICKUP, mc.player);
                Thread.sleep(50 + (long)(Math.random() * 50));
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
                Thread.sleep(50 + (long)(Math.random() * 50));
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, InventoryUtils.indexToSlot(slot), 0, SlotActionType.PICKUP, mc.player);
                
                if (matrixBypass.getValue()) {
                    // Matrix post-swap delay
                    Thread.sleep(50 + (long)(Math.random() * 50));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void performGrimBypass(int slot, int targetSlot) {
        // Grim anti-cheat detection prevention
        if (grimBypass.getValue()) {
            // Grim detects rapid inventory actions, so we need to space them out
            long currentTime = System.currentTimeMillis();
            long timeSinceLastSwap = currentTime - lastSwapTime;
            
            // Minimum delay between swaps for Grim
            int minDelay = 150;
            if (timeSinceLastSwap < minDelay) {
                swapCooldown = (int)((minDelay - timeSinceLastSwap) / 50);
                return;
            }
            
            lastSwapTime = currentTime;
        }

        // Use slower, more human-like swap pattern for Grim
        new Thread(() -> {
            try {
                // Add random human-like delay before starting
                Thread.sleep(100 + (long)(Math.random() * 100));
                
                // Perform swap with human-like timing
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, InventoryUtils.indexToSlot(slot), 0, SlotActionType.PICKUP, mc.player);
                Thread.sleep(200 + (long)(Math.random() * 100));
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
                Thread.sleep(200 + (long)(Math.random() * 100));
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, InventoryUtils.indexToSlot(slot), 0, SlotActionType.PICKUP, mc.player);
                
                // Add post-swap delay
                Thread.sleep(150 + (long)(Math.random() * 100));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void performHybridBypass(int slot, int targetSlot) {
        // Hybrid approach combining Matrix and Grim techniques
        if (matrixBypass.getValue() || grimBypass.getValue()) {
            // Combine both random delays and timing patterns
            int randomDelay = randomDelayMin.getValue().intValue() + (int)(Math.random() * (randomDelayMax.getValue().intValue() - randomDelayMin.getValue().intValue() + 1));
            swapCooldown = randomDelay / 50;
            
            // Send spoofed packets for Matrix
            if (matrixBypass.getValue()) {
                // Additional matrix timing randomization
                try {
                    Thread.sleep(30 + (long)(Math.random() * 70));
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }

        // Use threaded approach with combined timing
        new Thread(() -> {
            try {
                // Initial random delay
                Thread.sleep(100 + (long)(Math.random() * 100));
                
                // Perform swap with variable timing
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, InventoryUtils.indexToSlot(slot), 0, SlotActionType.PICKUP, mc.player);
                Thread.sleep(150 + (long)(Math.random() * 100));
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
                Thread.sleep(150 + (long)(Math.random() * 100));
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, InventoryUtils.indexToSlot(slot), 0, SlotActionType.PICKUP, mc.player);
                
                // Final delay and cleanup
                Thread.sleep(100 + (long)(Math.random() * 50));
                
                if (matrixBypass.getValue()) {
                    // Final matrix cleanup delay
                    Thread.sleep(40 + (long)(Math.random() * 60));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }
}