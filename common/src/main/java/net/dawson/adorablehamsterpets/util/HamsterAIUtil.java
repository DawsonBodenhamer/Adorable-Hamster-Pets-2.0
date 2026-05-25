package net.dawson.adorablehamsterpets.util;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;

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
                    if (jbe.getManager().isPlaying() && jbe.getManager().getSong() != null) {
                        // Check if the currently playing song's SoundEvent matches my custom music disc sound
                        if (jbe.getManager().getSong().soundEvent().value().equals(ModSounds.AHP_THEME_SONG.get())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}