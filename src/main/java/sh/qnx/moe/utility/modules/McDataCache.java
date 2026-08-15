package sh.qnx.moe.utility.modules;


import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.MapColor;

import java.util.HashMap;

public class McDataCache {

    protected static HashMap<Item, MapColor> ITEM_TO_COLOR = new HashMap<>();

    public static MapColor getColor(ItemStack stack) {
        return getColor(stack.getItem());
    }

    public static MapColor getColor(Item item) {
        return ITEM_TO_COLOR.getOrDefault(item, MapColor.NONE);
    }

}
