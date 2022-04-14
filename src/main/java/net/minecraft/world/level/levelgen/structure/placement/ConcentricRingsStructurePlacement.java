package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;

public record ConcentricRingsStructurePlacement(int c, int d, int e) implements StructurePlacement {
   private final int distance;
   private final int spread;
   private final int count;
   public static final Codec<ConcentricRingsStructurePlacement> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(Codec.intRange(0, 1023).fieldOf("distance").forGetter(ConcentricRingsStructurePlacement::distance), Codec.intRange(0, 1023).fieldOf("spread").forGetter(ConcentricRingsStructurePlacement::spread), Codec.intRange(1, 4095).fieldOf("count").forGetter(ConcentricRingsStructurePlacement::count)).apply(var0, ConcentricRingsStructurePlacement::new);
   });

   public ConcentricRingsStructurePlacement(int var1, int var2, int var3) {
      this.distance = var1;
      this.spread = var2;
      this.count = var3;
   }

   public boolean isFeatureChunk(ChunkGenerator var1, long var2, int var4, int var5) {
      List var6 = var1.getRingPositionsFor(this);
      return var6 == null ? false : var6.contains(new ChunkPos(var4, var5));
   }

   public StructurePlacementType<?> type() {
      return StructurePlacementType.CONCENTRIC_RINGS;
   }

   public int distance() {
      return this.distance;
   }

   public int spread() {
      return this.spread;
   }

   public int count() {
      return this.count;
   }
}
