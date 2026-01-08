package fun.motherhack.modules.impl.misc;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.modules.settings.impl.StringSetting;
import fun.motherhack.modules.settings.api.Bind;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AntiCheatDetector extends Module {
    
    private final Map<String, Integer> violationCount = new ConcurrentHashMap<>();
    private final Map<String, Long> lastCheck = new ConcurrentHashMap<>();
    private final Set<String> detectedAC = new HashSet<>();
    private long lastDetectionTime = 0;
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final List<Integer> transactions = new ArrayList<>();
    private boolean isCapturingTransactions = false;
    
    // Settings
    private final BooleanSetting verbose = new BooleanSetting("Verbose", true);
    private final BooleanSetting detectOnJoin = new BooleanSetting("Detect on Join", true);
    private final BooleanSetting autoDisable = new BooleanSetting("Auto Disable", true);
    private final NumberSetting checkInterval = new NumberSetting("Check Interval", 5, 1, 30, 1);
    private final NumberSetting violationThreshold = new NumberSetting("Violation Threshold", 3, 1, 10, 1);
    
    // Common anti-cheat signatures
    private final Map<String, String> acSignatures = new HashMap<String, String>() {{
        put("NCP", "NoCheatPlus");
        put("Spartan", "Spartan");
        put("Vulcan", "Vulcan");
        put("GrimAC", "GrimAC");
        put("Matrix", "Matrix");
        put("Verus", "Verus");
        put("Watchdog", "Watchdog (Hypixel)");
        put("AAC", "Advanced AntiCheat");
        put("AAC5", "AAC 5.0");
        put("Intave", "Intave");
        put("Horizon", "Horizon");
        put("Kauri", "Kauri");
        put("AntiCheatReloaded", "AntiCheatReloaded");
        put("Themis", "Themis");
        put("Negativity", "Negativity");
    }};

    public AntiCheatDetector() {
        super("AntiCheatDetector", Category.Misc);
        getSettings().addAll(Arrays.asList(verbose, detectOnJoin, autoDisable, checkInterval, violationThreshold));
        MotherHack.getInstance().getEventHandler().subscribe(this);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        violationCount.clear();
        detectedAC.clear();
        lastCheck.clear();
        lastDetectionTime = System.currentTimeMillis();
        
        if (mc.player != null) {
            sendMessage("§aAnti-Cheat Detector enabled. Starting detection...");
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        sendMessage("§cAnti-Cheat Detector disabled.");
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick event) {
        if (mc.player == null || mc.world == null) return;
        
        long currentTime = System.currentTimeMillis();
        
        // Periodic checks
        if (currentTime - lastDetectionTime > checkInterval.getValue().longValue() * 1000) {
            checkMovementAnomalies();
            lastDetectionTime = currentTime;
        }
        
        // Check for AC on join
        if (detectOnJoin.getValue() && isToggled()) {
            String antiCheat = guessAntiCheat(mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : null);
            if (antiCheat != null) {
                sendMessage("§a[AC Detect] Detected anti-cheat: §f" + antiCheat);
                if (autoDisable.getValue()) {
                    toggle();
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPacketReceive(EventPacket.Receive event) {
        Packet<?> packet = event.getPacket();
        
        if (packet instanceof GameJoinS2CPacket) {
            transactions.clear();
            isCapturingTransactions = true;
        }
    }

    
    private void checkForAC(String checkType) {
        if (System.currentTimeMillis() - lastCheck.getOrDefault(checkType, 0L) < 1000) {
            return; // Prevent spam
        }
        
        lastCheck.put(checkType, System.currentTimeMillis());
        int count = violationCount.getOrDefault(checkType, 0) + 1;
        violationCount.put(checkType, count);
        
        if (verbose.getValue() && count % 3 == 0) {
            sendMessage("§e[AC Detection] " + checkType + " detected (x" + count + ")");
        }
        
        if (count >= violationThreshold.getValue().intValue()) {
            String acType = acSignatures.getOrDefault(checkType, "Unknown");
            if (detectedAC.add(acType)) {
                sendMessage("§c[!] Detected Anti-Cheat: §f" + acType);
                
                // Additional detection logic based on the check type
                if (checkType.contains("Rotation")) {
                    detectRotationBasedAC();
                }
            }
        }
    }
    
    private void checkMovementAnomalies() {
        if (mc.player == null) return;
        
        // Get player velocity
        Vec3d velocity = mc.player.getVelocity();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        
        // Check for abnormal movement patterns
        if (horizontalSpeed > 1.0 && mc.player.isOnGround()) {
            checkForAC("Speed Anomaly (" + String.format("%.2f", horizontalSpeed) + ")");
        }
        
        // Check for flight
        if (!mc.player.isOnGround() && velocity.y > 0 && velocity.y < 0.1) {
            checkForAC("Flight Anomaly (Hover)");
        }
    }
    
    
    private void detectRotationBasedAC() {
        if (mc.player == null) return;
        
        // Check for specific rotation patterns
        float yaw = mc.player.getYaw() % 360;
        if (yaw < 0) yaw += 360;
        
        // Some ACs have specific rotation handling
        if (yaw % 45 == 0 && yaw % 90 != 0) {
            checkForAC("Rotation Precision (45°)");
        }
    }
    
    /**
     * Attempts to guess the anti-cheat used by the server based on transaction patterns
     * @param address The server address (can be null)
     * @return The detected anti-cheat name or null if unknown
     */
    public String guessAntiCheat(String address) {
        if (transactions.size() < 5) {
            return null;
        }

        List<Integer> diffs = new ArrayList<>();
        for (int i = 1; i < transactions.size(); i++) {
            diffs.add(transactions.get(i) - transactions.get(i - 1));
        }
        int first = transactions.get(0);

        // Check for specific server patterns first
        if (address != null && address.endsWith("hypixel.net")) {
            return "Watchdog (Hypixel)";
        }

        // Check for transaction patterns
        if (diffs.stream().allMatch(d -> d.equals(diffs.get(0)))) {
            int diff = diffs.get(0);
            if (diff == 1) {
                if (first >= -23772 && first <= -23762) return "Vulcan";
                if ((first >= 95 && first <= 105) || (first >= -20005 && first <= -19995)) return "Matrix";
                if (first >= -32773 && first <= -32762) return "Grizzly";
                return "Verus";
            } else if (diff == -1) {
                if (first >= -8287 && first <= -8280) return "Errata";
                if (first < -3000) return "Intave";
                if (first >= -5 && first <= 0) return "Grim";
                if (first >= -3000 && first <= -2995) return "Karhu";
                return "Polar";
            }
        }

        // Check for specific patterns
        if (transactions.size() >= 2 && 
            transactions.get(0).equals(transactions.get(1)) &&
            transactions.subList(2, transactions.size()).stream()
                .allMatch(i -> i == transactions.get(transactions.indexOf(i) - 1) + 1)) {
            return "Verus";
        }

        if (diffs.size() >= 2 && 
            diffs.get(0) >= 100 && 
            diffs.get(1) == -1 &&
            diffs.subList(2, diffs.size()).stream().allMatch(d -> d == -1)) {
            return "Polar";
        }

        if (transactions.get(0) < -3000 && transactions.contains(0)) {
            return "Intave";
        }

        if (transactions.size() >= 3 &&
            transactions.get(0) == -30767 &&
            transactions.get(1) == -30766 &&
            transactions.get(2) == -25767 &&
            transactions.subList(3, transactions.size()).stream()
                .allMatch(i -> i == transactions.get(transactions.indexOf(i) - 1) + 1)) {
            return "Old Vulcan";
        }

        return null;
    }
    
    private void sendMessage(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.of(message), false);
        }
    }
}
