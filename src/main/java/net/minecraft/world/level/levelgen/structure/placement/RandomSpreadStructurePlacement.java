package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public record RandomSpreadStructurePlacement(int c, int d, RandomSpreadType e, int f, Vec3i g) implements StructurePlacement {
   private final int spacing;
   private final int separation;
   private final RandomSpreadType spreadType;
   private final int salt;
   private final Vec3i locateOffset;
   public static final Codec<RandomSpreadStructurePlacement> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
      return var0.group(Codec.intRange(0, 4096).fieldOf("spacing").forGetter(RandomSpreadStructurePlacement::spacing), Codec.intRange(0, 4096).fieldOf("separation").forGetter(RandomSpreadStructurePlacement::separation), RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter(RandomSpreadStructurePlacement::spreadType), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("salt").forGetter(RandomSpreadStructurePlacement::salt), Vec3i.offsetCodec(16).optionalFieldOf("locate_offset", Vec3i.ZERO).forGetter(RandomSpreadStructurePlacement::locateOffset)).apply(var0, RandomSpreadStructurePlacement::new);
   }).flatXmap((var0) -> {
      return var0.spacing <= var0.separation ? DataResult.error("Spacing has to be larger than separation") : DataResult.success(var0);
   }, DataResult::success).codec();

   public RandomSpreadStructurePlacement(int var1, int var2, RandomSpreadType var3, int var4) {
      this(var1, var2, var3, var4, Vec3i.ZERO);
   }

   public RandomSpreadStructurePlacement(int var1, int var2, RandomSpreadType var3, int var4, Vec3i var5) {
      this.spacing = var1;
      this.separation = var2;
      this.spreadType = var3;
      this.salt = var4;
      this.locateOffset = var5;
   }

   public ChunkPos getPotentialFeatureChunk(long var1, int var3, int var4) {
      int var5 = this.spacing();
      int var6 = this.separation();
      int var7 = Math.floorDiv(var3, var5);
      int var8 = Math.floorDiv(var4, var5);
      WorldgenRandom var9 = new WorldgenRandom(new LegacyRandomSource(0L));
      var9.setLargeFeatureWithSalt(var1, var7, var8, this.salt());
      int var10 = var5 - var6;
      int var11 = this.spreadType().evaluate(var9, var10);
      int var12 = this.spreadType().evaluate(var9, var10);
      return new ChunkPos(var7 * var5 + var11, var8 * var5 + var12);
   }

   public boolean isFeatureChunk(ChunkGenerator var1, long var2, int var4, int var5) {
      ChunkPos var6 = this.getPotentialFeatureChunk(var2, var4, var5);
      return var6.x == var4 && var6.z == var5;
   }

   public StructurePlacementType<?> type() {
      return StructurePlacementType.RANDOM_SPREAD;
   }

   public int spacing() {
      return this.spacing;
   }

   public int separation() {
      return this.separation;
   }

   public RandomSpreadType spreadType() {
      return this.spreadType;
   }

   public int salt() {
      return this.salt;
   }

   public Vec3i locateOffset() {
      return this.locateOffset;
   }
}
