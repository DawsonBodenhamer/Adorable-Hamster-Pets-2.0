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

public class FedHamsterSteamedBeansCriterion extends AbstractCriterion<FedHamsterSteamedBeansCriterion.Conditions> {

    public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                    EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("hamster").forGetter(Conditions::hamster)
            ).apply(instance, Conditions::new));

    /**
     * Triggers the criterion.
     * @param player The player who fed the hamster.
     * @param hamster The hamster that was fed.
     */
    public void trigger(ServerPlayerEntity player, HamsterEntity hamster) {
        LootContext hamsterContext = EntityPredicate.createAdvancementEntityLootContext(player, hamster);
        this.trigger(player, conditions -> conditions.matches(player, hamsterContext));
    }

    @Override
    public Codec<Conditions> getConditionsCodec() {
        return CODEC;
    }

    public record Conditions(Optional<LootContextPredicate> player, Optional<LootContextPredicate> hamster)
            implements AbstractCriterion.Conditions {

        /**
         * Checks if the conditions match the given player and hamster context.
         * @param playerEntity The player who performed the action.
         * @param hamsterContext The loot context created for the hamster.
         * @return True if conditions match, false otherwise.
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