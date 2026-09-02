package net.dawson.adorablehamsterpets.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public final class RedstoneFeverCriterion extends SimpleCriterionTrigger<RedstoneFeverCriterion.Conditions> {

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Constants
     * ───────────────────────────────────────────────────────────────────────────*/

    private static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player)
    ).apply(instance, Conditions::new));

    /* ────────────────────────────────────────────────────────────────────────────
     *        Public API Methods
     * ──────────────────────────────────────────────────────────────────────────────*/

    public void trigger(ServerPlayer player) {
        this.trigger(player, conditions -> conditions.matches(player));
    }

    /* ────────────────────────────────────────────────────────────────────────────
     *        Overrides
     * ─────────────────────────────────────────────────────────────────────────────*/

    @Override
    public Codec<Conditions> codec() {
        return CODEC;
    }

    /* ─────────────────────────────────────────────────────────────────────────────
     *        Nested Types
     * ─────────────────────────────────────────────────────────────────────────────*/

    public record Conditions(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
        private boolean matches(ServerPlayer playerEntity) {
            // Empty predicate keeps direct server-side triggers lightweight
            return this.player.isEmpty()
                    || this.player.get().matches(EntityPredicate.createContext(playerEntity, playerEntity));
        }
    }
}
