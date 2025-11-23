package net.dawson.adorablehamsterpets.advancement.criterion;

import com.google.gson.JsonObject;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.BlockPredicate;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class HamsterLedToDiamondCriterion extends AbstractCriterion<HamsterLedToDiamondCriterion.Conditions> {
    private final Identifier id;

    public HamsterLedToDiamondCriterion(Identifier id) {
        this.id = id;
    }

    @Override
    public Identifier getId() {
        return this.id;
    }

    @Override
    public HamsterLedToDiamondCriterion.Conditions conditionsFromJson(JsonObject jsonObject, LootContextPredicate playerPredicate, AdvancementEntityPredicateDeserializer predicateDeserializer) {
        LootContextPredicate hamsterPredicate = EntityPredicate.contextPredicateFromJson(jsonObject, "hamster", predicateDeserializer);
        BlockPredicate oreBlockPredicate = BlockPredicate.fromJson(jsonObject.get("ore_block"));
        return new HamsterLedToDiamondCriterion.Conditions(this.id, playerPredicate, hamsterPredicate, oreBlockPredicate);
    }

    public void trigger(ServerPlayerEntity player, HamsterEntity hamster, BlockPos orePos) {
        LootContext hamsterContext = EntityPredicate.createAdvancementEntityLootContext(player, hamster);
        this.trigger(player, (conditions) -> conditions.matches(hamsterContext, (ServerWorld) player.getWorld(), orePos));
    }

    public static class Conditions extends AbstractCriterionConditions {
        private final LootContextPredicate hamster;
        private final BlockPredicate oreBlock;

        public Conditions(Identifier id, LootContextPredicate player, LootContextPredicate hamster, BlockPredicate oreBlock) {
            super(id, player);
            this.hamster = hamster;
            this.oreBlock = oreBlock;
        }

        public boolean matches(LootContext hamsterContext, ServerWorld world, BlockPos pos) {
            if (!this.hamster.test(hamsterContext)) {
                return false;
            }
            return this.oreBlock.test(world, pos);
        }
    }
}