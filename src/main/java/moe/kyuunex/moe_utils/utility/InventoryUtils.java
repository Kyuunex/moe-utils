package moe.kyuunex.moe_utils.utility;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import java.util.function.Predicate;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.CraftingScreenHandler;

/**
 * The type Inv utils.
 */
public class InventoryUtils {
    public static final Predicate<ItemStack> IS_BLOCK =
        (itemStack) -> Item.BLOCK_ITEMS.containsValue(itemStack.getItem());

    /**
     * Swap slot.
     *
     * @param i      the
     * @param silent the silent
     */
    public static void swapSlot(int i, boolean silent) {
        assert mc.player != null;
        if (mc.player.getInventory().selectedSlot != i) {
            if (!silent) mc.player.getInventory().selectedSlot = i;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i));
        }
    }

    public static int findEmptySlotInHotbar(int i) {
        if (mc.player != null) {
            for (var ref =
                 new Object() {
                     int i = 0;
                 };
                 ref.i < 9;
                 ref.i++) {
                if (mc.player.getInventory().getStack(getHotbarOffset() + ref.i).isEmpty()) {
                    return ref.i;
                }
            }
        }
        return i;
    }

    public static int getInventoryOffset() {
        assert mc.player != null;
        return mc.player.currentScreenHandler.slots.size() == 46
            ? mc.player.currentScreenHandler instanceof CraftingScreenHandler ? 10 : 9
            : mc.player.currentScreenHandler.slots.size() - 36;
    }

    public static int getHotbarOffset() {
        return getInventoryOffset() + 27;
    }
}
