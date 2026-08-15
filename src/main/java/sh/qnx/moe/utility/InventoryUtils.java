package sh.qnx.moe.utility;

import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

import static meteordevelopment.meteorclient.MeteorClient.mc;


public class InventoryUtils {

    public static final Predicate<ItemStack> IS_BLOCK = (itemStack) -> Item.BY_BLOCK.containsValue(
        itemStack.getItem());


    public static void swapSlot(int i) {
        assert mc.player != null;
        mc.player.getInventory().setSelectedSlot(i);
        mc.player.connection.send(new ServerboundSetCarriedItemPacket(i));
    }

    public static int findEmptySlotInHotbar(int i) {
        if (mc.player != null) {
            for (int k = 0; k < 9; k++) {
                if (mc.player.getInventory().getItem(getHotbarOffset() + k).isEmpty()) {
                    return k;
                }
            }
        }
        return i;
    }

    public static int getInventoryOffset() {
        assert mc.player != null;
        return mc.player.containerMenu.slots.size() == 46 ?
            mc.player.containerMenu instanceof CraftingMenu ? 10 : 9
            : mc.player.containerMenu.slots.size() - 36;
    }

    public static int getHotbarOffset() {
        return getInventoryOffset() + 27;
    }

    public static void swapToHotbar(int slot, int hot) {
        if (mc.player == null || mc.gameMode == null) {
            return;
        }

        mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, slot, hot,
            ContainerInput.SWAP, mc.player);
    }

}
