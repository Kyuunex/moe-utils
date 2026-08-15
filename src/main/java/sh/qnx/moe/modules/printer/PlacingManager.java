package sh.qnx.moe.modules.printer;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import it.unimi.dsi.fastutil.Pair;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.systems.modules.player.AutoGap;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import sh.qnx.moe.MoeUtils;
import sh.qnx.moe.utility.BlockUtils;
import sh.qnx.moe.utility.InventoryUtils;
import sh.qnx.moe.utility.modules.McDataCache;
import sh.qnx.moe.utility.render.RenderWrap;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PlacingManager {

    public static boolean sortPlaceableBlocks() {
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();

        if (worldSchematic == null || mc.level == null || mc.player == null) {
            return false;
        }

        List<int[]> printerBlocks = new ArrayList<>(
            PrinterUtils.PRINTER.placeRadius.get() * PrinterUtils.PRINTER.placeRadius.get());

        if (!PrinterUtils.PRINTER.anchor.get() && !PrinterUtils.PRINTER.lockToY.get()) {
            for (int i = -PrinterUtils.PRINTER.placeRadius.get();
                i <= PrinterUtils.PRINTER.placeRadius.get(); i++) {
                printerBlocks.addAll(getBlocksForYLevel(worldSchematic, mc.player.getBlockY() + i));
            }
        } else {
            printerBlocks.addAll(
                getBlocksForYLevel(worldSchematic, PrinterUtils.PRINTER.yLevel.get()));
        }

        PrinterUtils.PRINTER.toSort.clear();

        int printerMaxSorting = 0;

        printerBlocks.sort(PrinterUtils.CLOSEST_XZ_COMPARATOR);

        for (int[] posVec : printerBlocks) {
            if (printerMaxSorting > 32) {
                break;
            }

            printerMaxSorting++;
            PrinterUtils.PRINTER.toSort.add(posVec);

        }

        printerBlocks.clear();

//        if (PrinterUtils.PRINTER.firstAlgorithm.get() != Printer.SortAlgorithm.Closest) {
//            if (PrinterUtils.PRINTER.firstAlgorithm.get().applySecondSorting) {
//                if (PrinterUtils.PRINTER.secondAlgorithm.get() != Printer.SortingSecond.None) {
//                    PrinterUtils.PRINTER.toSort.sort(PrinterUtils.PRINTER.secondAlgorithm.get().algorithm);
//                }
//            }
//            PrinterUtils.PRINTER.toSort.sort(PrinterUtils.PRINTER.firstAlgorithm.get().algorithm);
//        }

        return true;
    }

    private static List<int[]> getBlocksForYLevel(WorldSchematic worldSchematic, int y) {

        BlockPos.MutableBlockPos srcBlock = new BlockPos.MutableBlockPos(0, 0, 0);

        return PrinterUtils.findNearBlocksByRadius(mc.player.blockPosition().mutable().setY(y),
            PrinterUtils.PRINTER.placeRadius.get(),
            (pos) -> {
                srcBlock.set(pos[0], y, pos[2]);

                BlockState blockState = mc.level.getBlockState(srcBlock);

                BlockState required = worldSchematic.getBlockState(srcBlock);

                if (mc.player.blockPosition()
                    .closerThan(srcBlock, PrinterUtils.PRINTER.placeRadius.get())
                    && blockState.canBeReplaced()
                    && !required.isAir()
                    && blockState.getBlock() != required.getBlock()
                    && (BlockUtils.canPlace(srcBlock, PrinterUtils.PRINTER.placeDistance.get()) || (
                    PrinterUtils.PRINTER.liquidPlace.get() && BlockUtils.canPlace(srcBlock,
                        PrinterUtils.PRINTER.placeDistance.get(), true)))
                    && !mc.player
                    .getBoundingBox()
                    .intersects(srcBlock.getCenter(), srcBlock.getCenter().add(1, 1, 1))) {

                    return (PrinterUtils.PRINTER.blockSwapping.get()
                        && PrinterUtils.PRINTER.containedColors.contains(
                        McDataCache.getColor(required.getBlock().asItem())))
                        || (!PrinterUtils.PRINTER.blockSwapping.get()
                        && PrinterUtils.PRINTER.containedBlocks.contains(
                        required.getBlock().asItem()));
                }

                return false;
            }
        );
    }

    public static void tryPlacingBlocks() {
        if (mc.player == null || mc.gameMode == null) {
            return;
        }

        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();

        if (worldSchematic == null || !sortPlaceableBlocks()) {
            return;
        }

        int placed = 0;

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        List<int[]> waterPlaceable = new ArrayList<>();

        for (int[] pos : PrinterUtils.PRINTER.toSort) {
            if (Modules.get().get(AutoEat.class).eating
                || Modules.get().get(AutoGap.class).isEating()
                || Modules.get().get(KillAura.class).getTarget() != null) {
                PrinterUtils.PRINTER.interestPoint = 8;
                break;
            }

            if (PrinterUtils.PRINTER.placeTimer < PrinterUtils.PRINTER.delay.get()
                || placed >= PrinterUtils.PRINTER.blocksPerTick.get()
                || PrinterUtils.PRINTER.blocksPlacedThisSec
                >= PrinterUtils.PRINTER.blocksPerSec.get()) {
                PrinterUtils.PRINTER.interestPoint = 9;
                break;
            }

            blockPos.set(pos[0], pos[1], pos[2]);

            BlockState state = worldSchematic.getBlockState(blockPos);
            Item item = state.getBlock().asItem();

            FindItemResult itemResult =
                InvUtils.find(
                    (stack) ->
                        !PrinterUtils.PRINTER.blockSwapping.get()
                            ? stack.getItem() == item
                            : McDataCache.getColor(stack) == McDataCache.getColor(item)
                                && PrinterUtils.PRINTER.blockExclusion.get().stream()
                                .noneMatch((block -> stack.getItem() == block.asItem())));

            if (!itemResult.found()) {
                continue;
            }

            InteractionHand hand = InteractionHand.MAIN_HAND;

            if (itemResult.isOffhand()) {
                hand = InteractionHand.OFF_HAND;
            }

            if (PrinterUtils.PRINTER.swapTimer > 0) {
                PrinterUtils.PRINTER.interestPoint = 10;
                break;
            }

            if ((PrinterUtils.PRINTER.blockSwapping.get()
                && McDataCache.getColor(mc.player.getMainHandItem())
                != McDataCache.getColor(item)
                || (!PrinterUtils.PRINTER.blockSwapping.get()
                && mc.player.getMainHandItem().getItem() != item))
                && hand != InteractionHand.OFF_HAND) {
                PrinterUtils.PRINTER.swapTimer = PrinterUtils.PRINTER.swapDelay.get();
                if (itemResult.isHotbar()) {
                    InventoryUtils.swapSlot(itemResult.slot());
                } else {
                    int emptySlot = InventoryUtils.findEmptySlotInHotbar(7);
                    InventoryUtils.swapSlot(emptySlot);
                    InventoryUtils.swapToHotbar(itemResult.slot(), emptySlot);
                }

                break;
            }

            if (BlockUtils.canPlace(blockPos, PrinterUtils.PRINTER.placeDistance.get())) {
                if (PrinterUtils.placeBlock(hand, itemResult, blockPos)) {
                    if (PrinterUtils.PRINTER.placeFading.stream()
                        .noneMatch((pair) -> pair.right().equals(blockPos))) {
                        PrinterUtils.PRINTER.placeFading.add(
                            Pair.of(new RenderWrap(PrinterUtils.PRINTER.fadeTime.get(), 0),
                                new BlockPos(blockPos)));
                    }

                    PrinterUtils.PRINTER.placeTimer = 0;
                    placed++;
                    PrinterUtils.PRINTER.blocksPlacedThisSec++;
                }
            } else if (PrinterUtils.PRINTER.liquidPlace.get() && BlockUtils.canPlace(blockPos,
                PrinterUtils.PRINTER.placeDistance.get(), true)) {
                waterPlaceable.add(pos);
            } else {
                MoeUtils.LOGGER.info("Failed liquid place check & Air: {} - {}",
                    blockPos.toShortString(), BlockUtils.shouldLiquidPlace(blockPos));
            }
        }

        for (int[] pos : waterPlaceable) {
            if (PrinterUtils.PRINTER.lastLiquidPlace > 0) {
                break;
            }

            if (PrinterUtils.PRINTER.placeTimer < PrinterUtils.PRINTER.delay.get()
                || placed >= PrinterUtils.PRINTER.blocksPerTick.get()
                || PrinterUtils.PRINTER.blocksPlacedThisSec
                >= PrinterUtils.PRINTER.blocksPerSec.get()) {
                PrinterUtils.PRINTER.interestPoint = 9;
                break;
            }

            blockPos.set(pos[0], pos[1], pos[2]);

            BlockState state = worldSchematic.getBlockState(blockPos);
            Item item = state.getBlock().asItem();

            FindItemResult itemResult =
                InvUtils.find(
                    (stack) ->
                        !PrinterUtils.PRINTER.blockSwapping.get()
                            ? stack.getItem() == item
                            : McDataCache.getColor(stack) == McDataCache.getColor(item)
                                && PrinterUtils.PRINTER.blockExclusion.get().stream()
                                .noneMatch((block -> stack.getItem() == block.asItem())));

            if (!itemResult.found()) {
                continue;
            }

            InteractionHand hand = InteractionHand.MAIN_HAND;

            if (itemResult.isOffhand()) {
                hand = InteractionHand.OFF_HAND;
            }

            if (PrinterUtils.PRINTER.swapTimer > 0) {
                PrinterUtils.PRINTER.interestPoint = 10;
                break;
            }

            if ((PrinterUtils.PRINTER.blockSwapping.get()
                && McDataCache.getColor(mc.player.getMainHandItem())
                != McDataCache.getColor(item)
                || (!PrinterUtils.PRINTER.blockSwapping.get()
                && mc.player.getMainHandItem().getItem() != item))
                && hand != InteractionHand.OFF_HAND) {
                PrinterUtils.PRINTER.swapTimer = PrinterUtils.PRINTER.swapDelay.get();
                if (itemResult.isHotbar()) {
                    InventoryUtils.swapSlot(itemResult.slot());
                } else {
                    int emptySlot = InventoryUtils.findEmptySlotInHotbar(7);
                    InventoryUtils.swapSlot(emptySlot);
                    InventoryUtils.swapToHotbar(itemResult.slot(), emptySlot);
                }

                break;
            }

            PrinterUtils.PRINTER.lastLiquidPlace = PrinterUtils.PRINTER.liquidPlaceTimeout.get();

            BlockPos lowerPos = blockPos.relative(Direction.DOWN);
            MoeUtils.LOGGER.info("Trying to liquid place on {}", lowerPos.toShortString());

            mc.gameMode.useItemOn(mc.player, hand,
                new BlockHitResult(BlockUtils.clickOffset(blockPos, Direction.UP), Direction.UP,
                    lowerPos, false));
            PrinterUtils.placeBlock(hand, itemResult, blockPos);

            if (PrinterUtils.PRINTER.placeFading.stream()
                .noneMatch((pair) -> pair.right().equals(blockPos))) {
                PrinterUtils.PRINTER.placeFading.add(
                    Pair.of(new RenderWrap(PrinterUtils.PRINTER.fadeTime.get(), 0),
                        new BlockPos(blockPos)));
            }

            PrinterUtils.PRINTER.placeTimer = 0;
            placed++;
            PrinterUtils.PRINTER.blocksPlacedThisSec++;
        }

        waterPlaceable.clear();
    }
}
