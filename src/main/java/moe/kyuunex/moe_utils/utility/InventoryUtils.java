package moe.kyuunex.moe_utils.utility;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import java.util.function.Predicate;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.inventory.CraftingMenu;

/**
 * The type Inv utils.
 */
public class InventoryUtils {
    public static final Predicate<ItemStack> IS_BLOCK =
        (itemStack) -> Item.BY_BLOCK.containsValue(itemStack.getItem());

    /**
     * Swap slot.
     *
     * @param i      the
     * @param silent the silent
     */
    public static void swapSlot(int i, boolean silent) {
        if (mc.player == null) return;
        if (mc.getConnection() == null) return;

        if (mc.player.getInventory().getSelectedSlot() != i) {
            if (!silent) mc.player.getInventory().setSelectedSlot(i);
            mc.getConnection().getConnection().send(new ServerboundSetCarriedItemPacket(i), null, true);
        }
    }

    public static int findEmptySlotInHotbar(int preferred) {
        if (mc.player == null) return preferred;
        if (mc.player.getInventory().getItem(preferred).isEmpty()) return preferred;

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).isEmpty()) return i;
        }
        return preferred;
    }

    public static int getInventoryOffset() {
        assert mc.player != null;
        return mc.player.containerMenu.slots.size() == 46
            ? mc.player.containerMenu instanceof CraftingMenu ? 10 : 9
            : mc.player.containerMenu.slots.size() - 36;
    }

    public static int getHotbarOffset() {
        return getInventoryOffset() + 27;
    }
}
