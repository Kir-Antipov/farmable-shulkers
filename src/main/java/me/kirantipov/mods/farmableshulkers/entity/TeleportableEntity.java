package me.kirantipov.mods.farmableshulkers.entity;

/**
 * Represents an entity that can be teleported.
 */
public interface TeleportableEntity {
    /**
     * Refreshes the position of the entity after teleportation.
     *
     * @param x X coordinate.
     * @param y Y coordinate.
     * @param z Z coordinate.
     */
    void refreshPositionAfterTeleport(double x, double y, double z);
}
