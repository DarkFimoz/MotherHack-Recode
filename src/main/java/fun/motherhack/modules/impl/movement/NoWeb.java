package fun.motherhack.modules.impl.movement;

import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.utils.movement.MoveUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Iterator;

public class NoWeb extends Module {

    public NoWeb() {
        super("NoWeb", Category.Movement);
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick e) {
        if (fullNullCheck()) return;

        boolean cobweb = false;
        Box box = mc.player.getBoundingBox();
        Iterator<BlockPos> var4 = BlockPos.iterate(
                MathHelper.floor(box.minX),
                MathHelper.floor(box.minY),
                MathHelper.floor(box.minZ),
                MathHelper.floor(box.maxX),
                MathHelper.floor(box.maxY),
                MathHelper.floor(box.maxZ)
        ).iterator();

        while (var4.hasNext()) {
            BlockPos pos = var4.next();
            if (mc.world.getBlockState(pos).isOf(Blocks.COBWEB)) {
                cobweb = true;
                break;
            }
        }

        if (cobweb) {
            Vec3d velocity = mc.player.getVelocity();
            float yaw = mc.player.getYaw();
            double forward = 0.0D;
            double strafe = 0.0D;

            if (mc.player.input.playerInput.forward()) {
                ++forward;
            }
            if (mc.player.input.playerInput.backward()) {
                --forward;
            }
            if (mc.player.input.playerInput.left()) {
                ++strafe;
            }
            if (mc.player.input.playerInput.right()) {
                --strafe;
            }

            if (forward != 0.0D || strafe != 0.0D) {
                if (forward != 0.0D) {
                    if (strafe > 0.0D) {
                        yaw += (float) (forward > 0.0D ? -45 : 45);
                    } else if (strafe < 0.0D) {
                        yaw += (float) (forward > 0.0D ? 45 : -45);
                    }
                    strafe = 0.0D;
                    if (forward > 0.0D) {
                        forward = 1.0D;
                    } else {
                        forward = -1.0D;
                    }
                }

                double movementYaw = Math.toDegrees(Math.atan2(strafe, forward)) + (double) yaw;
                yaw = (float) ((movementYaw % 360.0D + 360.0D) % 360.0D);
            }

            float result = 0.63F;

            if ((!(yaw >= 313.0F) || !(yaw <= 317.0F)) && (!(yaw >= 223.0F) || !(yaw <= 227.0F)) && (!(yaw >= 133.0F) || !(yaw <= 137.0F)) && (!(yaw >= 43.0F) || !(yaw <= 47.0F))) {
                if ((!(yaw >= 311.0F) || !(yaw <= 319.0F)) && (!(yaw >= 221.0F) || !(yaw <= 229.0F)) && (!(yaw >= 131.0F) || !(yaw <= 139.0F)) && (!(yaw >= 41.0F) || !(yaw <= 49.0F))) {
                    if ((!(yaw >= 310.8F) || !(yaw <= 320.8F)) && (!(yaw >= 220.8F) || !(yaw <= 230.8F)) && (!(yaw >= 130.8F) || !(yaw <= 140.8F)) && (!(yaw >= 40.8F) || !(yaw <= 50.8F))) {
                        if ((!(yaw >= 308.7F) || !(yaw <= 322.7F)) && (!(yaw >= 218.7F) || !(yaw <= 232.7F)) && (!(yaw >= 128.7F) || !(yaw <= 142.7F)) && (!(yaw >= 38.7F) || !(yaw <= 52.7F))) {
                            if ((!(yaw >= 306.5F) || !(yaw <= 324.5F)) && (!(yaw >= 216.5F) || !(yaw <= 234.5F)) && (!(yaw >= 126.5F) || !(yaw <= 144.5F)) && (!(yaw >= 36.5F) || !(yaw <= 54.5F))) {
                                if (yaw >= 304.0F && yaw <= 327.0F || yaw >= 214.0F && yaw <= 237.0F || yaw >= 124.0F && yaw <= 147.0F || yaw >= 34.0F && yaw <= 57.0F) {
                                    result = 0.75F;
                                }
                            } else {
                                result = 0.79F;
                            }
                        } else {
                            result = 0.81F;
                        }
                    } else {
                        result = 0.83F;
                    }
                } else {
                    result = 0.85F;
                }
            } else {
                result = 0.88F;
            }

            if (!mc.options.jumpKey.isPressed()) {
                if (mc.options.sneakKey.isPressed()) {
                    mc.player.setVelocity(velocity.x, -3.6D, velocity.z);
                } else {
                    mc.player.setVelocity(velocity.x, 0.0D, velocity.z);
                }
            } else {
                mc.player.setVelocity(velocity.x, forward == 0.0D && strafe == 0.0D ? 1.4D : 1.2D, velocity.z);
            }

            setVelocity((double) result);
        }
    }

    private void setVelocity(double speed) {
        if (!MoveUtils.isMoving()) return;

        double yaw = getDirection();
        mc.player.setVelocity(-Math.sin(yaw) * speed, mc.player.getVelocity().y, Math.cos(yaw) * speed);
    }

    private double getDirection() {
        float rotationYaw = mc.player.getYaw();

        if (mc.player.input.playerInput.backward()) {
            rotationYaw += 180f;
        }

        float forward = 1f;

        if (mc.player.input.playerInput.backward()) {
            forward = -0.5f;
        } else if (mc.player.input.playerInput.forward()) {
            forward = 0.5f;
        }

        if (mc.player.input.playerInput.left()) {
            rotationYaw -= 90f * forward;
        }

        if (mc.player.input.playerInput.right()) {
            rotationYaw += 90f * forward;
        }

        return Math.toRadians(rotationYaw);
    }
}
