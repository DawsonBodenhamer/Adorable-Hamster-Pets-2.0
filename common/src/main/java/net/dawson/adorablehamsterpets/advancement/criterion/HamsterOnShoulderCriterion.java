package net.dawson.adorablehamsterpets.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

public class HamsterOnShoulderCriterion extends AbstractCriterion<HamsterOnShoulderCriterion.Conditions> {

    public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    // Use EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC for the optional player field
                    EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(Conditions::player)
            ).apply(instance, Conditions::new));

    public void trigger(ServerPlayerEntity player) {
        // The LootContextPredicate is built from the player's context
        // The 'conditions.player()' accesses the Optional<LootContextPredicate>
        // The 'test(LootContext)' method is part of LootContextPredicate
        this.trigger(player, conditions -> conditions.player().isEmpty() || conditions.player().get().test(EntityPredicate.createAdvancementEntityLootContext(player, player)));
    }

    @Override
    public Codec<Conditions> getConditionsCodec() {
        return CODEC;
    }

    // The record field can be named 'player' as the getter will be player()
    public record Conditions(Optional<LootContextPredicate> player) implements AbstractCriterion.Conditions {
    }
}