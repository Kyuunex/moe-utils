package moe.kyuunex.moe_utils.utility;

import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class EntityUtils {
    private static final List<EntityType<?>> collidable =
        List.of(EntityType.ITEM, EntityType.TRIDENT, EntityType.ARROW, EntityType.AREA_EFFECT_CLOUD);

    public static boolean canPlaceIn(Entity entity) {
        return collidable.contains(entity.getType()) || entity.isRemoved() || entity.isSpectator();
    }
}
