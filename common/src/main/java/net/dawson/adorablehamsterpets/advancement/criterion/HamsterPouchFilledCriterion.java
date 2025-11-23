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

public class HamsterPouchFilledCriterion extends AbstractCriterion<HamsterPouchFilledCriterion.Conditions> {

    public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                    EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("hamster").forGetter(Conditions::hamster)
            ).apply(instance, Conditions::new)
    );

    /**
     * Triggers the criterion when a hamster's pouch is filled.
     * @param player The player interacting with the hamster.
     * @param hamster The hamster whose pouch was filled.
     */
    public void trigger(ServerPlayerEntity player, HamsterEntity hamster) {
        LootContext hamsterContext = EntityPredicate.createAdvancementEntityLootContext(player, hamster);
        this.trigger(player, conditions -> conditions.matches(player, hamsterContext));
    }

    @Override
    public Codec<Conditions> getConditionsCodec() {
        return CODEC;
    }

    /**
     * Conditions for the HamsterPouchFilledCriterion.
     */
    public record Conditions(Optional<LootContextPredicate> player, Optional<LootContextPredicate> hamster)
            implements AbstractCriterion.Conditions {
        public boolean matches(ServerPlayerEntity playerEntity, LootContext hamsterContext) {
            if (this.player.isPresent() && !this.player.get().test(EntityPredicate.createAdvancementEntityLootContext(playerEntity, playerEntity))) {
                return false;
            }
            return this.hamster.isEmpty() || this.hamster.get().test(hamsterContext);
        }
    }
}