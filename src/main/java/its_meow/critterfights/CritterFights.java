package its_meow.critterfights;

import java.util.HashSet;
import java.util.Set;

import its_meow.critterfights.ai.EntityAIAggressiveTargeting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIAttackRanged;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = CritterFights.MODID)
@Mod(modid = CritterFights.MODID, name = CritterFights.NAME, version = CritterFights.VERSION)
public class CritterFights {

    public static final String MODID = "critterfights";
    public static final String NAME = "Critter Fights";
    public static final String VERSION = "@VERSION@";

    @EventHandler
    public void serverInit(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandCFight());
        event.registerServerCommand(new CommandCAggro());
    }

    @SubscribeEvent
    public static void onEntityLoaded(EntityJoinWorldEvent event) {
        String id = CritterFights.getAITag(event.getEntity());
        if(!id.isEmpty() && event.getEntity() instanceof EntityCreature) {
            EntityCreature entity = (EntityCreature) event.getEntity();
            float damage = CritterFights.getDamageTag(entity);
            if(entity.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.ATTACK_DAMAGE) == null) {
               entity.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(damage == 0 ? 1D : damage);
            } else {
                entity.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(damage);
            }
            entity.getAttributeMap().onAttributeModified(entity.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE));
            NBTTagCompound tag2 = new NBTTagCompound();
            tag2.setString("id", id);
            if(CritterFights.isPlayerID(id)) {
                CritterFights.checkAndAddAttackAI(entity);
                CritterFights.addTargetingAI(entity, EntityPlayer.class);
            } else {
                Entity entity2 = AnvilChunkLoader.readWorldEntityPos(tag2.copy(), event.getEntity().world, event.getEntity().posX, event.getEntity().posY, event.getEntity().posZ, false);
                if(entity2 instanceof EntityLiving) {
                    Class<? extends EntityLiving> targetClass = ((EntityLiving)entity2).getClass();
                    entity2.setDead();
                    if(entity2 instanceof EntityLiving) {
                        CritterFights.checkAndAddAttackAI(entity);
                        CritterFights.addTargetingAI(entity, targetClass);
                    }
                } else if(entity2 != null) {
                    entity2.setDead();
                }
            }
        }
    }

    /*
     * Helpers
     */

    public static void addAITag(Entity el, String entityID) {
        el.getEntityData().setString("critterfights_aggro", entityID);
    }

    public static String getAITag(Entity e) {
        return e.getEntityData().getString("critterfights_aggro");
    }

    public static void addDamageTag(Entity el, float damage) {
        el.getEntityData().setFloat("critterfights_dmg", damage);
    }

    public static float getDamageTag(Entity e) {
        return e.getEntityData().getFloat("critterfights_dmg");
    }

    public static void addTargetingAI(EntityCreature el, Class<? extends EntityLivingBase> targetClass) {
        el.targetTasks.addTask(0, new EntityAIAggressiveTargeting<>((EntityCreature) el, targetClass, false));
        el.targetTasks.addTask(1, new EntityAIHurtByTarget((EntityCreature) el, false, new Class[0]));
    }
    
    public static boolean isValidEntity(String id) {
        return id.equalsIgnoreCase("player") || id.equalsIgnoreCase("minecraft:player") || net.minecraftforge.fml.common.registry.ForgeRegistries.ENTITIES.containsKey(new ResourceLocation(id));
    }
    
    public static boolean isPlayerID(String id) {
        return id.equalsIgnoreCase("player") || id.equalsIgnoreCase("minecraft:player");
    }

    public static void checkAndAddAttackAI(EntityCreature el) {
        Set<EntityAITaskEntry> keepTasks = CritterFights.findAttackTasks(el.tasks);
        el.targetTasks.taskEntries.clear();
        el.tasks.taskEntries.clear();
        if(keepTasks.size() > 0) {
            for(EntityAITaskEntry taskE : keepTasks) {
                el.tasks.addTask(0, taskE.action);
            }
        } else {
            el.tasks.addTask(0, new EntityAIAttackMelee((EntityCreature) el, 1.2D, true) {
                protected void checkAndPerformAttack(EntityLivingBase enemy, double distToEnemySqr) {
                    double d0 = this.getAttackReachSqr(enemy);
                    if(distToEnemySqr <= d0 && this.attackTick <= 0) {
                        this.attackTick = 20;
                        this.attacker.swingArm(EnumHand.MAIN_HAND);
                        enemy.attackEntityFrom(DamageSource.causeMobDamage(this.attacker), (float) this.attacker.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue());
                    }
                }
            });
        }
    }

    public static Set<EntityAITaskEntry> findAttackTasks(EntityAITasks taskList) {
        Set<EntityAITaskEntry> keepTasks = new HashSet<EntityAITaskEntry>();
        for(EntityAITaskEntry taskE : taskList.taskEntries) {
            EntityAIBase ai = taskE.action;
            if(ai instanceof EntityAIAttackMelee || ai instanceof EntityAIAttackRanged || ai.getClass().getSimpleName().toLowerCase().contains("attack")) {
                keepTasks.add(taskE);
            }
        }
        return keepTasks;
    }

    public static void setAttributes(EntityLivingBase el, double damage, double health) {
        el.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(health);
        if(el.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.ATTACK_DAMAGE) == null) {
            el.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(damage);
            CritterFights.addDamageTag(el, (float) damage);
        } else {
            el.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(damage);
        }
        el.getAttributeMap().onAttributeModified(el.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH));
        el.getAttributeMap().onAttributeModified(el.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE));
        el.setHealth((float) health);
    }

}