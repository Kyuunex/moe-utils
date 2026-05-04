package moe.utils.utility.modules;

import net.minecraft.world.level.Level;

import java.util.concurrent.Callable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@SuppressWarnings("unused")
public enum DimensionCheck {
    OW(() -> {
        if (mc.level == null) {
            return false;
        }

        return mc.level.dimension() == Level.OVERWORLD;
    }),
    OW_OR_NETHER(() -> {
        if (mc.level == null) {
            return false;
        }

        return mc.level.dimension() == Level.NETHER || mc.level.dimension() == Level.OVERWORLD;
    }),
    OW_OR_END(() -> {
        if (mc.level == null) {
            return false;
        }

        return mc.level.dimension() == Level.OVERWORLD || mc.level.dimension() == Level.END;
    }),
    NETHER(() -> {
        if (mc.level == null) {
            return false;
        }

        return mc.level.dimension() == Level.NETHER;
    }),
    NETHER_OR_END(() -> {
        if (mc.level == null) {
            return false;
        }

        return mc.level.dimension() == Level.NETHER || mc.level.dimension() == Level.END;
    }),
    END(() -> {
        if (mc.level == null) {
            return false;
        }

        return mc.level.dimension() == Level.END;
    });

    public final Callable<Boolean> check;

    DimensionCheck(Callable<Boolean> check) {
        this.check = check;
    }
}
