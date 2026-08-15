package sh.qnx.moe;

import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.elements.MeteorTextHud;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import sh.qnx.moe.commands.QuicksaveMapart;
import sh.qnx.moe.modules.printer.Printer;
import sh.qnx.moe.modules.LoadEntireSchematics;
import sh.qnx.moe.modules.MapHighlighter;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoeUtils extends MeteorAddon {

    public static final Logger LOGGER = LoggerFactory.getLogger("Moe Utils");
    public static final Category CATEGORY = new Category(
        "Moe Utils", () -> DisplayItemUtils.toStack(Items.MAP)
    );

    @Override
    public void onInitialize() {
        LOGGER.info("Moe Utils loading :3");

        Modules.get().add(new Printer());
        Modules.get().add(new LoadEntireSchematics());
        Modules.get().add(new MapHighlighter());

        Commands.add(new QuicksaveMapart());

        MeteorTextHud.INFO.addPreset(
            "Map Quad",
            textHud -> {
                textHud.text.set("#1{map_pos.x}, {map_pos.z}");
                textHud.updateDelay.set(0);
            });

        MeteorTextHud.INFO.addPreset(
            "Printer Interest Point",
            textHud -> {
                textHud.text.set("I.P: #1{printer.rp}");
                textHud.updateDelay.set(0);
            });

    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "sh.qnx.moe";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("qnxt", "moe-utils");
    }
}
