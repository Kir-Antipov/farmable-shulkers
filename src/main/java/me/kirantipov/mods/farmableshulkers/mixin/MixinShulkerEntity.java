package me.kirantipov.mods.farmableshulkers.mixin;

import me.kirantipov.mods.farmableshulkers.entity.ColorableEntity;
import me.kirantipov.mods.farmableshulkers.util.math.DirectionalBlockPos;
import me.kirantipov.mods.farmableshulkers.entity.TeleportableEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * This mixin:
 *
 * <ul>
 *     <li>Implements shulker duplication logic.</li>
 *     <li>
 *         Fixes a bug of the original game that caused shulkers
 *         to preserve their original coordinates after teleportation between dimensions.
 *     </li>
 *     <li>Shulkers can no longer teleport to non-square surfaces.</li>
 * </ul>
 */
@Mixin(ShulkerEntity.class)
public abstract class MixinShulkerEntity extends GolemEntity implements ColorableEntity {
    @Final
    @Shadow
    protected static TrackedData<Byte> COLOR;

    @Final
    @Shadow
    protected static TrackedData<Optional<BlockPos>> ATTACHED_BLOCK;


    @Shadow
    protected abstract boolean method_7127();

    /**
     * Attempts to teleport the shulker to a random location.
     *
     * @return true if the shulker was teleported; otherwise, false.
     */
    protected boolean tryTeleport() { return method_7127(); }


    @Shadow
    protected abstract boolean method_7124();

    /**
     * Returns true if the shulker is closed; otherwise, false.
     * @return true if the shulker is closed; otherwise, false.
     */
    protected boolean isClosed() { return method_7124(); }


    /**
     * {@inheritDoc}
     */
    @Shadow
    public abstract DyeColor getColor();

    /**
     * {@inheritDoc}
     */
    public void setColor(DyeColor color) {
        this.dataTracker.set(COLOR, (byte)color.getId());
    }


    protected MixinShulkerEntity(EntityType<? extends GolemEntity> entityType, World world) {
        super(entityType, world);
    }


    /**
     * We already have a property that reflects a position of the entity.
     * Why not add another one and then forget to process it?
     *
     * @param x X coordinate.
     * @param y Y coordinate.
     * @param z Z coordinate.
     * @param ci The callback info.
     */
    @Inject(method = "updatePosition(DDD)V", at = @At(value = "HEAD"))
    protected void setAttachedBlock(double x, double y, double z, CallbackInfo ci) {
        if (this.dataTracker != null && this.age == 0) {
            Optional<BlockPos> pos = this.dataTracker.get(ATTACHED_BLOCK);
            Optional<BlockPos> newPos = Optional.of(new BlockPos(x, y, z));
            if (!newPos.equals(pos)) {
                this.dataTracker.set(ATTACHED_BLOCK, newPos);
            }
        }
    }

    @Redirect(method = "method_7127", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;offset(Lnet/minecraft/util/math/Direction;)Lnet/minecraft/util/math/BlockPos;"))
    protected BlockPos preserveBlockPosDirection(BlockPos pos, Direction direction) {
        int dX = direction.getOffsetX();
        int dY = direction.getOffsetY();
        int dZ = direction.getOffsetZ();
        direction = direction.getOpposite();
        return new DirectionalBlockPos(pos.getX() + dX, pos.getY() + dY, pos.getZ() + dZ, direction);
    }

    @Redirect(method = "method_7127", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isTopSolid(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)Z"))
    protected boolean canAttachTo(World world, BlockPos blockPos, Entity entity) {
        if (World.isHeightInvalid(blockPos)) {
            return false;
        }

        Chunk chunk = world.getChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4, ChunkStatus.FULL, false);
        if (chunk == null) {
            return false;
        }

        BlockState state = chunk.getBlockState(blockPos);
        VoxelShape shape = state.getCollisionShape(world, blockPos, EntityContext.of(entity));
        Direction direction = ((DirectionalBlockPos)blockPos).getDirection();
        return Block.isFaceFullSquare(shape, direction);
    }

    /**
     * Implements shulker duplication logic.
     *
     * @param damageSource The damage source.
     * @param damageAmount The damage amount.
     * @param cir The callback info.
     */
    @Inject(method = "damage", at = @At("RETURN"), cancellable = true)
    protected void onDamage(DamageSource damageSource, float damageAmount, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            Entity entitySource = damageSource.getSource();
            if (entitySource != null && entitySource.getType() == EntityType.SHULKER_BULLET) {
                this.spawnNewShulker();
            }
        }
    }

    /**
     * Attempts to teleport the shulker and spawn a new one at its original location.
     */
    private void spawnNewShulker() {
        Vec3d pos = this.getPos();
        Box box = this.getBoundingBox();
        if (!this.isClosed() && this.tryTeleport()) {
            int i = this.world.getEntities(EntityType.SHULKER, box.expand(8.0D), Entity::isAlive).size();
            float f = (float)(i - 1) / 5.0F;
            if (this.world.random.nextFloat() >= f) {
                ShulkerEntity shulkerEntity = EntityType.SHULKER.create(this.world);
                if (shulkerEntity == null) {
                    return;
                }

                DyeColor dyeColor = this.getColor();
                if (dyeColor != null) {
                    ((ColorableEntity)shulkerEntity).setColor(dyeColor);
                }

                ((TeleportableEntity)shulkerEntity).refreshPositionAfterTeleport(pos.x, pos.y, pos.z);
                this.world.spawnEntity(shulkerEntity);
            }
        }
    }
}
