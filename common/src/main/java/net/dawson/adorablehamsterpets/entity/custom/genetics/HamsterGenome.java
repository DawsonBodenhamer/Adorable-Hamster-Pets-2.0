package net.dawson.adorablehamsterpets.entity.custom.genetics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;

/**
 * The absolute source of truth for a hamster's appearance and genetic makeup.
 */
public record HamsterGenome(
        String basePaletteId,
        int wildOverlayPattern,          // 0 for none, 1-8 for shapes
        @Nullable String wildOverlayPaletteId,
        int breedingOverlayPattern,      // 0 for none, 1-8 for shapes
        @Nullable String breedingOverlayPaletteId,
        int eyeGenotype                  // 0 = BB (Black), 1 = Br (Carrier), 2 = rr (Red)
) {

    public static final Codec<HamsterGenome> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("basePaletteId").forGetter(HamsterGenome::basePaletteId),
            Codec.INT.fieldOf("wildOverlayPattern").orElse(0).forGetter(HamsterGenome::wildOverlayPattern),
            Codec.STRING.optionalFieldOf("wildOverlayPaletteId").forGetter(g -> Optional.ofNullable(g.wildOverlayPaletteId())),
            Codec.INT.fieldOf("breedingOverlayPattern").orElse(0).forGetter(HamsterGenome::breedingOverlayPattern),
            Codec.STRING.optionalFieldOf("breedingOverlayPaletteId").forGetter(g -> Optional.ofNullable(g.breedingOverlayPaletteId())),
            Codec.INT.fieldOf("eyeGenotype").orElse(0).forGetter(HamsterGenome::eyeGenotype)
    ).apply(instance, (base, wPat, wPal, bPat, bPal, eye) ->
            new HamsterGenome(base, wPat, wPal.orElse(null), bPat, bPal.orElse(null), eye)
    ));

    public static HamsterGenome createDefault() {
        return new HamsterGenome("orange", 0, null, 0, null, 0);
    }

    public NbtCompound saveToNbt() {
        return (NbtCompound) CODEC.encodeStart(NbtOps.INSTANCE, this)
                .getOrThrow(false, error -> {
                    throw new IllegalStateException("Could not encode HamsterGenome: " + error);
                });
    }

    public static HamsterGenome readFromNbt(NbtCompound nbt) {
        return CODEC.parse(NbtOps.INSTANCE, nbt).result().orElseGet(HamsterGenome::createDefault);
    }
}