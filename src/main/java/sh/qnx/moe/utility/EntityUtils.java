package sh.qnx.moe.utility;


import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

import java.util.List;


public class EntityUtils {
    private static final List<EntityType<?>> collidable =
            List.of(EntityTypes.ITEM, EntityTypes.TRIDENT, EntityTypes.ARROW, EntityTypes.AREA_EFFECT_CLOUD);

    public static boolean canPlaceIn(Entity entity) {
        return collidable.contains(entity.getType()) || entity.isRemoved() || entity.isSpectator();
    }

}
