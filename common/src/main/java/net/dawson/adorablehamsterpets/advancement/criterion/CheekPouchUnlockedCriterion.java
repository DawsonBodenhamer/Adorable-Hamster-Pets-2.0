package net.dawson.adorablehamsterpets.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootContext;
import java.util.Optional;

public class CheekPouchUnlockedCriterion extends SimpleCriterionTrigger<CheekPouchUnlockedCriterion.Conditions> {

    public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("hamster").forGetter(Conditions::hamster) // Optional: if you want to check hamster properties
            ).apply(instance, Conditions::new));

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
        public boolean matches(ServerPlayer playerEntity, LootContext hamsterContext) {
            if (this.player.isPresent() && !this.player.get().matches(EntityPredicate.createContext(playerEntity, playerEntity))) {
                return false;
            }
            return this.hamster.isEmpty() || this.hamster.get().matches(hamsterContext);
        }
    }
}