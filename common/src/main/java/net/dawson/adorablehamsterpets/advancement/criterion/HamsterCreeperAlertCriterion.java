package net.dawson.adorablehamsterpets.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public class HamsterCreeperAlertCriterion extends SimpleCriterionTrigger<HamsterCreeperAlertCriterion.Conditions> {

    public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player)
            ).apply(instance, Conditions::new)
    );

    /**
     * Triggers the criterion when a shoulder hamster alerts to a creeper.
     * @param player The player who received the alert.
     */
    public void trigger(ServerPlayer player) {
        this.trigger(player, conditions -> conditions.matches(player));
    }

    @Override
    public Codec<Conditions> codec() {
        return CODEC;
    }

    /**
     * Conditions for the HamsterCreeperAlertCriterion.
     */
    public record Conditions(Optional<ContextAwarePredicate> player)
            implements SimpleCriterionTrigger.SimpleInstance {
        public boolean matches(ServerPlayer playerEntity) {
            return this.player.isEmpty() || this.player.get().matches(EntityPredicate.createContext(playerEntity, playerEntity));
        }
    }
}