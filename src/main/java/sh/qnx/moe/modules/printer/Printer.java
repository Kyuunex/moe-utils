package sh.qnx.moe.modules.printer;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalNear;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import it.unimi.dsi.fastutil.Pair;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import sh.qnx.moe.MoeUtils;
import sh.qnx.moe.modules.printer.movesets.AdvancedMove;
import sh.qnx.moe.modules.printer.movesets.MoveSets;
import sh.qnx.moe.utility.BlockUtils;
import sh.qnx.moe.utility.InventoryUtils;
import sh.qnx.moe.utility.MathUtils;
import sh.qnx.moe.utility.modules.McDataCache;
import sh.qnx.moe.utility.packets.PacketUtils;
import sh.qnx.moe.utility.render.RenderWrap;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static sh.qnx.moe.modules.printer.PrinterUtils.PRINTER;

public class Printer extends Module {

    // anchoringTo must be here.
    public static BlockPos.MutableBlockPos anchoringTo = new BlockPos.MutableBlockPos();

    // Settings
    private final SettingGroup sgDefault = settings.getDefaultGroup();
    public final Setting<Integer> swapDelay = sgDefault.add(new IntSetting.Builder()
        .name("switch-delay")
        .description("How long to wait before placing after switching.")
        .defaultValue(4)
        .sliderRange(0, 10)
        .build()
    );
    public final Setting<Integer> placeRadius = sgDefault.add(new IntSetting.Builder()
        .name("place-radius")
        .description("The range to check for placeable blocks.")
        .defaultValue(5)
        .sliderRange(1, 5)
        .build()
    );
    public final Setting<Double> placeDistance = sgDefault.add(new DoubleSetting.Builder()
        .name("place-distance")
        .description("The max distance to place blocks.")
        .defaultValue(3.75)
        .sliderRange(3.2, 5.0)
        .build()
    );
    public final Setting<Integer> delay = sgDefault.add(new IntSetting.Builder()
        .name("printing-delay")
        .description("Delay between printing blocks in ticks.")
        .defaultValue(0)
        .sliderRange(0, 20)
        .build()
    );
    public final Setting<Integer> blocksPerTick = sgDefault.add(new IntSetting.Builder()
        .name("blocks/tick")
        .description("How many blocks place per tick.")
        .defaultValue(3)
        .sliderRange(1, 4)
        .build()
    );
    public final Setting<Integer> blocksPerSec = sgDefault.add(new IntSetting.Builder()
        .name("blocks/sec")
        .description("The maximum blocks per second.")
        .defaultValue(60)
        .sliderRange(10, 80)
        .build()
    );
    public final Setting<Boolean> lockToY = sgDefault.add(new BoolSetting.Builder()
        .name("lock-to-y")
        .description("Prevents placing blocks unless at a specific Y level.")
        .defaultValue(false)
        .build()
    );
    public final Setting<Boolean> noRotations = sgDefault.add(new BoolSetting.Builder()
        .name("no-rotations")
        .description("Completely disables rotations, for lenient Grim.")
        .defaultValue(true)
        .build()
    );
    public final Setting<Boolean> skipUnneededRotations = sgDefault.add(new BoolSetting.Builder()
        .name("skip-rotations")
        .description("Skips rotations in a certain Yaw/Pitch distance to place more, causes camera to snap around.")
        .defaultValue(true)
        .build()
    );
    public final Setting<Boolean> experimentalRotations = sgDefault.add(new BoolSetting.Builder()
        .name("experimental-rotations")
        .description("An experimental rotation change, makes it much more aggressive.")
        .defaultValue(false)
        .build()
    );
    public final Setting<Double> rotationTolerance = sgDefault.add(new DoubleSetting.Builder()
        .name("rotation-tolerance")
        .description("How much space to tolerate before rotating.")
        .defaultValue(15)
        .sliderRange(0.1, 30)
        .visible(skipUnneededRotations::get)
        .build()
    );
    public final Setting<Boolean> raytraceCarpet = sgDefault.add(new BoolSetting.Builder()
        .name("raytrace-carpet")
        .description("Raytracing for carpet, is likely not needed and will decrease speed.")
        .defaultValue(false)
        .build()
    );
    public final Setting<Boolean> raytraceFull = sgDefault.add(new BoolSetting.Builder()
        .name("raytrace-full")
        .description("Raytracing for full-blocks, not required on grim.")
        .defaultValue(true)
        .build()
    );
//    public final Setting<SortAlgorithm> firstAlgorithm = sgDefault.add(new EnumSetting.Builder<SortAlgorithm>()
//        .name("first-sorting-mode")
//        .description("The blocks you want to place first.")
//        .defaultValue(SortAlgorithm.Closest)
//        .build());
//    public final Setting<SortingSecond> secondAlgorithm = sgDefault.add(new EnumSetting.Builder<SortingSecond>()
//        .name("second-sorting-mode")
//        .description("Second pass of sorting eg. place first blocks higher and closest to you.")
//        .defaultValue(SortingSecond.None)
//        .visible(() -> firstAlgorithm.get().applySecondSorting)
//        .build());
    public final Setting<Boolean> liquidPlace = sgDefault.add(new BoolSetting.Builder()
        .name("liquid-place")
        .description("Places inside of liquids if it would let you place a target block.")
        .defaultValue(true)
        .build()
    );
    public final Setting<Integer> liquidPlaceTimeout = sgDefault.add(new IntSetting.Builder()
        .name("liquid-place-timeout")
        .description("Timeout between liquid placing, 0 to effectively disable.")
        .defaultValue(10)
        .sliderRange(0, 40)
        .build()
    );
    public final Setting<Boolean> onlyOnGround = sgDefault.add(new BoolSetting.Builder()
        .name("on-ground")
        .description("Only places if the player is on-ground.")
        .defaultValue(true)
        .build()
    );
    public final Setting<Boolean> notInLiquid = sgDefault.add(new BoolSetting.Builder()
        .name("out-of-liquid")
        .description("Only places if the player is not in a liquid.")
        .defaultValue(true)
        .build()
    );

    // Color Swapping
    private final SettingGroup sgBlockSwapping = settings.createGroup("Block Swapping (broken)");
    public final Setting<Boolean> blockSwapping = sgBlockSwapping.add(new BoolSetting.Builder()
        .name("block-swapping")
        .description("Swap blocks with ones that are same color.")
        .defaultValue(false)
        .build()
    );
    public final Setting<List<Block>> blockExclusion = sgBlockSwapping.add(new BlockListSetting.Builder()
        .name("block-exclusion")
        .description("Excludes blocks.")
        .build()
    );

    // Anchoring
    private final SettingGroup sgAnchor = settings.createGroup("Anchoring");
    public final Setting<Boolean> anchor = sgAnchor.add(new BoolSetting.Builder()
        .name("anchor")
        .description("Anchors player to placeable blocks.")
        .defaultValue(false)
        .onChanged((b) -> anchoringTo.set(0, -999, 0))
        .build()
    );
    public final Setting<Integer> yLevel = sgAnchor.add(new IntSetting.Builder()
        .name("y-level")
        .description("The Y level to scan")
        .defaultValue(64)
        .sliderRange(-64, 320)
        .build()
    );
    public final Setting<AnchorMovement> anchorMove = sgAnchor.add(new EnumSetting.Builder<AnchorMovement>()
        .name("anchor-movement")
        .description("How you move to destinations")
        .defaultValue(AnchorMovement.Vanilla)
        .build()
    );
    public final Setting<Boolean> alwaysSprint = sgAnchor.add(new BoolSetting.Builder()
        .name("always-sprint")
        .description("Advanced move will always sprint.")
        .visible(() -> anchorMove.get() == AnchorMovement.Vanilla)
        .defaultValue(false)
        .build()
    );
    public final Setting<Boolean> forceFrostWalkerProc = sgAnchor.add(new BoolSetting.Builder()
        .name("force-frost-walker")
        .description("Attempts to force frost walker to proc instead of walking into water.")
        .visible(() -> anchorMove.get() == AnchorMovement.Vanilla)
        .defaultValue(true)
        .build()
    );
    public final Setting<Integer> backHoldTime = sgAnchor.add(new IntSetting.Builder()
        .name("back-hold")
        .description("How many ticks to hold back for frost walker.")
        .visible(() -> anchorMove.get() == AnchorMovement.Vanilla && forceFrostWalkerProc.get())
        .defaultValue(3)
        .sliderRange(0, 5)
        .build()
    );
    public final Setting<Boolean> useElytra = sgAnchor.add(new BoolSetting.Builder()
        .name("use-elytra")
        .description("Will fly to land near blocks if it is available.")
        .visible(() -> anchorMove.get() == AnchorMovement.Vanilla)
        .defaultValue(false)
        .build()
    );
    public final Setting<AdvancedMove.PitchMode> pitchMode = sgAnchor.add(new EnumSetting.Builder<AdvancedMove.PitchMode>()
        .name("pitch-mode")
        .description("How anchoring should handle pitches.")
        .visible(() -> anchorMove.get() == AnchorMovement.Vanilla)
        .defaultValue(AdvancedMove.PitchMode.NONE)
        .build()
    );
    public final Setting<Integer> distanceBeforeFlight = sgAnchor.add(new IntSetting.Builder()
        .name("distance-before-flight")
        .description("How far from a anchor pos before resorting to using efly.")
        .visible(() -> anchorMove.get() == AnchorMovement.Vanilla && useElytra.get())
        .defaultValue(12)
        .sliderRange(12, 64)
        .build()
    );
    public final Setting<Integer> landScanRadius = sgAnchor.add(new IntSetting.Builder()
        .name("elytra-scan-radius")
        .description("How far to scan for land to use for elytra landing.")
        .visible(() -> anchorMove.get() == AnchorMovement.Vanilla && useElytra.get())
        .defaultValue(16)
        .sliderRange(16, 64)
        .build()
    );
    public final Setting<Boolean> differing = sgAnchor.add(new BoolSetting.Builder()
        .name("differing")
        .description("Re-routes when certain conditions are met.")
        .defaultValue(true)
        .build()
    );
    public final Setting<Double> differDistance = sgAnchor.add(new DoubleSetting.Builder()
        .name("differ-distance")
        .description("How close to a block you can be before re-routing.")
        .defaultValue(0.75)
        .sliderRange(0.1, 1.5)
        .build()
    );
//    public final Setting<AnchorSortAlgorithm> anchorAlgorithm = sgAnchor.add(new EnumSetting.Builder<AnchorSortAlgorithm>()
//        .name("anchor-sorting-mode")
//        .description("The blocks you want to place first.")
//        .defaultValue(AnchorSortAlgorithm.ClosestToLastBlock)
//        .build());
    protected final Setting<Integer> sortingDivisionFactor = sgAnchor.add(new IntSetting.Builder()
        .name("sort-division")
        .description("The division factor for direction based sorting.")
        .defaultValue(16)
        .sliderRange(1, 16)
        .build()
    );
    public final Setting<Integer> anchorRange = sgAnchor.add(new IntSetting.Builder()
        .name("anchor-range")
        .description("The range to anchor to blocks, by chunks.")
        .defaultValue(16)
        .sliderRange(2, 32)
        .range(1, 128)
        .build()
    );
    public final Setting<Integer> anchorResetDelay = sgAnchor.add(new IntSetting.Builder()
        .name("anchor-reset-delay")
        .description("Delay between resetting the anchor.")
        .defaultValue(100)
        .sliderRange(5, 1200)
        .range(5, 1200)
        .build()
    );
    public final Setting<Integer> anchorSortDelay = sgAnchor.add(new IntSetting.Builder()
        .name("anchor-sort-delay")
        .description("Delay between re-sorting the anchor list.")
        .defaultValue(10)
        .sliderRange(1, 1200)
        .range(1, 1200)
        .build()
    );

    // Auto Swim
    private final SettingGroup sgAutoSwim = settings.createGroup("Auto Swim (WIP)");
    public final Setting<Boolean> autoSwim = sgAutoSwim.add(new BoolSetting.Builder()
        .name("auto-swim")
        .description("Navigate out of water.")
        .defaultValue(true)
        .build()
    );
    protected final Setting<Integer> savingGraceDelay = sgAutoSwim.add(new IntSetting.Builder()
        .name("saving-grace-delay")
        .description("How often to look for suitable land.")
        .defaultValue(5)
        .sliderRange(0, 20)
        .build()
    );
    protected final Setting<Integer> savingGraceRadius = sgAutoSwim.add(new IntSetting.Builder()
        .name("saving-grace-radius")
        .description("The distance around the selected position to look for suitable land.")
        .defaultValue(64)
        .sliderRange(16, 64)
        .build());
    protected final Setting<Integer> o2Radius = sgAutoSwim.add(new IntSetting.Builder()
        .name("o2-radius")
        .description("The distance around the selected position to look for an opening of air.")
        .defaultValue(24)
        .sliderRange(8, 64)
        .build()
    );

    // Auto Sleep
    private final SettingGroup sgAutoSleep = settings.createGroup("Auto Sleep (WIP)");
    public final Setting<Boolean> autoSleep = sgAutoSleep.add(new BoolSetting.Builder()
        .name("auto-sleep")
        .description("Sleep after a certain periods of time.")
        .defaultValue(false)
        .build()
    );
    public final Setting<Integer> sleepLastRest = sgAutoSleep.add(new IntSetting.Builder()
        .name("since-last-rest")
        .description("THe minimum last rest before sleeping.")
        .defaultValue(36000)
        .sliderRange(12000, 128000)
        .build()
    );
    public final Setting<BlockPos> bedPos = sgAutoSleep.add(new BlockPosSetting.Builder()
        .name("bed-pos")
        .description("The position of the bed.")
        .defaultValue(BlockPos.ZERO)
        .build()
    );

    // Auto Return
    private final SettingGroup sgAutoReturn = settings.createGroup("Auto Return");
    public final Setting<Boolean> autoReturn = sgAutoReturn.add(new BoolSetting.Builder()
        .name("auto-return")
        .description("Return to a set position once out of materials.")
        .defaultValue(false)
        .build()
    );
    public final Setting<MoveSets> returnMove = sgAutoReturn.add(new EnumSetting.Builder<MoveSets>()
        .name("return-movement")
        .description("How you return to the 'home' position.")
        .defaultValue(MoveSets.BARITONE)
        .build()
    );
    public final Setting<Double> takeOffPitch = sgAutoReturn.add(new DoubleSetting.Builder()
        .name("take-off-pitch")
        .description("Pitch for taking off.")
        .defaultValue(-7.4)
        .decimalPlaces(1)
        .sliderRange(-14, -5)
        .visible(() -> returnMove.get() == MoveSets.CONST_EFLY)
        .build()
    );
    public final Setting<Double> descendRange = sgAutoReturn.add(new DoubleSetting.Builder()
        .name("descend-range")
        .description("Range from return pos before descending.")
        .defaultValue(5)
        .decimalPlaces(1)
        .sliderRange(2, 10)
        .visible(() -> returnMove.get() == MoveSets.CONST_EFLY)
        .build()
    );
    public final Setting<Double> minDistanceAbove = sgAutoReturn.add(new DoubleSetting.Builder()
        .name("min-stay-above")
        .description("Distance to stay over while flying.")
        .defaultValue(3)
        .decimalPlaces(1)
        .sliderRange(3, 12)
        .visible(() -> returnMove.get() == MoveSets.CONST_EFLY)
        .build()
    );
    public final Setting<Double> maxDistanceAbove = sgAutoReturn.add(new DoubleSetting.Builder()
        .name("max-stay-above")
        .description("Distance to decline if reached over return pos, adds over minimum.")
        .defaultValue(3)
        .decimalPlaces(1)
        .sliderRange(1, 12)
        .visible(() -> returnMove.get() == MoveSets.CONST_EFLY)
        .build()
    );
    public final Setting<Double> constantPitch = sgAutoReturn.add(new DoubleSetting.Builder()
        .name("constant-pitch")
        .description("Pitch for flying.")
        .defaultValue(-6.7)
        .decimalPlaces(1)
        .sliderRange(-14, -5)
        .visible(() -> returnMove.get() == MoveSets.CONST_EFLY)
        .build()
    );
    public final Setting<Double> dramaticPitchStep = sgAutoReturn.add(new DoubleSetting.Builder()
        .name("dramatic-pitch-step")
        .description("The step for urgent pitch changes.")
        .defaultValue(0.34)
        .decimalPlaces(1)
        .sliderRange(0.1, 3)
        .visible(() -> returnMove.get() == MoveSets.CONST_EFLY)
        .build()
    );
    public final Setting<Double> subtlePitchStep = sgAutoReturn.add(new DoubleSetting.Builder()
        .name("subtle-pitch-step")
        .description("The step for subtle pitch changes.")
        .defaultValue(0.05)
        .decimalPlaces(1)
        .sliderRange(0.1, 3)
        .visible(() -> returnMove.get() == MoveSets.CONST_EFLY)
        .build()
    );
    public final Setting<Double> elytraSpeed = sgAutoReturn.add(new DoubleSetting.Builder()
        .name("elytra-speed")
        .description("The speed to fly in bps.")
        .sliderRange(20, 44)
        .defaultValue(24)
        .visible(() -> returnMove.get() == MoveSets.CONST_EFLY)
        .build()
    );
    public final Setting<Double> elytraSpeedStep = sgAutoReturn.add(new DoubleSetting.Builder()
        .name("elytra-speed-step-up")
        .description("The step for elytra speed changes.")
        .defaultValue(1)
        .decimalPlaces(1)
        .sliderRange(0.5, 6)
        .visible(() -> returnMove.get() == MoveSets.CONST_EFLY)
        .build()
    );
    public final Setting<Double> elytraSpeedStepDec = sgAutoReturn.add(new DoubleSetting.Builder()
        .name("elytra-speed-step-down")
        .description("The step for elytra speed changes.")
        .defaultValue(1.5)
        .decimalPlaces(1)
        .sliderRange(0.5, 6)
        .visible(() -> returnMove.get() == MoveSets.CONST_EFLY)
        .build()
    );
    public final Setting<BlockPos> returnPos = sgAutoReturn.add(new BlockPosSetting.Builder()
        .name("return-pos")
        .description("The return 'home' position.")
        .defaultValue(BlockPos.ZERO)
        .build()
    );

    private final SettingGroup sgRendering = settings.createGroup("Rendering");
    public final Setting<Double> contraction = sgRendering.add(new DoubleSetting.Builder()
        .name("contraction")
        .description("The rate of contraction.")
        .sliderRange(0, 5)
        .decimalPlaces(4)
        .defaultValue(0.085)
        .build()
    );
    public final Setting<Double> strokeOffset = sgRendering.add(new DoubleSetting.Builder()
        .name("stroke-offset")
        .description("The offset for stroke lines.")
        .sliderRange(0, 0.15)
        .decimalPlaces(4)
        .defaultValue(0.085)
        .build()
    );
    public final Setting<RenderMode> renderModePlacing = sgRendering.add(new EnumSetting.Builder<RenderMode>()
        .name("placing-render")
        .description("The mode for rendering placed blocks.")
        .defaultValue(RenderMode.Static)
        .build()
    );
    public final Setting<SettingColor> placingColor = sgRendering.add(new ColorSetting.Builder()
        .name("placing-color")
        .defaultValue(new Color(255, 59, 59, 255))
        .build()
    );
    public final Setting<SettingColor> placingStrokeColor = sgRendering.add(new ColorSetting.Builder()
        .name("placing-stroke")
        .defaultValue(new Color(255, 59, 59, 255))
        .build()
    );
    public final Setting<RenderMode> renderModeDestination = sgRendering.add(new EnumSetting.Builder<RenderMode>()
        .name("destination-render")
        .description("The mode for rendering placed blocks.")
        .defaultValue(RenderMode.Static)
        .build()
    );
    public final Setting<SettingColor> destinationColor = sgRendering.add(new ColorSetting.Builder()
        .name("destination-color")
        .defaultValue(new Color(255, 59, 59, 255))
        .build()
    );
    public final Setting<SettingColor> destinationStrokeColor = sgRendering.add(new ColorSetting.Builder()
        .name("destination-stroke")
        .defaultValue(new Color(255, 59, 59, 255))
        .build()
    );
    public final Setting<Integer> fadeTime = sgRendering.add(new IntSetting.Builder()
        .name("fade-time")
        .description("Time for the rendering to fade, in ticks.")
        .defaultValue(3)
        .range(1, 1000)
        .sliderRange(1, 20)
        .build()
    );


    private final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
    public final List<int[]> toSort = new ArrayList<>();
    final List<int[]> anchorToSort = new ArrayList<>();
    public final List<MapColor> containedColors = new ArrayList<>();
    public final List<Item> containedBlocks = new ArrayList<>();
    int anchorRefreshTimer = 0;
    int anchorSortTimer = 0;
    public int lastLiquidPlace = 0;

    // Render
    public final List<Pair<RenderWrap, BlockPos>> placeFading = new ArrayList<>();
    protected double renderOffset = 0;
    protected boolean isGoingUp = true;

    // Swimming ?
    protected BlockPos airOpeningTemp;
    protected boolean wasSwimmingUp = false;
    protected BlockPos savingGrace;
    protected int savingGraceTimer = 0;

    // Sleeping
    protected boolean sleepJob = false;
    protected int sleepAttemptTimer = 0;
    protected BlockPos sleepReturnTo;
    protected long tickTimestamp = -1;
    public int swapTimer = 0;
    private boolean pauseTillRefilled = false;
    public int placeTimer;
    private int anchorResetTimer;
    public int jumpTimer = 0;
    private BlockPos returnTo;
    public int blocksPlacedThisSec = 0;
    private int lastSecond = 0;
    private int lastMessageSecond = -1;
    public int interestPoint = 0;

    public Printer() {
        super(
            MoeUtils.CATEGORY,
            "Moe Printer",
            "Places litematica schematics, designed for mapart."
        );
        PrinterUtils.PRINTER = this;
    }

    @Override
    public void onActivate() {
        onDeactivate();
    }

    @Override
    public void onDeactivate() {
        placeFading.clear();
        anchorToSort.clear();
        toSort.clear();
        anchoringTo.set(0, -999, 0);
        pauseTillRefilled = false;
        if (baritone.getPathingBehavior().hasPath()) {
            baritone.getPathingBehavior().cancelEverything();
        }
        mc.options.keyUp.setDown(false);
        if (wasSwimmingUp) {
            wasSwimmingUp = false;
            mc.options.keyJump.setDown(false);
        }
        airOpeningTemp = null;
        savingGrace = null;
        sleepJob = false;
        sleepAttemptTimer = 0;
        sleepReturnTo = null;

        anchorRefreshTimer = 749;

        returnMove.get().movement.cancel(BlockPos.ZERO);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        interestPoint = 0;

        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            placeFading.clear();
            interestPoint = 1;
            return;
        }

        threadedTick();
        // PrinterUtils.EXECUTOR.execute(this::threadedTick);
    }

    private void threadedTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            placeFading.clear();
            interestPoint = 1;
            return;
        }

        int second = LocalDateTime.now().getSecond();

        if (lastSecond != second) {
            lastSecond = second;
            blocksPlacedThisSec = 0;
        }

        lastLiquidPlace--;

        tickTimestamp = System.currentTimeMillis();

        renderingTick();

        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();

        if (worldSchematic == null) {
            placeFading.clear();
            toggle();
            interestPoint = 2;
            return;
        }

        if (swapTimer > 0) {
            swapTimer--;
        }

        containedColors.clear();
        containedBlocks.clear();

        for (ItemStack stack : mc.player.getInventory().items) {
            if (InventoryUtils.IS_BLOCK.test(stack)) {
                if (!blockSwapping.get()) {
                    containedBlocks.add(stack.getItem());
                } else if (blockExclusion.get().stream().noneMatch((block -> stack.getItem() == block.asItem()))) {
                    containedColors.add(McDataCache.getColor(stack));
                }
            }
        }

        if (autoSleep.get()) {
            if (PrinterUtils.isNight() && PrinterUtils.getTimeSinceLastRest() >= sleepLastRest.get()) {
                if (!bedPos.get().equals(BlockPos.ZERO)) {
                    if (sleepReturnTo == null) {
                        sleepReturnTo = mc.player.blockPosition();
                    }

                    sleepJob = true;

                    if (MathUtils.xzDistanceBetween(mc.player.getEyePosition(), bedPos.get()) > 2) {
                        returnMovement(bedPos.get());
                    } else {
                        returnMove.get().movement.cancel(bedPos.get());

                        if (sleepAttemptTimer > 0) {
                            sleepAttemptTimer--;
                            interestPoint = 3;
                            return;
                        }

                        if (baritone.getPathingBehavior().hasPath()) {
                            baritone.getPathingBehavior().cancelEverything();
                        }

                        var rot = BlockUtils.getRotation(true, bedPos.get());

                        if (BlockUtils.canRaycast(bedPos.get(), rot.getValue(), rot.getKey())) {
                            PacketUtils.rotate(rot.getKey(), rot.getValue());

                            mc.gameMode.useItemOn(
                                mc.player,
                                InteractionHand.OFF_HAND,
                                new BlockHitResult(
                                    Vec3.atCenterOf(bedPos.get()),
                                    Direction.UP,
                                    bedPos.get(),
                                    false
                                )
                            );
                            sleepAttemptTimer = 20;
                        }
                    }
                }
            } else if (sleepJob) {
                if (mc.player.isSleeping()) {
                    if (PrinterUtils.getTimeSinceLastRest() < sleepLastRest.get()) {
                        PacketUtils.send(
                            new ServerboundPlayerCommandPacket(
                                mc.player,
                                ServerboundPlayerCommandPacket.Action.STOP_SLEEPING
                            )
                        );
                    }
                } else {
                    if (pauseTillRefilled) {
                        sleepJob = false;
                        sleepAttemptTimer = 0;
                        sleepReturnTo = null;
                    } else {
                        returnMovement(sleepReturnTo);

                        if (MathUtils.xzDistanceBetween(sleepReturnTo, mc.player.blockPosition()) <= 2) {
                            sleepJob = false;
                            sleepAttemptTimer = 0;
                            sleepReturnTo = null;
                            returnMove.get().movement.cancel(sleepReturnTo);
                        }
                    }
                }
            } else {
                sleepAttemptTimer = 0;
                sleepReturnTo = null;
            }

            if (sleepJob) {
                interestPoint = 4;
                return;
            }
        }

        if (autoSwim.get()) {
            StuckFixManager.runSwimTask();

            if (mc.player.onGround()) {
                if (wasSwimmingUp) {
                    mc.options.keyJump.setDown(false);
                    mc.options.keyUp.setDown(false);
                    PRINTER.wasSwimmingUp = false;
                    PRINTER.savingGrace = null;
                }
            }

            if (StuckFixManager.shouldCancelForSwimmingTask()) {
                interestPoint = 5;
                return;
            }
        }

        if (pauseTillRefilled && autoReturn.get()) {
            placeFading.clear();
            if (mc.player.getInventory().getFreeSlot() == -1) {
                returnMovement(returnTo);
                if (MathUtils.xzDistanceBetween(returnTo, mc.player.blockPosition()) <= 0.35) {
                    if (mc.player.onGround()) {
                        pauseTillRefilled = false;
                    }

                    anchorRefreshTimer = 749;
                    returnMove.get().movement.cancel(returnTo);
                }
            } else {
                returnMovement(returnPos.get());

                anchorRefreshTimer = 749;

                if (MathUtils.xzDistanceBetween(returnPos.get(), mc.player.blockPosition()) <= 0.35) {
                    returnMove.get().movement.cancel(returnPos.get());
                }
            }
            interestPoint = 6;
            return;
        }

        if (anchor.get()) {
            if (swapTimer <= 0) {
                if (
                    (
                        anchoringTo.getY() != -999
                        && (isDiffered(anchoringTo) || BlockUtils.isNotAir(anchoringTo) || BlockUtils.hasEntitiesInside(anchoringTo))
                    )
                    || anchorResetTimer >= anchorResetDelay.get()
                ) {
                    anchoringTo.set(0, -999, 0);

                    interestPoint = 10;

                    anchorResetTimer = 1;
                    switch (anchorMove.get()) {
                        case Vanilla -> MoveSets.ADVANCED.movement.cancel(BlockPos.ZERO);
                        case Baritone -> {
                            if (baritone.getPathingBehavior().hasPath()) {
                                baritone.getPathingBehavior().cancelEverything();
                            }
                        }
                    }
                } else if (anchoringTo.getY() == -999) {
                    switch (anchorMove.get()) {
                        case Vanilla -> MoveSets.ADVANCED.movement.cancel(BlockPos.ZERO);
                        case Baritone -> {
                            if (baritone.getPathingBehavior().hasPath()) {
                                baritone.getPathingBehavior().cancelEverything();
                            }
                        }
                    }
                } else {
                    if (anchorSortDelay.get() >= anchorSortTimer) {
                        anchorToSort.sort(PrinterUtils.CLOSEST_XZ_COMPARATOR);

                        anchorSortTimer = 0;
                    }

                    anchorSortTimer++;

                    switch (anchorMove.get()) {
                        case Vanilla -> MoveSets.ADVANCED.movement.tick(anchoringTo);
                        case Baritone -> {
                            if (!baritone.getPathingBehavior().hasPath()) {
                                baritoneTo(anchoringTo);
                            }
                        }
                    }
                }

                anchorRefreshTimer++;

                if (anchoringTo.getY() == -999) {
                    if (anchorRefreshTimer >= 60) {
                        BlockPos.MutableBlockPos srcBlock = new BlockPos.MutableBlockPos(0, 0, 0);

                        List<int[]> anchoredBlocks = PrinterUtils.findNearBlocksByChunk(
                            mc.player.blockPosition().mutable().setY(yLevel.get()).immutable(),
                            anchorRange.get(),
                            (pos) -> {
                                srcBlock.set(pos[0], yLevel.get(), pos[2]);

                                BlockState blockState = mc.level.getBlockState(srcBlock);
                                BlockState required = worldSchematic.getBlockState(srcBlock);

                                if (blockState.isAir() && !required.isAir()
                                    && !BlockUtils.hasEntitiesInside(srcBlock)) {
                                    return ((blockSwapping.get()
                                        && containedColors.contains(
                                        McDataCache.getColor(required.getBlock().asItem())))
                                        || (!blockSwapping.get()
                                        && containedBlocks.contains(required.getBlock().asItem())))
                                        && !isDiffered(srcBlock);
                                }

                                return false;
                            });

                        anchorToSort.clear();

                        anchoredBlocks.sort(PrinterUtils.CLOSEST_XZ_COMPARATOR);

                        int maxAnchoredBlocks = 0;

                        for (int[] posVec : anchoredBlocks) {
                            if (maxAnchoredBlocks >= 32) {
                                break;
                            }

                            maxAnchoredBlocks++;
                            anchorToSort.add(posVec);
                        }

                        anchoredBlocks.clear();

                        anchorRefreshTimer = 0;
                        anchorSortTimer = 0;
                    }

                    if (!anchorToSort.isEmpty()) {
                        anchoringTo = anchoringTo.set(
                            anchorToSort.getFirst()[0],
                            anchorToSort.getFirst()[1],
                            anchorToSort.getFirst()[2]
                        );
                        anchorToSort.removeFirst();
                    } else if (
                        (!pauseTillRefilled && anchorRefreshTimer == 0)
                        || containedColors.isEmpty()
                    ) {
                        if (lastMessageSecond != lastSecond) {
                            info("No block to anchor to, going to return position.");
                            lastMessageSecond = lastSecond;
                        }

                        returnTo = mc.player.blockPosition();
                        pauseTillRefilled = true;
                    }
                }

                anchorResetTimer++;
            } else {
                interestPoint = 11;
                mc.options.keyUp.setDown(false);
                if (baritone.getPathingBehavior().hasPath()) {
                    baritone.getPathingBehavior().cancelEverything();
                }
            }
        }

        placeFading.forEach(
            s -> {
                if (renderModePlacing.get() == RenderMode.Breath) {
                    s.left().breath(s.left().breath() - 1);
                } else {
                    s.left().fadeTime(s.left().fadeTime() - 1);
                }
            });
        placeFading.removeIf(
            s -> s.left().fadeTime() <= 0 || s.left().breath() * contraction.get() <= -1);

        toSort.clear();

        if (placeTimer >= delay.get()) {
            if (mc.player.isUsingItem() || !passesChecks()) {
                interestPoint = 7;
                return;
            }

            PlacingManager.tryPlacingBlocks();

        } else {
            placeTimer++;
        }
    }

    public boolean passesChecks() {
        if (mc.player == null) {
            return false;
        }

        if (onlyOnGround.get() && !mc.player.onGround()) {
            return false;
        }

        // Don't simplify these in case we add more! The IDE does not know the vision!
        // u sure about that mate?

        if (notInLiquid.get() && mc.player.isInLiquid()) {
            return false;
        }

        return true;
    }

    public boolean isDiffered(BlockPos pos) {
        if (mc.player == null || !differing.get()) {
            return false;
        }

        return MathUtils.xzDistanceBetween(mc.player.getEyePosition(), pos) <= differDistance.get();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        assert mc.player != null : "Player has not joined the game.";
        placeFading.forEach(
            s ->
                renderBlock(
                    event.renderer,
                    s.right().getX(),
                    s.right().getY(),
                    s.right().getZ(),
                    s.right().getX() + 1,
                    s.right().getY() + 1,
                    s.right().getZ() + 1,
                    RenderType.Placing,
                    renderModePlacing.get(),
                    s.left().breath()
                )
        );

        if (airOpeningTemp != null && mc.player.getEyeY() < airOpeningTemp.getY()) {
            renderBlock(
                event.renderer,
                airOpeningTemp.getX(),
                airOpeningTemp.getY(),
                airOpeningTemp.getZ(),
                airOpeningTemp.getX() + 1,
                airOpeningTemp.getY() + 1,
                airOpeningTemp.getZ() + 1,
                RenderType.Destination,
                renderModeDestination.get(),
                0);
        } else if (savingGrace != null) {
            renderBlock(
                event.renderer,
                savingGrace.getX(),
                savingGrace.getY(),
                savingGrace.getZ(),
                savingGrace.getX() + 1,
                savingGrace.getY() + 1,
                savingGrace.getZ() + 1,
                RenderType.Destination,
                renderModeDestination.get(),
                0);
        } else if (anchoringTo.getY() != -999) {
            renderBlock(
                event.renderer,
                anchoringTo.getX(),
                anchoringTo.getY(),
                anchoringTo.getZ(),
                anchoringTo.getX() + 1,
                anchoringTo.getY() + 1,
                anchoringTo.getZ() + 1,
                RenderType.Destination,
                renderModeDestination.get(),
                0);
        }
    }

    public void returnMovement(BlockPos pos) {
        returnMove.get().movement.tick(pos);
    }

    private void baritoneTo(BlockPos pos) {
        if (baritone.getPathingBehavior().hasPath()) {
            baritone.getPathingBehavior().cancelEverything();
        }
        baritone.getCustomGoalProcess().setGoalAndPath(new GoalNear(pos, 2));
    }

    public void renderingTick() {
        if (renderModePlacing.get() != RenderMode.Static
            || renderModeDestination.get() != RenderMode.Static) {
            if (renderOffset <= 0) {
                isGoingUp = true;
            } else if (renderOffset >= 1) {
                isGoingUp = false;
            }

            renderOffset += isGoingUp ? contraction.get() : -contraction.get();
        } else {
            renderOffset = 0;
        }
    }

    public void renderBlock(
        Renderer3D renderer,
        double x1,
        double y1,
        double z1,
        double x2,
        double y2,
        double z2,
        RenderType type,
        RenderMode mode,
        int breath) {
        Color color =
            switch (type) {
                case Placing -> placingColor.get();
                case Destination -> destinationColor.get();
            };

        int origAlpha = color.a;

        switch (mode) {
            case Static -> {
                renderer.boxLines(x1, y1, z1, x2, y2, z2, color, 0);
                color.a(60);
                renderer.boxSides(x1, y1, z1, x2, y2, z2, color, 0);
                color.a(origAlpha);
            }

            case Breath -> {
                renderer.boxLines(x1, y1, z1, x2, y2 + (breath * contraction.get()), z2, color, 0);
                color.a(60);
                renderer.boxSides(x1, y1, z1, x2, y2 + (breath * contraction.get()), z2, color, 0);
                color.a(origAlpha);
            }

            case Stroke -> {
                renderer.boxLines(x1, y1, z1, x2, y2, z2, color, 0);
                color.a(60);
                renderer.boxSides(x1, y1, z1, x2, y2, z2, color, 0);
                color.a(140);
                renderer.boxLines(
                    x1 - strokeOffset.get(),
                    y1 + renderOffset,
                    z1 - strokeOffset.get(),
                    x2 + strokeOffset.get(),
                    y1 + renderOffset,
                    z2 + strokeOffset.get(),
                    switch (type) {
                        case Placing -> placingStrokeColor.get();
                        case Destination -> destinationStrokeColor.get();
                    },
                    0);
                renderer.boxLines(
                    x1 - strokeOffset.get(),
                    y1 + Math.min(Math.max(0, renderOffset / 1.7), 1),
                    z1 - strokeOffset.get(),
                    x2 + strokeOffset.get(),
                    y1 + renderOffset,
                    z2 + strokeOffset.get(),
                    switch (type) {
                        case Placing -> placingStrokeColor.get();
                        case Destination -> destinationStrokeColor.get();
                    },
                    0);
                color.a(origAlpha);
            }
        }
    }

    public enum AnchorMovement {
        Baritone,
        Vanilla
    }

    @SuppressWarnings("unused")
    public enum AnchorSortAlgorithm {
        RSNorth(
            SortAlgorithm.Closest,
            Comparator.comparingDouble(
                value -> {
                    int v = Math.toIntExact(
                        value.getZ() / PrinterUtils.PRINTER.sortingDivisionFactor.get()
                    );

                    assert MeteorClient.mc.player != null;
                    float altDist = Math.toIntExact(
                        value.getX()
                            / PrinterUtils.PRINTER.sortingDivisionFactor.get())
                        - Math.toIntExact(
                            MeteorClient.mc.player.getBlockX()
                                / PrinterUtils.PRINTER.sortingDivisionFactor.get()
                    );
                    return v + ((double) Mth.sqrt(altDist * altDist) / 64);
                })),
        RSEast(
            SortAlgorithm.Closest,
            Comparator.comparingDouble(
                value -> {
                    int v =
                        Math.toIntExact(
                            value.getX() / PrinterUtils.PRINTER.sortingDivisionFactor.get());

                    assert MeteorClient.mc.player != null;
                    float altDist =
                        Math.toIntExact(
                            value.getZ() / PrinterUtils.PRINTER.sortingDivisionFactor.get())
                            - Math.toIntExact(
                            MeteorClient.mc.player.getBlockZ()
                                / PrinterUtils.PRINTER.sortingDivisionFactor.get());
                    return -v + ((double) Mth.sqrt(altDist * altDist) / 64);
                })),
        RSSouth(
            SortAlgorithm.Closest,
            Comparator.comparingDouble(
                value -> {
                    int v =
                        Math.toIntExact(
                            value.getZ() / PrinterUtils.PRINTER.sortingDivisionFactor.get());

                    assert MeteorClient.mc.player != null;
                    float altDist =
                        Math.toIntExact(
                            value.getX() / PrinterUtils.PRINTER.sortingDivisionFactor.get())
                            - Math.toIntExact(
                            MeteorClient.mc.player.getBlockX()
                                / PrinterUtils.PRINTER.sortingDivisionFactor.get());
                    return -v + ((double) Mth.sqrt(altDist * altDist) / 64);
                })),
        RSWest(
            SortAlgorithm.Closest,
            Comparator.comparingDouble(
                value -> {
                    int v =
                        Math.toIntExact(
                            value.getX() / PrinterUtils.PRINTER.sortingDivisionFactor.get());

                    assert MeteorClient.mc.player != null;
                    float altDist =
                        Math.toIntExact(
                            value.getZ() / PrinterUtils.PRINTER.sortingDivisionFactor.get())
                            - Math.toIntExact(
                            MeteorClient.mc.player.getBlockZ()
                                / PrinterUtils.PRINTER.sortingDivisionFactor.get());
                    return v + ((double) Mth.sqrt(altDist * altDist) / 64);
                })),
        ClosestToLastBlock(
            SortAlgorithm.Closest,
            Comparator.comparingDouble(
                value ->
                    MeteorClient.mc.player != null
                        ? Utils.squaredDistance(
                        anchoringTo.getY() != -999 ? anchoringTo.getX()
                            : MeteorClient.mc.player.getX(),
                        anchoringTo.getY() != -999 ? anchoringTo.getY()
                            : MeteorClient.mc.player.getY(),
                        anchoringTo.getY() != -999 ? anchoringTo.getZ()
                            : MeteorClient.mc.player.getZ(),
                        value.getX() + 0.5,
                        value.getY() + 0.5,
                        value.getZ() + 0.5)
                        : 0));

        final SortAlgorithm secondaryAlgorithm;
        final Comparator<BlockPos> algorithm;

        AnchorSortAlgorithm(SortAlgorithm secondaryAlgorithm, Comparator<BlockPos> algorithm) {
            this.secondaryAlgorithm = secondaryAlgorithm;
            this.algorithm = algorithm;
        }
    }

    @SuppressWarnings("unused")
    public enum SortAlgorithm {
        Closest(
            false,
            Comparator.comparingDouble(
                value ->
                    MeteorClient.mc.player != null
                        ? Utils.squaredDistance(
                        MeteorClient.mc.player.getX(),
                        MeteorClient.mc.player.getY(),
                        MeteorClient.mc.player.getZ(),
                        value.getX() + 0.5,
                        value.getY() + 0.5,
                        value.getZ() + 0.5)
                        : 0)),
        ClosestToLastBlock(
            false,
            Comparator.comparingDouble(
                value ->
                    MeteorClient.mc.player != null
                        ? Utils.squaredDistance(
                        anchoringTo.getY() != -999 ? anchoringTo.getX()
                            : MeteorClient.mc.player.getX(),
                        anchoringTo.getY() != -999 ? anchoringTo.getY()
                            : MeteorClient.mc.player.getY(),
                        anchoringTo.getY() != -999 ? anchoringTo.getZ()
                            : MeteorClient.mc.player.getZ(),
                        value.getX() + 0.5,
                        value.getY() + 0.5,
                        value.getZ() + 0.5)
                        : 0)),
        Furthest(
            false,
            Comparator.comparingDouble(
                value ->
                    MeteorClient.mc.player != null
                        ? -(Utils.squaredDistance(
                        MeteorClient.mc.player.getX(),
                        MeteorClient.mc.player.getY(),
                        MeteorClient.mc.player.getZ(),
                        value.getX() + 0.5,
                        value.getY() + 0.5,
                        value.getZ() + 0.5))
                        : 0));

        final boolean applySecondSorting;
        final Comparator<BlockPos> algorithm;

        SortAlgorithm(boolean applySecondSorting, Comparator<BlockPos> algorithm) {
            this.applySecondSorting = applySecondSorting;
            this.algorithm = algorithm;
        }
    }

    public enum RenderType {
        Placing,
        Destination
    }

    public enum RenderMode {
        Breath,
        Stroke,
        Static
    }

    @SuppressWarnings("unused")
    public enum SortingSecond {
        None(SortAlgorithm.Closest.algorithm),
        Nearest(SortAlgorithm.Closest.algorithm),
        Furthest(SortAlgorithm.Furthest.algorithm);

        final Comparator<BlockPos> algorithm;

        SortingSecond(Comparator<BlockPos> algorithm) {
            this.algorithm = algorithm;
        }
    }
}
