package net.dawson.adorablehamsterpets.advancement.criterion;

import com.google.gson.JsonObject;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class RedstoneFeverCriterion extends AbstractCriterion<RedstoneFeverCriterion.Conditions> {

    private final Identifier id;

    public RedstoneFeverCriterion(Identifier id) {
        this.id = id;
    }

    /* ────────────────────────────────────────────────────────────────────────────
     *        Public API Methods
     * ──────────────────────────────────────────────────────────────────────────────*/

    public void trigger(ServerPlayerEntity player) {
        this.trigger(player, conditions -> true);
    }

    /* ────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ─────────────────────────────────────────────────────────────────────────────*/

    @Override
    public Identifier getId() {
        return this.id;
    }

    @Override
    public Conditions conditionsFromJson(
            JsonObject jsonObject,
            LootContextPredicate playerPredicate,
            AdvancementEntityPredicateDeserializer predicateDeserializer
    ) {
        return new Conditions(this.id, playerPredicate);
    }

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Nested Types
     * ─────────────────────────────────────────────────────────────────────────────*/

    public static final class Conditions extends AbstractCriterionConditions {

        public Conditions(Identifier id, LootContextPredicate player) {
            super(id, player);
        }
    }
}
