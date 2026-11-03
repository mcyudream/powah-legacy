package owmii.powah.item;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public final class ChargedSnowball extends EntityThrowable {
    public ChargedSnowball(World world) { super(world); }
    public ChargedSnowball(World world, EntityLivingBase thrower) { super(world, thrower); }
    @Override protected void onImpact(RayTraceResult hit) {
        if (world.isRemote) return;
        if (hit.entityHit != null) hit.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, getThrower()), hit.entityHit instanceof EntityBlaze ? 3 : 0);
        world.addWeatherEffect(new EntityLightningBolt(world, posX, posY, posZ, false)); world.setEntityState(this, (byte) 3); setDead();
    }
}
