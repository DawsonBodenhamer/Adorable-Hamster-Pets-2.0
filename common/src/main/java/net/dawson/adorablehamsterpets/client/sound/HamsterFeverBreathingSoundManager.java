package net.dawson.adorablehamsterpets.client.sound;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Owns moving Redstone Fever breathing sounds for loaded client-side hamsters.
 */
public final class HamsterFeverBreathingSoundManager {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Static Access and Instance State
     * ────────────────────────────────────────────────────────────────────────────*/

    public static final HamsterFeverBreathingSoundManager INSTANCE = new HamsterFeverBreathingSoundManager();

    private final Map<UUID, HamsterFeverBreathingSoundInstance> activeSounds = new HashMap<>();

    private HamsterFeverBreathingSoundManager() {}

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Lifecycle
     * ────────────────────────────────────────────────────────────────────────────*/

    public void tick(MinecraftClient client) {
        if (client.world == null) {
            this.reset(client);
            return;
        }

        this.removeStoppedSounds(client);

        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof HamsterEntity hamster
                    && hamster.isAlive()
                    && !hamster.isRemoved()
                    && hamster.hasRedstoneFever()) {
                this.ensureSoundPlaying(client, hamster);
            }
        }
    }

    public void reset(MinecraftClient client) {
        for (HamsterFeverBreathingSoundInstance sound : this.activeSounds.values()) {
            sound.stop();
            client.getSoundManager().stop(sound);
        }
        this.activeSounds.clear();
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    private void removeStoppedSounds(MinecraftClient client) {
        Iterator<Map.Entry<UUID, HamsterFeverBreathingSoundInstance>> iterator =
                this.activeSounds.entrySet().iterator();
        while (iterator.hasNext()) {
            HamsterFeverBreathingSoundInstance sound = iterator.next().getValue();
            if (sound.isDone()
                    || !sound.shouldRemainActive()
                    || !sound.belongsTo(client.world)
                    || !client.getSoundManager().isPlaying(sound)) {
                sound.stop();
                client.getSoundManager().stop(sound);
                iterator.remove();
            }
        }
    }

    private void ensureSoundPlaying(MinecraftClient client, HamsterEntity hamster) {
        HamsterFeverBreathingSoundInstance activeSound = this.activeSounds.get(hamster.getUuid());
        if (activeSound != null && activeSound.belongsTo(hamster)) return;

        if (activeSound != null) {
            activeSound.stop();
            client.getSoundManager().stop(activeSound);
        }

        HamsterFeverBreathingSoundInstance newSound = new HamsterFeverBreathingSoundInstance(
                hamster,
                ModSounds.getRandomTimedBreathingSound(hamster.getRandom())
        );
        this.activeSounds.put(hamster.getUuid(), newSound);
        client.getSoundManager().play(newSound);
    }
}
