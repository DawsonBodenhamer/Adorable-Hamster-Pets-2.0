package net.dawson.adorablehamsterpets.integration.jade;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterGenome;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterPaletteManager;
import net.dawson.adorablehamsterpets.util.MiscUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum HamsterGeneticsServerData implements IServerDataProvider<EntityAccessor> {
    INSTANCE;

    private static final Identifier UID = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "hamster_genetics");

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (accessor.getEntity() instanceof HamsterEntity hamster) {
            data.put("HamsterGenome", hamster.getGenome().saveToNbt());
            data.putLong("TotalAgeTicks", hamster.totalAgeTicks);
            data.putInt("AggressionState", hamster.getAggressionState().ordinal());
            data.putBoolean("RedstoneFevered", hamster.hasRedstoneFever());
            data.putInt("RedstoneFeverRecoveryStage", hamster.getRedstoneFeverRecoveryStage());
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
