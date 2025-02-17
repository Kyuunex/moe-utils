package moe.kyuunex.moe_utils.mixin.litematica;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import moe.kyuunex.moe_utils.modules.KeepSchematicLoaded;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import meteordevelopment.meteorclient.systems.modules.Modules;

@Mixin(value = SchematicPlacementManager.class, remap = false)
public class SchematicPlacementManagerMixin {
    @Redirect(method = "onClientChunkUnload",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/malilib/config/options/ConfigBoolean;getBooleanValue()Z"))
    public boolean chunkUnloadPatcher(ConfigBoolean instance) {
        return Modules.get().get(KeepSchematicLoaded.class).isActive();
    }
}
