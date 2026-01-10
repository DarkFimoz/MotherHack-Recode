package fun.motherhack.modules.impl.movement;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.api.events.impl.rotations.EventMotion;
import fun.motherhack.api.events.impl.rotations.EventTravel;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Bind;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BindSetting;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.math.MathUtils;
import fun.motherhack.utils.math.TimerUtils;
import fun.motherhack.utils.movement.MoveUtils;
import fun.motherhack.utils.network.NetworkUtils;
import fun.motherhack.utils.notify.Notify;
import fun.motherhack.utils.notify.NotifyIcons;
import fun.motherhack.utils.world.InventoryUtils;
import lombok.AllArgsConstructor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class ElytraPlus extends Module {
    public ElytraPlus() {
        super("Elytra+", Category.Movement);
    }

    // Settings
    public final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.FireWork);
    private final NumberSetting disablerDelay = new NumberSetting("DisablerDelay", 1f, 0f, 10f, 1f, () -> mode.getValue() == Mode.SunriseOld);
    private final BooleanSetting twoBee = new BooleanSetting("2b2t", false, () -> mode.getValue() == Mode.Boost);
    private final BooleanSetting onlySpace = new BooleanSetting("OnlySpace", true, () -> mode.getValue() == Mode.Boost && twoBee.getValue());
    private final BooleanSetting stopOnGround = new BooleanSetting("StopOnGround", false, () -> mode.getValue() == Mode.Packet);
    private final BooleanSetting infDurability = new BooleanSetting("InfDurability", true, () -> mode.getValue() == Mode.Packet);
    private final BooleanSetting vertical = new BooleanSetting("Vertical", false, () -> mode.getValue() == Mode.Packet);
    private final EnumSetting<NCPStrict> ncpStrict = new EnumSetting<>("NCPStrict", NCPStrict.Off, () -> mode.getValue() == Mode.Packet);
    private final EnumSetting<AntiKick> antiKick = new EnumSetting<>("AntiKick", AntiKick.Jitter, () -> mode.getValue() == Mode.FireWork || mode.getValue() == Mode.SunriseOld);
    private final NumberSetting xzSpeed = new NumberSetting("XZSpeed", 1.55f, 0.1f, 10f, 0.05f, () -> mode.getValue() != Mode.Boost && mode.getValue() != Mode.Pitch40Infinite);
    private final NumberSetting ySpeed = new NumberSetting("YSpeed", 0.47f, 0f, 2f, 0.01f, () -> mode.getValue() == Mode.FireWork || mode.getValue() == Mode.SunriseOld || (mode.getValue() == Mode.Packet && vertical.getValue()));
    private final NumberSetting fireSlot = new NumberSetting("FireSlot", 1f, 1f, 9f, 1f, () -> mode.getValue() == Mode.FireWork);
    private final BooleanSetting accelerate = new BooleanSetting("Acceleration", false, () -> mode.getValue() == Mode.Control || mode.getValue() == Mode.Packet);
    private final NumberSetting accelerateFactor = new NumberSetting("AccelerateFactor", 9f, 0f, 100f, 1f, () -> (mode.getValue() == Mode.Control || mode.getValue() == Mode.Packet) && accelerate.getValue());
    private final NumberSetting fireDelay = new NumberSetting("FireDelay", 1.5f, 0f, 1.5f, 0.1f, () -> mode.getValue() == Mode.FireWork);
    private final BooleanSetting grim = new BooleanSetting("Grim", false, () -> mode.getValue() == Mode.FireWork);
    private final BooleanSetting rotate = new BooleanSetting("Rotate", true, () -> mode.getValue() == Mode.FireWork && grim.getValue());
    private final BooleanSetting fireWorkExtender = new BooleanSetting("FireWorkExtender", true, () -> mode.getValue() == Mode.FireWork && grim.getValue());
    private final BooleanSetting stayMad = new BooleanSetting("GroundSafe", false, () -> mode.getValue() == Mode.FireWork);
    private final BooleanSetting keepFlying = new BooleanSetting("KeepFlying", false, () -> mode.getValue() == Mode.FireWork);
    private final BooleanSetting disableOnFlag = new BooleanSetting("DisableOnFlag", false, () -> mode.getValue() == Mode.FireWork);
    private final BooleanSetting allowFireSwap = new BooleanSetting("AllowFireSwap", false, () -> mode.getValue() == Mode.FireWork);
    private final BooleanSetting bowBomb = new BooleanSetting("BowBomb", false, () -> mode.getValue() == Mode.FireWork || mode.getValue() == Mode.SunriseOld);
    private final BindSetting bombKey = new BindSetting("BombKey", new Bind(-1, false), () -> mode.getValue() == Mode.SunriseOld);
    private final BooleanSetting instantFly = new BooleanSetting("InstantFly", true, () -> (mode.getValue() == Mode.Boost && !twoBee.getValue()) || mode.getValue() == Mode.Control);
    private final BooleanSetting cruiseControl = new BooleanSetting("CruiseControl", false, () -> mode.getValue() == Mode.Boost);
    private final NumberSetting factor = new NumberSetting("Factor", 5.86f, 0.1f, 50.0f, 0.01f, () -> mode.getValue() == Mode.Boost);
    private final NumberSetting upSpeed = new NumberSetting("UpSpeed", 1.0f, 0.01f, 5.0f, 0.01f, () -> (mode.getValue() == Mode.Boost && !twoBee.getValue()) || mode.getValue() == Mode.Control);
    private final NumberSetting downFactor = new NumberSetting("Glide", 1.0f, 0.0f, 2.0f, 0.01f, () -> (mode.getValue() == Mode.Boost && !twoBee.getValue()) || mode.getValue() == Mode.Control);
    private final BooleanSetting stopMotion = new BooleanSetting("StopMotion", true, () -> mode.getValue() == Mode.Boost && !twoBee.getValue());
    private final NumberSetting minUpSpeed = new NumberSetting("MinUpSpeed", 0.5f, 0.1f, 5.0f, 0.1f, () -> mode.getValue() == Mode.Boost && cruiseControl.getValue());
    private final BooleanSetting forceHeight = new BooleanSetting("ForceHeight", false, () -> mode.getValue() == Mode.Boost && cruiseControl.getValue());
    private final NumberSetting manualHeight = new NumberSetting("Height", 121f, 1f, 256f, 1f, () -> mode.getValue() == Mode.Boost && forceHeight.getValue());
    private final NumberSetting sneakDownSpeed = new NumberSetting("DownSpeed", 1.0f, 0.01f, 5.0f, 0.01f, () -> mode.getValue() == Mode.Control);
    private final BooleanSetting speedLimit = new BooleanSetting("SpeedLimit", true, () -> mode.getValue() == Mode.Boost);
    private final NumberSetting maxSpeed = new NumberSetting("MaxSpeed", 2.5f, 0.1f, 10.0f, 0.1f, () -> mode.getValue() == Mode.Boost);
    private final NumberSetting redeployInterval = new NumberSetting("RedeployInterval", 1f, 0.1f, 5f, 0.1f, () -> mode.getValue() == Mode.Boost && !twoBee.getValue());
    private final NumberSetting redeployTimeOut = new NumberSetting("RedeployTimeout", 5f, 0.1f, 20f, 0.1f, () -> mode.getValue() == Mode.Boost && !twoBee.getValue());
    private final NumberSetting redeployDelay = new NumberSetting("RedeployDelay", 0.5f, 0.1f, 1f, 0.1f, () -> mode.getValue() == Mode.Boost && !twoBee.getValue());
    private final NumberSetting infiniteMaxSpeed = new NumberSetting("InfiniteMaxSpeed", 150f, 50f, 170f, 1f, () -> mode.getValue() == Mode.Pitch40Infinite);
    private final NumberSetting infiniteMinSpeed = new NumberSetting("InfiniteMinSpeed", 25f, 10f, 70f, 1f, () -> mode.getValue() == Mode.Pitch40Infinite);
    private final NumberSetting infiniteMaxHeight = new NumberSetting("InfiniteMaxHeight", 200f, 50f, 360f, 1f, () -> mode.getValue() == Mode.Pitch40Infinite);

    // Enums
    @AllArgsConstructor
    public enum Mode implements Nameable {
        FireWork("FireWork"), SunriseOld("SunriseOld"), Boost("Boost"), Control("Control"), 
        Pitch40Infinite("Pitch40Infinite"), SunriseNew("SunriseNew"), Packet("Packet"), BoostPlus("BoostPlus");
        private final String name;
        @Override public String getName() { return name; }
    }

    @AllArgsConstructor
    public enum AntiKick implements Nameable {
        Off("Off"), Jitter("Jitter"), Glide("Glide");
        private final String name;
        @Override public String getName() { return name; }
    }

    @AllArgsConstructor
    public enum NCPStrict implements Nameable {
        Off("Off"), Old("Old"), New("New"), Motion("Motion");
        private final String name;
        @Override public String getName() { return name; }
    }

    // Timers
    private final TimerUtils startTimer = new TimerUtils();
    private final TimerUtils redeployTimer = new TimerUtils();
    private final TimerUtils strictTimer = new TimerUtils();
    private final TimerUtils pingTimer = new TimerUtils();

    // State variables
    private boolean infiniteFlag, hasTouchedGround, elytraEquiped, flying, started;
    private float acceleration, accelerationY, height, prevClientPitch, infinitePitch, lastInfinitePitch;
    private ItemStack prevArmorItemCopy, getStackInSlotCopy;
    private Item prevArmorItem = Items.AIR;
    private Item prevItemInHand = Items.AIR;
    private Vec3d flightZonePos;
    private int prevElytraSlot = -1, disablerTicks;
    private int slotWithFireWorks = -1;
    private long lastFireworkTime;

    @Override
    public void onEnable() {
        super.onEnable();
        if (fullNullCheck()) return;

        if (mc.player.getY() < infiniteMaxHeight.getValue() && mode.getValue() == Mode.Pitch40Infinite) {
            disableWithMessage("Go above " + infiniteMaxHeight.getValue().intValue() + " height!");
            return;
        }

        flying = false;
        reset();
        infiniteFlag = false;
        acceleration = 0;
        accelerationY = 0;

        if (mc.player != null)
            height = (float) mc.player.getY();

        pingTimer.reset();

        if (mode.getValue() == Mode.FireWork) fireworkOnEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (fullNullCheck()) return;
        
        MotherHack.TICK_TIMER = 1.0f;
        mc.player.getAbilities().flying = false;
        mc.player.getAbilities().setFlySpeed(0.05F);
        if (mode.getValue() == Mode.FireWork)
            fireworkOnDisable();
    }

    private void disableWithMessage(String message) {
        MotherHack.getInstance().getNotifyManager().add(new Notify(NotifyIcons.failIcon, "Elytra+: " + message, 2000));
        setToggled(false);
    }

    // ==================== EVENT HANDLERS ====================

    @EventHandler
    public void modifyVelocity(EventTravel e) {
        if (fullNullCheck()) return;
        
        if (mode.getValue() == Mode.Pitch40Infinite) {
            prevClientPitch = mc.player.getPitch();
            e.setPitch(lastInfinitePitch);
        }
        if (mode.getValue() == Mode.FireWork) {
            if (getTicksElytraFlying() < 4) {
                prevClientPitch = mc.player.getPitch();
                e.setPitch(-45f);
            }
        }
        if (mode.getValue() == Mode.SunriseNew) {
            if (mc.options.jumpKey.isPressed()) {
                prevClientPitch = mc.player.getPitch();
                e.setPitch(-45f);
            } else if (mc.options.sneakKey.isPressed()) {
                prevClientPitch = mc.player.getPitch();
                e.setPitch(45f);
            }
        }
    }

    @EventHandler
    public void onSync(EventMotion e) {
        if (fullNullCheck()) return;
        
        switch (mode.getValue()) {
            case SunriseOld -> doSunrise(e);
            case SunriseNew -> doSunriseNew(e);
            case Boost, Control -> doPreLegacy();
            case FireWork -> fireworkOnSync(e);
            case Pitch40Infinite -> doPitch40Infinite();
            case Packet -> doPacket(e);
        }
    }

    @EventHandler
    public void onTick(EventPlayerTick e) {
        if (fullNullCheck()) return;
        
        switch (mode.getValue()) {
            case FireWork -> fireWorkOnPlayerUpdate();
            case Pitch40Infinite -> lastInfinitePitch = fixAngle(getInfinitePitch());
            case Boost -> handleBoostMovement();
            case BoostPlus -> handleBoostPlusMovement();
            case Control -> handleControlMovement();
            case Packet -> handlePacketMovement();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPacketSend(EventPacket.Send event) {
        if (fullNullCheck()) return;

        if (event.getPacket() instanceof ClientCommandC2SPacket command && mode.getValue() == Mode.FireWork)
            if (command.getMode() == ClientCommandC2SPacket.Mode.START_FALL_FLYING)
                doFireWork(false);

        if (event.getPacket() instanceof PlayerInteractEntityC2SPacket && mode.getValue() == Mode.FireWork && grim.getValue() && fireWorkExtender.getValue())
            if (flying && flightZonePos != null)
                sendChatMessage("In this mode, you cannot hit entities that spawned after the module was turned on!");
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive e) {
        if (fullNullCheck()) return;
        
        if (e.getPacket() instanceof EntityTrackerUpdateS2CPacket pac && pac.id() == mc.player.getId() && (mode.getValue() == Mode.Packet || mode.getValue() == Mode.SunriseOld)) {
            List<DataTracker.SerializedEntry<?>> values = pac.trackedValues();
            if (values.isEmpty()) return;

            for (DataTracker.SerializedEntry<?> value : values)
                if (value.value().toString().equals("FALL_FLYING") || (value.id() == 0 && (value.value().toString().equals("-120") || value.value().toString().equals("-128") || value.value().toString().equals("-126"))))
                    e.cancel();
        }

        if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
            acceleration = 0;
            accelerationY = 0;
            pingTimer.reset();

            if (disableOnFlag.getValue() && mode.getValue() == Mode.FireWork)
                disableWithMessage("Disabled due to flag!");
        }

        if (e.getPacket() instanceof CommonPingS2CPacket && mode.getValue() == Mode.FireWork && grim.getValue() && fireWorkExtender.getValue() && flying)
            if (pingTimer.getElapsed() < 50000) {
                if (pingTimer.passed(1000) && getSquaredDistance2D(flightZonePos) < 7000)
                    e.cancel();
            } else pingTimer.reset();
    }

    // ==================== MODE IMPLEMENTATIONS ====================

    private void handleBoostMovement() {
        double[] motion = {mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z};
        doBoost(motion);
    }

    private void handleBoostPlusMovement() {
        double[] motion = {mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z};
        doBoostPlus(motion);
        mc.player.setVelocity(motion[0], motion[1], motion[2]);
    }

    private void handleControlMovement() {
        double[] motion = {mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z};
        doControl(motion);
    }

    private void handlePacketMovement() {
        double[] motion = {mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z};
        doMotionPacket(motion);
        mc.player.setVelocity(motion[0], motion[1], motion[2]);
    }

    private void handleFireworkMovement() {
        double[] motion = {mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z};
        fireworkOnMove(motion);
        mc.player.setVelocity(motion[0], motion[1], motion[2]);
    }

    private void doPacket(EventMotion e) {
        if ((!isBoxCollidingGround() || !stopOnGround.getValue()) && mc.player.getInventory().getStack(38).getItem() == Items.ELYTRA) {
            if (infDurability.getValue() || !mc.player.isGliding())
                NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));

            if (mc.player.age % 3 != 0 && ncpStrict.getValue() == NCPStrict.Motion)
                e.cancel();
        }
    }

    private void doPitch40Infinite() {
        ItemStack is = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (is.isOf(Items.ELYTRA)) {
            mc.player.setPitch(lastInfinitePitch);
            if (is.getDamage() > 380 && mc.player.age % 100 == 0) {
                MotherHack.getInstance().getNotifyManager().add(new Notify(NotifyIcons.failIcon, "Elytra's about to break!", 2000));
                mc.world.playSound(mc.player, mc.player.getX(), mc.player.getY(), mc.player.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.AMBIENT, 10.0f, 1.0F, 0);
            }
        }
    }

    private void doSunriseNew(EventMotion e) {
        if (mc.player.horizontalCollision)
            acceleration = 0;

        int elytra = getElytraSlot();
        if (elytra == -1) return;

        if (mc.player.isOnGround()) {
            mc.player.jump();
            acceleration = 0;
            return;
        }

        if (mc.player.fallDistance <= 0) return;

        if (mc.options.jumpKey.isPressed() || mc.options.sneakKey.isPressed()) {
            acceleration = 0;
            takeOnElytra();
        } else {
            takeOnChestPlate();
            if (mc.player.age % 8 == 0)
                matrixDisabler(elytra);

            setMotion(Math.min((acceleration = (acceleration + 8.0F / xzSpeed.getValue())) / 100.0F, xzSpeed.getValue()));
            if (!MoveUtils.isMoving()) acceleration = 0;
            mc.player.setVelocity(mc.player.getVelocity().getX(), -0.005F, mc.player.getVelocity().getZ());
        }
    }

    private void takeOnElytra() {
        int elytra = getElytraSlot();
        if (elytra == -1) return;
        elytra = elytra >= 0 && elytra < 9 ? elytra + 36 : elytra;
        if (elytra != -2) {
            clickSlot(elytra);
            clickSlot(6);
            clickSlot(elytra);
            NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        }
    }

    private void takeOnChestPlate() {
        int slot = getChestPlateSlot();
        if (slot == -1) return;
        if (slot != -2) {
            clickSlot(slot);
            clickSlot(6);
            clickSlot(slot);
        }
    }

    private float getInfinitePitch() {
        if (mc.player.getY() < infiniteMaxHeight.getValue()) {
            if (getCurrentPlayerSpeed() * 72f < infiniteMinSpeed.getValue() && !infiniteFlag)
                infiniteFlag = true;
            if (getCurrentPlayerSpeed() * 72f > infiniteMaxSpeed.getValue() && infiniteFlag)
                infiniteFlag = false;
        } else infiniteFlag = true;

        if (infiniteFlag) infinitePitch += 3;
        else infinitePitch -= 3;

        infinitePitch = MathHelper.clamp(infinitePitch, -40, 40);
        return infinitePitch;
    }

    private void doSunrise(EventMotion e) {
        if (mc.player.horizontalCollision)
            acceleration = 0;
        if (mc.player.verticalCollision) {
            acceleration = 0;
            mc.player.setVelocity(mc.player.getVelocity().getX(), 0.41999998688697815, mc.player.getVelocity().getZ());
        }

        int elytra = getElytraSlot();
        if (elytra == -1) return;
        if (mc.player.isOnGround()) mc.player.jump();

        if (disablerTicks-- <= 0)
            matrixDisabler(elytra);

        if (mc.player.fallDistance > 0.25f) {
            setMotion(Math.min((acceleration = (acceleration + 11.0F / xzSpeed.getValue())) / 100.0F, xzSpeed.getValue()));
            if (!MoveUtils.isMoving()) acceleration = 0;

            if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), bombKey.getValue().getKey())) {
                setMotion(0.8f);
                mc.player.setVelocity(mc.player.getVelocity().getX(), mc.player.age % 2 == 0 ? 0.41999998688697815 : -0.41999998688697815, mc.player.getVelocity().getZ());
                acceleration = 70;
            } else {
                switch (antiKick.getValue()) {
                    case Jitter -> mc.player.setVelocity(mc.player.getVelocity().getX(), mc.player.age % 2 == 0 ? 0.08 : -0.08, mc.player.getVelocity().getZ());
                    case Glide -> mc.player.setVelocity(mc.player.getVelocity().getX(), -0.01F - (mc.player.age % 2 == 0 ? 1.0E-4F : 0.006F), mc.player.getVelocity().getZ());
                    case Off -> mc.player.setVelocity(mc.player.getVelocity().getX(), 0, mc.player.getVelocity().getZ());
                }
            }

            if (!mc.player.isSneaking() && mc.options.jumpKey.isPressed())
                mc.player.setVelocity(mc.player.getVelocity().getX(), ySpeed.getValue(), mc.player.getVelocity().getZ());

            if (mc.options.sneakKey.isPressed())
                mc.player.setVelocity(mc.player.getVelocity().getX(), -ySpeed.getValue(), mc.player.getVelocity().getZ());
        }
    }

    private void doPreLegacy() {
        if (twoBee.getValue() && mode.getValue() == Mode.Boost) return;
        if (mc.player.isOnGround()) hasTouchedGround = true;
        if (!cruiseControl.getValue()) height = (float) mc.player.getY();

        if (strictTimer.passed(1500) && !strictTimer.passed(2000))
            MotherHack.TICK_TIMER = 1.0f;

        if (!mc.player.isGliding()) {
            if (hasTouchedGround && !mc.player.isOnGround() && mc.player.fallDistance > 0 && instantFly.getValue())
                MotherHack.TICK_TIMER = 0.3f;

            if (!mc.player.isOnGround() && instantFly.getValue() && mc.player.getVelocity().getY() < 0D) {
                if (!startTimer.passed((long) (1000 * redeployDelay.getValue()))) return;
                startTimer.reset();
                NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                hasTouchedGround = false;
                strictTimer.reset();
            }
        }
    }

    private void doBoost(double[] motion) {
        if (mc.player.getInventory().getStack(38).getItem() != Items.ELYTRA || !mc.player.isGliding() || mc.player.isTouchingWater() || mc.player.isInLava())
            return;

        float moveForward = mc.player.input.movementForward;

        if (cruiseControl.getValue()) {
            if (mc.options.jumpKey.isPressed()) height++;
            else if (mc.options.sneakKey.isPressed()) height--;
            if (forceHeight.getValue()) height = manualHeight.getValue();

            if (twoBee.getValue()) {
                if (getCurrentPlayerSpeed() >= minUpSpeed.getValue())
                    mc.player.setPitch((float) MathHelper.clamp(MathHelper.wrapDegrees(Math.toDegrees(Math.atan2((height - mc.player.getY()) * -1.0, 10))), -50, 50));
                else
                    mc.player.setPitch(0.25F);
            } else {
                double heightPct = 1 - Math.sqrt(MathHelper.clamp(getCurrentPlayerSpeed() / 1.7, 0.0, 1.0));
                if (getCurrentPlayerSpeed() >= minUpSpeed.getValue() && startTimer.passed((long) (2000 * redeployInterval.getValue()))) {
                    double pitch = -(44.4 * heightPct + 0.6);
                    double diff = (height + 1 - mc.player.getY()) * 2;
                    double pDist = -Math.toDegrees(Math.atan2(Math.abs(diff), getCurrentPlayerSpeed() * 30.0)) * Math.signum(diff);
                    mc.player.setPitch((float) (pitch + (pDist - pitch) * MathHelper.clamp(Math.abs(diff), 0.0, 1.0)));
                } else {
                    mc.player.setPitch(0.25F);
                    moveForward = 1;
                }
            }
        }

        if (twoBee.getValue()) {
            if ((mc.options.jumpKey.isPressed() || !onlySpace.getValue() || cruiseControl.getValue())) {
                double[] m = forwardWithoutStrafe((factor.getValue() / 10f));
                motion[0] = motion[0] + m[0];
                motion[2] = motion[2] + m[1];
            }
        } else {
            Vec3d rotationVec = mc.player.getRotationVec(mc.getRenderTickCounter().getTickDelta(true));
            double d6 = Math.hypot(rotationVec.x, rotationVec.z);
            double currentSpeed = Math.hypot(motion[0], motion[2]);

            float f4 = (float) (Math.pow(Math.cos(Math.toRadians(mc.player.getPitch())), 2) * Math.min(1, rotationVec.length() / 0.4));

            motion[1] = motion[1] + (-0.08D + (double) f4 * 0.06);

            if (motion[1] < 0 && d6 > 0) {
                double ySpd = motion[1] * -0.1 * (double) f4;
                motion[1] = motion[1] + ySpd;
                motion[0] = motion[0] + rotationVec.x * ySpd / d6;
                motion[2] = motion[2] + rotationVec.z * ySpd / d6;
            }

            if (mc.player.getPitch() < 0) {
                double ySpd = currentSpeed * -Math.sin(Math.toRadians(mc.player.getPitch())) * 0.04;
                motion[1] = motion[1] + ySpd * 3.2;
                motion[0] = motion[0] - rotationVec.x * ySpd / d6;
                motion[2] = motion[2] - rotationVec.z * ySpd / d6;
            }

            if (d6 > 0) {
                motion[0] = motion[0] + (rotationVec.x / d6 * currentSpeed - motion[0]) * 0.1D;
                motion[2] = motion[2] + (rotationVec.z / d6 * currentSpeed - motion[2]) * 0.1D;
            }

            if (mc.player.getPitch() > 0 && motion[1] < 0) {
                if (moveForward != 0 && startTimer.passed((long) (2000 * redeployInterval.getValue())) && redeployTimer.passed((long) (1000 * redeployTimeOut.getValue()))) {
                    if (stopMotion.getValue()) {
                        motion[0] = 0;
                        motion[2] = 0;
                    }
                    startTimer.reset();
                    NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                } else if (!startTimer.passed((long) (2000 * redeployInterval.getValue()))) {
                    motion[0] = motion[0] - moveForward * Math.sin(Math.toRadians(mc.player.getYaw())) * factor.getValue() / 20F;
                    motion[2] = motion[2] + moveForward * Math.cos(Math.toRadians(mc.player.getYaw())) * factor.getValue() / 20F;
                    redeployTimer.reset();
                }
            }
        }

        double speed = Math.hypot(motion[0], motion[2]);
        if (speedLimit.getValue() && speed > maxSpeed.getValue()) {
            motion[0] = motion[0] * maxSpeed.getValue() / speed;
            motion[2] = motion[2] * maxSpeed.getValue() / speed;
        }

        mc.player.setVelocity(motion[0], motion[1], motion[2]);
    }

    private void doControl(double[] motion) {
        if (mc.player.getInventory().getStack(38).getItem() != Items.ELYTRA || !mc.player.isGliding())
            return;

        double[] dir = forward(xzSpeed.getValue() * (accelerate.getValue() ? Math.min((acceleration += accelerateFactor.getValue()) / 100.0f, 1.0f) : 1f));
        motion[0] = dir[0];
        motion[1] = mc.options.jumpKey.isPressed() ? upSpeed.getValue() : mc.options.sneakKey.isPressed() ? -sneakDownSpeed.getValue() : -0.08 * downFactor.getValue();
        motion[2] = dir[1];

        if (!MoveUtils.isMoving())
            acceleration = 0;

        mc.player.setVelocity(motion[0], motion[1], motion[2]);
    }

    private void doBoostPlus(double[] motion) {
        if (!mc.player.isGliding()) {
            if (mc.player.isOnGround() || mc.player.getVelocity().getY() < 0D) {
                NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
            return;
        }

        double boostFactor = factor.getValue() * 1.7;
        double forward = mc.player.input.movementForward;
        double strafe = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();

        if (forward == 0.0 && strafe == 0.0) {
            motion[0] = 0;
            motion[2] = 0;
        } else {
            if (forward != 0.0) {
                if (strafe > 0.0) {
                    yaw += (forward > 0.0 ? -45 : 45);
                } else if (strafe < 0.0) {
                    yaw += (forward > 0.0 ? 45 : -45);
                }
                strafe = 0.0;
                if (forward > 0.0) forward = 1.0;
                else if (forward < 0.0) forward = -1.0;
            }

            double cos = Math.cos(Math.toRadians(yaw + 90.0f));
            double sin = Math.sin(Math.toRadians(yaw + 90.0f));
            double randomFactor = 1.0 + MathUtils.randomFloat(-0.03f, 0.03f);
            motion[0] = ((forward * boostFactor * cos) + (strafe * boostFactor * sin)) * randomFactor;
            motion[2] = ((forward * boostFactor * sin) - (strafe * boostFactor * cos)) * randomFactor;
        }

        if (mc.options.jumpKey.isPressed()) {
            motion[1] = upSpeed.getValue() * 0.85;
        } else if (mc.options.sneakKey.isPressed()) {
            motion[1] = -downFactor.getValue() * 0.85;
        } else {
            motion[1] = -0.01;
        }

        if (grim.getValue()) {
            if (mc.player.age % 3 == 0) {
                motion[0] = motion[0] * 0.97;
                motion[2] = motion[2] * 0.97;
            }
            if (mc.player.horizontalCollision) {
                motion[1] = -0.07840000152587923;
            }
        }

        if (cruiseControl.getValue()) {
            if (mc.player.getVelocity().getY() < minUpSpeed.getValue() && !mc.options.sneakKey.isPressed()) {
                motion[1] = upSpeed.getValue() * 0.9;
            }
            if (forceHeight.getValue() && mc.player.getY() < manualHeight.getValue()) {
                motion[1] = upSpeed.getValue() * 0.9;
            }
        }

        if (speedLimit.getValue()) {
            double currentSpeed = Math.sqrt(motion[0] * motion[0] + motion[2] * motion[2]);
            if (currentSpeed > maxSpeed.getValue()) {
                double scale = maxSpeed.getValue() / currentSpeed;
                motion[0] = motion[0] * scale;
                motion[2] = motion[2] * scale;
            }
        }
    }

    private void doMotionPacket(double[] motion) {
        mc.player.getAbilities().flying = false;
        mc.player.getAbilities().setFlySpeed(0.05F);

        if ((isBoxCollidingGround() && stopOnGround.getValue()) || mc.player.getInventory().getStack(38).getItem() != Items.ELYTRA)
            return;

        mc.player.getAbilities().flying = true;
        mc.player.getAbilities().setFlySpeed((xzSpeed.getValue() / 15f) * (accelerate.getValue() ? Math.min((acceleration += accelerateFactor.getValue()) / 100.0f, 1.0f) : 1f));

        if (mc.player.age % 3 == 0 && ncpStrict.getValue() == NCPStrict.Motion) {
            motion[0] = 0;
            motion[1] = 0;
            motion[2] = 0;
            return;
        }

        if (Math.abs(motion[0]) < 0.05) motion[0] = 0;
        if (Math.abs(motion[2]) < 0.05) motion[2] = 0;

        motion[1] = vertical.getValue() ? mc.options.jumpKey.isPressed() ? ySpeed.getValue() : mc.options.sneakKey.isPressed() ? -ySpeed.getValue() : 0 : 0;

        switch (ncpStrict.getValue()) {
            case New -> motion[1] = -1.000088900582341E-12;
            case Motion -> motion[1] = -4.000355602329364E-12;
            case Old -> motion[1] = 0.0002 - (mc.player.age % 2 == 0 ? 0 : 0.000001) + MathUtils.randomFloat(0, 0.0000009f);
        }

        if (mc.player.horizontalCollision && (ncpStrict.getValue() == NCPStrict.New || ncpStrict.getValue() == NCPStrict.Motion) && mc.player.age % 2 == 0)
            motion[1] = -0.07840000152587923;

        if (infDurability.getValue() || ncpStrict.getValue() == NCPStrict.Motion) {
            if (!MoveUtils.isMoving() && Math.abs(motion[0]) < 0.121) {
                float angleToRad = (float) Math.toRadians(4.5 * (mc.player.age % 80));
                motion[0] = Math.sin(angleToRad) * 0.12;
                motion[2] = Math.cos(angleToRad) * 0.12;
            }
        }
    }

    // ==================== FIREWORK MODE ====================

    public void fireWorkOnPlayerUpdate() {
        boolean inAir = mc.world.isAir(BlockPos.ofFloored(mc.player.getPos()));
        boolean aboveLiquid = isAboveLiquid(0.1f) && inAir && mc.player.getVelocity().getY() < 0.0;
        if (mc.player.fallDistance > 0.0f && inAir || aboveLiquid) {
            equipElytra();
        } else if (mc.player.isOnGround()) {
            started = false;
            return;
        }

        if (!MoveUtils.isMoving())
            acceleration = 0;
        if (!canFly()) return;

        if (!mc.player.isGliding() && !started && mc.player.getVelocity().getY() < 0.0) {
            NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            started = true;
        }
        if (getTicksElytraFlying() < 4) {
            mc.options.jumpKey.setPressed(false);
        }
        doFireWork(true);
        handleFireworkMovement();
    }

    public void fireworkOnSync(EventMotion e) {
        if (grim.getValue() && rotate.getValue()) {
            if (mc.options.jumpKey.isPressed() && mc.player.isGliding() && flying)
                e.setPitch(-45f);

            if (mc.options.sneakKey.isPressed() && mc.player.isGliding() && flying)
                e.setPitch(45f);

            e.setYaw(getMoveDirection());
        }

        if (!MoveUtils.isMoving() && mc.options.jumpKey.isPressed() && mc.player.isGliding() && flying)
            e.setPitch(-90f);

        if (getTicksElytraFlying() < 5 && !mc.player.isOnGround())
            e.setPitch(-45f);
    }

    public void fireworkOnMove(double[] motion) {
        if (mc.player.isGliding() && flying) {
            if (mc.player.horizontalCollision || mc.player.verticalCollision) {
                acceleration = 0;
                accelerationY = 0;
            }

            if (getTicksElytraFlying() < 4) {
                motion[1] = 0.2f;
                return;
            }

            if (mc.options.jumpKey.isPressed()) {
                motion[1] = ySpeed.getValue() * Math.min((accelerationY += 9) / 100.0f, 1.0f);
            } else if (mc.options.sneakKey.isPressed()) {
                motion[1] = -ySpeed.getValue() * Math.min((accelerationY += 9) / 100.0f, 1.0f);
            } else if (bowBomb.getValue() && checkGround(2.0f)) {
                motion[1] = mc.player.age % 2 == 0 ? 0.42f : -0.42f;
            } else {
                switch (antiKick.getValue()) {
                    case Jitter -> motion[1] = mc.player.age % 2 == 0 ? 0.08f : -0.08f;
                    case Glide -> motion[1] = -0.08f;
                    case Off -> motion[1] = 0f;
                }
            }

            if (!MoveUtils.isMoving())
                acceleration = 0;

            if (mc.player.input.movementSideways > 0) {
                mc.player.input.movementSideways = 1;
            } else if (mc.player.input.movementSideways < 0) {
                mc.player.input.movementSideways = -1;
            }

            modifySpeed(motion, xzSpeed.getValue() * Math.min((acceleration += 9) / 100.0f, 1.0f));
            if (stayMad.getValue() && !checkGround(3.0f) && getTicksElytraFlying() > 10)
                motion[1] = 0.42f;
        }
    }

    private void doFireWork(boolean started) {
        if (started && (float) (System.currentTimeMillis() - lastFireworkTime) < fireDelay.getValue() * 1000.0f)
            return;

        if (grim.getValue() && fireWorkExtender.getValue() && started && pingTimer.passed(200) && flightZonePos != null && getSquaredDistance2D(flightZonePos) < 7000)
            return;

        if (started && !mc.player.isGliding()) return;
        if (!started && getTicksElytraFlying() > 1) return;

        int slot = getFireworks();
        if (slot == -1) {
            slotWithFireWorks = -1;
            return;
        }
        slotWithFireWorks = slot;

        boolean inOffhand = mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET;
        int prevSlot = mc.player.getInventory().selectedSlot;

        if (!inOffhand && prevSlot != slot)
            NetworkUtils.sendPacket(new UpdateSelectedSlotC2SPacket(slot));

        sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(inOffhand ? Hand.OFF_HAND : Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));

        if (!inOffhand && prevSlot != mc.player.getInventory().selectedSlot)
            NetworkUtils.sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));

        flying = true;
        lastFireworkTime = System.currentTimeMillis();
        pingTimer.reset();
        flightZonePos = mc.player.getPos();
    }

    private void equipElytra() {
        int elytraSlot = getElytraSlot();
        if (elytraSlot == -1 && mc.player.currentScreenHandler.getCursorStack().getItem() != Items.ELYTRA) {
            noElytra();
            return;
        }
        if (!shouldSwapToElytra()) return;
        if (prevElytraSlot == -1) {
            ItemStack is = mc.player.getEquippedStack(EquipmentSlot.CHEST);
            prevElytraSlot = elytraSlot;
            prevArmorItem = is.getItem();
            prevArmorItemCopy = is.copy();
        }

        clickSlot(elytraSlot);
        clickSlot(6);
        if (prevElytraSlot != -1)
            clickSlot(prevElytraSlot);

        elytraEquiped = true;
    }

    private void returnChestPlate() {
        if (prevElytraSlot != -1 && prevArmorItem != Items.AIR) {
            if (!elytraEquiped) return;

            ItemStack is = mc.player.getInventory().getStack(prevElytraSlot);
            boolean bl2 = is != ItemStack.EMPTY && !ItemStack.areItemsEqual(is, prevArmorItemCopy);
            int n2 = findInInventory(prevArmorItemCopy, prevArmorItem);
            n2 = n2 < 9 && n2 != -1 ? n2 + 36 : n2;
            if (mc.player.currentScreenHandler.getCursorStack().getItem() != Items.AIR) {
                clickSlot(6);
                if (prevElytraSlot != -1)
                    clickSlot(prevElytraSlot);
                return;
            }
            if (n2 == -1) return;

            clickSlot(n2);
            clickSlot(6);
            if (!bl2) {
                clickSlot(n2);
            } else {
                int n4 = findEmpty(false);
                if (n4 != -1) {
                    clickSlot(n4);
                }
            }
        }
        resetPrevItems();
    }

    public void fireworkOnEnable() {
        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA && mc.player.currentScreenHandler.getCursorStack().getItem() != Items.ELYTRA && getElytraSlot() == -1) {
            noElytra();
            return;
        }
        if (getFireWorks(false) == -1) {
            noFireworks();
            return;
        }
        if (getFireWorks(true) != -1) return;
        getStackInSlotCopy = mc.player.getInventory().getStack(fireSlot.getValue().intValue() - 1).copy();
        prevItemInHand = mc.player.getInventory().getStack(fireSlot.getValue().intValue() - 1).getItem();
    }

    public void fireworkOnDisable() {
        started = false;
        if (keepFlying.getValue()) return;
        mc.player.setVelocity(0, mc.player.getVelocity().getY(), 0);
        new Thread(() -> {
            NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            MotherHack.TICK_TIMER = 0.1f;
            returnItem();
            reset();
            try {
                Thread.sleep(200L);
            } catch (InterruptedException interruptedException) {
                MotherHack.TICK_TIMER = 1f;
                interruptedException.printStackTrace();
            }
            returnChestPlate();
            resetPrevItems();
            MotherHack.TICK_TIMER = 1f;
        }).start();
    }

    // ==================== UTILITY METHODS ====================

    public void matrixDisabler(int elytra) {
        elytra = elytra >= 0 && elytra < 9 ? elytra + 36 : elytra;
        if (elytra != -2) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytra, 1, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 1, SlotActionType.PICKUP, mc.player);
        }
        NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        if (elytra != -2) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 1, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytra, 1, SlotActionType.PICKUP, mc.player);
        }
        disablerTicks = (int) disablerDelay.getValue().floatValue();
    }

    private int getFireWorks(boolean hotbar) {
        if (hotbar) {
            return InventoryUtils.findHotbar(Items.FIREWORK_ROCKET);
        } else return InventoryUtils.find(Items.FIREWORK_ROCKET);
    }

    private void noFireworks() {
        disableWithMessage("No fireworks in the hotbar!");
        flying = false;
    }

    private void noElytra() {
        disableWithMessage("No elytras found in the inventory!");
        flying = false;
    }

    private void reset() {
        slotWithFireWorks = -1;
        prevItemInHand = Items.AIR;
        getStackInSlotCopy = null;
    }

    private void resetPrevItems() {
        prevElytraSlot = -1;
        prevArmorItem = Items.AIR;
        prevArmorItemCopy = null;
    }

    private void moveFireworksToHotbar(int n2) {
        clickSlot(n2);
        clickSlot(fireSlot.getValue().intValue() - 1 + 36);
        clickSlot(n2);
    }

    private void returnItem() {
        if (slotWithFireWorks == -1 || getStackInSlotCopy == null || prevItemInHand == Items.FIREWORK_ROCKET || prevItemInHand == Items.AIR) {
            return;
        }
        int n2 = findInInventory(getStackInSlotCopy, prevItemInHand);
        n2 = n2 < 9 && n2 != -1 ? n2 + 36 : n2;
        clickSlot(n2);
        clickSlot(fireSlot.getValue().intValue() - 1 + 36);
        clickSlot(n2);
    }

    public static int findInInventory(ItemStack stack, Item item) {
        if (stack == null) return -1;
        for (int i2 = 0; i2 < 45; ++i2) {
            ItemStack is = mc.player.getInventory().getStack(i2);
            if (!ItemStack.areItemsEqual(is, stack) || is.getItem() != item) continue;
            return i2;
        }
        return -1;
    }

    private int getFireworks() {
        if (mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET) {
            return -2;
        }
        int firesInHotbar = getFireWorks(true);
        int firesInInventory = getFireWorks(false);
        if (firesInInventory == -1) {
            noFireworks();
            return -1;
        }
        if (firesInHotbar == -1) {
            if (!allowFireSwap.getValue()) {
                disableWithMessage("No fireworks!");
                return fireSlot.getValue().intValue() - 1;
            }
            moveFireworksToHotbar(firesInInventory);
            return fireSlot.getValue().intValue() - 1;
        }
        return firesInHotbar;
    }

    private boolean canFly() {
        if (shouldSwapToElytra()) return false;
        return getFireworks() != -1;
    }

    private boolean shouldSwapToElytra() {
        ItemStack is = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        return is.getItem() != Items.ELYTRA || is.getDamage() >= is.getMaxDamage() - 1;
    }

    public static int findEmpty(boolean hotbar) {
        for (int i2 = hotbar ? 0 : 9; i2 < (hotbar ? 9 : 45); ++i2) {
            if (!mc.player.getInventory().getStack(i2).isEmpty()) continue;
            return i2;
        }
        return -1;
    }

    private int getElytraSlot() {
        for (int i = 0; i < 45; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.ELYTRA && stack.getDamage() < stack.getMaxDamage() - 1) {
                return i < 9 ? i + 36 : i;
            }
        }
        return -1;
    }

    private int getChestPlateSlot() {
        for (int i = 0; i < 45; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.DIAMOND_CHESTPLATE || stack.getItem() == Items.NETHERITE_CHESTPLATE ||
                stack.getItem() == Items.IRON_CHESTPLATE || stack.getItem() == Items.GOLDEN_CHESTPLATE ||
                stack.getItem() == Items.CHAINMAIL_CHESTPLATE || stack.getItem() == Items.LEATHER_CHESTPLATE) {
                return i < 9 ? i + 36 : i;
            }
        }
        return -1;
    }

    private void clickSlot(int slot) {
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
    }

    private void sendSequencedPacket(java.util.function.IntFunction<net.minecraft.network.packet.Packet<?>> packetCreator) {
        NetworkUtils.sendPacket(packetCreator.apply(0));
    }

    private boolean isBoxCollidingGround() {
        return mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-0.25, 0.0, -0.25).offset(0.0, -0.3, 0.0)).iterator().hasNext();
    }

    public static boolean checkGround(float f2) {
        if (mc.player.getY() < 0.0) return false;
        return !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0.0, -f2, 0.0)).iterator().hasNext();
    }

    public static boolean isAboveLiquid(float offset) {
        if (mc.player == null) return false;
        return mc.world.getBlockState(BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - (double) offset, mc.player.getZ())).getBlock() instanceof FluidBlock;
    }

    private int getTicksElytraFlying() {
        return mc.player.isGliding() ? mc.player.age : 0;
    }

    private double getCurrentPlayerSpeed() {
        return Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ);
    }

    private double getSquaredDistance2D(Vec3d pos) {
        double dx = mc.player.getX() - pos.x;
        double dz = mc.player.getZ() - pos.z;
        return dx * dx + dz * dz;
    }

    private float fixAngle(float angle) {
        return MathHelper.wrapDegrees(angle);
    }

    private void sendChatMessage(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(net.minecraft.text.Text.literal("§c[Elytra+] §7" + message), false);
        }
    }

    // Movement utilities
    private void setMotion(double speed) {
        double[] motion = forward(speed);
        mc.player.setVelocity(motion[0], mc.player.getVelocity().getY(), motion[1]);
    }

    private double[] forward(double speed) {
        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();

        if (forward == 0 && strafe == 0) {
            return new double[]{0, 0};
        }

        if (forward != 0) {
            if (strafe > 0) yaw += (forward > 0 ? -45 : 45);
            else if (strafe < 0) yaw += (forward > 0 ? 45 : -45);
            strafe = 0;
            if (forward > 0) forward = 1;
            else if (forward < 0) forward = -1;
        }

        double sin = Math.sin(Math.toRadians(yaw + 90));
        double cos = Math.cos(Math.toRadians(yaw + 90));
        return new double[]{forward * speed * cos + strafe * speed * sin, forward * speed * sin - strafe * speed * cos};
    }

    private double[] forwardWithoutStrafe(double speed) {
        float yaw = mc.player.getYaw();
        double sin = Math.sin(Math.toRadians(yaw + 90));
        double cos = Math.cos(Math.toRadians(yaw + 90));
        return new double[]{speed * cos, speed * sin};
    }

    private void modifySpeed(double[] motion, double speed) {
        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();

        if (forward == 0 && strafe == 0) {
            motion[0] = 0;
            motion[2] = 0;
            return;
        }

        if (forward != 0) {
            if (strafe > 0) yaw += (forward > 0 ? -45 : 45);
            else if (strafe < 0) yaw += (forward > 0 ? 45 : -45);
            strafe = 0;
            if (forward > 0) forward = 1;
            else if (forward < 0) forward = -1;
        }

        double sin = Math.sin(Math.toRadians(yaw + 90));
        double cos = Math.cos(Math.toRadians(yaw + 90));
        motion[0] = forward * speed * cos + strafe * speed * sin;
        motion[2] = forward * speed * sin - strafe * speed * cos;
    }

    private float getMoveDirection() {
        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();

        if (forward == 0 && strafe == 0) return yaw;

        if (forward != 0) {
            if (strafe > 0) yaw += (forward > 0 ? -45 : 45);
            else if (strafe < 0) yaw += (forward > 0 ? 45 : -45);
        } else {
            if (strafe > 0) yaw -= 90;
            else if (strafe < 0) yaw += 90;
        }

        return yaw;
    }
}
