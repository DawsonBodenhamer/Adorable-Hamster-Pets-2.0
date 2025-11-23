package net.dawson.adorablehamsterpets.mixin.client.accessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClickableWidget.class)
@Environment(EnvType.CLIENT)
public interface ClickableWidgetAccessor {
    @Accessor("height")
    void adorablehamsterpets$setHeight(int height);
}