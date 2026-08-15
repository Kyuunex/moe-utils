package sh.qnx.moe.utility.packets;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

public record BlockPlacement(InteractionHand hand, BlockHitResult hitResult) {

}
