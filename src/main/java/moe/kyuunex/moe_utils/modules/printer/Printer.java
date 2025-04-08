package moe.kyuunex.moe_utils.modules.printer;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

import java.time.LocalDateTime;
import java.util.*;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.systems.modules.player.AutoGap;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Tuple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import moe.kyuunex.moe_utils.MoeUtils;
import moe.kyuunex.moe_utils.utility.*;

public class Printer extends Module {
    // Variables
    private final SettingGroup sgDefault = settings.getDefaultGroup();
    public final Setting<Integer> swapDelay =
        sgDefault.add(
            new IntSetting.Builder()
                .name("switch-delay")
                .description("How long to wait before placing after switching.")
                .defaultValue(4)
                .sliderRange(2, 10)
                .build());
    public final Setting<Integer> range =
        sgDefault.add(
            new IntSetting.Builder()
                .name("place-range")
                .description("The range to place blocks.")
                .defaultValue(3)
                .sliderRange(1, 5)
                .build());
    public final Setting<Double> placeDistance =
        sgDefault.add(
            new DoubleSetting.Builder()
                .name("place-distance")
                .description("The max distance to place blocks.")
                .defaultValue(3.75)
                .sliderRange(3.2, 5.0)
                .build());
    public final Setting<Integer> delay =
        sgDefault.add(
            new IntSetting.Builder()
                .name("printing-delay")
                .description("Delay between printing blocks in ticks.")
                .defaultValue(0)
                .sliderRange(0, 20)
                .build());
    public final Setting<Integer> blocksPerTick =
        sgDefault.add(
            new IntSetting.Builder()
                .name("blocks/tick")
                .description("How many blocks place per tick.")
                .defaultValue(1)
                .sliderRange(1, 3)
                .build());
    public final Setting<Integer> blocksPerSec =
        sgDefault.add(
            new IntSetting.Builder()
                .name("blocks/sec")
                .description("The maximum blocks per second.")
                .defaultValue(20)
                .sliderRange(10, 80)
                .build());
    // Grim makes me go loony -V
    public final Setting<Boolean> iHateGrim =
        sgDefault.add(
            new BoolSetting.Builder()
                .name("9b9t")
                .description("Uses 9b9t specific placing.")
                .defaultValue(false)
                .build());
    public final Setting<Boolean> raytraceCarpet =
        sgDefault.add(
            new BoolSetting.Builder()
                .name("raytrace-carpet")
                .description("Raytracing for carpet, is likely not needed and will decrease speed.")
                .visible(() -> !iHateGrim.get())
                .defaultValue(false)
                .build());
    public final Setting<Boolean> raytraceFull =
        sgDefault.add(
            new BoolSetting.Builder()
                .name("raytrace-full")
                .description("Raytracing for full-blocks, not required on grim.")
                .visible(() -> !iHateGrim.get())
                .defaultValue(true)
                .build());
    public final Setting<SortAlgorithm> firstAlgorithm =
        sgDefault.add(
            new EnumSetting.Builder<SortAlgorithm>()
                .name("first-sorting-mode")
                .description("The blocks you want to place first.")
                .defaultValue(SortAlgorithm.None)
                .build());
    public final Setting<SortingSecond> secondAlgorithm =
        sgDefault.add(
            new EnumSetting.Builder<SortingSecond>()
                .name("second-sorting-mode")
                .description(
                    "Second pass of sorting eg. place first blocks higher and closest to you.")
                .defaultValue(SortingSecond.None)
                .visible(() -> firstAlgorithm.get().applySecondSorting)
                .build());
    private final SettingGroup sgRendering = settings.createGroup("Rendering");
    public final Setting<Double> contraction =
        sgRendering.add(
            new DoubleSetting.Builder()
                .name("contraction")
                .description("The rate of contraction.")
                .sliderRange(0, 5)
                .decimalPlaces(4)
                .defaultValue(0.085)
                .build());
    public final Setting<Double> strokeOffset =
        sgRendering.add(
            new DoubleSetting.Builder()
                .name("stroke-offset")
                .description("The offset for stroke lines.")
                .sliderRange(0, 0.15)
                .decimalPlaces(4)
                .defaultValue(0.085)
                .build());
    public final Setting<RenderMode> renderModePlacing =
        sgRendering.add(
            new EnumSetting.Builder<RenderMode>()
                .name("placing-render")
                .description("The mode for rendering placed blocks.")
                .defaultValue(RenderMode.Static)
                .build());
    public final Setting<SettingColor> placingColor =
        sgRendering.add(
            new ColorSetting.Builder()
                .name("placing-color")
                .defaultValue(new Color(255, 59, 59, 255))
                .build());
    public final Setting<SettingColor> placingStrokeColor =
        sgRendering.add(
            new ColorSetting.Builder()
                .name("placing-stroke")
                .defaultValue(new Color(255, 59, 59, 255))
                .build());
    public final Setting<RenderMode> renderModeDestination =
        sgRendering.add(
            new EnumSetting.Builder<RenderMode>()
                .name("destination-render")
                .description("The mode for rendering placed blocks.")
                .defaultValue(RenderMode.Static)
                .build());
    public final Setting<SettingColor> destinationColor =
        sgRendering.add(
            new ColorSetting.Builder()
                .name("destination-color")
                .defaultValue(new Color(255, 59, 59, 255))
                .build());
    public final Setting<SettingColor> destinationStrokeColor =
        sgRendering.add(
            new ColorSetting.Builder()
                .name("destination-stroke")
                .defaultValue(new Color(255, 59, 59, 255))
                .build());
    public final Setting<Integer> fadeTime =
        sgRendering.add(
            new IntSetting.Builder()
                .name("fade-time")
                .description("Time for the rendering to fade, in ticks.")
                .defaultValue(3)
                .range(1, 1000)
                .sliderRange(1, 20)
                .build());
    private final List<BlockPos> toSort = new ArrayList<>();
    private final List<Item> containedBlocks = new ArrayList<>();
    // Render
    private final List<Tuple<RenderWrap, BlockPos>> placeFading = new ArrayList<>();
    protected double renderOffset = 0;
    protected boolean isGoingUp = true;
    // Sleeping
    protected long tickTimestamp = -1;
    private int swapTimer = 0;
    private int placeTimer;
    private int blocksPlacedThisSec = 0;
    private int lastSecond = 0;

    public Printer() {
        super(
            MoeUtils.CATEGORY,
            "moe-printer",
            "Slimmed down version of V's printer. Places litematica schematics, designed for mapart."
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
        toSort.clear();
        mc.options.keyUp.setDown(false);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            placeFading.clear();
            return;
        }

        int second = LocalDateTime.now().getSecond();

        if (lastSecond != second) {
            lastSecond = second;
            blocksPlacedThisSec = 0;
        }

        tickTimestamp = System.currentTimeMillis();

        renderingTick();

        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();

        if (worldSchematic == null) {
            placeFading.clear();
            toggle();
            return;
        }

        containedBlocks.clear();

        for (ItemStack stack : mc.player.getInventory().items) {
            if (InventoryUtils.IS_BLOCK.test(stack)) {
                containedBlocks.add(stack.getItem());
            }
        }

        placeFading.forEach(
            s -> {
                if (renderModePlacing.get() == RenderMode.Breath) {
                    s.getA().breath(s.getA().breath() - 1);
                } else {
                    s.getA().fadeTime(s.getA().fadeTime() - 1);
                }
            });
        placeFading.removeIf(
            s -> s.getA().fadeTime() <= 0 || s.getA().breath() * contraction.get() <= -1);

        toSort.clear();

        if (placeTimer >= delay.get()) {
            if (mc.player.isUsingItem()) return;

            BlockIterator.register(
                range.get() + 1,
                3,
                (pos, blockState) -> {
                    BlockState required = worldSchematic.getBlockState(pos);

                    if (mc.player.blockPosition().closerThan(pos, range.get())
                        && blockState.canBeReplaced()
                        && !required.isAir()
                        && blockState.getBlock() != required.getBlock()
                        && DataManager.getRenderLayerRange().isPositionWithinRange(pos)
                        && !mc.player
                        .getBoundingBox()
                        .intersects(Vec3.atLowerCornerOf(pos), Vec3.atLowerCornerOf(pos).add(1, 1, 1))) {

                        if ((containedBlocks.contains(required.getBlock().asItem()))) {
                            if (iHateGrim.get()) {
                                toSort.add(new BlockPos(pos));
                            } else {
                                boolean isCarpet =
                                    required.getBlock().asItem().getDescriptionId().endsWith("carpet"); // wait, so this only works in English?
                                if (isCarpet) {
                                    toSort.add(new BlockPos(pos));
                                } else {
                                    Map.Entry<Float, Float> rot = BlockUtils.getRotation(true, pos);

                                    if (BlockUtils.canRaycast(pos, rot.getValue(), rot.getKey())) {
                                        toSort.add(new BlockPos(pos));
                                    }
                                }
                            }
                        }
                    }
                });

            BlockIterator.after(
                () -> {
                    if (firstAlgorithm.get() != SortAlgorithm.None) {
                        if (firstAlgorithm.get().applySecondSorting) {
                            if (secondAlgorithm.get() != SortingSecond.None) {
                                toSort.sort(secondAlgorithm.get().algorithm);
                            }
                        }
                        toSort.sort(firstAlgorithm.get().algorithm);
                    }

                    int placed = 0;

                    for (BlockPos pos : toSort) {
                        if (Modules.get().get(AutoEat.class).eating
                            || Modules.get().get(AutoGap.class).isEating()
                            || Modules.get().get(KillAura.class).getTarget() != null) {
                            break;
                        }

                        if (placeTimer < delay.get()
                            || placed >= blocksPerTick.get()
                            || blocksPlacedThisSec >= blocksPerSec.get()) break;

                        if (!mc.player.blockPosition().closerThan(pos, range.get())) continue;

                        BlockState state = worldSchematic.getBlockState(pos);
                        Item item = state.getBlock().asItem();

                        FindItemResult itemResult =
                            InvUtils.find(
                                (stack) -> stack.getItem() == item);

                        if (!itemResult.found()) continue;

                        InteractionHand hand = InteractionHand.MAIN_HAND;

                        if (itemResult.isOffhand()) hand = InteractionHand.OFF_HAND;

                        if (swapTimer > 0) {
                            swapTimer--;
                            break;
                        }

                        if (((mc.player.getInventory().getSelected().getItem() != item))
                            && hand != InteractionHand.OFF_HAND) {
                            swapTimer = swapDelay.get();
                            if (itemResult.isHotbar()) {
                                InventoryUtils.swapSlot(itemResult.slot(), false);
                            } else {
                                int emptySlot = InventoryUtils.findEmptySlotInHotbar(7);
                                InventoryUtils.swapSlot(emptySlot, false);

                                mc.gameMode.handleInventoryMouseClick(
                                    mc.player.containerMenu.containerId,
                                    itemResult.slot(),
                                    emptySlot,
                                    ClickType.SWAP,
                                    mc.player
                                );
                            }

                            break;
                        }

                        if (BlockUtils.canPlace(pos, placeDistance.get())) {
                            if (BlockUtils.placeBlock(hand, itemResult, pos, tickTimestamp)) {
                                if (placeFading.stream().noneMatch((pair) -> pair.getB().equals(pos)))
                                    placeFading.add(
                                        new Tuple<>(new RenderWrap(fadeTime.get(), 0), new BlockPos(pos)));
                                placeTimer = 0;
                                placed++;
                                blocksPlacedThisSec++;
                            }
                        }
                    }
                });

        } else placeTimer++;
    }


    @EventHandler
    private void onRender(Render3DEvent event) {
        assert mc.player != null : "Player has not joined the game.";
        placeFading.forEach(
            s ->
                renderBlock(
                    event.renderer,
                    s.getB().getX(),
                    s.getB().getY(),
                    s.getB().getZ(),
                    s.getB().getX() + 1,
                    s.getB().getY() + 1,
                    s.getB().getZ() + 1,
                    RenderType.Placing,
                    renderModePlacing.get(),
                    s.getA().breath()));
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


    @SuppressWarnings("unused")
    public enum SortAlgorithm {
        None(false, (a, b) -> 0),
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
        None(SortAlgorithm.None.algorithm),
        Nearest(SortAlgorithm.Closest.algorithm),
        Furthest(SortAlgorithm.Furthest.algorithm);

        final Comparator<BlockPos> algorithm;

        SortingSecond(Comparator<BlockPos> algorithm) {
            this.algorithm = algorithm;
        }
    }
}
