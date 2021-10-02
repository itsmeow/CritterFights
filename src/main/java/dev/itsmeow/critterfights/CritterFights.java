package dev.itsmeow.critterfights;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.EntitySummonArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = CritterFights.MODID)
@Mod(CritterFights.MODID)
public class CritterFights {

    public static final String MODID = "critterfights";

    public CritterFights() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        // I hate Brigadier. God forgive me for this absolute spaghetti I have been forced to make
        d.register(Commands.literal("cfight").requires(source -> source.hasPermission(2))
                .then(Commands.argument("pos", Vec3Argument.vec3())
                        // ENTITY 1 - WITH POS
                        .then(Commands.argument("entity", EntitySummonArgument.id()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity")))
                                // ENTITY 1 NBT
                                .then(Commands.argument("nbt", CompoundTagArgument.compoundTag())
                                        .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt")))
                                        // ENTITY 2 CHAIN - ENTITY 1 NBT
                                        .then(Commands.argument("entity2", EntitySummonArgument.id()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                                .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt"), EntitySummonArgument.getSummonableEntity(c, "entity2")))
                                                // ENTITY 2 NBT
                                                .then(Commands.argument("nbt2", CompoundTagArgument.compoundTag())
                                                        .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt"), EntitySummonArgument.getSummonableEntity(c, "entity2"), CompoundTagArgument.getCompoundTag(c, "nbt2")))
                                                        // HEALTH & DAMAGE
                                                        .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                                                .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt"), EntitySummonArgument.getSummonableEntity(c, "entity2"), CompoundTagArgument.getCompoundTag(c, "nbt2"), IntegerArgumentType.getInteger(c, "health")))
                                                                .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                                        .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt"), EntitySummonArgument.getSummonableEntity(c, "entity2"), CompoundTagArgument.getCompoundTag(c, "nbt2"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                                                )
                                                        )
                                                )
                                                // HEALTH & DAMAGE
                                                .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                                        .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt"), EntitySummonArgument.getSummonableEntity(c, "entity2"), null, IntegerArgumentType.getInteger(c, "health")))
                                                        .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                                .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt"), EntitySummonArgument.getSummonableEntity(c, "entity2"), null, IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                                        )
                                                )
                                        )
                                )
                                // ENTITY 2 CHAIN - NO ENTITY 1 NBT
                                .then(Commands.argument("entity2", EntitySummonArgument.id()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                        .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), EntitySummonArgument.getSummonableEntity(c, "entity2")))
                                        // ENTITY 2 NBT
                                        .then(Commands.argument("nbt2", CompoundTagArgument.compoundTag())
                                                .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), EntitySummonArgument.getSummonableEntity(c, "entity2"), CompoundTagArgument.getCompoundTag(c, "nbt2")))
                                                // HEALTH & DAMAGE
                                                .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                                        .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), null, EntitySummonArgument.getSummonableEntity(c, "entity2"), CompoundTagArgument.getCompoundTag(c, "nbt2"), IntegerArgumentType.getInteger(c, "health")))
                                                        .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                                .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), null, EntitySummonArgument.getSummonableEntity(c, "entity2"), CompoundTagArgument.getCompoundTag(c, "nbt2"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                                        )
                                                )
                                        )
                                        // HEALTH & DAMAGE
                                        .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                                .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), null, EntitySummonArgument.getSummonableEntity(c, "entity2"), null, IntegerArgumentType.getInteger(c, "health")))
                                                .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                        .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), null, EntitySummonArgument.getSummonableEntity(c, "entity2"), null, IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                                )
                                        )
                                )
                                // HEALTH & DAMAGE
                                .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                        .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), IntegerArgumentType.getInteger(c, "health")))
                                        .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                        )
                                )
                        )
                )
                // ENTITY 1 - NO POS
                .then(Commands.argument("entity", EntitySummonArgument.id()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                        .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity")))
                        // ENTITY 1 NBT
                        .then(Commands.argument("nbt", CompoundTagArgument.compoundTag())
                                .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt")))
                                // ENTITY 2 CHAIN - ENTITY 1 NBT
                                .then(Commands.argument("entity2", EntitySummonArgument.id()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                        .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt"), EntitySummonArgument.getSummonableEntity(c, "entity2")))
                                        // ENTITY 2 NBT
                                        .then(Commands.argument("nbt2", CompoundTagArgument.compoundTag())
                                                .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt"), EntitySummonArgument.getSummonableEntity(c, "entity2"), CompoundTagArgument.getCompoundTag(c, "nbt2")))
                                                // HEALTH & DAMAGE
                                                .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                                        .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt"), EntitySummonArgument.getSummonableEntity(c, "entity2"), CompoundTagArgument.getCompoundTag(c, "nbt2"), IntegerArgumentType.getInteger(c, "health")))
                                                        .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                                .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt"), EntitySummonArgument.getSummonableEntity(c, "entity2"), CompoundTagArgument.getCompoundTag(c, "nbt2"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                                        )
                                                )
                                        )
                                        // HEALTH & DAMAGE
                                        .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                                .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt"), EntitySummonArgument.getSummonableEntity(c, "entity2"), null, IntegerArgumentType.getInteger(c, "health")))
                                                .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                        .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt"), EntitySummonArgument.getSummonableEntity(c, "entity2"), null, IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                                )
                                        )
                                )
                        )
                        // ENTITY 2 CHAIN - NO ENTITY 1 NBT
                        .then(Commands.argument("entity2", EntitySummonArgument.id()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), EntitySummonArgument.getSummonableEntity(c, "entity2")))
                                // ENTITY 2 NBT
                                .then(Commands.argument("nbt2", CompoundTagArgument.compoundTag())
                                        .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), EntitySummonArgument.getSummonableEntity(c, "entity2"), CompoundTagArgument.getCompoundTag(c, "nbt2")))
                                        // HEALTH & DAMAGE
                                        .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                                .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), null, EntitySummonArgument.getSummonableEntity(c, "entity2"), CompoundTagArgument.getCompoundTag(c, "nbt2"), IntegerArgumentType.getInteger(c, "health")))
                                                .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                        .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), null, EntitySummonArgument.getSummonableEntity(c, "entity2"), CompoundTagArgument.getCompoundTag(c, "nbt2"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                                )
                                        )
                                )
                                // HEALTH & DAMAGE
                                .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                        .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), null, EntitySummonArgument.getSummonableEntity(c, "entity2"), null, IntegerArgumentType.getInteger(c, "health")))
                                        .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), null, EntitySummonArgument.getSummonableEntity(c, "entity2"), null, IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                        )
                                )
                        )
                        // HEALTH & DAMAGE
                        .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), IntegerArgumentType.getInteger(c, "health")))
                                .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                        .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                )
                        )
                )
        );

        d.register(Commands.literal("caggro").requires(source -> source.hasPermission(2))
                .then(Commands.argument("pos", Vec3Argument.vec3())
                        // ENTITY 1 - WITH POS
                        .then(Commands.argument("entity", EntitySummonArgument.id()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                .executes(c -> CommandCAggro.caggro(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity")))
                                // ENTITY 1 NBT
                                .then(Commands.argument("nbt", CompoundTagArgument.compoundTag())
                                        .executes(c -> CommandCAggro.caggro(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt")))
                                        // ENTITY 2 CHAIN - ENTITY 1 NBT
                                        .then(Commands.argument("entity2", CommandCAggro.EntityAndPlayerArgument.entityAndPlayer()).suggests(CommandCAggro.EntityAndPlayerArgument.SUMMONABLE_ENTITIES_AND_PLAYER)
                                                .executes(c -> CommandCAggro.caggro(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt"), CommandCAggro.EntityAndPlayerArgument.getSummonableEntity(c, "entity2")))
                                                // HEALTH & DAMAGE
                                                .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                                        .executes(c -> CommandCAggro.caggro(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt"), CommandCAggro.EntityAndPlayerArgument.getSummonableEntity(c, "entity2"), IntegerArgumentType.getInteger(c, "health")))
                                                        .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                                .executes(c -> CommandCAggro.caggro(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt"), CommandCAggro.EntityAndPlayerArgument.getSummonableEntity(c, "entity2"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                                        )
                                                )
                                        )
                                )
                                // ENTITY 2 CHAIN - NO ENTITY 1 NBT
                                .then(Commands.argument("entity2", CommandCAggro.EntityAndPlayerArgument.entityAndPlayer()).suggests(CommandCAggro.EntityAndPlayerArgument.SUMMONABLE_ENTITIES_AND_PLAYER)
                                        .executes(c -> CommandCAggro.caggro(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), CommandCAggro.EntityAndPlayerArgument.getSummonableEntity(c, "entity2")))
                                        // HEALTH & DAMAGE
                                        .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                                .executes(c -> CommandCAggro.caggro(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), null, CommandCAggro.EntityAndPlayerArgument.getSummonableEntity(c, "entity2"), IntegerArgumentType.getInteger(c, "health")))
                                                .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                        .executes(c -> CommandCAggro.caggro(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), null, CommandCAggro.EntityAndPlayerArgument.getSummonableEntity(c, "entity2"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                                )
                                        )
                                )
                                // HEALTH & DAMAGE
                                .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                        .executes(c -> CommandCAggro.caggro(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), IntegerArgumentType.getInteger(c, "health")))
                                        .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                .executes(c -> CommandCAggro.caggro(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getSummonableEntity(c, "entity"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                        )
                                )
                        )
                )
                // ENTITY 1 - NO POS
                .then(Commands.argument("entity", EntitySummonArgument.id()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                        .executes(c -> CommandCAggro.caggro(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity")))
                        // ENTITY 1 NBT
                        .then(Commands.argument("nbt", CompoundTagArgument.compoundTag())
                                .executes(c -> CommandCAggro.caggro(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt")))
                                // ENTITY 2 CHAIN - ENTITY 1 NBT
                                .then(Commands.argument("entity2", CommandCAggro.EntityAndPlayerArgument.entityAndPlayer()).suggests(CommandCAggro.EntityAndPlayerArgument.SUMMONABLE_ENTITIES_AND_PLAYER)
                                        .executes(c -> CommandCAggro.caggro(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt"), CommandCAggro.EntityAndPlayerArgument.getSummonableEntity(c, "entity2")))
                                        // HEALTH & DAMAGE
                                        .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                                .executes(c -> CommandCAggro.caggro(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt"), CommandCAggro.EntityAndPlayerArgument.getSummonableEntity(c, "entity2"), IntegerArgumentType.getInteger(c, "health")))
                                                .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                        .executes(c -> CommandCAggro.caggro(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), CompoundTagArgument.getCompoundTag(c, "nbt"), CommandCAggro.EntityAndPlayerArgument.getSummonableEntity(c, "entity2"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                                )
                                        )
                                )
                        )
                        // ENTITY 2 CHAIN - NO ENTITY 1 NBT
                        .then(Commands.argument("entity2", CommandCAggro.EntityAndPlayerArgument.entityAndPlayer()).suggests(CommandCAggro.EntityAndPlayerArgument.SUMMONABLE_ENTITIES_AND_PLAYER)
                                .executes(c -> CommandCAggro.caggro(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), CommandCAggro.EntityAndPlayerArgument.getSummonableEntity(c, "entity2")))
                                // HEALTH & DAMAGE
                                .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                        .executes(c -> CommandCAggro.caggro(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), null, CommandCAggro.EntityAndPlayerArgument.getSummonableEntity(c, "entity2"), IntegerArgumentType.getInteger(c, "health")))
                                        .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                .executes(c -> CommandCAggro.caggro(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), null, CommandCAggro.EntityAndPlayerArgument.getSummonableEntity(c, "entity2"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                        )
                                )
                        )
                        // HEALTH & DAMAGE
                        .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                .executes(c -> CommandCAggro.caggro(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), IntegerArgumentType.getInteger(c, "health")))
                                .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                        .executes(c -> CommandCAggro.caggro(c.getSource(), c.getSource().getPosition(), EntitySummonArgument.getSummonableEntity(c, "entity"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                )
                        )
                )
        );
    }

    @SubscribeEvent
    public static void onEntityLoaded(EntityJoinWorldEvent event) {
        String id = CritterFights.getAITag(event.getEntity());
        if (!id.isEmpty() && event.getEntity() instanceof PathfinderMob entity) {
            float damage = CritterFights.getDamageTag(entity);
            if (damage == 0) damage = 1;
            if (!entity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                AttributeInstance attr = new AttributeInstance(Attributes.ATTACK_DAMAGE, a -> {
                });
                attr.setBaseValue(damage);
                entity.getAttributes().dirtyAttributes.add(attr);
                entity.getAttributes().attributes.put(Attributes.ATTACK_DAMAGE, attr);
            } else {
                entity.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
            }
            if (CritterFights.isPlayerID(id)) {
                CritterFights.checkAndAddAttackAI(entity);
                CritterFights.addTargetingAI(entity, Player.class);
            } else {
                CompoundTag tag2 = new CompoundTag();
                tag2.putString("id", id);
                Entity entity2 = EntityType.loadEntityRecursive(tag2.copy(), event.getEntity().getCommandSenderWorld(), (e) -> {
                    e.moveTo(event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity().getYRot(), event.getEntity().getXRot());
                    return e;
                });
                if (entity2 instanceof LivingEntity livingEntity) {
                    Class<? extends LivingEntity> targetClass = livingEntity.getClass();
                    entity2.remove(Entity.RemovalReason.DISCARDED);
                    CritterFights.checkAndAddAttackAI(entity);
                    CritterFights.addTargetingAI(entity, targetClass);
                } else if (entity2 != null) {
                    entity2.remove(Entity.RemovalReason.DISCARDED);
                }
            }
        }
    }

    public static void addAITag(Entity el, ResourceLocation entityID) {
        el.getPersistentData().putString("critterfights_aggro", entityID.toString());
    }

    public static String getAITag(Entity e) {
        return e.getPersistentData().getString("critterfights_aggro");
    }

    public static void addDamageTag(Entity el, float damage) {
        el.getPersistentData().putFloat("critterfights_dmg", damage);
    }

    /*
     * Helpers
     */

    public static float getDamageTag(Entity e) {
        return e.getPersistentData().getFloat("critterfights_dmg");
    }

    public static <T extends LivingEntity> void addTargetingAI(PathfinderMob entity, Class<T> targetClass) {
        entity.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(entity, targetClass, 0, false, false, e -> true));
        entity.targetSelector.addGoal(1, new HurtByTargetGoal(entity));
    }

    public static boolean isPlayerID(ResourceLocation id) {
        return isPlayerID(id.toString());
    }

    public static boolean isPlayerID(String id) {
        return id.equalsIgnoreCase("player") || id.equalsIgnoreCase("minecraft:player");
    }

    public static void checkAndAddAttackAI(PathfinderMob el) {
        Set<WrappedGoal> keepTasks = CritterFights.findAttackTasks(el.goalSelector);
        el.targetSelector.getAvailableGoals().clear();
        el.goalSelector.getAvailableGoals().clear();
        if (keepTasks.size() > 0) {
            for (WrappedGoal taskE : keepTasks) {
                el.goalSelector.addGoal(0, taskE.getGoal());
            }
        } else {
            el.goalSelector.addGoal(0, new MeleeAttackGoal(el, 1.2D, true) {
                @Override
                protected void checkAndPerformAttack(LivingEntity enemy, double distToEnemySqr) {
                    double d0 = this.getAttackReachSqr(enemy);
                    if (distToEnemySqr <= d0 && this.getTicksUntilNextAttack() <= 0) {
                        this.resetAttackCooldown();
                        this.mob.swing(InteractionHand.MAIN_HAND);
                        enemy.hurt(DamageSource.mobAttack(this.mob), (float) this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
                    }
                }
            });
        }
    }

    public static Set<WrappedGoal> findAttackTasks(GoalSelector taskList) {
        Set<WrappedGoal> keepTasks = new HashSet<>();
        for (WrappedGoal taskE : taskList.getAvailableGoals()) {
            Goal ai = taskE.getGoal();
            if (ai instanceof MeleeAttackGoal || ai instanceof RangedAttackGoal || ai.getClass().getSimpleName().toLowerCase().contains("attack")) {
                keepTasks.add(taskE);
            }
        }
        return keepTasks;
    }

    public static void setAttributes(LivingEntity el, double damage, double health) {
        if (health != -1) {
            el.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
            el.setHealth((float) health);
        }
        if (damage == -1) {
            damage = 1;
        }
        AttributeInstance attr = new AttributeInstance(Attributes.ATTACK_DAMAGE, a -> {
        });
        attr.setBaseValue(damage);
        el.getAttributes().dirtyAttributes.add(attr);
        el.getAttributes().attributes.put(Attributes.ATTACK_DAMAGE, attr);
        CritterFights.addDamageTag(el, (float) damage);
    }

    public void setup(FMLCommonSetupEvent event) {
        ArgumentTypes.register("entity_summon_and_player", CommandCAggro.EntityAndPlayerArgument.class, new EmptyArgumentSerializer<>(CommandCAggro.EntityAndPlayerArgument::entityAndPlayer));
    }

    public static class CommandCFight {

        private static int cfight(CommandSourceStack source, Vec3 pos, ResourceLocation entity1) {
            return cfight(source, pos, entity1, (CompoundTag) null);
        }

        private static int cfight(CommandSourceStack source, Vec3 pos, ResourceLocation entity1, int health) {
            return cfight(source, pos, entity1, health, -1);
        }

        private static int cfight(CommandSourceStack source, Vec3 pos, ResourceLocation entity1, int health, int damage) {
            return cfight(source, pos, entity1, null, entity1, null, health, damage);
        }

        private static int cfight(CommandSourceStack source, Vec3 pos, ResourceLocation entity1, CompoundTag entity1nbt) {
            return cfight(source, pos, entity1, entity1nbt, entity1, entity1nbt);
        }

        private static int cfight(CommandSourceStack source, Vec3 pos, ResourceLocation entity1, ResourceLocation entity2) {
            return cfight(source, pos, entity1, null, entity2);
        }

        private static int cfight(CommandSourceStack source, Vec3 pos, ResourceLocation entity1, ResourceLocation entity2, CompoundTag entity2nbt) {
            return cfight(source, pos, entity1, null, entity2, entity2nbt);
        }

        private static int cfight(CommandSourceStack source, Vec3 pos, ResourceLocation entity1, CompoundTag entity1nbt, ResourceLocation entity2) {
            return cfight(source, pos, entity1, entity1nbt, entity2, null);
        }

        private static int cfight(CommandSourceStack source, Vec3 pos, ResourceLocation entity1, CompoundTag entity1nbt, ResourceLocation entity2, CompoundTag entity2nbt) {
            return cfight(source, pos, entity1, entity1nbt, entity2, entity2nbt, -1, -1);
        }

        private static int cfight(CommandSourceStack source, Vec3 pos, ResourceLocation entity1, CompoundTag entity1nbt, ResourceLocation entity2, CompoundTag entity2nbt, int health) {
            return cfight(source, pos, entity1, entity1nbt, entity2, entity2nbt, health, -1);
        }

        private static int cfight(CommandSourceStack source, Vec3 pos, ResourceLocation entity1type, CompoundTag entity1nbt, ResourceLocation entity2type, CompoundTag entity2nbt, int health, int damage) {
            // DEBUG
            //source.sendFeedback(new StringTextComponent("/cfight " + source.getName() + " " + pos + " " + entity1 + " " + entity1nbt + " " + entity2 + " " + entity2nbt + " " + health + " " + damage), true);
            boolean flag1 = false;
            boolean flag2 = false;
            if (entity1nbt == null) {
                flag1 = true;
                entity1nbt = new CompoundTag();
            }
            if (entity2nbt == null) {
                flag2 = true;
                entity2nbt = new CompoundTag();
            }
            // java is so picky and for what
            final boolean flag1F = flag1;
            final boolean flag2F = flag2;
            entity1nbt.putString("id", entity1type.toString());
            entity2nbt.putString("id", entity2type.toString());
            Entity entity = EntityType.loadEntityRecursive(entity1nbt, source.getLevel(), e -> {
                e.moveTo(pos.x(), pos.y(), pos.z(), e.getYRot(), e.getXRot());
                if (e instanceof Mob el && !flag1F) {
                    el.finalizeSpawn(source.getLevel(), source.getLevel().getCurrentDifficultyAt(new BlockPos(pos)), MobSpawnType.MOB_SUMMONED, null, null);
                }
                return e;
            });
            Entity entity2 = EntityType.loadEntityRecursive(entity2nbt, source.getLevel(), e -> {
                e.moveTo(pos.x(), pos.y(), pos.z(), e.getYRot(), e.getXRot());
                if (e instanceof Mob el && !flag2F) {
                    el.finalizeSpawn(source.getLevel(), source.getLevel().getCurrentDifficultyAt(new BlockPos(pos)), MobSpawnType.MOB_SUMMONED, null, null);
                }
                return e;
            });
            if (!(entity instanceof PathfinderMob) || !(entity2 instanceof PathfinderMob)) {
                source.sendFailure(new TextComponent("The entity you are trying to fight is not considered a Creature and cannot use attack AI!"));
                if (entity != null)
                    entity.remove(Entity.RemovalReason.DISCARDED);
                if (entity2 != null)
                    entity2.remove(Entity.RemovalReason.DISCARDED);
                return 0;
            }
            PathfinderMob el = (PathfinderMob) entity;
            PathfinderMob el2 = (PathfinderMob) entity2;
            CritterFights.setAttributes(el, damage, health);
            CritterFights.checkAndAddAttackAI(el);
            el.setTarget(el2);
            CritterFights.setAttributes(el2, damage, health);
            CritterFights.checkAndAddAttackAI(el2);
            el2.setTarget(el);
            source.getLevel().addFreshEntity(el);
            source.getLevel().addFreshEntity(el2);
            source.sendSuccess(new TranslatableComponent("commands.summon.success", el.getDisplayName()), true);
            source.sendSuccess(new TranslatableComponent("commands.summon.success", el2.getDisplayName()), true);
            return 1;
        }
    }

    public static class CommandCAggro {

        private static int caggro(CommandSourceStack source, Vec3 pos, ResourceLocation entity1) {
            return caggro(source, pos, entity1, entity1);
        }

        private static int caggro(CommandSourceStack source, Vec3 pos, ResourceLocation entity1, CompoundTag entity1nbt) {
            return caggro(source, pos, entity1, entity1nbt, entity1);
        }

        private static int caggro(CommandSourceStack source, Vec3 pos, ResourceLocation entity1, int health) {
            return caggro(source, pos, entity1, null, entity1, health, -1);
        }

        private static int caggro(CommandSourceStack source, Vec3 pos, ResourceLocation entity1, int health, int damage) {
            return caggro(source, pos, entity1, null, entity1, health, damage);
        }

        private static int caggro(CommandSourceStack source, Vec3 pos, ResourceLocation entity1, ResourceLocation entity2) {
            return caggro(source, pos, entity1, null, entity2);
        }

        private static int caggro(CommandSourceStack source, Vec3 pos, ResourceLocation entity1, CompoundTag entity1nbt, ResourceLocation entity2) {
            return caggro(source, pos, entity1, entity1nbt, entity2, -1);
        }

        private static int caggro(CommandSourceStack source, Vec3 pos, ResourceLocation entity1, CompoundTag entity1nbt, ResourceLocation entity2, int health) {
            return caggro(source, pos, entity1, entity1nbt, entity2, health, -1);
        }

        private static int caggro(CommandSourceStack source, Vec3 pos, ResourceLocation entity1type, CompoundTag entity1nbt, ResourceLocation targetType, int health, int damage) {
            // DEBUG
            //source.sendFeedback(new StringTextComponent("/caggro " + source.getName() + " " + pos + " " + entity1type + " " + entity1nbt + " " + targetType + " " + health + " " + damage), true);
            boolean flag = false;
            if (entity1nbt == null) {
                flag = true;
                entity1nbt = new CompoundTag();
            }
            final boolean flagF = flag;
            entity1nbt.putString("id", entity1type.toString());
            Entity entity = EntityType.loadEntityRecursive(entity1nbt, source.getLevel(), e -> {
                e.moveTo(pos.x(), pos.y(), pos.z(), e.getYRot(), e.getXRot());
                if (e instanceof Mob el && !flagF) {
                    el.finalizeSpawn(source.getLevel(), source.getLevel().getCurrentDifficultyAt(new BlockPos(pos)), MobSpawnType.MOB_SUMMONED, null, null);
                }
                return e;
            });
            if (!(entity instanceof PathfinderMob el)) {
                source.sendFailure(new TextComponent("The entity you are trying to fight is not considered a Creature and cannot use attack AI!"));
                if (entity != null)
                    entity.remove(Entity.RemovalReason.DISCARDED);
                return 0;
            }
            if (CritterFights.isPlayerID(targetType)) {
                CritterFights.setAttributes(el, damage, health);
                CritterFights.checkAndAddAttackAI(el);
                CritterFights.addTargetingAI(el, Player.class);
                CritterFights.addAITag(el, targetType);
            } else {
                CompoundTag tag2 = new CompoundTag();
                tag2.putString("id", targetType.toString());
                Entity entity2 = EntityType.loadEntityRecursive(tag2.copy(), source.getLevel(), e -> e);
                if (entity2 instanceof LivingEntity livingEntity) {
                    Class<? extends LivingEntity> targetClass = livingEntity.getClass();
                    entity2.remove(Entity.RemovalReason.DISCARDED);
                    CritterFights.setAttributes(el, damage, health);
                    CritterFights.checkAndAddAttackAI(el);
                    CritterFights.addTargetingAI(el, targetClass);
                    CritterFights.addAITag(el, targetType);
                } else if (entity2 != null) {
                    entity2.remove(Entity.RemovalReason.DISCARDED);
                }
            }
            source.getLevel().addFreshEntity(el);
            source.sendSuccess(new TranslatableComponent("commands.summon.success", el.getDisplayName()), true);
            return 1;
        }

        public static class EntityAndPlayerArgument implements ArgumentType<ResourceLocation> {

            public static final SuggestionProvider<CommandSourceStack> SUMMONABLE_ENTITIES_AND_PLAYER = SuggestionProviders.register(new ResourceLocation("summonable_entities_and_player"), (et, builder) ->
                    SharedSuggestionProvider.suggestResource(ForgeRegistries.ENTITIES.getValues().stream().filter(et2 -> et2.canSummon() || et2 == EntityType.PLAYER), builder, EntityType::getKey, et2 ->
                            new TranslatableComponent(Util.makeDescriptionId("entity", EntityType.getKey(et2)))
                    )
            );
            public static final DynamicCommandExceptionType ENTITY_UNKNOWN_TYPE = EntitySummonArgument.ERROR_UNKNOWN_ENTITY;
            private static final Collection<String> EXAMPLES = Arrays.asList("minecraft:pig", "cow");

            public static EntityAndPlayerArgument entityAndPlayer() {
                return new EntityAndPlayerArgument();
            }

            public static ResourceLocation getSummonableEntity(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
                return checkIfEntityExists(context.getArgument(name, ResourceLocation.class));
            }

            private static ResourceLocation checkIfEntityExists(ResourceLocation id) throws CommandSyntaxException {
                EntityType<?> type = ForgeRegistries.ENTITIES.getValue(id);
                if (type != null && (type.canSummon() || type == EntityType.PLAYER)) {
                    return id;
                } else {
                    throw ENTITY_UNKNOWN_TYPE.create(id);
                }
            }

            public ResourceLocation parse(StringReader stringReader) throws CommandSyntaxException {
                return checkIfEntityExists(ResourceLocation.read(stringReader));
            }

            @Override
            public Collection<String> getExamples() {
                return EXAMPLES;
            }
        }
    }

}