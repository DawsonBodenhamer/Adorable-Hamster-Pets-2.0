package net.dawson.adorablehamsterpets.mixin.client.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import vazkii.patchouli.client.book.gui.GuiBook;

@Mixin(value = GuiBook.class, remap = false)
public interface GuiBookAccessor {
    @Accessor("bookLeft")
    int adorablehamsterpets$getBookLeft();

    @Accessor("bookTop")
    int adorablehamsterpets$getBookTop();

    @Accessor("spread")
    int adorablehamsterpets$getSpread();

    @Accessor("maxSpreads")
    int adorablehamsterpets$getMaxSpreads();

    @Accessor("spread")
    void adorablehamsterpets$setSpread(int spread);

    @Accessor("maxSpreads")
    void adorablehamsterpets$setMaxSpreads(int maxSpreads);
}