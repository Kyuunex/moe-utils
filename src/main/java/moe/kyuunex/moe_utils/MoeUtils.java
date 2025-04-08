package moe.kyuunex.moe_utils;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.Category;
import moe.kyuunex.moe_utils.commands.QuicksaveMapart;
import moe.kyuunex.moe_utils.modules.KeepSchematicLoaded;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import moe.kyuunex.moe_utils.modules.printer.Printer;
import moe.kyuunex.moe_utils.modules.MapHighlighter;

public class MoeUtils extends MeteorAddon {
    public static final Logger LOGGER = LoggerFactory.getLogger("Moe Utils");
    public static final Category CATEGORY = new Category("MoeUtils");

    public static void postInit() {
        mc = Minecraft.getInstance();
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Moe Utils loading :3");

        Modules.get().add(new Printer());
        Modules.get().add(new KeepSchematicLoaded());
        Modules.get().add(new MapHighlighter());

        Commands.add(new QuicksaveMapart());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "moe.kyuunex.moe_utils";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("Kyuunex", "moe-utils");
    }
}
