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

    /**
     * Preserves the direction of the BlockPos offset for future use.
     *
     * @param pos Initial position.
     * @param offsetDirection The offset direction.
     *
     * @return Offset position.
     */
    public static DirectionalBlockPos offset(BlockPos pos, Direction offsetDirection) {
        int dX = offsetDirection.getOffsetX();
        int dY = offsetDirection.getOffsetY();
        int dZ = offsetDirection.getOffsetZ();
        
        return new DirectionalBlockPos(pos.getX() + dX, pos.getY() + dY, pos.getZ() + dZ, offsetDirection);
    }
}
