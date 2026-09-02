package net.dawson.adorablehamsterpets.block.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

import java.util.HashMap;
import java.util.Map;

/** 26.2 port: block-entity render state with GeckoLib's data map bolted on. */
public class HamsterBedRenderState extends BlockEntityRenderState implements GeoRenderState {
    private final Map<DataTicket<?>, Object> geckolibData = new HashMap<>();

    @Override
    public Map<DataTicket<?>, Object> getDataMap() {
        return this.geckolibData;
    }

    /* GeckoLib 5 mixes GeoRenderState into the vanilla render state and its own
       addGeckolibData/hasGeckolibData write to a mixin-private map. Override them so
       every read and write goes through this class's single map. */
    @Override
    public <D> void addGeckolibData(DataTicket<D> dataTicket, D data) {
        this.geckolibData.put(dataTicket, data);
    }

    @Override
    public boolean hasGeckolibData(DataTicket<?> dataTicket) {
        return this.geckolibData.containsKey(dataTicket);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D> D getGeckolibData(DataTicket<D> dataTicket) {
        return (D) this.geckolibData.get(dataTicket);
    }
}
