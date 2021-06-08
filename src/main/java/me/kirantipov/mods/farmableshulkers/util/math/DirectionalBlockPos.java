package me.kirantipov.mods.farmableshulkers.util.math;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Allows to specify a direction of the BlockPos.
 */
public class DirectionalBlockPos extends BlockPos {
    private final Direction direction;

    public DirectionalBlockPos(double d, double e, double f, Direction direction) {
        super(d, e, f);
        this.direction = direction;
    }

    /**
     * Returns preserved direction of the BlockPos.
     * @return Preserved direction of the BlockPos.
     */
    public Direction getDirection() {
        return this.direction;
    }
}
