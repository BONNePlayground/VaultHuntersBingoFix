# Vault Hunters Bingo Fix Mod

This mod addresses the issue with Bingo tiles in vault that relies on rooms inside vault.
It targets 2 tasks: free a villager and find vault room.

These 2 tasks are removed from bingo tiles pool if it is predicted that there will be no required rooms in the vault.

These 2 tasks are limited to have `max-amount` on player joining the vault to be same as total room count in the vault.

As this is based on `prediction` of rooms, then under certain situations or significant VH code changes, this mod may stop working.
