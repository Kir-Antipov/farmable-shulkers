package me.kirantipov.mods.farmableshulkers.mixin;

import me.kirantipov.mods.farmableshulkers.entity.ColorableEntity;
import me.kirantipov.mods.farmableshulkers.entity.TeleportableEntity;
import me.kirantipov.mods.farmableshulkers.util.math.DirectionalBlockPos;
import me.kirantipov.mods.farmableshulkers.util.math.WorldHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
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
import net.minecraft.world.World;
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


    protected MixinShulkerEntity(EntityType<? extends GolemEntity> entityType, World world) {
        super(entityType, world);
    }


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
    public DyeColor getColor() {
        Byte colorByte = this.dataTracker.get(COLOR);
        return colorByte != 16 && colorByte <= 15 ? DyeColor.byId(colorByte) : null;
    }

    /**
     * {@inheritDoc}
     */
    public void setColor(DyeColor color) {
        this.dataTracker.set(COLOR, (byte)color.getId());
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

    /**
     * Preserves the direction of the BlockPos offset for future use.
     *
     * @param pos Initial position.
     * @param direction The offset direction.
     *
     * @return Offset position.
     */
    @Redirect(method = { "tick", "method_7127" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;offset(Lnet/minecraft/util/math/Direction;)Lnet/minecraft/util/math/BlockPos;"))
    protected BlockPos preserveBlockPosDirection(BlockPos pos, Direction direction) {
        return DirectionalBlockPos.offset(pos, direction);
    }

    /**
     * Replaces the original `isTopSolid` check with the appropriate logic.
     *
     * @param world The world.
     * @param blockPos The BlockPos.
     * @param entity The shulker.
     *
     * @return true if the block is attachable; otherwise, false.
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isTopSolid(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)Z", ordinal = 0))
    protected boolean isFirstBlockAttachableOnTick(World world, BlockPos blockPos, Entity entity) {
        Direction direction = ((DirectionalBlockPos)blockPos).getDirection();
        return isBlockAttachable(blockPos.offset(direction.getOpposite()), direction);
    }

    /**
     * Same as `isFirstBlockAttachableOnTick`.
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isTopSolid(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)Z", ordinal = 1))
    protected boolean isSecondBlockAttachableOnTick(World world, BlockPos blockPos, Entity entity) {
        Direction direction = ((DirectionalBlockPos)blockPos).getDirection();
        return isBlockAttachable(blockPos.offset(direction.getOpposite()), direction);
    }

    /**
     * The third check that the block is a valid attachment point is useless.
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isTopSolid(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)Z", ordinal = 2))
    protected boolean discardThirdIsBlockAttachableCheckOnTick(World world, BlockPos pos, Entity entity) {
        return false;
    }

    /**
     * Same as `isFirstBlockAttachableOnTick`.
     */
    @Redirect(method = "method_7127", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isTopSolid(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)Z"))
    protected boolean isBlockAttachableOnTeleport(World world, BlockPos blockPos, Entity entity) {
        Direction direction = ((DirectionalBlockPos)blockPos).getDirection();
        return isBlockAttachable(blockPos.offset(direction.getOpposite()), direction);
    }

    /**
     * Returns true if the block is empty; otherwise, false.
     *
     * @param pos The position to check.
     * @return true if the block is empty; otherwise, false.
     */
    private boolean isBlockEmpty(BlockPos pos) {
        BlockState blockState = this.world.getBlockState(pos);
        return blockState.isAir() || (blockState.getBlock() == Blocks.MOVING_PISTON && pos.equals(this.getBlockPos()));
    }

    /**
     * Creates intersection box of the shulker.
     *
     * @param direction The side to which the shulker opens.
     * @param prevOffset Shulker's opening progress on the previous step.
     * @param offset Shulker's opening progress.
     *
     * @return An intersection box of the shulker.
     */
    private static Box createIntersectionBox(Direction direction, float prevOffset, float offset) {
        double max = Math.max(prevOffset, offset);
        double min = Math.min(prevOffset, offset);
        Box testBox = new Box(BlockPos.ORIGIN);
        return testBox.stretch(
            direction.getOffsetX() * max,
            direction.getOffsetY() * max,
            direction.getOffsetZ() * max
        ).shrink(
            -direction.getOffsetX() * (1.0D + min),
            -direction.getOffsetY() * (1.0D + min),
            -direction.getOffsetZ() * (1.0D + min)
        );
    }

    /**
     * Returns true if the given block is attachable; otherwise, false.
     *
     * @param pos The position to check.
     * @param direction The direction to check.
     * @return true if the given block is attachable; otherwise, false.
     */
    private boolean isBlockAttachable(BlockPos pos, Direction direction) {
        if (this.isBlockEmpty(pos)) {
            Direction opposite = direction.getOpposite();
            if (WorldHelper.isDirectionSolid(this.world, pos.offset(direction), this, opposite)) {
                Box box = createIntersectionBox(opposite, -1.0F, 1.0F).offset(pos).contract(1.0E-6D);
                return WorldHelper.isSpaceEmpty(this.world, this, box);
            }
        }

        return false;
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

                DyeColor dyeColor = ((ColorableEntity)this).getColor();
                if (dyeColor != null) {
                    ((ColorableEntity)shulkerEntity).setColor(dyeColor);
                }

                ((TeleportableEntity)shulkerEntity).refreshPositionAfterTeleport(pos.x, pos.y, pos.z);
                this.world.spawnEntity(shulkerEntity);
            }
        }
    }
}
