package net.dawson.adorablehamsterpets.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

public final class RedstoneFeverCriterion extends AbstractCriterion<RedstoneFeverCriterion.Conditions> {

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ───────────────────────────────────────────────────────────────────────────*/

    private static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(Conditions::player)
    ).apply(instance, Conditions::new));

    /* ────────────────────────────────────────────────────────────────────────────
     *        Public API Methods
     * ──────────────────────────────────────────────────────────────────────────────*/

    public void trigger(ServerPlayerEntity player) {
        this.trigger(player, conditions -> conditions.matches(player));
    }

    /* ────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ─────────────────────────────────────────────────────────────────────────────*/

    @Override
    public Codec<Conditions> getConditionsCodec() {
        return CODEC;
    }

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Nested Types
     * ─────────────────────────────────────────────────────────────────────────────*/

    public record Conditions(Optional<LootContextPredicate> player) implements AbstractCriterion.Conditions {
        private boolean matches(ServerPlayerEntity playerEntity) {
            // Empty predicate keeps direct server-side triggers lightweight
            return this.player.isEmpty()
                    || this.player.get().test(EntityPredicate.createAdvancementEntityLootContext(playerEntity, playerEntity));
        }
    }
}
