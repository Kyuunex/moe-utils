package moe.utils.modules;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.utils.Utils;
import moe.utils.MoeUtils;
import meteordevelopment.meteorclient.systems.modules.Module;

public class LoadEntireSchematics extends Module {

    public LoadEntireSchematics() {
        super(
            MoeUtils.CATEGORY,
            "load-entire-schem",
            "Load entire schematics, instead of just render distance sections."
        );
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        return theme.label(
            "This forces Litematica to load entire schematics " +
                "instead of just what's inside your render distance. " +
                "Useful for very very large maparts. " +
                "May bump your CPU usage up and/or lead to stuttering if your hardware is underpowered.",
            Utils.getWindowWidth() / 4.0
        );
    }
}
