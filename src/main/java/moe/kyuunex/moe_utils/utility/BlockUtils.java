package moe.kyuunex.moe_utils.utility;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import baritone.api.BaritoneAPI;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import moe.kyuunex.moe_utils.modules.printer.PrinterUtils;

public class BlockUtils {
    private static long LAST_TIMESTAMP = -1;

    public static Direction[] getDirections() {
        return Direction.values();
    }

    public static boolean isReplaceable(BlockPos pos) {
        return mc.player != null && mc.player.getWorld().getBlockState(pos).isAir()
            || mc.player.getWorld().getBlockState(pos).isReplaceable()
            || isLiquid(pos);
    }

    public static boolean isLiquid(BlockPos pos) {
        return mc.player != null
            && mc.player.getWorld().getBlockState(pos).getBlock() instanceof FluidBlock;
    }

    public static boolean isNotAir(BlockPos pos) {
        return mc.player == null || !mc.player.getWorld().getBlockState(pos).isAir();
    }

    public static boolean canPlace(BlockPos pos) {
        assert mc.player != null;

        List<Entity> entities =
            mc.player
                .getWorld()
                .getOtherEntities(
                    null,
                    new Box(pos.toCenterPos().add(3, 3, 3), pos.toCenterPos().add(-3, -3, -3)),
                    entity -> {
                        if (EntityUtils.canPlaceIn(entity)) {
                            return false;
                        }

                        return entity.collidesWithStateAtPos(pos, Blocks.BEDROCK.getDefaultState());
                    });

        return isReplaceable(pos) && entities.isEmpty();
    }

    public static Direction getPlaceDirection(BlockPos pos) {
        for (Direction direction : getDirections()) {
            if (isNotAir(pos.offset(direction))) return direction;
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
                Vec3d vec3d =
                    new Vec3d(
                        (double) pos.getX() + ((double) direction.getOpposite().getOffsetX() * 0.5),
                        (double) pos.getY() + ((double) direction.getOpposite().getOffsetY() * 0.5),
                        (double) pos.getZ() + ((double) direction.getOpposite().getOffsetZ() * 0.5));
                double yaw = Rotations.getYaw(vec3d), pitch = Rotations.getPitch(vec3d);

                rotation =
                    RotationUtils.reachable(
                        BaritoneAPI.getProvider().getBaritoneForPlayer(mc.player).getPlayerContext(),
                        pos.offset(direction),
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
                        new Vec3d(
                            (double) pos.getX() + direction.getOpposite().getOffsetX(),
                            (double) pos.getY() + direction.getOpposite().getOffsetY(),
                            (double) pos.getZ() + direction.getOpposite().getOffsetZ());
                    yaw = Rotations.getYaw(vec3d);
                    pitch = Rotations.getPitch(vec3d);

                    rotation =
                        RotationUtils.reachable(
                            BaritoneAPI.getProvider().getBaritoneForPlayer(mc.player).getPlayerContext(),
                            pos.offset(direction),
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
        PrinterUtils.fakePlayer.getAttributes().setFrom(mc.player.getAttributes());
        PrinterUtils.fakePlayer.setPose(mc.player.getPose());
        PrinterUtils.fakePlayer.setPitch(pitch);
        PrinterUtils.fakePlayer.prevPitch = pitch;
        PrinterUtils.fakePlayer.setYaw(yaw);
        PrinterUtils.fakePlayer.prevYaw = yaw;
        PrinterUtils.fakePlayer.prevBodyYaw = yaw;
        PrinterUtils.fakePlayer.prevHeadYaw = yaw;
        PrinterUtils.fakePlayer.setHeadYaw(yaw);
        PrinterUtils.fakePlayer.setBodyYaw(yaw);
        PrinterUtils.fakePlayer.calculateDimensions();

        HitResult pHitResult = PrinterUtils.fakePlayer.raycast(4, 1.0f, false);

        if (pHitResult.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        BlockHitResult hitResult = (BlockHitResult) pHitResult;

        return hitResult.getBlockPos().offset(hitResult.getSide()).equals(pos);
    }

    public static Vec3d clickOffset(BlockPos pos) {
        return new Vec3d(
            (double) pos.getX() + ((double) getPlaceDirection(pos).getOffsetX() * 0.5),
            (double) pos.getY() + ((double) getPlaceDirection(pos).getOffsetY() * 0.5),
            (double) pos.getZ() + ((double) getPlaceDirection(pos).getOffsetZ() * 0.5));
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
            PrinterUtils.fakePlayer.getAttributes().setFrom(mc.player.getAttributes());
            PrinterUtils.fakePlayer.setPose(mc.player.getPose());
            PrinterUtils.fakePlayer.setPitch(rot.getValue());
            PrinterUtils.fakePlayer.prevPitch = rot.getValue();
            PrinterUtils.fakePlayer.setYaw(rot.getKey());
            PrinterUtils.fakePlayer.prevYaw = rot.getKey();
            PrinterUtils.fakePlayer.prevBodyYaw = rot.getKey();
            PrinterUtils.fakePlayer.prevHeadYaw = rot.getKey();
            PrinterUtils.fakePlayer.setHeadYaw(rot.getKey());
            PrinterUtils.fakePlayer.setBodyYaw(rot.getKey());
            PrinterUtils.fakePlayer.calculateDimensions();

            return (BlockHitResult) PrinterUtils.fakePlayer.raycast(4.5, 1.0f, false);
        }
        return new BlockHitResult(
            clickOffset(pos),
            direction == null ? getPlaceDirection(pos).getOpposite() : direction,
            pos.offset(direction == null ? getPlaceDirection(pos) : direction.getOpposite()),
            false);
    }

    public static boolean placeBlock(
        Hand hand, FindItemResult itemResult, BlockPos pos, long tickTimestamp) {
        assert mc.player != null : "Player has not joined the game.";
        assert mc.interactionManager != null : "Interaction Manager is not defined.";
        assert mc.getNetworkHandler() != null : "Network Handler is not defined.";
        assert mc.world != null : "The world is null.";

        Direction dir = getPlaceDirection(pos);

        boolean isCarpet =
            mc.player
                .getInventory()
                .getStack(itemResult.slot())
                .getItem()
                .getTranslationKey()
                .endsWith("carpet");

        if (isCarpet && !PrinterUtils.PRINTER.raytraceCarpet.get()) {
            dir = Direction.UP;
            Map.Entry<Float, Float> rot = getRotation(false, pos);

            BlockPos offsetPos = pos.down();

            if (BlockUtils.isReplaceable(offsetPos)) {
                Block block = mc.world.getBlockState(offsetPos).getBlock();
                if (block == Blocks.KELP
                    || block == Blocks.KELP_PLANT
                    || block == Blocks.TALL_SEAGRASS
                    || block == Blocks.SEAGRASS) {
                    mc.interactionManager.attackBlock(offsetPos, dir);
                } else if (mc.world.getBlockState(offsetPos).getBlock() == Blocks.BUBBLE_COLUMN) {
                    mc.interactionManager.interactBlock(
                        mc.player, hand, getBlockHitResult(false, offsetPos, dir));
                } else {
                    return false;
                }
            }

            if (!mc.player.handSwinging) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));

            if (tickTimestamp == -1 || tickTimestamp != LAST_TIMESTAMP) {
                LAST_TIMESTAMP = tickTimestamp;
                mc.getNetworkHandler().sendPacket(
                    new PlayerMoveC2SPacket.LookAndOnGround(
                        rot.getKey(),
                        rot.getValue(),
                        mc.player.isOnGround(), mc.player.horizontalCollision
                    )
                );
            }

            // mc.interactionManager.interactBlock(mc.player, hand, getBlockHitResult(false, pos,
            // getPlaceDirection(pos)));

            mc.interactionManager.interactBlock(mc.player, hand, getBlockHitResult(false, pos, dir));
            return true;
        }

        Map.Entry<Float, Float> rot = getRotation(true, pos);

        if (canRaycast(pos, rot.getValue(), rot.getKey()) || !PrinterUtils.PRINTER.raytraceFull.get()) {
            if (!mc.player.handSwinging) {
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
            }
            mc.getNetworkHandler().sendPacket(
                new PlayerMoveC2SPacket.LookAndOnGround(
                    rot.getKey(),
                    rot.getValue(),
                    mc.player.isOnGround(),
                    mc.player.horizontalCollision
                )
            );
            // mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, getBlockHitResult(true, pos, dir), 0));
            mc.interactionManager.interactBlock(
                mc.player, hand, getBlockHitResult(PrinterUtils.PRINTER.raytraceFull.get(), pos, dir));
            return true;
        }

        return false;
    }
}
