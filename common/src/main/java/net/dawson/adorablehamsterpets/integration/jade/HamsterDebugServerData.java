package net.dawson.adorablehamsterpets.integration.jade;

import com.geckolib.animation.RawAnimation;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.block.entity.HamsterBedBlockEntity;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.entity.custom.genetics.HamsterGenome;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import com.geckolib.animation.AnimationController;

public enum HamsterDebugServerData implements IServerDataProvider<EntityAccessor> {
    INSTANCE;

    private static final Identifier UID = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "hamster_debug_info");

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        Entity entity = accessor.getEntity();
        if (entity instanceof HamsterEntity hamster) {
            data.putBoolean("IsWanderModeActive", hamster.isWanderModeActive());
            data.putBoolean("IsOnTheWayToBed", hamster.isOnTheWayToBed());
            data.putInt("GoToBedDelay", hamster.getGoToBedDelayTicks());

            if (hamster.isWanderModeActive()) {
                hamster.getLinkedBedPos().ifPresent(globalPos -> {
                    Level world = hamster.level();
                    if (world instanceof ServerLevel serverWorld && serverWorld.dimension() == globalPos.dimension()) {
                        if (serverWorld.getBlockEntity(globalPos.pos()) instanceof HamsterBedBlockEntity bedEntity) {
                            data.putString("WanderDistance", bedEntity.getWanderDistance().getSerializedName());
                        }
                    }
                });
            }
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
