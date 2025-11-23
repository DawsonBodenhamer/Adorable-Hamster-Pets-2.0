package net.dawson.adorablehamsterpets.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.BlockPredicate;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public class HamsterLedToDiamondCriterion extends AbstractCriterion<HamsterLedToDiamondCriterion.Conditions> {

    public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                    EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("hamster").forGetter(Conditions::hamster),
                    BlockPredicate.CODEC.optionalFieldOf("ore_block").forGetter(Conditions::oreBlock) // Optional: check ore type
            ).apply(instance, Conditions::new));

    public void trigger(ServerPlayerEntity player, HamsterEntity hamster, BlockPos orePos) {
        LootContext hamsterContext = EntityPredicate.createAdvancementEntityLootContext(player, hamster);
        this.trigger(player, conditions -> conditions.matches(player, hamsterContext, orePos));
    }

    @Override
    public Codec<Conditions> getConditionsCodec() {
        return CODEC;
    }

    public record Conditions(
            Optional<LootContextPredicate> player,
            Optional<LootContextPredicate> hamster,
            Optional<BlockPredicate> oreBlock // Condition for the ore block itself
    ) implements AbstractCriterion.Conditions {
        public boolean matches(ServerPlayerEntity playerEntity, LootContext hamsterContext, BlockPos orePos) {
            if (this.player.isPresent() && !this.player.get().test(EntityPredicate.createAdvancementEntityLootContext(playerEntity, playerEntity))) {
                return false;
            }
            if (this.hamster.isPresent() && !this.hamster.get().test(hamsterContext)) {
                return false;
            }
            return true;
        }
    }
}