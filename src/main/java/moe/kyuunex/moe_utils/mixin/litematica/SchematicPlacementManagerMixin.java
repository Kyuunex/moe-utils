package moe.kyuunex.moe_utils.mixin.litematica;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import moe.kyuunex.moe_utils.modules.KeepSchematicLoaded;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SchematicPlacementManager.class, remap = false, priority = 7777)
public class SchematicPlacementManagerMixin {
    @Inject(method = "onClientChunkUnload",
            at = @At(
                    value = "HEAD"))
    public void chunkUnloadPatcher(int chunkX, int chunkZ, CallbackInfo ci) {
        Configs.Generic.LOAD_ENTIRE_SCHEMATICS.setBooleanValue(Modules.get().get(KeepSchematicLoaded.class).isActive());
    }
}
