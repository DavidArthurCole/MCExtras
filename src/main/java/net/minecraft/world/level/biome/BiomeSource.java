package net.minecraft.world.level.biome;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.util.Graph;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.apache.commons.lang3.mutable.MutableInt;

public abstract class BiomeSource implements BiomeResolver {
   public static final Codec<BiomeSource> CODEC;
   private final Set<Holder<Biome>> possibleBiomes;
   private final Supplier<List<BiomeSource.StepFeatureData>> featuresPerStep;

   protected BiomeSource(Stream<Holder<Biome>> var1) {
      this(var1.distinct().toList());
   }

   protected BiomeSource(List<Holder<Biome>> var1) {
      this.possibleBiomes = new ObjectLinkedOpenHashSet(var1);
      this.featuresPerStep = Suppliers.memoize(() -> {
         return this.buildFeaturesPerStep(var1, true);
      });
   }

   private List<BiomeSource.StepFeatureData> buildFeaturesPerStep(List<Holder<Biome>> var1, boolean var2) {
      Object2IntOpenHashMap var3 = new Object2IntOpenHashMap();
      MutableInt var4 = new MutableInt(0);
      Comparator var5 = Comparator.comparingInt(FeatureData::step).thenComparingInt(FeatureData::featureIndex);
      TreeMap var6 = new TreeMap(var5);
      int var7 = 0;
      Iterator var8 = var1.iterator();

      record FeatureData(int a, int b, PlacedFeature c) {
         private final int featureIndex;
         private final int step;
         private final PlacedFeature feature;

         FeatureData(int var1, int var2, PlacedFeature var3) {
            this.featureIndex = var1;
            this.step = var2;
            this.feature = var3;
         }

         public int featureIndex() {
            return this.featureIndex;
         }

         public int step() {
            return this.step;
         }

         public PlacedFeature feature() {
            return this.feature;
         }
      }

      while(var8.hasNext()) {
         Holder var9 = (Holder)var8.next();
         Biome var10 = (Biome)var9.value();
         ArrayList var11 = Lists.newArrayList();
         List var12 = var10.getGenerationSettings().features();
         var7 = Math.max(var7, var12.size());

         int var13;
         for(var13 = 0; var13 < var12.size(); ++var13) {
            Iterator var14 = ((HolderSet)var12.get(var13)).iterator();

            while(var14.hasNext()) {
               Holder var15 = (Holder)var14.next();
               PlacedFeature var16 = (PlacedFeature)var15.value();
               var11.add(new FeatureData(var3.computeIfAbsent(var16, (var1x) -> {
                  return var4.getAndIncrement();
               }), var13, var16));
            }
         }

         for(var13 = 0; var13 < var11.size(); ++var13) {
            Set var26 = (Set)var6.computeIfAbsent((FeatureData)var11.get(var13), (var1x) -> {
               return new TreeSet(var5);
            });
            if (var13 < var11.size() - 1) {
               var26.add((FeatureData)var11.get(var13 + 1));
            }
         }
      }

      TreeSet var19 = new TreeSet(var5);
      TreeSet var20 = new TreeSet(var5);
      ArrayList var21 = Lists.newArrayList();
      Iterator var22 = var6.keySet().iterator();

      while(var22.hasNext()) {
         FeatureData var24 = (FeatureData)var22.next();
         if (!var20.isEmpty()) {
            throw new IllegalStateException("You somehow broke the universe; DFS bork (iteration finished with non-empty in-progress vertex set");
         }

         if (!var19.contains(var24)) {
            Objects.requireNonNull(var21);
            if (Graph.depthFirstSearch(var6, var19, var20, var21::add, var24)) {
               if (!var2) {
                  throw new IllegalStateException("Feature order cycle found");
               }

               ArrayList var27 = new ArrayList(var1);

               int var28;
               do {
                  var28 = var27.size();
                  ListIterator var30 = var27.listIterator();

                  while(var30.hasNext()) {
                     Holder var32 = (Holder)var30.next();
                     var30.remove();

                     try {
                        this.buildFeaturesPerStep(var27, false);
                     } catch (IllegalStateException var18) {
                        continue;
                     }

                     var30.add(var32);
                  }
               } while(var28 != var27.size());

               throw new IllegalStateException("Feature order cycle found, involved biomes: " + var27);
            }
         }
      }

      Collections.reverse(var21);
      Builder var23 = ImmutableList.builder();

      for(int var25 = 0; var25 < var7; ++var25) {
         List var29 = (List)var21.stream().filter((var1x) -> {
            return var1x.step() == var25;
         }).map(FeatureData::feature).collect(Collectors.toList());
         int var31 = var29.size();
         Object2IntOpenCustomHashMap var33 = new Object2IntOpenCustomHashMap(var31, Util.identityStrategy());

         for(int var17 = 0; var17 < var31; ++var17) {
            var33.put((PlacedFeature)var29.get(var17), var17);
         }

         var23.add(new BiomeSource.StepFeatureData(var29, var33));
      }

      return var23.build();
   }

   protected abstract Codec<? extends BiomeSource> codec();

   public abstract BiomeSource withSeed(long var1);

   public Set<Holder<Biome>> possibleBiomes() {
      return this.possibleBiomes;
   }

   public Set<Holder<Biome>> getBiomesWithin(int var1, int var2, int var3, int var4, Climate.Sampler var5) {
      int var6 = QuartPos.fromBlock(var1 - var4);
      int var7 = QuartPos.fromBlock(var2 - var4);
      int var8 = QuartPos.fromBlock(var3 - var4);
      int var9 = QuartPos.fromBlock(var1 + var4);
      int var10 = QuartPos.fromBlock(var2 + var4);
      int var11 = QuartPos.fromBlock(var3 + var4);
      int var12 = var9 - var6 + 1;
      int var13 = var10 - var7 + 1;
      int var14 = var11 - var8 + 1;
      HashSet var15 = Sets.newHashSet();

      for(int var16 = 0; var16 < var14; ++var16) {
         for(int var17 = 0; var17 < var12; ++var17) {
            for(int var18 = 0; var18 < var13; ++var18) {
               int var19 = var6 + var17;
               int var20 = var7 + var18;
               int var21 = var8 + var16;
               var15.add(this.getNoiseBiome(var19, var20, var21, var5));
            }
         }
      }

      return var15;
   }

   @Nullable
   public Pair<BlockPos, Holder<Biome>> findBiomeHorizontal(int var1, int var2, int var3, int var4, Predicate<Holder<Biome>> var5, Random var6, Climate.Sampler var7) {
      return this.findBiomeHorizontal(var1, var2, var3, var4, 1, var5, var6, false, var7);
   }

   @Nullable
   public Pair<BlockPos, Holder<Biome>> findBiomeHorizontal(int var1, int var2, int var3, int var4, int var5, Predicate<Holder<Biome>> var6, Random var7, boolean var8, Climate.Sampler var9) {
      int var10 = QuartPos.fromBlock(var1);
      int var11 = QuartPos.fromBlock(var3);
      int var12 = QuartPos.fromBlock(var4);
      int var13 = QuartPos.fromBlock(var2);
      Pair var14 = null;
      int var15 = 0;
      int var16 = var8 ? 0 : var12;

      for(int var17 = var16; var17 <= var12; var17 += var5) {
         for(int var18 = SharedConstants.debugGenerateSquareTerrainWithoutNoise ? 0 : -var17; var18 <= var17; var18 += var5) {
            boolean var19 = Math.abs(var18) == var17;

            for(int var20 = -var17; var20 <= var17; var20 += var5) {
               if (var8) {
                  boolean var21 = Math.abs(var20) == var17;
                  if (!var21 && !var19) {
                     continue;
                  }
               }

               int var25 = var10 + var20;
               int var22 = var11 + var18;
               Holder var23 = this.getNoiseBiome(var25, var13, var22, var9);
               if (var6.test(var23)) {
                  if (var14 == null || var7.nextInt(var15 + 1) == 0) {
                     BlockPos var24 = new BlockPos(QuartPos.toBlock(var25), var2, QuartPos.toBlock(var22));
                     if (var8) {
                        return Pair.of(var24, var23);
                     }

                     var14 = Pair.of(var24, var23);
                  }

                  ++var15;
               }
            }
         }
      }

      return var14;
   }

   public abstract Holder<Biome> getNoiseBiome(int var1, int var2, int var3, Climate.Sampler var4);

   public void addDebugInfo(List<String> var1, BlockPos var2, Climate.Sampler var3) {
   }

   public List<BiomeSource.StepFeatureData> featuresPerStep() {
      return (List)this.featuresPerStep.get();
   }

   static {
      Registry.register(Registry.BIOME_SOURCE, (String)"fixed", FixedBiomeSource.CODEC);
      Registry.register(Registry.BIOME_SOURCE, (String)"multi_noise", MultiNoiseBiomeSource.CODEC);
      Registry.register(Registry.BIOME_SOURCE, (String)"checkerboard", CheckerboardColumnBiomeSource.CODEC);
      Registry.register(Registry.BIOME_SOURCE, (String)"the_end", TheEndBiomeSource.CODEC);
      CODEC = Registry.BIOME_SOURCE.byNameCodec().dispatchStable(BiomeSource::codec, Function.identity());
   }

   public static record StepFeatureData(List<PlacedFeature> a, ToIntFunction<PlacedFeature> b) {
      private final List<PlacedFeature> features;
      private final ToIntFunction<PlacedFeature> indexMapping;

      public StepFeatureData(List<PlacedFeature> var1, ToIntFunction<PlacedFeature> var2) {
         this.features = var1;
         this.indexMapping = var2;
      }

      public List<PlacedFeature> features() {
         return this.features;
      }

      public ToIntFunction<PlacedFeature> indexMapping() {
         return this.indexMapping;
      }
   }
}
