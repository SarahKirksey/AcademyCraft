/**
 * 
 */
package cn.academy.ability.electro.entity;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.academy.ability.electro.client.render.RenderElecArc;
import cn.academy.ability.electro.entity.fx.EntityExcitedArc;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.MotionHandler;
import cn.liutils.api.entityx.motion.LifeTime;
import cn.liutils.util.EntityUtils;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.space.Motion3D;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegEntity
@RegEntity.HasRender
public class EntityWeakArc extends EntityArcBase {
	
	public static WARender render;
	
	public static class OffSync extends EntityWeakArc {

		public OffSync(EntityPlayer creator, SkillBase sag) {
			super(creator, sag);
			addEffectUpdate();
			
			this.addDaemonHandler(new MotionHandler(this) {
				@Override
				public void onCreated() {
					MovingObjectPosition mop = performTrace();
					//System.out.println(mop);
					if(mop == null) return;
					List<Entity> list = EntityUtils.getEntitiesAround(worldObj, mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, aoeRange, 
							GenericUtils.selectorLiving, OffSync.this, getSpawner(), mop.entityHit);
					Motion3D mo = new Motion3D(getSpawner(), true);
					Vec3 hitVec = mop.hitVec;
					if(mop.typeOfHit == MovingObjectType.BLOCK) {
						hitVec.xCoord += 0.5;
						hitVec.yCoord += 0.5;
						hitVec.zCoord += 0.5;
					} else {
						hitVec.yCoord += mop.entityHit.height / 1.6;
					}
					
					mo.move(worldObj.getWorldVec3Pool().getVecFromPool(posX, posY, posZ).distanceTo(hitVec));
					for(Entity e : list) {
						if(e != mop.entityHit)
						createExcArc(mo.getPosVec(worldObj), 
								worldObj.getWorldVec3Pool()
								.getVecFromPool(e.posX, e.posY, e.posZ));
					}
				}
				@SideOnly(Side.CLIENT)
				private void createExcArc(Vec3 v1, Vec3 v2) {
					//System.out.println("create " + worldObj.isRemote);
					worldObj.spawnEntityInWorld(new EntityExcitedArc(worldObj, v1, v2, 10));
				}
				@Override
				public void onUpdate() {}
				@Override
				public String getID() {
					return "Attack";
				}
				
			});
		}
		
	}
	
	float dmg;
	double igniteProb;
	double aoeRange;

	public EntityWeakArc(final EntityPlayer creator, SkillBase sag) {
		super(creator);
		AbilityData data = AbilityDataMain.getData(creator);
		int skillID = data.getSkillID(sag);
		dmg = 3 + data.getSkillLevel(skillID) * 0.5F + data.getLevelID() + 1;
		igniteProb = 0.1 + 0.03 * data.getSkillLevel(skillID) + data.getLevelID() * 0.05;
		aoeRange = 2 + data.getSkillLevel(skillID) * 0.2;
		
		if(isSync) {
			this.addDaemonHandler(new MotionHandler(this) {
				@Override
				public void onCreated() {
					MovingObjectPosition mop = performTrace(GenericUtils.selectorLiving);
					if(mop == null) return;
					
					if(mop.typeOfHit == MovingObjectType.BLOCK) {
						if(rand.nextDouble() < igniteProb) {
							if(worldObj.isAirBlock(mop.blockX, mop.blockY + 1, mop.blockZ)) {
								worldObj.setBlock(mop.blockX, mop.blockY + 1, mop.blockZ, Blocks.fire);
							}
						}
					} else {
						mop.entityHit.attackEntityFrom(DamageSource.causeMobDamage(getThrower()), dmg);
					}
					
					List<Entity> list = EntityUtils.getEntitiesAround(worldObj, mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, aoeRange, 
							GenericUtils.selectorLiving, EntityWeakArc.this, getThrower(), mop.entityHit);
					for(Entity e : list) {
						e.attackEntityFrom(DamageSource.causePlayerDamage(creator), dmg * 0.3F);
					}
				}
				
				private void createExcArc(Vec3 v1, Vec3 v2) {
					worldObj.spawnEntityInWorld(new EntityExcitedArc(worldObj, v1, v2, 10));
				}
				
				@Override
				public void onUpdate() {}
				@Override
				public String getID() {
					return "Attack";
				}
				
			});
		}
		
		this.addDaemonHandler(new LifeTime(this, 8));
	}

	public EntityWeakArc(World world) {
		super(world);
		randomDraw = false;
	}
	
	public static class WARender extends RenderElecArc {
		public WARender() {
			this.alpha = 0.6;
		}
	}

}