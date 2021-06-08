package me.kirantipov.mods.farmableshulkers.mixin;

import me.kirantipov.mods.farmableshulkers.entity.TeleportableEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * This mixin:
 *
 * <ul>
 *     <li>
 *         Adds `refreshPositionAfterTeleport` method to the `Entity` class.
 *     </li>
 * </ul>
 */
@Mixin(Entity.class)
public abstract class MixinEntity implements TeleportableEntity {
    @Shadow
    public float yaw;

    @Shadow
    public float pitch;

    @Shadow
    public abstract void setPositionAndAngles(double x, double y, double z, float yaw, float pitch);

    /**
     * {@inheritDoc}
     */
    public void refreshPositionAfterTeleport(double x, double y, double z) {
        this.setPositionAndAngles(x, y, z, this.yaw, this.pitch);
    }
}
