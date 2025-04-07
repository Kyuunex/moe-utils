package moe.kyuunex.moe_utils.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.block.MapColor;
import net.minecraft.command.CommandSource;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class QuicksaveMapart extends Command {
    private final Path mapPath = Path.of(MeteorClient.FOLDER.getPath()).resolve("maps");

    public QuicksaveMapart() {
        super("quicksave-map", "Quickly save a map you are holding as image");

        try {
            Files.createDirectories(mapPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(this::quickSave);
    }

    private int quickSave(CommandContext<CommandSource> context) {
        if (mc.player == null) return -1;
        if (mc.player.getMainHandStack().getItem() != Items.FILLED_MAP) return -1;

        int mapId = mc.player.getMainHandStack().get(DataComponentTypes.MAP_ID).id();

        MapState mapState = FilledMapItem.getMapState(mc.player.getMainHandStack(), mc.world);

        BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);

        for (int i = 0; i < 128; ++i) {
            for (int j = 0; j < 128; ++j) {
                int k = j + i * 128;
                image.setRGB(j, i, MapColor.getRenderColor(Objects.requireNonNull(mapState).colors[k]));
            }
        }

        Path outputPath = mapPath.resolve(
            mapId + "_"
                + (mc.getCurrentServerEntry() != null && !mc.isInSingleplayer()
                ? mc.getCurrentServerEntry().address
                : "OFFLINE")
                + "_single"
                + ".png");

        File outputFile = new File(outputPath.toString());

        try {
            ImageIO.write(image, "PNG", outputFile);
            info("Successfully saved " + outputPath);
        } catch (IOException ignored) {
            info("Error saving");
        }

        return SINGLE_SUCCESS;
    }
}
