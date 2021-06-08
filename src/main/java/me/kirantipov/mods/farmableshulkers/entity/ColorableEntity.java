package me.kirantipov.mods.farmableshulkers.entity;

import net.minecraft.util.DyeColor;

/**
 * Represents an entity that can be colored.
 */
public interface ColorableEntity {
    /**
     * Returns the actual color of the entity, if any;
     * otherwise, null;
     *
     * @return The actual color of the entity, if any;
     * otherwise, null;
     */
    DyeColor getColor();

    /**
     * Applies the specified color to the entity.
     *
     * @param color The color.
     */
    void setColor(DyeColor color);
}
