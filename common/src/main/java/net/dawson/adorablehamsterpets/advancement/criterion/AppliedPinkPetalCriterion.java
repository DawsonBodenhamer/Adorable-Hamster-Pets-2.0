package net.dawson.adorablehamsterpets.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

public class AppliedPinkPetalCriterion extends AbstractCriterion<AppliedPinkPetalCriterion.Conditions> {

    // Codec for the conditions, allowing optional player and hamster predicates
    public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                    EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("hamster").forGetter(Conditions::hamster)
            ).apply(instance, Conditions::new)
    );

    /**
     * Triggers the criterion for the given player and hamster.
     * @param player The player who applied the petal.
     * @param hamster The hamster that received the petal.
     */
    public void trigger(ServerPlayerEntity player, HamsterEntity hamster) {
        LootContext hamsterContext = EntityPredicate.createAdvancementEntityLootContext(player, hamster);
        // Trigger for the player if conditions match
        this.trigger(player, conditions -> conditions.matches(player, hamsterContext));
    }

    @Override
    public Codec<Conditions> getConditionsCodec() {
        return CODEC;
    }

    /**
     * Conditions for the AppliedPinkPetalCriterion.
     */
    public record Conditions(Optional<LootContextPredicate> player, Optional<LootContextPredicate> hamster)
            implements AbstractCriterion.Conditions {

        /**
         * Checks if the provided player and hamster context match the conditions.
         * @param playerEntity The player entity.
         * @param hamsterContext The loot context for the hamster.
         * @return True if conditions are met, false otherwise.
         */
        public boolean matches(ServerPlayerEntity playerEntity, LootContext hamsterContext) {
            // Check player predicate if present
            if (this.player.isPresent() && !this.player.get().test(EntityPredicate.createAdvancementEntityLootContext(playerEntity, playerEntity))) {
                return false;
            }
            // Check hamster predicate if present
            return this.hamster.isEmpty() || this.hamster.get().test(hamsterContext);
        }
    }
}