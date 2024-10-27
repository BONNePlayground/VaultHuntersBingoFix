//
// Created by BONNe
// Copyright - 2024
//


package lv.id.bonne.vaulthunters.bingofix.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

import iskallia.vault.core.data.adapter.Adapters;
import iskallia.vault.core.vault.objective.BingoObjective;
import iskallia.vault.task.counter.TargetTaskCounter;


@Mixin(value = BingoObjective.class, remap = false)
public class MixinBingoObjective
{
    @Redirect(method = "lambda$tickServer$4",
        at = @At(value = "INVOKE", target = "Liskallia/vault/task/counter/TargetTaskCounter;setTarget(Ljava/lang/Object;)V"))
    private void limitMaxRoomValue(TargetTaskCounter instance, Object target)
    {
        Optional<Integer> maxRooms = instance.get("maxRooms", Adapters.INT);

        if (maxRooms.isPresent())
        {
            instance.setTarget(Math.min(maxRooms.get(), (int) target));
        }
        else
        {
            instance.setTarget(target);
        }
    }
}
