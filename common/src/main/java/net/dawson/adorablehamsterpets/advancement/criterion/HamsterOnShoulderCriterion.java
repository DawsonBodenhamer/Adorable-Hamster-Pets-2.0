package net.dawson.adorablehamsterpets.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public class HamsterOnShoulderCriterion extends SimpleCriterionTrigger<HamsterOnShoulderCriterion.Conditions> {

    public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    // Use EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC for the optional player field
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player)
            ).apply(instance, Conditions::new));

    public void trigger(ServerPlayer player) {
        // The LootContextPredicate is built from the player's context
        // The 'conditions.player()' accesses the Optional<LootContextPredicate>
        // The 'test(LootContext)' method is part of LootContextPredicate
        this.trigger(player, conditions -> conditions.player().isEmpty() || conditions.player().get().matches(EntityPredicate.createContext(player, player)));
    }

    @Override
    public Codec<Conditions> codec() {
        return CODEC;
    }

    // The record field can be named 'player' as the getter will be player()
    public record Conditions(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
    }
}