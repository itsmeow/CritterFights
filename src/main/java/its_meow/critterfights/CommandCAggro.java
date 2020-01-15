package its_meow.critterfights;

import java.util.Arrays;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;

public class CommandCAggro extends CommandBase {

    @Override
    public String getName() {
        return "caggro";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/caggro <damage> <health> <x> <y> <z> <entity1> (entity2/tag1) (entity2)";
    }

    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if(args.length < 6) {
            throw new WrongUsageException(this.getUsage(sender));
        } else {
            String id = args[5];
            BlockPos blockpos = sender.getPosition();
            Vec3d vec3d = sender.getPositionVector();
            double posx = vec3d.x;
            double posy = vec3d.y;
            double posz = vec3d.z;
            posx = parseDouble(posx, args[2], true);
            posy = parseDouble(posy, args[3], false);
            posz = parseDouble(posz, args[4], true);
            blockpos = new BlockPos(posx, posy, posz);
            double damage = parseDouble(0, args[0], 0, 10000, false);
            double health = parseDouble(0, args[1], 0, 10000, false);

            World world = sender.getEntityWorld();

            if(!world.isBlockLoaded(blockpos)) {
                throw new CommandException("commands.summon.outOfWorld", new Object[0]);
            } else {
                NBTTagCompound tag = new NBTTagCompound();
                NBTTagCompound tag2 = new NBTTagCompound();
                boolean flag = false;

                if(args.length == 7) {
                    String s1 = buildString(args, 6);

                    try {
                        tag = JsonToNBT.getTagFromJson(s1);
                        flag = true;
                    } catch(NBTException nbtexception) {
                        if(CritterFights.isValidEntity(args[6])) {
                            tag2.setString("id", args[6]);
                            flag = false;
                            tag = new NBTTagCompound();
                        } else {
                            throw new CommandException("commands.summon.tagError", new Object[] { nbtexception.getMessage() });
                        }
                    }
                } else if(args.length >= 8) {
                    int trimTo = args.length;
                    for(int i = 0; i < args.length; i++) {
                        String arg = args[i];
                        if(i >= 6) {
                            if(CritterFights.isValidEntity(arg)) {
                                trimTo = i;
                            }
                        }
                    }
                    String[] argsT = (String[]) Arrays.copyOfRange(args, 0, trimTo);
                    if(CritterFights.isValidEntity(args[6])) {
                        tag2.setString("id", args[6]);
                    } else if(CritterFights.isValidEntity(args[7])) {
                        tag2.setString("id", args[7]);
                        String s1 = buildString(argsT, 6);

                        try {
                            tag = JsonToNBT.getTagFromJson(s1);
                            tag2.setString("id", args[7]);
                            flag = true;
                        } catch(NBTException nbtexception) {
                            throw new CommandException("commands.summon.tagError", new Object[] { nbtexception.getMessage() });
                        }
                    }
                }

                tag.setString("id", id);
                if(!tag2.hasKey("id")) {
                    tag2 = tag.copy();
                }
                Entity entity = AnvilChunkLoader.readWorldEntityPos(tag.copy(), world, posx, posy, posz, true);
                setupEntity(sender, world, flag, posx, posy, posz, entity);
                if(CritterFights.isPlayerID(tag2.getString("id"))) {
                    if(entity != null) {
                        if(entity instanceof EntityCreature) {
                            setupAI(damage, health, (EntityCreature) entity, EntityPlayer.class, tag2.getString("id").toLowerCase());
                        }
                    }
                } else {
                    Entity entity2 = AnvilChunkLoader.readWorldEntityPos(tag2.copy(), world, posx, posy, posz, false);
                    if(entity != null) {
                        if(entity2 instanceof EntityLiving) {
                            Class<? extends EntityLiving> targetClass = ((EntityLiving)entity2).getClass();
                            entity2.setDead();
                            if(entity instanceof EntityCreature && entity2 instanceof EntityLiving) {
                                setupAI(damage, health, (EntityCreature) entity, targetClass, tag2.getString("id"));
                            }
                        } else if(entity2 != null) {
                            entity2.setDead();
                        }
                    }
                }
            }
        }
    }

    private static void setupAI(double damage, double health, EntityCreature el, Class<? extends EntityLivingBase> targetClass, String targetID) {
        CritterFights.setAttributes(el, damage, health);
        CritterFights.checkAndAddAttackAI(el);
        CritterFights.addTargetingAI(el, targetClass);
        CritterFights.addAITag(el, targetID);
    }

    private void setupEntity(ICommandSender sender, World world, boolean flag, double posx, double posy, double posz, Entity entity) throws CommandException {
        if(entity == null) {
            throw new CommandException("commands.summon.failed", new Object[0]);
        } else {
            if(!(entity instanceof EntityCreature)) {
                entity.setDead();
                throw new CommandException("The entity you are trying to fight is not considered a Creature and cannot use attack AI!");
            }
            entity.setLocationAndAngles(posx, posy, posz, entity.rotationYaw, entity.rotationPitch);

            if(entity instanceof EntityLiving) {
                EntityLiving el = (EntityLiving) entity;
                if(!flag) {
                    el.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(entity)), (IEntityLivingData) null);
                }
            }

            notifyCommandListener(sender, this, "commands.summon.success", new Object[0]);
        }

    }

    public int getRequiredPermissionLevel() {
        return 2;
    }

}
