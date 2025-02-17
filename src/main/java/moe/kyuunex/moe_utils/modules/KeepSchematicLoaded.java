package moe.kyuunex.moe_utils.modules;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.utils.Utils;
import moe.kyuunex.moe_utils.MoeUtils;
import meteordevelopment.meteorclient.systems.modules.Module;

public class KeepSchematicLoaded extends Module {
    public KeepSchematicLoaded() {
        super(
            MoeUtils.CATEGORY,
            "keep-schematic-loaded",
            "Prevents Litematica from unloading the schematic."
        );
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        return theme.label(
            "This modifies Litematica to never unload the schematic chunks when you are too far away. " +
            "Effectively extending the schematic \"render distance\". Useful for very very large maparts. " +
            "May bump your CPU usage up and/or lead to stuttering if your hardware is underpowered.",
            Utils.getWindowWidth() / 4.0
        );
    }
}
