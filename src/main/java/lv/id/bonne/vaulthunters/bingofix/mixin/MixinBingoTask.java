//
// Created by BONNe
// Copyright - 2024
//


package lv.id.bonne.vaulthunters.bingofix.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import iskallia.vault.core.util.WeightedList;
import iskallia.vault.core.world.data.tile.PartialTile;
import iskallia.vault.init.ModBlocks;
import iskallia.vault.task.*;
import iskallia.vault.task.counter.TaskCounter;
import lv.id.bonne.vaulthunters.bingofix.BingoFixMod;
import lv.id.bonne.vaulthunters.bingofix.util.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;


/**
 * This mixin removes impossible bingo tasks, and add limitation on how much they can be increased.
 */
@Mixin(value = BingoTask.class, remap = false)
public class MixinBingoTask
{
    @Inject(method = "onPopulate",
        at = @At(value = "INVOKE", target = "Liskallia/vault/task/BingoTask;getWidth()I", ordinal = 1),
        locals = LocalCapture.CAPTURE_FAILSOFT)
    public void validatePossibleTasks(TaskContext context, CallbackInfo ci, WeightedList<Task> pool)
    {
        List<ResourceLocation> allVaultRooms = Util.collectVaultRooms(context.getVault());

        if (allVaultRooms.isEmpty())
        {
            BingoFixMod.LOGGER.debug("Cannot adjust tasks as vault rooms are not detected.");
            return;
        }

        long numberOfVillageRooms = allVaultRooms.stream().
            filter(resourceLocation -> resourceLocation.getPath().startsWith("vault/rooms/challenge/village")).
            count();

        // Remove task if it cannot be fulfilled.
        pool.entrySet().removeIf(entry -> {
            if (entry.getKey() instanceof MineBlockTask task && numberOfVillageRooms == 0)
            {
                if (task.getConfig().filter.test(PartialTile.of(ModBlocks.BARRED_TRAPDOOR.defaultBlockState())) ||
                    task.getConfig().filter.test(PartialTile.of(ModBlocks.BARRED_DOOR.defaultBlockState())))
                {
                    BingoFixMod.LOGGER.debug("Remove Task: " + task.getId() + " because cannot find village rooms in vault!");
                    return true;
                }
            }
            else if (entry.getKey() instanceof FindVaultRoomTask task)
            {
                ResourceLocation[] filter = task.getConfig().getFilter();

                if (filter != null)
                {
                    boolean found = Arrays.stream(filter).anyMatch(allVaultRooms::contains);

                    if (!found)
                    {
                        BingoFixMod.LOGGER.debug("Remove Task: " + task.getId() + " because cannot find rooms that match its requirements in vault!");
                        return true;
                    }
                }
            }

            return false;
        });

        // Adjust max value for tasks.
        pool.keySet().forEach(unformatted -> {
            if (unformatted instanceof MineBlockTask task)
            {
                if (task.getConfig().filter.test(PartialTile.of(ModBlocks.BARRED_TRAPDOOR.defaultBlockState())) ||
                    task.getConfig().filter.test(PartialTile.of(ModBlocks.BARRED_DOOR.defaultBlockState())))
                {
                    if (numberOfVillageRooms == 0)
                    {
                        BingoFixMod.LOGGER.debug("Can not find rooms for task.");
                        return;
                    }

                    readjustRoomTargetCounter(task.getCounter(), (int) numberOfVillageRooms);
                }
            }
            else if (unformatted instanceof FindVaultRoomTask task)
            {
                ResourceLocation[] filter = task.getConfig().getFilter();

                if (filter != null)
                {
                    long roomCount = allVaultRooms.stream().filter(room -> Arrays.asList(filter).contains(room)).count();

                    if (roomCount == 0)
                    {
                        BingoFixMod.LOGGER.debug("Can not find rooms for task.");
                        return;
                    }

                    readjustRoomTargetCounter(task.getCounter(), (int) roomCount);
                }
            }
        });
    }


    /**
     * This method change targets based on how many rooms there are in the vault.
     */
    @Unique
    private static void readjustRoomTargetCounter(TaskCounter<?, ?> counter, int roomCount)
    {
        Optional<CompoundTag> optionalTag = counter.writeNbt();

        if (optionalTag.isEmpty())
        {
            BingoFixMod.LOGGER.debug("Can not generate task counter for task.");
            return;
        }

        optionalTag.ifPresent(tag ->
        {
            if (!tag.contains("target", Tag.TAG_COMPOUND))
            {
                BingoFixMod.LOGGER.debug("Could not find target object.");
                return;
            }

            CompoundTag target = tag.getCompound("target");

            if (!target.contains("max"))
            {
                BingoFixMod.LOGGER.debug("Could not find max object.");
                return;
            }

            if (!target.contains("min"))
            {
                BingoFixMod.LOGGER.debug("Could not find min object.");
                return;
            }

            int max = target.getInt("max");
            int min = target.getInt("min");

            if (max > roomCount)
            {
                target.putInt("max", roomCount);

                if (min > roomCount)
                {
                    target.putInt("min", roomCount);
                }

                // Reload tag
                BingoFixMod.LOGGER.debug("Change target counter for task.");
            }
            else
            {
                BingoFixMod.LOGGER.debug("Add maxRooms variable.");
            }

            if (tag.contains("variables"))
            {
                CompoundTag variables = tag.getCompound("variables");
                variables.putInt("maxRooms", roomCount);
            }

            counter.readNbt(tag);
        });
    }
}
