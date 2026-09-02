package net.dawson.adorablehamsterpets.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootContext;
import java.util.Optional;

public class FedHamsterSteamedBeansCriterion extends SimpleCriterionTrigger<FedHamsterSteamedBeansCriterion.Conditions> {

    public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("hamster").forGetter(Conditions::hamster)
            ).apply(instance, Conditions::new));

    /**
     * Triggers the criterion.
     * @param player The player who fed the hamster.
     * @param hamster The hamster that was fed.
     */
    public void trigger(ServerPlayer player, HamsterEntity hamster) {
        LootContext hamsterContext = EntityPredicate.createContext(player, hamster);
        this.trigger(player, conditions -> conditions.matches(player, hamsterContext));
    }

    @Override
    public Codec<Conditions> codec() {
        return CODEC;
    }

    public record Conditions(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> hamster)
            implements SimpleCriterionTrigger.SimpleInstance {

        /**
         * Checks if the conditions match the given player and hamster context.
         * @param playerEntity The player who performed the action.
         * @param hamsterContext The loot context created for the hamster.
         * @return True if conditions match, false otherwise.
         */
        public boolean matches(ServerPlayer playerEntity, LootContext hamsterContext) {
            // Check player predicate if present
            if (this.player.isPresent() && !this.player.get().matches(EntityPredicate.createContext(playerEntity, playerEntity))) {
                return false;
            }
            // Check hamster predicate if present
            return this.hamster.isEmpty() || this.hamster.get().matches(hamsterContext);
        }
    }
}