package fun.motherhack.modules.impl.movement;

import meteordevelopment.orbit.EventHandler;
import org.jetbrains.annotations.NotNull;
import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.utils.movement.MoveUtils;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class PacketFly extends Module {
    public PacketFly() {
        super("PacketFly", Category.Movement);
    }

    public final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.Fast);
    public final EnumSetting<Type> type = new EnumSetting<>("Type", Type.Preserve);
    public final EnumSetting<Phase> phase = new EnumSetting<>("Phase", Phase.Full);
    public final BooleanSetting limit = new BooleanSetting("Limit", true);
    
    public final BooleanSetting antiKick = new BooleanSetting("AntiKick", true);
    public final NumberSetting interval = new NumberSetting("Interval", 4f, 1f, 50f, 1f);
    public final NumberSetting upInterval = new NumberSetting("UpInterval", 20f, 1f, 50f, 1f);
    public final NumberSetting anticKickOffset = new NumberSetting("AnticKickOffset", 0.04f, 0.008f, 1f, 0.001f);
    
    public final NumberSetting speed = new NumberSetting("Speed", 1.0f, 0.0f, 10.0f, 0.1f);
    public final NumberSetting upSpeed = new NumberSetting("UpSpeed", 0.062f, 0.001f, 0.1f, 0.001f);
    public final NumberSetting timer = new NumberSetting("Timer", 1f, 0.1f, 5f, 0.1f);
    public final NumberSetting increaseTicks = new NumberSetting("IncreaseTicks", 1f, 1f, 20f, 1f);
    public final NumberSetting factor = new NumberSetting("Factor", 1f, 1f, 10f, 0.1f);
    public final NumberSetting offset = new NumberSetting("Offset", 1337f, 1f, 1337f, 1f);

    private final ConcurrentHashMap<Integer, Teleport> teleports = new ConcurrentHashMap<>();
    private final ArrayList<PlayerMoveC2SPacket> movePackets = new ArrayList<>();
    private int ticks, factorTicks, teleportId = -1;
    private boolean flip = false;

    @Override
    public void onEnable() {
        super.onEnable();
        teleportId = -1;
        if (!fullNullCheck() && mc.player != null) {
            ticks = 0;
            teleportId = 0;
            movePackets.clear();
            teleports.clear();
        }
        factorTicks = 0;
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            mc.player.noClip = false;
        }
        MotherHack.TICK_TIMER = 1.0f;
        super.onDisable();
    }

    public boolean getTickCounter(int n) {
        ++ticks;
        if (ticks >= n) {
            ticks = 0;
            return true;
        }
        return false;
    }

    private int getWorldBorder() {
        if (mc.isInSingleplayer()) {
            return 1;
        }
        int n = ThreadLocalRandom.current().nextInt(29000000);
        if (ThreadLocalRandom.current().nextBoolean()) {
            return n;
        }
        return -n;
    }

    public Vec3d getVectorByMode(@NotNull Vec3d vec3d, Vec3d vec3d2) {
        Vec3d vec3d3 = vec3d.add(vec3d2);
        switch (type.getValue()) {
            case Preserve -> vec3d3 = vec3d3.add(getWorldBorder(), 0.0, getWorldBorder());
            case Up -> vec3d3 = vec3d3.add(0.0, offset.getValue(), 0.0);
            case Down -> vec3d3 = vec3d3.add(0.0, -offset.getValue(), 0.0);
            case Bounds -> vec3d3 = new Vec3d(vec3d3.x, mc.player.getY() <= 10.0 ? 255.0 : 1.0, vec3d3.z);
        }
        return vec3d3;
    }

    public void sendPackets(Vec3d vec3d, boolean confirm) {
        Vec3d motion = mc.player.getPos().add(vec3d);
        Vec3d rubberBand = getVectorByMode(vec3d, motion);
        
        PlayerMoveC2SPacket motionPacket = new PlayerMoveC2SPacket.PositionAndOnGround(
            motion.x, motion.y, motion.z, mc.player.isOnGround(), mc.player.horizontalCollision
        );
        movePackets.add(motionPacket);
        mc.player.networkHandler.sendPacket(motionPacket);
        
        PlayerMoveC2SPacket rubberBandPacket = new PlayerMoveC2SPacket.PositionAndOnGround(
            rubberBand.x, rubberBand.y, rubberBand.z, mc.player.isOnGround(), mc.player.horizontalCollision
        );
        movePackets.add(rubberBandPacket);
        mc.player.networkHandler.sendPacket(rubberBandPacket);
        
        if (confirm) {
            mc.player.networkHandler.sendPacket(new TeleportConfirmC2SPacket(++teleportId));
            teleports.put(teleportId, new Teleport(motion.x, motion.y, motion.z, System.currentTimeMillis()));
        }
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive event) {
        if (fullNullCheck()) return;
        
        if (mc.player != null && event.getPacket() instanceof PlayerPositionLookS2CPacket pac) {
            int packetTeleportId = pac.teleportId();
            Vec3d packetPos = pac.change().position();
            
            Teleport teleport = teleports.remove(packetTeleportId);
            
            if (mc.player.isAlive()
                    && mc.world.isChunkLoaded((int) mc.player.getX() >> 4, (int) mc.player.getZ() >> 4)
                    && !(mc.currentScreen instanceof DownloadingTerrainScreen)
                    && mode.getValue() != Mode.Rubber
                    && teleport != null
                    && teleport.x == packetPos.x
                    && teleport.y == packetPos.y
                    && teleport.z == packetPos.z) {
                event.cancel();
                return;
            }
            
            teleportId = packetTeleportId;
        }
    }

    @EventHandler
    public void onPacketSend(EventPacket.@NotNull Send event) {
        if (event.getPacket() instanceof PlayerMoveC2SPacket) {
            if (movePackets.contains((PlayerMoveC2SPacket) event.getPacket())) {
                movePackets.remove((PlayerMoveC2SPacket) event.getPacket());
                return;
            }
            event.cancel();
        }
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick event) {
        if (fullNullCheck()) return;
        
        // Clean old teleports
        teleports.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue().time > 30000);
        
        // Timer
        if (timer.getValue() != 1.0f)
            MotherHack.TICK_TIMER = timer.getValue();
        
        mc.player.setVelocity(0.0, 0.0, 0.0);
        
        if (mode.getValue() != Mode.Rubber && teleportId == 0) {
            if (getTickCounter(4))
                sendPackets(Vec3d.ZERO, false);
            return;
        }
        
        boolean insideBlock = mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-0.0625, -0.0625, -0.0625)).iterator().hasNext();
        
        double upMotion = 0;
        if (mc.options.jumpKey.isPressed() && (insideBlock || !MoveUtils.isMoving())) {
            if (antiKick.getValue() && !insideBlock)
                upMotion = getTickCounter(mode.getValue() == Mode.Rubber ? (int)(upInterval.getValue() / 2f) : upInterval.getValue().intValue()) ? -upSpeed.getValue() / 2f : upSpeed.getValue();
            else
                upMotion = upSpeed.getValue();
        } else if (mc.options.sneakKey.isPressed())
            upMotion = -upSpeed.getValue();
        else if (antiKick.getValue() && !insideBlock)
            upMotion = getTickCounter(interval.getValue().intValue()) ? -anticKickOffset.getValue() : 0.0;
        
        if (phase.getValue() == Phase.Full && insideBlock && MoveUtils.isMoving() && upMotion != 0.0)
            upMotion = mc.options.jumpKey.isPressed() ? upMotion / 2.5 : upMotion / 1.5;
        
        double[] motion = forward(phase.getValue() == Phase.Full && insideBlock ? 0.034444444444444444 : (double) (speed.getValue()) * 0.26);
        
        int factorInt = 1;
        if (mode.getValue() == Mode.Factor && mc.player.age % increaseTicks.getValue().intValue() == 0) {
            factorInt = (int) Math.floor(factor.getValue());
            factorTicks++;
            if (factorTicks > (int) (20D / ((factor.getValue() - factorInt) * 20D))) {
                factorInt += 1;
                factorTicks = 0;
            }
        }
        
        for (int i = 1; i <= factorInt; ++i) {
            if (mode.getValue() == Mode.Limit) {
                if (mc.player.age % 2 == 0) {
                    if (flip && upMotion >= 0.0) {
                        flip = false;
                        upMotion = -upSpeed.getValue() / 2f;
                    }
                    mc.player.setVelocity(motion[0] * i, upMotion * i, motion[1] * i);
                    sendPackets(mc.player.getVelocity(), !limit.getValue());
                    continue;
                }
                if (!(upMotion < 0.0)) continue;
                flip = true;
                continue;
            }
            mc.player.setVelocity(motion[0] * i, upMotion * i, motion[1] * i);
            sendPackets(mc.player.getVelocity(), !mode.getValue().equals(Mode.Rubber));
        }
        
        // Phase
        if (phase.getValue() != Phase.Off && (phase.getValue() == Phase.Semi || mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-0.0625, -0.0625, -0.0625)).iterator().hasNext())) {
            mc.player.noClip = true;
        }
    }

    private double[] forward(double speed) {
        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();
        
        if (forward == 0 && strafe == 0) {
            return new double[]{0, 0};
        }
        
        if (forward != 0) {
            if (strafe > 0) {
                yaw += (forward > 0 ? -45 : 45);
            } else if (strafe < 0) {
                yaw += (forward > 0 ? 45 : -45);
            }
            strafe = 0;
            if (forward > 0) {
                forward = 1;
            } else if (forward < 0) {
                forward = -1;
            }
        }
        
        double sin = Math.sin(Math.toRadians(yaw + 90));
        double cos = Math.cos(Math.toRadians(yaw + 90));
        double x = forward * speed * cos + strafe * speed * sin;
        double z = forward * speed * sin - strafe * speed * cos;
        
        return new double[]{x, z};
    }

    public enum Mode implements Nameable {
        Fast, Factor, Rubber, Limit;
        
        @Override
        public String getName() {
            return name();
        }
    }

    public enum Phase implements Nameable {
        Full, Off, Semi;
        
        @Override
        public String getName() {
            return name();
        }
    }

    public enum Type implements Nameable {
        Preserve, Up, Down, Bounds;
        
        @Override
        public String getName() {
            return name();
        }
    }

    public record Teleport(double x, double y, double z, long time) {
    }
}
