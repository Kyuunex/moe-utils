package moe.utils.utility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class MathUtils {

    public static double xzDistanceBetween(Vec3 s, BlockPos e) {
        var dx = s.x - e.getX();
        var dz = s.z - e.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static double xzDistanceBetween(BlockPos s, BlockPos e) {
        var dx = s.getX() - e.getX();
        var dz = s.getZ() - e.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
}
