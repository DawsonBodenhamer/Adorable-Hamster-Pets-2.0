package net.dawson.adorablehamsterpets.advancement.criterion;

import com.google.gson.JsonObject;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class HamsterBedPlacedUpsideDownCriterion extends AbstractCriterion<HamsterBedPlacedUpsideDownCriterion.Conditions> {
    private final Identifier id;

    public HamsterBedPlacedUpsideDownCriterion(Identifier id) {
        this.id = id;
    }

    @Override
    public Identifier getId() {
        return this.id;
    }

    @Override
    public Conditions conditionsFromJson(JsonObject obj, LootContextPredicate playerPredicate, AdvancementEntityPredicateDeserializer predicateDeserializer) {
        return new Conditions(this.id, playerPredicate);
    }

    public void trigger(ServerPlayerEntity player) {
        this.trigger(player, conditions -> true);
    }

    public static class Conditions extends AbstractCriterionConditions {
        public Conditions(Identifier id, LootContextPredicate playerPredicate) {
            super(id, playerPredicate);
        }
    }
}