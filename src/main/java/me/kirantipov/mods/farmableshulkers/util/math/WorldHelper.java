package me.kirantipov.mods.farmableshulkers.util.math;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Backports some useful methods of the `World` from the newest versions.
 */
public final class WorldHelper {
    public static boolean isDirectionSolid(World world, BlockPos pos, Entity entity, Direction direction) {
        if (World.isHeightInvalid(pos)) {
            return false;
        }

        Chunk chunk = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
        if (chunk == null) {
            return false;
        }

        BlockState state = chunk.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(world, pos, EntityContext.of(entity));
        return Block.isFaceFullSquare(shape, direction);
    }

    public static boolean isSpaceEmpty(World world, Entity entity, Box box) {
        return isSpaceEmpty(world, entity, box, x -> true);
    }

    public static boolean isSpaceEmpty(World world, Entity entity, Box box, Predicate<Entity> predicate) {
        return getCollisions(world, entity, box, predicate).allMatch(VoxelShape::isEmpty);
    }

    public static Stream<VoxelShape> getCollisions(World world, Entity entity, Box box, Predicate<Entity> predicate) {
        Box expandedBox = box.expand(1.0E-7D);
        List<Entity> except = world.getEntities(entity, box, predicate.and(x -> {
            if (x.collides() && x.getBoundingBox().intersects(expandedBox)) {
                return entity != null && entity.isConnectedThroughVehicle(x);
            }

            return true;
        }));

        return world.getCollisions(entity, box, except.size() == 0 ? Collections.emptySet() : new HashSet<>(except));
    }
}
