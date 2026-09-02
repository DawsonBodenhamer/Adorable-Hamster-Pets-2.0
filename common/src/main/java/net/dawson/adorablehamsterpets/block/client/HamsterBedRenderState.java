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
}
