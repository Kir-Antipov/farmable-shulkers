package me.kirantipov.mods.farmableshulkers.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ProjectileUtil;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RayTraceContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

/**
 * Old implementation of shulker bullets has two problems:
 *
 * <ul>
 *     <li>For some reason, they aren't implemented as proper projectiles.</li>
 *     <li>
 *         They can only hit an original target,
 *         even if another entity got in the way.
 *     </li>
 * </ul>
 *
 * This mixin fixes both issues.
 */
@Mixin(ShulkerBulletEntity.class)
public abstract class MixinShulkerBulletEntity extends Entity {
    /**
     * The owner of the bullet, if any; otherwise, null.
     */
    @Shadow
    private LivingEntity owner;

    /**
     * Uuid of the bullet's owner, if any; otherwise, null.
     */
    @Shadow
    private UUID ownerUuid;

    /**
     * Indicates whether the bullet has left its owner.
     */
    private boolean leftOwner;


    private MixinShulkerBulletEntity(EntityType<?> entityType, World world) {
        super(entityType, world);
    }


    /**
     * Returns the owner of the bullet, if any; otherwise, null.
     * @return The owner of the bullet, if any; otherwise, null.
     */
    private Entity getOwner() {
        if (this.owner == null && this.ownerUuid != null && this.world instanceof ServerWorld) {
            Entity entityWithOwnerUuid = ((ServerWorld)this.world).getEntity(this.ownerUuid);
            if (entityWithOwnerUuid instanceof LivingEntity) {
                this.owner = (LivingEntity)entityWithOwnerUuid;
            }
        }

        return this.owner;
    }

    /**
     * Calculates whether the bullet should leave its owner.
     *
     * @return true if the bullet should leave its owner; otherwise, false.
     */
    private boolean shouldLeaveOwner() {
        Entity owner = this.getOwner();
        if (owner != null) {
            List<Entity> entities = this.world.getEntities(this, this.getBoundingBox().stretch(this.getVelocity()).expand(1.0D), x -> x != this && !x.isSpectator() && x.collides());

            for (Entity target : entities) {
                if (target.getRootVehicle() == owner.getRootVehicle()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Calculates whether the bullet can hit the specified entity.
     *
     * @param entity The entity.
     *
     * @return true if the bullet can hit the specified entity; otherwise, false.
     */
    private boolean canHit(Entity entity) {
        if (!entity.noClip && !entity.isSpectator() && entity.isAlive() && entity.collides()) {
            Entity owner = this.getOwner();
            return owner == null || this.leftOwner || !owner.isConnectedThroughVehicle(entity);
        } else {
            return false;
        }
    }

    /**
     * Recalculates the value of the `leftOwner` property.
     *
     * @param ci The callback info.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (!this.leftOwner) {
            this.leftOwner = this.shouldLeaveOwner();
        }
    }

    /**
     * Redefines the collision search logic so the bullet can hit not only the original target,
     * but any entity that got in its way.
     *
     * @return The hit result.
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ProjectileUtil;getCollision(Lnet/minecraft/entity/Entity;ZZLnet/minecraft/entity/Entity;Lnet/minecraft/world/RayTraceContext$ShapeType;)Lnet/minecraft/util/hit/HitResult;"))
    private HitResult getCollisionRedirect(Entity entity, boolean bl, boolean bl2, Entity entity2, RayTraceContext.ShapeType shapeType) {
        Vec3d velocity = entity.getVelocity();
        World world = entity.world;
        Vec3d pos = entity.getPos();
        Vec3d nextPos = pos.add(velocity);

        HitResult hitResult = world.rayTrace(new RayTraceContext(pos, nextPos, RayTraceContext.ShapeType.COLLIDER, RayTraceContext.FluidHandling.NONE, entity));
        if (hitResult.getType() != HitResult.Type.MISS) {
            nextPos = hitResult.getPos();
        }

        HitResult entityHitResult = ProjectileUtil.getEntityCollision(world, entity, pos, nextPos, entity.getBoundingBox().stretch(entity.getVelocity()).expand(1.0D), this::canHit);
        if (entityHitResult != null) {
            hitResult = entityHitResult;
        }

        return hitResult;
    }
}