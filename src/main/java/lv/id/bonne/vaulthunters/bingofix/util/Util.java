//
// Created by BONNe
// Copyright - 2024
//


package lv.id.bonne.vaulthunters.bingofix.util;


import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import iskallia.vault.core.random.ChunkRandom;
import iskallia.vault.core.util.RegionPos;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.WorldManager;
import iskallia.vault.core.world.generator.GridGenerator;
import iskallia.vault.core.world.generator.VaultGenerator;
import iskallia.vault.core.world.generator.layout.ClassicInfiniteLayout;
import iskallia.vault.core.world.generator.layout.ClassicVaultLayout;
import iskallia.vault.core.world.generator.layout.GridLayout;
import iskallia.vault.core.world.generator.layout.VaultLayout;
import iskallia.vault.core.world.processor.ProcessorContext;
import iskallia.vault.core.world.template.JigsawTemplate;
import iskallia.vault.core.world.template.PlacementSettings;
import iskallia.vault.core.world.template.Template;
import lv.id.bonne.vaulthunters.bingofix.BingoFixMod;
import net.minecraft.resources.ResourceLocation;


public class Util
{
    /**
     * This method predicts vault rooms from given Vault object.
     * @param vault The vault object that stores all information.
     * @return List of vault rooms that are predicted to be inside vault.
     */
    public static List<ResourceLocation> collectVaultRooms(Vault vault)
    {
        WorldManager worldManager = vault.get(Vault.WORLD);
        VaultGenerator vaultGenerator = worldManager.get(WorldManager.GENERATOR);

        if (!(vaultGenerator instanceof GridGenerator gridGenerator))
        {
            BingoFixMod.LOGGER.error("Prediction can be done only on Grid Generator");
            return Collections.emptyList();
        }

        Integer cellX = gridGenerator.get(GridGenerator.CELL_X);
        Integer cellZ = gridGenerator.get(GridGenerator.CELL_Z);
        GridLayout gridLayout = gridGenerator.get(GridGenerator.LAYOUT);

        if (gridLayout instanceof ClassicInfiniteLayout)
        {
            BingoFixMod.LOGGER.error("Infinite layout should in theory have the rooms, unless your generator is broken.");
            return Collections.emptyList();
        }

        if (!(gridLayout instanceof ClassicVaultLayout layout))
        {
            BingoFixMod.LOGGER.error("Prediction can be done only on Classic Layout");
            return Collections.emptyList();
        }

        final int range = 50;
        List<ResourceLocation> roomIDList = new ArrayList<>();

        // Start spiral search
        int x = 0;
        int z = 0;
        int dx = 1;
        int dz = 0;
        int stepSize = 1;
        boolean roomFound = true;

        // Continue while rooms are found in each completed ring
        while (roomFound)
        {
            roomFound = false;
            int stepsInRing = 0;

            // Complete one ring of steps
            while (stepsInRing < stepSize * 4)
            {
                x += dx;
                z += dz;

                if (Math.abs(x) <= range && Math.abs(z) <= range)
                {
                    // Vault rooms are every 2 ring. Tunnels are in between.
                    RegionPos region = RegionPos.of(x << 1, z << 1, cellX, cellZ);
                    VaultLayout.PieceType type = layout.getType(vault, region);

                    if (type == VaultLayout.PieceType.ROOM)
                    {
                        ResourceLocation resourceLocation = processRegion(vault, layout, region);

                        if (resourceLocation != null)
                        {
                            roomIDList.add(resourceLocation);
                        }

                        // mark that there is room in ring.
                        roomFound = true;
                    }
                }

                stepsInRing++;

                // Turn 90 degrees every 'stepSize' steps
                if (stepsInRing % stepSize == 0)
                {
                    int temp = dx;
                    dx = -dz;
                    dz = temp;

                    // Increase step size every two turns (i.e., after completing two sides of the square)
                    if (dz == 0)
                    {
                        stepSize++;
                    }
                }
            }
        }

        return roomIDList;
    }


    /**
     * This method returns room resource location that should be at given region position.
     * @param vault The vault that stores information.
     * @param gridLayout The grid layout for vault.
     * @param region The room location.
     * @return Room resource location or null.
     */
    @Nullable
    private static ResourceLocation processRegion(Vault vault,
        ClassicVaultLayout gridLayout,
        RegionPos region)
    {
        ChunkRandom random = ChunkRandom.any();
        // Change random to the vault seed and region location. Salt is same as in VH core.
        random.setRegionSeed(vault.get(Vault.SEED), region.getX(), region.getZ(), 1234567890L);

        PlacementSettings settings = new PlacementSettings(new ProcessorContext(vault, random)).setFlags(3);
        // As we do not care about actual storing into cache, we just get the template.
        Template template = gridLayout.getTemplate(gridLayout.getType(vault, region), vault, region, random, settings);

        if (template instanceof JigsawTemplate jigsawTemplate)
        {
            // I hope this will never crash :)
            return jigsawTemplate.getRoot().getKey().getId();
        }
        else
        {
            return null;
        }
    }
}
