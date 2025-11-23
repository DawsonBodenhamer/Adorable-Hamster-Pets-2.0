package net.dawson.adorablehamsterpets.mixin.accessor;

import me.fzzyhmstrs.fzzy_config.validation.ValidatedField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ValidatedField.class, remap = false)
public interface ValidatedFieldAccessor<T> {

    /**
     * Invokes the protected 'set' method on a ValidatedField.
     * @param value The new value to set.
     */
    @Invoker("set")
    void adorablehamsterpets$set(T value);
}