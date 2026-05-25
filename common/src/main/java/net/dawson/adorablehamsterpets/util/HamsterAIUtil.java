package net.dawson.adorablehamsterpets.util;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Encapsulates passive, environment-scanning AI logic for Hamsters.
 */
public final class HamsterAIUtil {

    private HamsterAIUtil() {}

    /**
     * Scans for a nearby jukebox actively playing the Cheese Music Disc.
     */
    public static boolean isCheeseSongPlayingNearby(HamsterEntity hamster) {
        World world = hamster.getWorld();

        for (BlockPos p : BlockPos.iterateOutwards(hamster.getBlockPos(), 8, 4, 8)) {
            if (world.getBlockState(p).isOf(Blocks.JUKEBOX)) {
                if (world.getBlockEntity(p) instanceof JukeboxBlockEntity jbe) {
                    // In 1.20.1, check if it's playing and verify the item directly
                    if (jbe.isPlayingRecord() && !jbe.getStack().isEmpty()) {
                        if (jbe.getStack().isOf(ModItems.MUSIC_DISC_CHEESE.get())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}