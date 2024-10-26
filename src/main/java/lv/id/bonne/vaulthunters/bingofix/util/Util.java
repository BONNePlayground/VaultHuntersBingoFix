//
// Created by BONNe
// Copyright - 2024
//


package lv.id.bonne.vaulthunters.bingofix.util;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import iskallia.vault.core.random.ChunkRandom;
import iskallia.vault.core.util.RegionPos;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.WorldManager;
import iskallia.vault.core.world.generator.GridGenerator;
import iskallia.vault.core.world.generator.VaultGenerator;
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

        int range = 50;

        if (!(gridLayout instanceof ClassicVaultLayout layout))
        {
            BingoFixMod.LOGGER.error("Prediction can be done only on Classic Layout");
            return Collections.emptyList();
        }

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
                    RegionPos region = RegionPos.of(x << 1, z << 1, cellX, cellZ);
                    VaultLayout.PieceType type = layout.getType(vault, region);

                    if (type == VaultLayout.PieceType.ROOM)
                    {
                        ResourceLocation resourceLocation = processRegion(vault, layout, region);

                        if (resourceLocation != null)
                        {
                            roomIDList.add(resourceLocation);
                        }

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


    static ResourceLocation processRegion(Vault vault,
        ClassicVaultLayout gridLayout,
        RegionPos region)
    {
        ChunkRandom random = ChunkRandom.any();

        random.setRegionSeed(vault.get(Vault.SEED), region.getX(), region.getZ(), 1234567890L);

        PlacementSettings settings = (new PlacementSettings(new ProcessorContext(vault, random))).setFlags(3);
        Template template = gridLayout.getTemplate(gridLayout.getType(vault, region), vault, region, random, settings);

        if (template instanceof JigsawTemplate jigsawTemplate)
        {
            return jigsawTemplate.getRoot().getKey().getId();
        }
        else
        {
            return null;
        }
    }
}
