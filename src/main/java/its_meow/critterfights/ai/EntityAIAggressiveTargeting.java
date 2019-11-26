package its_meow.critterfights.ai;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EntitySelectors;

public class EntityAIAggressiveTargeting<T extends EntityLivingBase> extends EntityAITarget {

    protected final Class<T> targetClass;
    protected T targetEntity;
    protected final EntityAINearestAttackableTarget.Sorter sorter;

    public EntityAIAggressiveTargeting(EntityCreature creature, Class<T> targetClass, boolean checkSight) {
        super(creature, checkSight);
        this.targetClass = targetClass;
        this.setMutexBits(1);
        this.sorter = new EntityAINearestAttackableTarget.Sorter(creature);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean shouldExecute() {
        if(this.targetClass == EntityPlayer.class || this.targetClass == EntityPlayerMP.class) {
            this.targetEntity = (T) this.taskOwner.world.getNearestAttackablePlayer(this.taskOwner.posX, this.taskOwner.posY + (double) this.taskOwner.getEyeHeight(), this.taskOwner.posZ, this.getTargetDistance(), this.getTargetDistance(), e -> 1D, e -> EntitySelectors.NOT_SPECTATING.apply(e));
            return this.targetEntity != null;
            
        } else {
            List<T> list = this.taskOwner.world.<T>getEntitiesWithinAABB(this.targetClass, this.taskOwner.getEntityBoundingBox().grow(4D), e -> e != this.taskOwner && EntitySelectors.NOT_SPECTATING.apply(e));

            if(list.isEmpty()) {
                return false;
            } else {
                Collections.sort(list, this.sorter);
                this.targetEntity = list.get(0);
                return true;
            }
        }
    }

    @Override
    public void startExecuting() {
        this.taskOwner.setAttackTarget(this.targetEntity);
        super.startExecuting();
    }

}
