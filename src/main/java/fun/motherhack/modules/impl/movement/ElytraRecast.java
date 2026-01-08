package fun.motherhack.modules.impl.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.api.events.impl.rotations.EventMotion;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.MotherHack;
import fun.motherhack.modules.settings.api.Nameable;

public class ElytraRecast extends Module {
    private int rotationTicks = 0;
    private float targetPitch = 0;
    private float prevClientPitch, prevClientYaw, jitter;
    private int jumpCounter = 0;
    private boolean hasJumpedOnce = false;
    
    // Legit mode variables
    private int legitDelay = 0;
    private int legitCooldown = 0;
    private boolean legitWaitingForFall = false;
    private long lastJumpTime = 0;
    private java.util.Random legitRandom = new java.util.Random();

    public ElytraRecast() {
        super("ElytraRecast", Category.Movement);
    }

    public EnumSetting<Exploit> exploit = new EnumSetting<>("Exploit", Exploit.None);
    public BooleanSetting changePitch = new BooleanSetting("ChangePitch", true);
    public NumberSetting pitchValue = new NumberSetting("PitchValue", 55f, -90f, 90f, 1f);
    public BooleanSetting autoWalk = new BooleanSetting("AutoWalk", true, () -> exploit.getValue() == Exploit.TH);
    public BooleanSetting autoJump = new BooleanSetting("AutoJump", true, () -> exploit.getValue() == Exploit.TH);
    public BooleanSetting allowBroken = new BooleanSetting("AllowBroken", true, () -> exploit.getValue() == Exploit.TH);
    
    // Legit mode settings
    public NumberSetting legitMinDelay = new NumberSetting("LegitMinDelay", 3f, 1f, 10f, 1f, () -> exploit.getValue() == Exploit.Legit);
    public NumberSetting legitMaxDelay = new NumberSetting("LegitMaxDelay", 6f, 2f, 15f, 1f, () -> exploit.getValue() == Exploit.Legit);
    public NumberSetting legitFallDistance = new NumberSetting("LegitFallDistance", 0.3f, 0.1f, 1.0f, 0.05f, () -> exploit.getValue() == Exploit.Legit);
    public BooleanSetting legitOnlyMoving = new BooleanSetting("LegitOnlyMoving", true, () -> exploit.getValue() == Exploit.Legit);

    private enum Exploit implements Nameable {
        None("None"),
        Strict("Strict"),
        Strong("Strong"),
        TH("TH"),
        Legit("Legit"),
        Liquid("Liquid");

        private final String name;

        Exploit(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    @EventHandler
    public void onTick(EventPlayerTick e) {
        if (mc.player == null || mc.world == null) return;

        if (exploit.getValue() == Exploit.Liquid) {
            handleLiquidMode();
        } else if (exploit.getValue() == Exploit.TH) {
            // Only press jump when autoJump is enabled and the conditions for elytra recast are plausible
            if (autoJump.getValue()) {
                if (checkElytra() || mc.player.isOnGround()) { mc.options.jumpKey.setPressed(true); }
                else mc.options.jumpKey.setPressed(false);
            } else {
                mc.options.jumpKey.setPressed(false);
            }

            // Auto walk should only be forced when explicitly enabled, otherwise ensure it's released
            if (autoWalk.getValue()) mc.options.forwardKey.setPressed(true);
            else mc.options.forwardKey.setPressed(false);

            if (!mc.player.isGliding() && mc.player.fallDistance > 0 && checkElytra())
                castElytra();

            jitter = (20 * (float) Math.sin((System.currentTimeMillis() - MotherHack.getInstance().getInitTime()) / 50f));

            if (mc.player.isOnGround()) {
                mc.player.setNoGravity(false);
            }
        } else if (exploit.getValue() == Exploit.Legit) {
            handleLegitMode();
        } else {
            if (!mc.player.isGliding() && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA && isMoving()) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }

            if (mc.player.isOnGround() && mc.player.isGliding()) {
                if (mc.options.jumpKey.isPressed()) {
                    targetPitch = -45;
                    rotationTicks = 12;
                } else {
                    targetPitch = 90;
                    rotationTicks = 2;
                    mc.player.jump();
                    mc.player.setVelocity(mc.player.getVelocity().x, 0.085, mc.player.getVelocity().z);
                }
            }

            if (rotationTicks > 0) {
                rotationTicks--;
            }
        }
    }

    @EventHandler
    public void onMotion(EventMotion em) {
        if (exploit.getValue() == Exploit.TH) {
            if (changePitch.getValue()) {
                em.setPitch(pitchValue.getValue());

                switch (exploit.getValue()) {
                    case None -> {}
                    case Strict -> em.setYaw(mc.player.getYaw() + jitter);
                    case Strong -> em.setPitch(pitchValue.getValue() - Math.abs(jitter / 2f));
                    case TH -> {
                        if (changePitch.getValue())
                            em.setPitch(pitchValue.getValue());
                    }
                    case Liquid -> {}
                    case Legit -> {}
                }
            }
        } else {
            if (rotationTicks > 0) {
                em.setPitch(targetPitch);
            }
        }
    }

    private boolean isMoving() {
        return mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;
    }
    
    private void handleLiquidMode() {
        if (shouldRecastLiquid()) {
            recastElytraLiquid();
        }
    }
    
    private boolean shouldRecastLiquid() {
        ItemStack itemStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        return !mc.player.getAbilities().flying && 
               !mc.player.hasVehicle() && 
               !mc.player.isClimbing() &&
               !mc.player.isTouchingWater() && 
               !mc.player.hasStatusEffect(StatusEffects.LEVITATION) &&
               itemStack.isOf(Items.ELYTRA) && 
               itemStack.getDamage() < itemStack.getMaxDamage() && 
               mc.options.jumpKey.isPressed();
    }
    
    private boolean recastElytraLiquid() {
        if (shouldRecastLiquid()) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            return true;
        }
        return false;
    }
    
    private void handleLegitMode() {
        ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        
        // Check if elytra is equipped
        if (!chestStack.isOf(Items.ELYTRA)) {
            resetLegitState();
            return;
        }
        
        // Check if elytra is broken
        if (chestStack.getDamage() >= chestStack.getMaxDamage()) {
            resetLegitState();
            return;
        }
        
        // If already gliding, nothing to do
        if (mc.player.isGliding()) {
            resetLegitState();
            return;
        }
        
        // Only activate when moving (if setting enabled)
        if (legitOnlyMoving.getValue() && !isMoving()) {
            resetLegitState();
            return;
        }
        
        // Don't activate in water, on ladder, or with levitation
        if (mc.player.isTouchingWater() || mc.player.isClimbing() || 
            mc.player.hasStatusEffect(StatusEffects.LEVITATION) || mc.player.hasVehicle()) {
            resetLegitState();
            return;
        }
        
        // Handle cooldown between recast attempts
        if (legitCooldown > 0) {
            legitCooldown--;
            return;
        }
        
        // On ground - wait for player to naturally jump or fall
        if (mc.player.isOnGround()) {
            legitWaitingForFall = true;
            legitDelay = 0;
            return;
        }
        
        // In air - check if we should deploy elytra
        if (legitWaitingForFall && mc.player.fallDistance >= legitFallDistance.getValue()) {
            // Add randomized delay to simulate human reaction time
            if (legitDelay <= 0) {
                int minDelay = (int) legitMinDelay.getValue().floatValue();
                int maxDelay = (int) legitMaxDelay.getValue().floatValue();
                legitDelay = minDelay + legitRandom.nextInt(Math.max(1, maxDelay - minDelay + 1));
            }
            
            legitDelay--;
            
            if (legitDelay <= 0) {
                // Deploy elytra like a real player would (double-tap space)
                long currentTime = System.currentTimeMillis();
                
                // Ensure minimum time between jumps (human-like)
                if (currentTime - lastJumpTime > 50 + legitRandom.nextInt(100)) {
                    mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                    lastJumpTime = currentTime;
                    
                    // Set cooldown before next attempt (randomized)
                    legitCooldown = 5 + legitRandom.nextInt(10);
                    legitWaitingForFall = false;
                }
            }
        }
    }
    
    private void resetLegitState() {
        legitWaitingForFall = false;
        legitDelay = 0;
    }

    public boolean castElytra() {
        if (checkElytra() && check()) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            return true;
        }
        return false;
    }

    private boolean checkElytra() {
        if (mc.options.jumpKey.isPressed() && !mc.player.getAbilities().flying && !mc.player.hasVehicle() && !mc.player.isClimbing()) {
            ItemStack is = mc.player.getEquippedStack(EquipmentSlot.CHEST);
            return is.isOf(Items.ELYTRA) && (is.getDamage() < is.getMaxDamage() || allowBroken.getValue());
        }
        return false;
    }

    private boolean check() {
        if (!mc.player.isTouchingWater() && !mc.player.hasStatusEffect(StatusEffects.LEVITATION)) {
            ItemStack is = mc.player.getEquippedStack(EquipmentSlot.CHEST);
            if (is.isOf(Items.ELYTRA) && (is.getDamage() < is.getMaxDamage() || allowBroken.getValue())) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
         
        // Check if Minecraft client and player exist
        if (mc == null || mc.player == null) return;
         
        // Reset key states safely
        try {
            mc.options.forwardKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
        } catch (Exception e) {
            // Silently handle any exceptions
        }
         
        // Reset any timers or states
        rotationTicks = 0;
        jitter = 0;
        jumpCounter = 0;
        hasJumpedOnce = false;
        
        // Reset legit mode state
        resetLegitState();
        legitCooldown = 0;
        lastJumpTime = 0;
    }
}