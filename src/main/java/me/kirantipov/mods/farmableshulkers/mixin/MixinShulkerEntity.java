package me.kirantipov.mods.farmableshulkers.mixin;

import me.kirantipov.mods.farmableshulkers.entity.ColorableEntity;
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


    /**
     * Attempts to teleport the shulker to a random location.
     *
     * @return true if the shulker was teleported; otherwise, false.
     */
    @Shadow
    protected abstract boolean tryTeleport();

    /**
     * Returns true if the shulker is closed; otherwise, false.
     * @return true if the shulker is closed; otherwise, false.
     */
    @Shadow
    protected abstract boolean isClosed();


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
     * Returns true if the block is empty; otherwise, false.
     *
     * @param pos The position to check.
     * @return true if the block is empty; otherwise, false.
     */
    private boolean isBlockEmpty(BlockPos pos) {
        BlockState blockState = this.world.getBlockState(pos);
        return blockState.isAir() || (blockState.isOf(Blocks.MOVING_PISTON) && pos.equals(this.getBlockPos()));
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
            if (this.world.isDirectionSolid(pos.offset(direction), this, opposite)) {
                Box box = createIntersectionBox(opposite, -1.0F, 1.0F).offset(pos).contract(1.0E-6D);
                return this.world.isSpaceEmpty(this, box);
            }
        }

        return false;
    }

    /**
     * Overrides the logic by which the shulker determines
     * whether it can use a block as an anchorage position.
     *
     * @param pos The position to check.
     * @param direction The direction to check.
     * @param cir The callback info.
     */
    @Inject(method = "canStay", at = @At("HEAD"), cancellable = true)
    private void isBlockAttachable(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(isBlockAttachable(pos, direction));
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
            int i = this.world.getEntitiesByType(EntityType.SHULKER, box.expand(8.0D), Entity::isAlive).size();
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

                shulkerEntity.refreshPositionAfterTeleport(pos.x, pos.y, pos.z);
                this.world.spawnEntity(shulkerEntity);
            }
        }
    }
}
