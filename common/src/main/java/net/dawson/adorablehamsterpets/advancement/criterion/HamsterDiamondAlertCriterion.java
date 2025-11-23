package net.dawson.adorablehamsterpets.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

public class HamsterDiamondAlertCriterion extends AbstractCriterion<HamsterDiamondAlertCriterion.Conditions> {

    public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(Conditions::player)
            ).apply(instance, Conditions::new)
    );

    /**
     * Triggers the criterion when a shoulder hamster alerts to diamonds.
     * @param player The player who received the alert.
     */
    public void trigger(ServerPlayerEntity player) {
        this.trigger(player, conditions -> conditions.matches(player));
    }

    @Override
    public Codec<Conditions> getConditionsCodec() {
        return CODEC;
    }

    /**
     * Conditions for the HamsterDiamondAlertCriterion.
     */
    public record Conditions(Optional<LootContextPredicate> player)
            implements AbstractCriterion.Conditions {
        public boolean matches(ServerPlayerEntity playerEntity) {
            return this.player.isEmpty() || this.player.get().test(EntityPredicate.createAdvancementEntityLootContext(playerEntity, playerEntity));
        }
    }
}