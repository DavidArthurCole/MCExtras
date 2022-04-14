package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public class TheEndBiomeSource extends BiomeSource {
   public static final Codec<TheEndBiomeSource> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(RegistryOps.retrieveRegistry(Registry.BIOME_REGISTRY).forGetter((var0x) -> {
         return null;
      }), Codec.LONG.fieldOf("seed").stable().forGetter((var0x) -> {
         return var0x.seed;
      })).apply(var0, var0.stable(TheEndBiomeSource::new));
   });
   private static final float ISLAND_THRESHOLD = -0.9F;
   public static final int ISLAND_CHUNK_DISTANCE = 64;
   private static final long ISLAND_CHUNK_DISTANCE_SQR = 4096L;
   private final SimplexNoise islandNoise;
   private final long seed;
   private final Holder<Biome> end;
   private final Holder<Biome> highlands;
   private final Holder<Biome> midlands;
   private final Holder<Biome> islands;
   private final Holder<Biome> barrens;

   public TheEndBiomeSource(Registry<Biome> var1, long var2) {
      this(var2, var1.getOrCreateHolder(Biomes.THE_END), var1.getOrCreateHolder(Biomes.END_HIGHLANDS), var1.getOrCreateHolder(Biomes.END_MIDLANDS), var1.getOrCreateHolder(Biomes.SMALL_END_ISLANDS), var1.getOrCreateHolder(Biomes.END_BARRENS));
   }

   private TheEndBiomeSource(long var1, Holder<Biome> var3, Holder<Biome> var4, Holder<Biome> var5, Holder<Biome> var6, Holder<Biome> var7) {
      super((List)ImmutableList.of(var3, var4, var5, var6, var7));
      this.seed = var1;
      this.end = var3;
      this.highlands = var4;
      this.midlands = var5;
      this.islands = var6;
      this.barrens = var7;
      WorldgenRandom var8 = new WorldgenRandom(new LegacyRandomSource(var1));
      var8.consumeCount(17292);
      this.islandNoise = new SimplexNoise(var8);
   }

   protected Codec<? extends BiomeSource> codec() {
      return CODEC;
   }

   public BiomeSource withSeed(long var1) {
      return new TheEndBiomeSource(var1, this.end, this.highlands, this.midlands, this.islands, this.barrens);
   }

   public Holder<Biome> getNoiseBiome(int var1, int var2, int var3, Climate.Sampler var4) {
      int var5 = var1 >> 2;
      int var6 = var3 >> 2;
      if ((long)var5 * (long)var5 + (long)var6 * (long)var6 <= 4096L) {
         return this.end;
      } else {
         float var7 = getHeightValue(this.islandNoise, var5 * 2 + 1, var6 * 2 + 1);
         if (var7 > 40.0F) {
            return this.highlands;
         } else if (var7 >= 0.0F) {
            return this.midlands;
         } else {
            return var7 < -20.0F ? this.islands : this.barrens;
         }
      }
   }

   public boolean stable(long var1) {
      return this.seed == var1;
   }

   public static float getHeightValue(SimplexNoise var0, int var1, int var2) {
      int var3 = var1 / 2;
      int var4 = var2 / 2;
      int var5 = var1 % 2;
      int var6 = var2 % 2;
      float var7 = 100.0F - Mth.sqrt((float)(var1 * var1 + var2 * var2)) * 8.0F;
      var7 = Mth.clamp(var7, -100.0F, 80.0F);

      for(int var8 = -12; var8 <= 12; ++var8) {
         for(int var9 = -12; var9 <= 12; ++var9) {
            long var10 = (long)(var3 + var8);
            long var12 = (long)(var4 + var9);
            if (var10 * var10 + var12 * var12 > 4096L && var0.getValue((double)var10, (double)var12) < -0.8999999761581421D) {
               float var14 = (Mth.abs((float)var10) * 3439.0F + Mth.abs((float)var12) * 147.0F) % 13.0F + 9.0F;
               float var15 = (float)(var5 - var8 * 2);
               float var16 = (float)(var6 - var9 * 2);
               float var17 = 100.0F - Mth.sqrt(var15 * var15 + var16 * var16) * var14;
               var17 = Mth.clamp(var17, -100.0F, 80.0F);
               var7 = Math.max(var7, var17);
            }
         }
      }

      return var7;
   }
}
