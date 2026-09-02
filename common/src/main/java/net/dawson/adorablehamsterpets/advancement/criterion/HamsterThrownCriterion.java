package net.dawson.adorablehamsterpets.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public class HamsterThrownCriterion extends SimpleCriterionTrigger<HamsterThrownCriterion.Conditions> {

    public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player)
            ).apply(instance, Conditions::new));

    public void trigger(ServerPlayer player) {
        this.trigger(player, conditions -> conditions.player().isEmpty() || conditions.player().get().matches(EntityPredicate.createContext(player, player)));
    }

    @Override
    public Codec<Conditions> codec() {
        return CODEC;
    }

    public record Conditions(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
    }
}