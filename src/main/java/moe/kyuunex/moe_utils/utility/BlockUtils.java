package moe.kyuunex.moe_utils.utility;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import baritone.api.BaritoneAPI;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import moe.kyuunex.moe_utils.modules.printer.PrinterUtils;

public class BlockUtils {
    private static long LAST_TIMESTAMP = -1;

    public static Direction[] getDirections() {
        return Direction.values();
    }

    public static boolean shouldAirPlace(BlockPos pos) {
        for (Direction direction : getDirections()) {
            if (!isReplaceable(pos.relative(direction))) return false;
        }
        return true;
    }

    public static boolean isReplaceable(BlockPos pos) {
        return mc.player != null && mc.player.level().getBlockState(pos).isAir()
            || mc.player.level().getBlockState(pos).canBeReplaced()
            || isLiquid(pos);
    }

    public static boolean isLiquid(BlockPos pos) {
        return mc.player != null
            && mc.player.level().getBlockState(pos).getBlock() instanceof LiquidBlock;
    }

    public static boolean isNotAir(BlockPos pos) {
        return mc.player == null || !mc.player.level().getBlockState(pos).isAir();
    }

    public static boolean canPlace(BlockPos pos, double dist) {
        assert mc.player != null;

        List<Entity> entities =
            mc.player
                .level()
                .getEntities(null, new AABB(pos.getCenter().add(3, 3, 3), pos.getCenter().add(-3, -3, -3))).stream().filter(
                    entity -> {
                        if (EntityUtils.canPlaceIn(entity)) {
                            return false;
                        }

                        return entity.isColliding(pos, Blocks.BEDROCK.defaultBlockState());
                    }).toList();

        // Distance check needs improvement maybe ?
        return isReplaceable(pos) && entities.isEmpty() && !shouldAirPlace(pos) && mc.player.getEyePosition().closerThan(getSafeHitResult(pos).getLocation(), dist);
    }

    public static Direction getPlaceDirection(BlockPos pos) {
        for (Direction direction : getDirections()) {
            if (isNotAir(pos.relative(direction))) return direction;
        }
        return Direction.UP;
    }

    public static Map.Entry<Float, Float> getRotation(boolean raytrace, BlockPos pos) {
        assert mc.player != null;

        if (raytrace) {
            if (canRaycast(pos, (float) Rotations.getYaw(pos), (float) Rotations.getPitch(pos))) {
                return Map.entry((float) Rotations.getYaw(pos), (float) Rotations.getPitch(pos));
            }

            Optional<Rotation> rotation =
                RotationUtils.reachable(
                    BaritoneAPI.getProvider().getBaritoneForPlayer(mc.player).getPlayerContext(),
                    pos,
                    4.5);

            if (rotation.isPresent()) {
                if (canRaycast(pos, rotation.get().getPitch(), rotation.get().getYaw())) {
                    return Map.entry(rotation.get().getYaw(), rotation.get().getPitch());
                }
            }

            for (Direction direction : Direction.values()) {
                Vec3 vec3d =
                    new Vec3(
                        (double) pos.getX() + ((double) direction.getOpposite().getStepX() * 0.5),
                        (double) pos.getY() + ((double) direction.getOpposite().getStepY() * 0.5),
                        (double) pos.getZ() + ((double) direction.getOpposite().getStepZ() * 0.5));
                double yaw = Rotations.getYaw(vec3d), pitch = Rotations.getPitch(vec3d);

                rotation =
                    RotationUtils.reachable(
                        BaritoneAPI.getProvider().getBaritoneForPlayer(mc.player).getPlayerContext(),
                        pos.relative(direction),
                        4.5);

                if (rotation.isPresent()) {
                    if (canRaycast(pos, rotation.get().getPitch(), rotation.get().getYaw())) {
                        return Map.entry(rotation.get().getYaw(), rotation.get().getPitch());
                    }
                }

                if (canRaycast(pos, (float) pitch, (float) yaw)) {
                    return Map.entry((float) yaw, (float) pitch);
                } else {
                    vec3d =
                        new Vec3(
                            (double) pos.getX() + direction.getOpposite().getStepX(),
                            (double) pos.getY() + direction.getOpposite().getStepY(),
                            (double) pos.getZ() + direction.getOpposite().getStepZ());
                    yaw = Rotations.getYaw(vec3d);
                    pitch = Rotations.getPitch(vec3d);

                    rotation =
                        RotationUtils.reachable(
                            BaritoneAPI.getProvider().getBaritoneForPlayer(mc.player).getPlayerContext(),
                            pos.relative(direction),
                            4.5);

                    if (rotation.isPresent()) {
                        if (canRaycast(pos, rotation.get().getPitch(), rotation.get().getYaw())) {
                            return Map.entry(rotation.get().getYaw(), rotation.get().getPitch());
                        }
                    }

                    if (canRaycast(pos, (float) pitch, (float) yaw)) {
                        return Map.entry((float) yaw, (float) pitch);
                    }
                }
            }

            return Map.entry(
                (float) Rotations.getYaw(clickOffset(pos)), (float) Rotations.getPitch(clickOffset(pos)));
        }
        return Map.entry(
            (float) Rotations.getYaw(clickOffset(pos)), (float) Rotations.getPitch(clickOffset(pos)));
    }

    public static boolean canRaycast(BlockPos pos, float pitch, float yaw) {
        assert mc.player != null;

        if (PrinterUtils.fakePlayer == null) {
            PrinterUtils.initFakePlayer();
        }

        PrinterUtils.fakePlayer.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        PrinterUtils.fakePlayer.getAttributes().assignValues(mc.player.getAttributes());
        PrinterUtils.fakePlayer.setPose(mc.player.getPose());
        PrinterUtils.fakePlayer.setXRot(pitch);
        //PrinterUtils.fakePlayer.prevPitch = pitch;
        PrinterUtils.fakePlayer.setYRot(yaw);
        //PrinterUtils.fakePlayer.prevYaw = yaw;
        //PrinterUtils.fakePlayer.prevBodyYaw = yaw;
        //PrinterUtils.fakePlayer.prevHeadYaw = yaw;
        PrinterUtils.fakePlayer.setYHeadRot(yaw);
        PrinterUtils.fakePlayer.setYBodyRot(yaw);
        PrinterUtils.fakePlayer.refreshDimensions();

        HitResult pHitResult = PrinterUtils.fakePlayer.pick(4, 1.0f, false);

        if (pHitResult.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        BlockHitResult hitResult = (BlockHitResult) pHitResult;

        return hitResult.getBlockPos().relative(hitResult.getDirection()).equals(pos);
    }

    public static Vec3 clickOffset(BlockPos pos) {
        return new Vec3(
            (double) pos.getX() + ((double) getPlaceDirection(pos).getStepX() * 0.5),
            (double) pos.getY() + ((double) getPlaceDirection(pos).getStepY() * 0.5),
            (double) pos.getZ() + ((double) getPlaceDirection(pos).getStepZ() * 0.5));
    }

    public static Vec3 clickOffset(BlockPos pos, Direction direction) {
        return Vec3.atCenterOf(pos).add(direction.getStepX() * 0.5, direction.getStepY() * 0.5, direction.getStepZ() * 0.5);
    }

    public static BlockHitResult getBlockHitResult(
        boolean raytrace, BlockPos pos, Direction direction) {
        if (raytrace) {
            assert mc.player != null;

            Map.Entry<Float, Float> rot = getRotation(true, pos);

            if (PrinterUtils.fakePlayer == null) {
                return null;
            }

            PrinterUtils.fakePlayer.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            PrinterUtils.fakePlayer.getAttributes().assignValues(mc.player.getAttributes());
            PrinterUtils.fakePlayer.setPose(mc.player.getPose());
            PrinterUtils.fakePlayer.setXRot(rot.getValue());
            //PrinterUtils.fakePlayer.prevPitch = rot.getValue();
            PrinterUtils.fakePlayer.setYRot(rot.getKey());
            //PrinterUtils.fakePlayer.prevYaw = rot.getKey();
            //PrinterUtils.fakePlayer.prevBodyYaw = rot.getKey();
            //PrinterUtils.fakePlayer.prevHeadYaw = rot.getKey();
            PrinterUtils.fakePlayer.setYHeadRot(rot.getKey());
            PrinterUtils.fakePlayer.setYBodyRot(rot.getKey());
            PrinterUtils.fakePlayer.refreshDimensions();

            return (BlockHitResult) PrinterUtils.fakePlayer.pick(4.5, 1.0f, false);
        }
        return new BlockHitResult(
            clickOffset(pos),
            direction == null ? getPlaceDirection(pos).getOpposite() : direction,
            pos.relative(direction == null ? getPlaceDirection(pos) : direction.getOpposite()),
            false);
    }

    public static double getHeight(BlockPos pos) {
        return mc.player.clientLevel.getBlockState(pos).getCollisionShape(mc.player.clientLevel, pos).max(Direction.Axis.Y);
    }

    public static BlockHitResult getSafeHitResult(BlockPos pos) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        Direction direction = Direction.UP;

        Vec3 offset;
        double yHeight = 0;

        for (Direction dir : getDirections()) {
            // Performance!
            mutable.set(pos.getX() + dir.getStepX(), pos.getY() + dir.getStepY(), pos.getZ() + dir.getStepZ());
            if (!isReplaceable(mutable)) {
                yHeight = getHeight(mutable);

                direction = dir;

                if (dir == Direction.DOWN) {
                    break;
                }
            }
        }

        offset = clickOffset(pos, direction);

        if (yHeight <= 0.6) {
            // Don't ask why, know it works.
            offset = new Vec3(offset.x, Math.floor(offset.y) + 0.0154, offset.z);
        }

        return new BlockHitResult(
            offset,
            direction.getOpposite(),
            mutable.set(pos.getX() + direction.getStepX(),
                pos.getY() + direction.getStepY(),
                pos.getZ() + direction.getStepZ()),
            false
        );
    }

    public static boolean placeBlock(
        InteractionHand hand, BlockPos pos, long tickTimestamp) {
        assert mc.player != null : "Player has not joined the game.";
        assert mc.gameMode != null : "Somethings wrong here and it really shouldn't be.";
        assert mc.getConnection() != null : "Somethings wrong here and it really shouldn't be. x2";
        assert mc.level != null : "The world hasn't loaded yet.";

        if (PrinterUtils.PRINTER.iHateGrim.get()) {
            if (isReplaceable(pos)) {
                mc.getConnection().getConnection().send(
                    new ServerboundSwingPacket(hand),
                    null,
                    true
                );
                mc.gameMode.useItemOn(mc.player, hand, getSafeHitResult(pos));
                return true;
            }

            return false;
        }

        Direction dir = getPlaceDirection(pos);

        boolean isPartialBlock = getHeight(pos) < 0.6;

        if (isPartialBlock && !PrinterUtils.PRINTER.raytracePartial.get()) {
            dir = Direction.UP;
            Map.Entry<Float, Float> rot = getRotation(false, pos);

            BlockPos offsetPos = pos.below();

            // Should work?
            if (mc.level.getFluidState(offsetPos).is(Fluids.WATER) && !isLiquid(offsetPos)) {
                Block block = mc.level.getBlockState(offsetPos).getBlock();
                if (block == Blocks.KELP
                    || block == Blocks.KELP_PLANT
                    || block == Blocks.TALL_SEAGRASS
                    || block == Blocks.SEAGRASS) {
                    mc.gameMode.startDestroyBlock(offsetPos, dir);
                } else if (mc.level.getBlockState(offsetPos).getBlock() == Blocks.BUBBLE_COLUMN) {
                    mc.getConnection().getConnection().send(
                        new ServerboundSwingPacket(hand),
                        null,
                        true
                    );
                    mc.gameMode.useItemOn(
                        mc.player, hand, getBlockHitResult(false, offsetPos, dir));
                } else {
                    return false;
                }
            }

            if (tickTimestamp == -1 || tickTimestamp != LAST_TIMESTAMP) {
                LAST_TIMESTAMP = tickTimestamp;
                mc.getConnection().getConnection().send(
                    new ServerboundMovePlayerPacket.Rot(
                        rot.getKey(),
                        rot.getValue(),
                        mc.player.onGround()
                    ),
                    null,
                    true
                );
            }

            // mc.gameMode.useItemOn(mc.player, hand, getBlockHitResult(false, pos, getPlaceDirection(pos)));
            mc.getConnection().getConnection().send(
                new ServerboundSwingPacket(hand),
                null,
                true
            );
            mc.gameMode.useItemOn(mc.player, hand, getBlockHitResult(false, pos, dir));
            return true;
        }

        Map.Entry<Float, Float> rot = getRotation(true, pos);

        if (canRaycast(pos, rot.getValue(), rot.getKey()) || !PrinterUtils.PRINTER.raytraceFull.get()) {
            mc.getConnection().getConnection().send(
                new ServerboundMovePlayerPacket.Rot(
                    rot.getKey(),
                    rot.getValue(),
                    mc.player.onGround()
                ),
                null,
                true
            );
            // mc.getConnection().getConnection().send(new PlayerInteractBlockC2SPacket(hand, getBlockHitResult(true, pos, dir), 0), null, true);
            mc.getConnection().getConnection().send(
                new ServerboundSwingPacket(hand),
                null,
                true
            );
            mc.gameMode.useItemOn(
                mc.player, hand, Objects.requireNonNull(getBlockHitResult(PrinterUtils.PRINTER.raytraceFull.get(), pos, dir)));
            return true;
        }

        return false;
    }
}
