package net.minecraft.world.level.dimension;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

public final class LevelStem {
   public static final Codec<LevelStem> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(DimensionType.CODEC.fieldOf("type").forGetter(LevelStem::typeHolder), ChunkGenerator.CODEC.fieldOf("generator").forGetter(LevelStem::generator)).apply(var0, var0.stable(LevelStem::new));
   });
   public static final ResourceKey<LevelStem> OVERWORLD;
   public static final ResourceKey<LevelStem> NETHER;
   public static final ResourceKey<LevelStem> END;
   private static final Set<ResourceKey<LevelStem>> BUILTIN_ORDER;
   private final Holder<DimensionType> type;
   private final ChunkGenerator generator;

   public LevelStem(Holder<DimensionType> var1, ChunkGenerator var2) {
      this.type = var1;
      this.generator = var2;
   }

   public Holder<DimensionType> typeHolder() {
      return this.type;
   }

   public ChunkGenerator generator() {
      return this.generator;
   }

   public static Registry<LevelStem> sortMap(Registry<LevelStem> var0) {
      MappedRegistry var1 = new MappedRegistry(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental(), (Function)null);
      Iterator var2 = BUILTIN_ORDER.iterator();

      while(var2.hasNext()) {
         ResourceKey var3 = (ResourceKey)var2.next();
         LevelStem var4 = (LevelStem)var0.get(var3);
         if (var4 != null) {
            var1.register(var3, var4, var0.lifecycle(var4));
         }
      }

      var2 = var0.entrySet().iterator();

      while(var2.hasNext()) {
         Entry var5 = (Entry)var2.next();
         ResourceKey var6 = (ResourceKey)var5.getKey();
         if (!BUILTIN_ORDER.contains(var6)) {
            var1.register(var6, (LevelStem)var5.getValue(), var0.lifecycle((LevelStem)var5.getValue()));
         }
      }

      return var1;
   }

   public static boolean stable(long var0, Registry<LevelStem> var2) {
      if (var2.size() != BUILTIN_ORDER.size()) {
         return false;
      } else {
         Optional var3 = var2.getOptional(OVERWORLD);
         Optional var4 = var2.getOptional(NETHER);
         Optional var5 = var2.getOptional(END);
         if (!var3.isEmpty() && !var4.isEmpty() && !var5.isEmpty()) {
            if (!((LevelStem)var3.get()).typeHolder().is(DimensionType.OVERWORLD_LOCATION) && !((LevelStem)var3.get()).typeHolder().is(DimensionType.OVERWORLD_CAVES_LOCATION)) {
               return false;
            } else if (!((LevelStem)var4.get()).typeHolder().is(DimensionType.NETHER_LOCATION)) {
               return false;
            } else if (!((LevelStem)var5.get()).typeHolder().is(DimensionType.END_LOCATION)) {
               return false;
            } else if (((LevelStem)var4.get()).generator() instanceof NoiseBasedChunkGenerator && ((LevelStem)var5.get()).generator() instanceof NoiseBasedChunkGenerator) {
               NoiseBasedChunkGenerator var6 = (NoiseBasedChunkGenerator)((LevelStem)var4.get()).generator();
               NoiseBasedChunkGenerator var7 = (NoiseBasedChunkGenerator)((LevelStem)var5.get()).generator();
               if (!var6.stable(var0, NoiseGeneratorSettings.NETHER)) {
                  return false;
               } else if (!var7.stable(var0, NoiseGeneratorSettings.END)) {
                  return false;
               } else if (!(var6.getBiomeSource() instanceof MultiNoiseBiomeSource)) {
                  return false;
               } else {
                  MultiNoiseBiomeSource var8 = (MultiNoiseBiomeSource)var6.getBiomeSource();
                  if (!var8.stable(MultiNoiseBiomeSource.Preset.NETHER)) {
                     return false;
                  } else {
                     BiomeSource var9 = ((LevelStem)var3.get()).generator().getBiomeSource();
                     if (var9 instanceof MultiNoiseBiomeSource && !((MultiNoiseBiomeSource)var9).stable(MultiNoiseBiomeSource.Preset.OVERWORLD)) {
                        return false;
                     } else if (!(var7.getBiomeSource() instanceof TheEndBiomeSource)) {
                        return false;
                     } else {
                        TheEndBiomeSource var10 = (TheEndBiomeSource)var7.getBiomeSource();
                        return var10.stable(var0);
                     }
                  }
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   static {
      OVERWORLD = ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, new ResourceLocation("overworld"));
      NETHER = ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, new ResourceLocation("the_nether"));
      END = ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, new ResourceLocation("the_end"));
      BUILTIN_ORDER = ImmutableSet.of(OVERWORLD, NETHER, END);
   }
}
