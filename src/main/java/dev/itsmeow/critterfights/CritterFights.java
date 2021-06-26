package dev.itsmeow.critterfights;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
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

    public void setup(FMLCommonSetupEvent event) {
        ArgumentTypes.register("entity_summon_and_player", CommandCAggro.EntityAndPlayerArgument.class, new ArgumentSerializer<>(CommandCAggro.EntityAndPlayerArgument::entityAndPlayer));
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSource> d = event.getDispatcher();
        // I hate Brigadier. God forgive me for this absolute spaghetti I have been forced to make
        d.register(Commands.literal("cfight").requires(source -> source.hasPermissionLevel(2))
                .then(Commands.argument("pos", Vec3Argument.vec3())
                        // ENTITY 1 - WITH POS
                        .then(Commands.argument("entity", EntitySummonArgument.entitySummon()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity")))
                                // ENTITY 1 NBT
                                .then(Commands.argument("nbt", NBTCompoundTagArgument.nbt())
                                        .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt")))
                                        // ENTITY 2 CHAIN - ENTITY 1 NBT
                                        .then(Commands.argument("entity2", EntitySummonArgument.entitySummon()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                                .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt"), EntitySummonArgument.getEntityId(c, "entity2")))
                                                // ENTITY 2 NBT
                                                .then(Commands.argument("nbt2", NBTCompoundTagArgument.nbt())
                                                        .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt"), EntitySummonArgument.getEntityId(c, "entity2"), NBTCompoundTagArgument.getNbt(c, "nbt2")))
                                                        // HEALTH & DAMAGE
                                                        .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                                                .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt"), EntitySummonArgument.getEntityId(c, "entity2"), NBTCompoundTagArgument.getNbt(c, "nbt2"), IntegerArgumentType.getInteger(c, "health")))
                                                                .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                                        .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt"), EntitySummonArgument.getEntityId(c, "entity2"), NBTCompoundTagArgument.getNbt(c, "nbt2"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                                                )
                                                        )
                                                )
                                                // HEALTH & DAMAGE
                                                .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                                        .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt"), EntitySummonArgument.getEntityId(c, "entity2"), null, IntegerArgumentType.getInteger(c, "health")))
                                                        .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                                .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt"), EntitySummonArgument.getEntityId(c, "entity2"), null, IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                                        )
                                                )
                                        )
                                )
                                // ENTITY 2 CHAIN - NO ENTITY 1 NBT
                                .then(Commands.argument("entity2", EntitySummonArgument.entitySummon()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                        .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), EntitySummonArgument.getEntityId(c, "entity2")))
                                        // ENTITY 2 NBT
                                        .then(Commands.argument("nbt2", NBTCompoundTagArgument.nbt())
                                                .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), EntitySummonArgument.getEntityId(c, "entity2"), NBTCompoundTagArgument.getNbt(c, "nbt2")))
                                                // HEALTH & DAMAGE
                                                .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                                        .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), null, EntitySummonArgument.getEntityId(c, "entity2"), NBTCompoundTagArgument.getNbt(c, "nbt2"), IntegerArgumentType.getInteger(c, "health")))
                                                        .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                                .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), null, EntitySummonArgument.getEntityId(c, "entity2"), NBTCompoundTagArgument.getNbt(c, "nbt2"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                                        )
                                                )
                                        )
                                        // HEALTH & DAMAGE
                                        .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                                .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), null, EntitySummonArgument.getEntityId(c, "entity2"), null, IntegerArgumentType.getInteger(c, "health")))
                                                .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                        .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), null, EntitySummonArgument.getEntityId(c, "entity2"), null, IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                                )
                                        )
                                )
                                // HEALTH & DAMAGE
                                .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                        .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), IntegerArgumentType.getInteger(c, "health")))
                                        .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                .executes(c -> CommandCFight.cfight(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                        )
                                )
                        )
                )
                // ENTITY 1 - NO POS
                .then(Commands.argument("entity", EntitySummonArgument.entitySummon()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                        .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity")))
                        // ENTITY 1 NBT
                        .then(Commands.argument("nbt", NBTCompoundTagArgument.nbt())
                                .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt")))
                                // ENTITY 2 CHAIN - ENTITY 1 NBT
                                .then(Commands.argument("entity2", EntitySummonArgument.entitySummon()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                        .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt"), EntitySummonArgument.getEntityId(c, "entity2")))
                                        // ENTITY 2 NBT
                                        .then(Commands.argument("nbt2", NBTCompoundTagArgument.nbt())
                                                .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt"), EntitySummonArgument.getEntityId(c, "entity2"), NBTCompoundTagArgument.getNbt(c, "nbt2")))
                                                // HEALTH & DAMAGE
                                                .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                                        .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt"), EntitySummonArgument.getEntityId(c, "entity2"), NBTCompoundTagArgument.getNbt(c, "nbt2"), IntegerArgumentType.getInteger(c, "health")))
                                                        .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                                .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt"), EntitySummonArgument.getEntityId(c, "entity2"), NBTCompoundTagArgument.getNbt(c, "nbt2"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                                        )
                                                )
                                        )
                                        // HEALTH & DAMAGE
                                        .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                                .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt"), EntitySummonArgument.getEntityId(c, "entity2"), null, IntegerArgumentType.getInteger(c, "health")))
                                                .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                        .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt"), EntitySummonArgument.getEntityId(c, "entity2"), null, IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                                )
                                        )
                                )
                        )
                        // ENTITY 2 CHAIN - NO ENTITY 1 NBT
                        .then(Commands.argument("entity2", EntitySummonArgument.entitySummon()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), EntitySummonArgument.getEntityId(c, "entity2")))
                                // ENTITY 2 NBT
                                .then(Commands.argument("nbt2", NBTCompoundTagArgument.nbt())
                                        .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), EntitySummonArgument.getEntityId(c, "entity2"), NBTCompoundTagArgument.getNbt(c, "nbt2")))
                                        // HEALTH & DAMAGE
                                        .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                                .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), null, EntitySummonArgument.getEntityId(c, "entity2"), NBTCompoundTagArgument.getNbt(c, "nbt2"), IntegerArgumentType.getInteger(c, "health")))
                                                .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                        .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), null, EntitySummonArgument.getEntityId(c, "entity2"), NBTCompoundTagArgument.getNbt(c, "nbt2"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                                )
                                        )
                                )
                                // HEALTH & DAMAGE
                                .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                        .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), null, EntitySummonArgument.getEntityId(c, "entity2"), null, IntegerArgumentType.getInteger(c, "health")))
                                        .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), null, EntitySummonArgument.getEntityId(c, "entity2"), null, IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                        )
                                )
                        )
                        // HEALTH & DAMAGE
                        .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), IntegerArgumentType.getInteger(c, "health")))
                                .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                        .executes(c -> CommandCFight.cfight(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                )
                        )
                )
        );

        d.register(Commands.literal("caggro").requires(source -> source.hasPermissionLevel(2))
                .then(Commands.argument("pos", Vec3Argument.vec3())
                        // ENTITY 1 - WITH POS
                        .then(Commands.argument("entity", EntitySummonArgument.entitySummon()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                .executes(c -> CommandCAggro.caggro(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity")))
                                // ENTITY 1 NBT
                                .then(Commands.argument("nbt", NBTCompoundTagArgument.nbt())
                                        .executes(c -> CommandCAggro.caggro(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt")))
                                        // ENTITY 2 CHAIN - ENTITY 1 NBT
                                        .then(Commands.argument("entity2", CommandCAggro.EntityAndPlayerArgument.entityAndPlayer()).suggests(CommandCAggro.EntityAndPlayerArgument.SUMMONABLE_ENTITIES_AND_PLAYER)
                                                .executes(c -> CommandCAggro.caggro(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt"), CommandCAggro.EntityAndPlayerArgument.getEntityId(c, "entity2")))
                                                // HEALTH & DAMAGE
                                                .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                                        .executes(c -> CommandCAggro.caggro(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt"), CommandCAggro.EntityAndPlayerArgument.getEntityId(c, "entity2"), IntegerArgumentType.getInteger(c, "health")))
                                                        .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                                .executes(c -> CommandCAggro.caggro(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt"), CommandCAggro.EntityAndPlayerArgument.getEntityId(c, "entity2"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                                        )
                                                )
                                        )
                                )
                                // ENTITY 2 CHAIN - NO ENTITY 1 NBT
                                .then(Commands.argument("entity2", CommandCAggro.EntityAndPlayerArgument.entityAndPlayer()).suggests(CommandCAggro.EntityAndPlayerArgument.SUMMONABLE_ENTITIES_AND_PLAYER)
                                        .executes(c -> CommandCAggro.caggro(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), CommandCAggro.EntityAndPlayerArgument.getEntityId(c, "entity2")))
                                        // HEALTH & DAMAGE
                                        .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                                .executes(c -> CommandCAggro.caggro(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), null, CommandCAggro.EntityAndPlayerArgument.getEntityId(c, "entity2"), IntegerArgumentType.getInteger(c, "health")))
                                                .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                        .executes(c -> CommandCAggro.caggro(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), null, CommandCAggro.EntityAndPlayerArgument.getEntityId(c, "entity2"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                                )
                                        )
                                )
                                // HEALTH & DAMAGE
                                .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                        .executes(c -> CommandCAggro.caggro(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), IntegerArgumentType.getInteger(c, "health")))
                                        .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                .executes(c -> CommandCAggro.caggro(c.getSource(), Vec3Argument.getVec3(c, "pos"), EntitySummonArgument.getEntityId(c, "entity"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                        )
                                )
                        )
                )
                // ENTITY 1 - NO POS
                .then(Commands.argument("entity", EntitySummonArgument.entitySummon()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                        .executes(c -> CommandCAggro.caggro(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity")))
                        // ENTITY 1 NBT
                        .then(Commands.argument("nbt", NBTCompoundTagArgument.nbt())
                                .executes(c -> CommandCAggro.caggro(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt")))
                                // ENTITY 2 CHAIN - ENTITY 1 NBT
                                .then(Commands.argument("entity2", CommandCAggro.EntityAndPlayerArgument.entityAndPlayer()).suggests(CommandCAggro.EntityAndPlayerArgument.SUMMONABLE_ENTITIES_AND_PLAYER)
                                        .executes(c -> CommandCAggro.caggro(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt"), CommandCAggro.EntityAndPlayerArgument.getEntityId(c, "entity2")))
                                        // HEALTH & DAMAGE
                                        .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                                .executes(c -> CommandCAggro.caggro(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt"), CommandCAggro.EntityAndPlayerArgument.getEntityId(c, "entity2"), IntegerArgumentType.getInteger(c, "health")))
                                                .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                        .executes(c -> CommandCAggro.caggro(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), NBTCompoundTagArgument.getNbt(c, "nbt"), CommandCAggro.EntityAndPlayerArgument.getEntityId(c, "entity2"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                                )
                                        )
                                )
                        )
                        // ENTITY 2 CHAIN - NO ENTITY 1 NBT
                        .then(Commands.argument("entity2", CommandCAggro.EntityAndPlayerArgument.entityAndPlayer()).suggests(CommandCAggro.EntityAndPlayerArgument.SUMMONABLE_ENTITIES_AND_PLAYER)
                                .executes(c -> CommandCAggro.caggro(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), CommandCAggro.EntityAndPlayerArgument.getEntityId(c, "entity2")))
                                // HEALTH & DAMAGE
                                .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                        .executes(c -> CommandCAggro.caggro(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), null, CommandCAggro.EntityAndPlayerArgument.getEntityId(c, "entity2"), IntegerArgumentType.getInteger(c, "health")))
                                        .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                                .executes(c -> CommandCAggro.caggro(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), null, CommandCAggro.EntityAndPlayerArgument.getEntityId(c, "entity2"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                        )
                                )
                        )
                        // HEALTH & DAMAGE
                        .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                .executes(c -> CommandCAggro.caggro(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), IntegerArgumentType.getInteger(c, "health")))
                                .then(Commands.argument("damage", IntegerArgumentType.integer(1))
                                        .executes(c -> CommandCAggro.caggro(c.getSource(), c.getSource().getPos(), EntitySummonArgument.getEntityId(c, "entity"), IntegerArgumentType.getInteger(c, "health"), IntegerArgumentType.getInteger(c, "damage")))
                                )
                        )
                )
        );
    }

    public static class CommandCFight {

        private static int cfight(CommandSource source, Vector3d pos, ResourceLocation entity1) {
            return cfight(source, pos, entity1, (CompoundNBT) null);
        }

        private static int cfight(CommandSource source, Vector3d pos, ResourceLocation entity1, int health) {
            return cfight(source, pos, entity1, health, -1);
        }

        private static int cfight(CommandSource source, Vector3d pos, ResourceLocation entity1, int health, int damage) {
            return cfight(source, pos, entity1, null, entity1, null, health, damage);
        }

        private static int cfight(CommandSource source, Vector3d pos, ResourceLocation entity1, CompoundNBT entity1nbt) {
            return cfight(source, pos, entity1, entity1nbt, entity1, entity1nbt);
        }

        private static int cfight(CommandSource source, Vector3d pos, ResourceLocation entity1, ResourceLocation entity2) {
            return cfight(source, pos, entity1, null, entity2);
        }

        private static int cfight(CommandSource source, Vector3d pos, ResourceLocation entity1, ResourceLocation entity2, CompoundNBT entity2nbt) {
            return cfight(source, pos, entity1, null, entity2, entity2nbt);
        }

        private static int cfight(CommandSource source, Vector3d pos, ResourceLocation entity1, CompoundNBT entity1nbt, ResourceLocation entity2) {
            return cfight(source, pos, entity1, entity1nbt, entity2, null);
        }

        private static int cfight(CommandSource source, Vector3d pos, ResourceLocation entity1, CompoundNBT entity1nbt, ResourceLocation entity2, CompoundNBT entity2nbt) {
            return cfight(source, pos, entity1, entity1nbt, entity2, entity2nbt, -1, -1);
        }

        private static int cfight(CommandSource source, Vector3d pos, ResourceLocation entity1, CompoundNBT entity1nbt, ResourceLocation entity2, CompoundNBT entity2nbt, int health) {
            return cfight(source, pos, entity1, entity1nbt, entity2, entity2nbt, health, -1);
        }

        private static int cfight(CommandSource source, Vector3d pos, ResourceLocation entity1type, CompoundNBT entity1nbt, ResourceLocation entity2type, CompoundNBT entity2nbt, int health, int damage) {
            // DEBUG
            //source.sendFeedback(new StringTextComponent("/cfight " + source.getName() + " " + pos + " " + entity1 + " " + entity1nbt + " " + entity2 + " " + entity2nbt + " " + health + " " + damage), true);
            boolean flag1 = false;
            boolean flag2 = false;
            if(entity1nbt == null) {
                flag1 = true;
                entity1nbt = new CompoundNBT();
            }
            if(entity2nbt == null) {
                flag2 = true;
                entity2nbt = new CompoundNBT();
            }
            // java is so picky and for what
            final boolean flag1F = flag1;
            final boolean flag2F = flag2;
            entity1nbt.putString("id", entity1type.toString());
            entity2nbt.putString("id", entity2type.toString());
            Entity entity = EntityType.loadEntityAndExecute(entity1nbt, source.getWorld(), (e) -> {
                e.setLocationAndAngles(pos.getX(), pos.getY(), pos.getZ(), e.rotationYaw, e.rotationPitch);
                if(e instanceof MobEntity) {
                    MobEntity el = (MobEntity) e;
                    if(!flag1F) {
                        el.onInitialSpawn(source.getWorld(), source.getWorld().getDifficultyForLocation(new BlockPos(pos)), SpawnReason.MOB_SUMMONED, null, null);
                    }
                }
                return e;
            });
            Entity entity2 = EntityType.loadEntityAndExecute(entity2nbt, source.getWorld(), (e) -> {
                e.setLocationAndAngles(pos.getX(), pos.getY(), pos.getZ(), e.rotationYaw, e.rotationPitch);
                if(e instanceof MobEntity) {
                    MobEntity el = (MobEntity) e;
                    if(!flag2F) {
                        el.onInitialSpawn(source.getWorld(), source.getWorld().getDifficultyForLocation(new BlockPos(pos)), SpawnReason.MOB_SUMMONED, null, null);
                    }
                }
                return e;
            });
            if(!(entity instanceof CreatureEntity) || !(entity2 instanceof CreatureEntity)) {
                source.sendErrorMessage(new StringTextComponent("The entity you are trying to fight is not considered a Creature and cannot use attack AI!"));
                if(entity != null)
                    entity.remove();
                if(entity2 != null)
                    entity2.remove();
                return 0;
            }
            CreatureEntity el = (CreatureEntity) entity;
            CreatureEntity el2 = (CreatureEntity) entity2;
            CritterFights.setAttributes(el, damage, health);
            CritterFights.checkAndAddAttackAI(el);
            el.setAttackTarget(el2);
            CritterFights.setAttributes(el2, damage, health);
            CritterFights.checkAndAddAttackAI(el2);
            el2.setAttackTarget(el);
            source.getWorld().addEntity(el);
            source.getWorld().addEntity(el2);
            source.sendFeedback(new TranslationTextComponent("commands.summon.success", el.getDisplayName()), true);
            source.sendFeedback(new TranslationTextComponent("commands.summon.success", el2.getDisplayName()), true);
            return 1;
        }
    }

    public static class CommandCAggro {

        public static class EntityAndPlayerArgument implements ArgumentType<ResourceLocation> {

            public static final SuggestionProvider<CommandSource> SUMMONABLE_ENTITIES_AND_PLAYER = SuggestionProviders.register(new ResourceLocation("summonable_entities_and_player"), (et, builder) ->
                    ISuggestionProvider.func_201725_a(Registry.ENTITY_TYPE.stream().filter(et2 -> et2.isSummonable() || et2 == EntityType.PLAYER), builder, EntityType::getKey, et2 ->
                            new TranslationTextComponent(Util.makeTranslationKey("entity", EntityType.getKey(et2)))
                    )
            );

            private static final Collection<String> EXAMPLES = Arrays.asList("minecraft:pig", "cow");
            public static final DynamicCommandExceptionType ENTITY_UNKNOWN_TYPE = EntitySummonArgument.ENTITY_UNKNOWN_TYPE;

            public static EntityAndPlayerArgument entityAndPlayer() {
                return new EntityAndPlayerArgument();
            }

            public static ResourceLocation getEntityId(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
                return checkIfEntityExists(context.getArgument(name, ResourceLocation.class));
            }

            private static ResourceLocation checkIfEntityExists(ResourceLocation id) throws CommandSyntaxException {
                EntityType<?> type = ForgeRegistries.ENTITIES.getValue(id);
                if(type != null && (type.isSummonable() || type == EntityType.PLAYER)) {
                    return id;
                } else {
                    throw ENTITY_UNKNOWN_TYPE.create(id);
                }
            }

            public ResourceLocation parse(StringReader p_parse_1_) throws CommandSyntaxException {
                return checkIfEntityExists(ResourceLocation.read(p_parse_1_));
            }

            public Collection<String> getExamples() {
                return EXAMPLES;
            }
        }


        private static int caggro(CommandSource source, Vector3d pos, ResourceLocation entity1) {
            return caggro(source, pos, entity1, entity1);
        }

        private static int caggro(CommandSource source, Vector3d pos, ResourceLocation entity1, CompoundNBT entity1nbt) {
            return caggro(source, pos, entity1, entity1nbt, entity1);
        }

        private static int caggro(CommandSource source, Vector3d pos, ResourceLocation entity1, int health) {
            return caggro(source, pos, entity1, null, entity1, health, -1);
        }

        private static int caggro(CommandSource source, Vector3d pos, ResourceLocation entity1, int health, int damage) {
            return caggro(source, pos, entity1, null, entity1, health, damage);
        }

        private static int caggro(CommandSource source, Vector3d pos, ResourceLocation entity1, ResourceLocation entity2) {
            return caggro(source, pos, entity1, null, entity2);
        }

        private static int caggro(CommandSource source, Vector3d pos, ResourceLocation entity1, CompoundNBT entity1nbt, ResourceLocation entity2) {
            return caggro(source, pos, entity1, entity1nbt, entity2, -1);
        }

        private static int caggro(CommandSource source, Vector3d pos, ResourceLocation entity1, CompoundNBT entity1nbt, ResourceLocation entity2, int health) {
            return caggro(source, pos, entity1, entity1nbt, entity2, health, -1);
        }

        private static int caggro(CommandSource source, Vector3d pos, ResourceLocation entity1type, CompoundNBT entity1nbt, ResourceLocation targetType, int health, int damage) {
            // DEBUG
            //source.sendFeedback(new StringTextComponent("/caggro " + source.getName() + " " + pos + " " + entity1type + " " + entity1nbt + " " + targetType + " " + health + " " + damage), true);
            boolean flag = false;
            if(entity1nbt == null) {
                flag = true;
                entity1nbt = new CompoundNBT();
            }
            final boolean flagF = flag;
            entity1nbt.putString("id", entity1type.toString());
            Entity entity = EntityType.loadEntityAndExecute(entity1nbt, source.getWorld(), (e) -> {
                e.setLocationAndAngles(pos.getX(), pos.getY(), pos.getZ(), e.rotationYaw, e.rotationPitch);
                if(e instanceof MobEntity) {
                    MobEntity el = (MobEntity) e;
                    if(!flagF) {
                        el.onInitialSpawn(source.getWorld(), source.getWorld().getDifficultyForLocation(new BlockPos(pos)), SpawnReason.MOB_SUMMONED, null, null);
                    }
                }
                return e;
            });
            if(!(entity instanceof CreatureEntity)) {
                source.sendErrorMessage(new StringTextComponent("The entity you are trying to fight is not considered a Creature and cannot use attack AI!"));
                if(entity != null)
                    entity.remove();
                return 0;
            }
            CreatureEntity el = (CreatureEntity) entity;
            if(CritterFights.isPlayerID(targetType)) {
                CritterFights.setAttributes(el, damage, health);
                CritterFights.checkAndAddAttackAI(el);
                CritterFights.addTargetingAI(el, PlayerEntity.class);
                CritterFights.addAITag(el, targetType);
            } else {
                CompoundNBT tag2 = new CompoundNBT();
                tag2.putString("id", targetType.toString());
                Entity entity2 = EntityType.loadEntityAndExecute(tag2.copy(), source.getWorld(), e -> e);
                if(entity2 instanceof LivingEntity) {
                    Class<? extends LivingEntity> targetClass = ((LivingEntity)entity2).getClass();
                    entity2.remove();
                    CritterFights.setAttributes(el, damage, health);
                    CritterFights.checkAndAddAttackAI(el);
                    CritterFights.addTargetingAI(el, targetClass);
                    CritterFights.addAITag(el, targetType);
                } else if(entity2 != null) {
                    entity2.remove();
                }
            }
            source.getWorld().addEntity(el);
            source.sendFeedback(new TranslationTextComponent("commands.summon.success", el.getDisplayName()), true);
            return 1;
        }
    }

    @SubscribeEvent
    public static void onEntityLoaded(EntityJoinWorldEvent event) {
        String id = CritterFights.getAITag(event.getEntity());
        if(!id.isEmpty() && event.getEntity() instanceof CreatureEntity) {
            CreatureEntity entity = (CreatureEntity) event.getEntity();
            float damage = CritterFights.getDamageTag(entity);
            if(damage == 0) damage = 1;
            if(!entity.getAttributeManager().hasAttributeInstance(Attributes.ATTACK_DAMAGE)) {
                ModifiableAttributeInstance attr = new ModifiableAttributeInstance(Attributes.ATTACK_DAMAGE, a -> {});
                attr.setBaseValue(damage);
                entity.getAttributeManager().instanceSet.add(attr);
                entity.getAttributeManager().instanceMap.put(Attributes.ATTACK_DAMAGE, attr);
            } else {
                entity.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
            }
            if(CritterFights.isPlayerID(id)) {
                CritterFights.checkAndAddAttackAI(entity);
                CritterFights.addTargetingAI(entity, PlayerEntity.class);
            } else {
                CompoundNBT tag2 = new CompoundNBT();
                tag2.putString("id", id);
                Entity entity2 = EntityType.loadEntityAndExecute(tag2.copy(), event.getEntity().getEntityWorld(), (e) -> {
                    e.setLocationAndAngles(event.getEntity().getPosX(), event.getEntity().getPosY(), event.getEntity().getPosZ(), event.getEntity().rotationYaw, event.getEntity().rotationPitch);
                    return e;
                });
                if(entity2 instanceof LivingEntity) {
                    Class<? extends LivingEntity> targetClass = ((LivingEntity)entity2).getClass();
                    entity2.remove();
                    CritterFights.checkAndAddAttackAI(entity);
                    CritterFights.addTargetingAI(entity, targetClass);
                } else if(entity2 != null) {
                    entity2.remove();
                }
            }
        }
    }

    /*
     * Helpers
     */

    public static void addAITag(Entity el, ResourceLocation entityID) {
        el.getPersistentData().putString("critterfights_aggro", entityID.toString());
    }

    public static String getAITag(Entity e) {
        return e.getPersistentData().getString("critterfights_aggro");
    }

    public static void addDamageTag(Entity el, float damage) {
        el.getPersistentData().putFloat("critterfights_dmg", damage);
    }

    public static float getDamageTag(Entity e) {
        return e.getPersistentData().getFloat("critterfights_dmg");
    }

    public static <T extends LivingEntity> void addTargetingAI(CreatureEntity entity, Class<T> targetClass) {
        entity.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(entity, targetClass, 0, false, false, e -> true));
        entity.targetSelector.addGoal(1, new HurtByTargetGoal(entity));
    }

    public static boolean isPlayerID(ResourceLocation id) {
        return isPlayerID(id.toString());
    }

    public static boolean isPlayerID(String id) {
        return id.equalsIgnoreCase("player") || id.equalsIgnoreCase("minecraft:player");
    }

    public static void checkAndAddAttackAI(CreatureEntity el) {
        Set<PrioritizedGoal> keepTasks = CritterFights.findAttackTasks(el.goalSelector);
        el.targetSelector.goals.clear();
        el.goalSelector.goals.clear();
        if(keepTasks.size() > 0) {
            for(PrioritizedGoal taskE : keepTasks) {
                el.goalSelector.addGoal(0, taskE.getGoal());
            }
        } else {
            el.goalSelector.addGoal(0, new MeleeAttackGoal(el, 1.2D, true) {
                @Override
                protected void checkAndPerformAttack(LivingEntity enemy, double distToEnemySqr) {
                    double d0 = this.getAttackReachSqr(enemy);
                    if (distToEnemySqr <= d0 && this.getSwingCooldown() <= 0) {
                        this.resetSwingCooldown();
                        this.attacker.swingArm(Hand.MAIN_HAND);
                        enemy.attackEntityFrom(DamageSource.causeMobDamage(this.attacker), (float) this.attacker.getAttributeValue(Attributes.ATTACK_DAMAGE));
                    }
                }
            });
        }
    }

    public static Set<PrioritizedGoal> findAttackTasks(GoalSelector taskList) {
        Set<PrioritizedGoal> keepTasks = new HashSet<>();
        for(PrioritizedGoal taskE : taskList.goals) {
            Goal ai = taskE.getGoal();
            if(ai instanceof MeleeAttackGoal || ai instanceof RangedAttackGoal || ai.getClass().getSimpleName().toLowerCase().contains("attack")) {
                keepTasks.add(taskE);
            }
        }
        return keepTasks;
    }

    public static void setAttributes(LivingEntity el, double damage, double health) {
        if(health != -1) {
            el.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
            el.setHealth((float) health);
        }
        if (damage == -1) {
            damage = 1;
        }
        ModifiableAttributeInstance attr = new ModifiableAttributeInstance(Attributes.ATTACK_DAMAGE, a -> {
        });
        attr.setBaseValue(damage);
        el.getAttributeManager().instanceSet.add(attr);
        el.getAttributeManager().instanceMap.put(Attributes.ATTACK_DAMAGE, attr);
        CritterFights.addDamageTag(el, (float) damage);
    }

}