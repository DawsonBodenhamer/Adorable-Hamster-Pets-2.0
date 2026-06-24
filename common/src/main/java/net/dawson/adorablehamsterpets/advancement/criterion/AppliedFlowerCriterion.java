package net.dawson.adorablehamsterpets.advancement.criterion;

import com.google.gson.JsonObject;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.AdvancementEntityPredicateSerializer;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class AppliedFlowerCriterion extends AbstractCriterion<AppliedFlowerCriterion.Conditions> {
    private final Identifier id;

    public AppliedFlowerCriterion(Identifier id) {
        this.id = id;
    }

    @Override
    public Identifier getId() {
        return this.id;
    }

    @Override
    protected Conditions conditionsFromJson(JsonObject obj, LootContextPredicate playerPredicate, AdvancementEntityPredicateDeserializer predicateDeserializer) {
        LootContextPredicate hamsterPredicate = EntityPredicate.contextPredicateFromJson(obj, "hamster", predicateDeserializer);
        return new Conditions(this.id, playerPredicate, hamsterPredicate);
    }

    public void trigger(ServerPlayerEntity player, HamsterEntity hamster) {
        LootContext hamsterContext = EntityPredicate.createAdvancementEntityLootContext(player, hamster);
        this.trigger(player, conditions -> conditions.matches(hamsterContext));
    }

    public static class Conditions extends AbstractCriterionConditions {
        private final LootContextPredicate hamster;

        public Conditions(Identifier id, LootContextPredicate playerPredicate, LootContextPredicate hamster) {
            super(id, playerPredicate);
            this.hamster = hamster;
        }

        public boolean matches(LootContext hamsterContext) {
            return this.hamster.test(hamsterContext);
        }

        @Override
        public JsonObject toJson(AdvancementEntityPredicateSerializer predicateSerializer) {
            JsonObject jsonObject = super.toJson(predicateSerializer);
            jsonObject.add("hamster", this.hamster.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}