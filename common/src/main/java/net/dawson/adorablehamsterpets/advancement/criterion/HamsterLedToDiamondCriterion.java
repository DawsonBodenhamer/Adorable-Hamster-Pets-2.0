package net.dawson.adorablehamsterpets.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootContext;
import java.util.Optional;

public class HamsterLedToDiamondCriterion extends SimpleCriterionTrigger<HamsterLedToDiamondCriterion.Conditions> {

    public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("hamster").forGetter(Conditions::hamster),
                    BlockPredicate.CODEC.optionalFieldOf("ore_block").forGetter(Conditions::oreBlock) // Optional: check ore type
            ).apply(instance, Conditions::new));

    public void trigger(ServerPlayer player, HamsterEntity hamster, BlockPos orePos) {
        LootContext hamsterContext = EntityPredicate.createContext(player, hamster);
        this.trigger(player, conditions -> conditions.matches(player, hamsterContext, orePos));
    }

    @Override
    public Codec<Conditions> codec() {
        return CODEC;
    }

    public record Conditions(
            Optional<ContextAwarePredicate> player,
            Optional<ContextAwarePredicate> hamster,
            Optional<BlockPredicate> oreBlock // Condition for the ore block itself
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public boolean matches(ServerPlayer playerEntity, LootContext hamsterContext, BlockPos orePos) {
            if (this.player.isPresent() && !this.player.get().matches(EntityPredicate.createContext(playerEntity, playerEntity))) {
                return false;
            }
            if (this.hamster.isPresent() && !this.hamster.get().matches(hamsterContext)) {
                return false;
            }
            return true;
        }
    }
}