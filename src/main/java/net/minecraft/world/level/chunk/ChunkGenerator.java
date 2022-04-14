package net.minecraft.world.level.chunk;

import com.google.common.base.Stopwatch;
import com.mojang.datafixers.Products.P1;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

public abstract class ChunkGenerator implements BiomeManager.NoiseBiomeSource {
   private static final Logger LOGGER;
   public static final Codec<ChunkGenerator> CODEC;
   protected final Registry<StructureSet> structureSets;
   protected final BiomeSource biomeSource;
   protected final BiomeSource runtimeBiomeSource;
   protected final Optional<HolderSet<StructureSet>> structureOverrides;
   private final Map<ConfiguredStructureFeature<?, ?>, List<StructurePlacement>> placementsForFeature;
   private final Map<ConcentricRingsStructurePlacement, CompletableFuture<List<ChunkPos>>> ringPositions;
   private boolean hasGeneratedPositions;
   /** @deprecated */
   @Deprecated
   private final long ringPlacementSeed;

   protected static final <T extends ChunkGenerator> P1<Mu<T>, Registry<StructureSet>> commonCodec(Instance<T> var0) {
      return var0.group(RegistryOps.retrieveRegistry(Registry.STRUCTURE_SET_REGISTRY).forGetter((var0x) -> {
         return var0x.structureSets;
      }));
   }

   public ChunkGenerator(Registry<StructureSet> var1, Optional<HolderSet<StructureSet>> var2, BiomeSource var3) {
      this(var1, var2, var3, var3, 0L);
   }

   public ChunkGenerator(Registry<StructureSet> var1, Optional<HolderSet<StructureSet>> var2, BiomeSource var3, BiomeSource var4, long var5) {
      this.placementsForFeature = new Object2ObjectOpenHashMap();
      this.ringPositions = new Object2ObjectArrayMap();
      this.structureSets = var1;
      this.biomeSource = var3;
      this.runtimeBiomeSource = var4;
      this.structureOverrides = var2;
      this.ringPlacementSeed = var5;
   }

   public Stream<Holder<StructureSet>> possibleStructureSets() {
      return this.structureOverrides.isPresent() ? ((HolderSet)this.structureOverrides.get()).stream() : this.structureSets.holders().map(Holder::hackyErase);
   }

   private void generatePositions() {
      Set var1 = this.runtimeBiomeSource.possibleBiomes();
      this.possibleStructureSets().forEach((var2) -> {
         StructureSet var3 = (StructureSet)var2.value();
         Iterator var4 = var3.structures().iterator();

         while(var4.hasNext()) {
            StructureSet.StructureSelectionEntry var5 = (StructureSet.StructureSelectionEntry)var4.next();
            ((List)this.placementsForFeature.computeIfAbsent((ConfiguredStructureFeature)var5.structure().value(), (var0) -> {
               return new ArrayList();
            })).add(var3.placement());
         }

         StructurePlacement var6 = var3.placement();
         if (var6 instanceof ConcentricRingsStructurePlacement) {
            ConcentricRingsStructurePlacement var7 = (ConcentricRingsStructurePlacement)var6;
            if (var3.structures().stream().anyMatch((var1x) -> {
               Objects.requireNonNull(var1);
               return var1x.generatesInMatchingBiome(var1::contains);
            })) {
               this.ringPositions.put(var7, this.generateRingPositions(var2, var7));
            }
         }

      });
   }

   private CompletableFuture<List<ChunkPos>> generateRingPositions(Holder<StructureSet> var1, ConcentricRingsStructurePlacement var2) {
      return var2.count() == 0 ? CompletableFuture.completedFuture(List.of()) : CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("placement calculation", () -> {
         Stopwatch var3 = Stopwatch.createStarted(Util.TICKER);
         ArrayList var4 = new ArrayList();
         Set var5 = (Set)((StructureSet)var1.value()).structures().stream().flatMap((var0) -> {
            return ((ConfiguredStructureFeature)var0.structure().value()).biomes().stream();
         }).collect(Collectors.toSet());
         int var6 = var2.distance();
         int var7 = var2.count();
         int var8 = var2.spread();
         Random var9 = new Random();
         var9.setSeed(this.ringPlacementSeed);
         double var10 = var9.nextDouble() * 3.141592653589793D * 2.0D;
         int var12 = 0;
         int var13 = 0;

         for(int var14 = 0; var14 < var7; ++var14) {
            double var15 = (double)(4 * var6 + var6 * var13 * 6) + (var9.nextDouble() - 0.5D) * (double)var6 * 2.5D;
            int var17 = (int)Math.round(Math.cos(var10) * var15);
            int var18 = (int)Math.round(Math.sin(var10) * var15);
            BiomeSource var10000 = this.biomeSource;
            int var10001 = SectionPos.sectionToBlockCoord(var17, 8);
            int var10003 = SectionPos.sectionToBlockCoord(var18, 8);
            Objects.requireNonNull(var5);
            Pair var19 = var10000.findBiomeHorizontal(var10001, 0, var10003, 112, var5::contains, var9, this.climateSampler());
            if (var19 != null) {
               BlockPos var20 = (BlockPos)var19.getFirst();
               var17 = SectionPos.blockToSectionCoord(var20.getX());
               var18 = SectionPos.blockToSectionCoord(var20.getZ());
            }

            var4.add(new ChunkPos(var17, var18));
            var10 += 6.283185307179586D / (double)var8;
            ++var12;
            if (var12 == var8) {
               ++var13;
               var12 = 0;
               var8 += 2 * var8 / (var13 + 1);
               var8 = Math.min(var8, var7 - var14);
               var10 += var9.nextDouble() * 3.141592653589793D * 2.0D;
            }
         }

         double var21 = (double)var3.stop().elapsed(TimeUnit.MILLISECONDS) / 1000.0D;
         LOGGER.debug("Calculation for {} took {}s", var1, var21);
         return var4;
      }), Util.backgroundExecutor());
   }

   protected abstract Codec<? extends ChunkGenerator> codec();

   public Optional<ResourceKey<Codec<? extends ChunkGenerator>>> getTypeNameForDataFixer() {
      return Registry.CHUNK_GENERATOR.getResourceKey(this.codec());
   }

   public abstract ChunkGenerator withSeed(long var1);

   public CompletableFuture<ChunkAccess> createBiomes(Registry<Biome> var1, Executor var2, Blender var3, StructureFeatureManager var4, ChunkAccess var5) {
      return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("init_biomes", () -> {
         BiomeSource var10001 = this.runtimeBiomeSource;
         Objects.requireNonNull(var10001);
         var5.fillBiomesFromNoise(var10001::getNoiseBiome, this.climateSampler());
         return var5;
      }), Util.backgroundExecutor());
   }

   public abstract Climate.Sampler climateSampler();

   public Holder<Biome> getNoiseBiome(int var1, int var2, int var3) {
      return this.getBiomeSource().getNoiseBiome(var1, var2, var3, this.climateSampler());
   }

   public abstract void applyCarvers(WorldGenRegion var1, long var2, BiomeManager var4, StructureFeatureManager var5, ChunkAccess var6, GenerationStep.Carving var7);

   @Nullable
   public Pair<BlockPos, Holder<ConfiguredStructureFeature<?, ?>>> findNearestMapFeature(ServerLevel var1, HolderSet<ConfiguredStructureFeature<?, ?>> var2, BlockPos var3, int var4, boolean var5) {
      Set var6 = (Set)var2.stream().flatMap((var0) -> {
         return ((ConfiguredStructureFeature)var0.value()).biomes().stream();
      }).collect(Collectors.toSet());
      if (var6.isEmpty()) {
         return null;
      } else {
         Set var7 = this.runtimeBiomeSource.possibleBiomes();
         if (Collections.disjoint(var7, var6)) {
            return null;
         } else {
            Pair var8 = null;
            double var9 = Double.MAX_VALUE;
            Object2ObjectArrayMap var11 = new Object2ObjectArrayMap();
            Iterator var12 = var2.iterator();

            while(true) {
               Holder var13;
               StructurePlacement var15;
               Stream var10000;
               HolderSet var10001;
               do {
                  if (!var12.hasNext()) {
                     ArrayList var23 = new ArrayList(var11.size());
                     Iterator var24 = var11.entrySet().iterator();

                     while(var24.hasNext()) {
                        Entry var26 = (Entry)var24.next();
                        var15 = (StructurePlacement)var26.getKey();
                        if (var15 instanceof ConcentricRingsStructurePlacement) {
                           ConcentricRingsStructurePlacement var16 = (ConcentricRingsStructurePlacement)var15;
                           BlockPos var17 = this.getNearestGeneratedStructure(var3, var16);
                           double var18 = var3.distSqr(var17);
                           if (var18 < var9) {
                              var9 = var18;
                              var8 = Pair.of(var17, (Holder)((Set)var26.getValue()).iterator().next());
                           }
                        } else if (var15 instanceof RandomSpreadStructurePlacement) {
                           var23.add(var26);
                        }
                     }

                     if (!var23.isEmpty()) {
                        int var25 = SectionPos.blockToSectionCoord(var3.getX());
                        int var27 = SectionPos.blockToSectionCoord(var3.getZ());

                        for(int var28 = 0; var28 <= var4; ++var28) {
                           boolean var29 = false;
                           Iterator var30 = var23.iterator();

                           while(var30.hasNext()) {
                              Entry var31 = (Entry)var30.next();
                              RandomSpreadStructurePlacement var19 = (RandomSpreadStructurePlacement)var31.getKey();
                              Pair var20 = getNearestGeneratedStructure((Set)var31.getValue(), var1, var1.structureFeatureManager(), var25, var27, var28, var5, var1.getSeed(), var19);
                              if (var20 != null) {
                                 var29 = true;
                                 double var21 = var3.distSqr((Vec3i)var20.getFirst());
                                 if (var21 < var9) {
                                    var9 = var21;
                                    var8 = var20;
                                 }
                              }
                           }

                           if (var29) {
                              return var8;
                           }
                        }
                     }

                     return var8;
                  }

                  var13 = (Holder)var12.next();
                  var10000 = var7.stream();
                  var10001 = ((ConfiguredStructureFeature)var13.value()).biomes();
                  Objects.requireNonNull(var10001);
               } while(var10000.noneMatch(var10001::contains));

               Iterator var14 = this.getPlacementsForFeature(var13).iterator();

               while(var14.hasNext()) {
                  var15 = (StructurePlacement)var14.next();
                  ((Set)var11.computeIfAbsent(var15, (var0) -> {
                     return new ObjectArraySet();
                  })).add(var13);
               }
            }
         }
      }
   }

   @Nullable
   private BlockPos getNearestGeneratedStructure(BlockPos var1, ConcentricRingsStructurePlacement var2) {
      List var3 = this.getRingPositionsFor(var2);
      if (var3 == null) {
         throw new IllegalStateException("Somehow tried to find structures for a placement that doesn't exist");
      } else {
         BlockPos var4 = null;
         double var5 = Double.MAX_VALUE;
         BlockPos.MutableBlockPos var7 = new BlockPos.MutableBlockPos();
         Iterator var8 = var3.iterator();

         while(var8.hasNext()) {
            ChunkPos var9 = (ChunkPos)var8.next();
            var7.set(SectionPos.sectionToBlockCoord(var9.x, 8), 32, SectionPos.sectionToBlockCoord(var9.z, 8));
            double var10 = var7.distSqr(var1);
            if (var4 == null) {
               var4 = new BlockPos(var7);
               var5 = var10;
            } else if (var10 < var5) {
               var4 = new BlockPos(var7);
               var5 = var10;
            }
         }

         return var4;
      }
   }

   @Nullable
   private static Pair<BlockPos, Holder<ConfiguredStructureFeature<?, ?>>> getNearestGeneratedStructure(Set<Holder<ConfiguredStructureFeature<?, ?>>> var0, LevelReader var1, StructureFeatureManager var2, int var3, int var4, int var5, boolean var6, long var7, RandomSpreadStructurePlacement var9) {
      int var10 = var9.spacing();

      for(int var11 = -var5; var11 <= var5; ++var11) {
         boolean var12 = var11 == -var5 || var11 == var5;

         for(int var13 = -var5; var13 <= var5; ++var13) {
            boolean var14 = var13 == -var5 || var13 == var5;
            if (var12 || var14) {
               int var15 = var3 + var10 * var11;
               int var16 = var4 + var10 * var13;
               ChunkPos var17 = var9.getPotentialFeatureChunk(var7, var15, var16);
               Iterator var18 = var0.iterator();

               while(var18.hasNext()) {
                  Holder var19 = (Holder)var18.next();
                  StructureCheckResult var20 = var2.checkStructurePresence(var17, (ConfiguredStructureFeature)var19.value(), var6);
                  if (var20 != StructureCheckResult.START_NOT_PRESENT) {
                     if (!var6 && var20 == StructureCheckResult.START_PRESENT) {
                        return Pair.of(StructureFeature.getLocatePos(var9, var17), var19);
                     }

                     ChunkAccess var21 = var1.getChunk(var17.x, var17.z, ChunkStatus.STRUCTURE_STARTS);
                     StructureStart var22 = var2.getStartForFeature(SectionPos.bottomOf(var21), (ConfiguredStructureFeature)var19.value(), var21);
                     if (var22 != null && var22.isValid()) {
                        if (var6 && var22.canBeReferenced()) {
                           var2.addReference(var22);
                           return Pair.of(StructureFeature.getLocatePos(var9, var22.getChunkPos()), var19);
                        }

                        if (!var6) {
                           return Pair.of(StructureFeature.getLocatePos(var9, var22.getChunkPos()), var19);
                        }
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   public void applyBiomeDecoration(WorldGenLevel var1, ChunkAccess var2, StructureFeatureManager var3) {
      ChunkPos var4 = var2.getPos();
      if (!SharedConstants.debugVoidTerrain(var4)) {
         SectionPos var5 = SectionPos.of(var4, var1.getMinSection());
         BlockPos var6 = var5.origin();
         Registry var7 = var1.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
         Map var8 = (Map)var7.stream().collect(Collectors.groupingBy((var0) -> {
            return var0.feature.step().ordinal();
         }));
         List var9 = this.biomeSource.featuresPerStep();
         WorldgenRandom var10 = new WorldgenRandom(new XoroshiroRandomSource(RandomSupport.seedUniquifier()));
         long var11 = var10.setDecorationSeed(var1.getSeed(), var6.getX(), var6.getZ());
         ObjectArraySet var13 = new ObjectArraySet();
         if (this instanceof FlatLevelSource) {
            Stream var10000 = this.biomeSource.possibleBiomes().stream().map(Holder::value);
            Objects.requireNonNull(var13);
            var10000.forEach(var13::add);
         } else {
            ChunkPos.rangeClosed(var5.chunk(), 1).forEach((var2x) -> {
               ChunkAccess var3 = var1.getChunk(var2x.x, var2x.z);
               LevelChunkSection[] var4 = var3.getSections();
               int var5 = var4.length;

               for(int var6 = 0; var6 < var5; ++var6) {
                  LevelChunkSection var7 = var4[var6];
                  var7.getBiomes().getAll((var1x) -> {
                     var13.add((Biome)var1x.value());
                  });
               }

            });
            var13.retainAll((Collection)this.biomeSource.possibleBiomes().stream().map(Holder::value).collect(Collectors.toSet()));
         }

         int var14 = var9.size();

         try {
            Registry var15 = var1.registryAccess().registryOrThrow(Registry.PLACED_FEATURE_REGISTRY);
            int var32 = Math.max(GenerationStep.Decoration.values().length, var14);

            for(int var17 = 0; var17 < var32; ++var17) {
               int var18 = 0;
               Iterator var20;
               CrashReportCategory var33;
               if (var3.shouldGenerateFeatures()) {
                  List var19 = (List)var8.getOrDefault(var17, Collections.emptyList());

                  for(var20 = var19.iterator(); var20.hasNext(); ++var18) {
                     ConfiguredStructureFeature var21 = (ConfiguredStructureFeature)var20.next();
                     var10.setFeatureSeed(var11, var18, var17);
                     Supplier var22 = () -> {
                        Optional var10000 = var7.getResourceKey(var21).map(Object::toString);
                        Objects.requireNonNull(var21);
                        return (String)var10000.orElseGet(var21::toString);
                     };

                     try {
                        var1.setCurrentlyGenerating(var22);
                        var3.startsForFeature(var5, var21).forEach((var6x) -> {
                           var6x.placeInChunk(var1, var3, this, var10, getWritableArea(var2), var4);
                        });
                     } catch (Exception var29) {
                        CrashReport var24 = CrashReport.forThrowable(var29, "Feature placement");
                        var33 = var24.addCategory("Feature");
                        Objects.requireNonNull(var22);
                        var33.setDetail("Description", var22::get);
                        throw new ReportedException(var24);
                     }
                  }
               }

               if (var17 < var14) {
                  IntArraySet var34 = new IntArraySet();
                  var20 = var13.iterator();

                  while(var20.hasNext()) {
                     Biome var36 = (Biome)var20.next();
                     List var38 = var36.getGenerationSettings().features();
                     if (var17 < var38.size()) {
                        HolderSet var23 = (HolderSet)var38.get(var17);
                        BiomeSource.StepFeatureData var41 = (BiomeSource.StepFeatureData)var9.get(var17);
                        var23.stream().map(Holder::value).forEach((var2x) -> {
                           var34.add(var41.indexMapping().applyAsInt(var2x));
                        });
                     }
                  }

                  int var35 = var34.size();
                  int[] var37 = var34.toIntArray();
                  Arrays.sort(var37);
                  BiomeSource.StepFeatureData var39 = (BiomeSource.StepFeatureData)var9.get(var17);

                  for(int var40 = 0; var40 < var35; ++var40) {
                     int var42 = var37[var40];
                     PlacedFeature var25 = (PlacedFeature)var39.features().get(var42);
                     Supplier var26 = () -> {
                        Optional var10000 = var15.getResourceKey(var25).map(Object::toString);
                        Objects.requireNonNull(var25);
                        return (String)var10000.orElseGet(var25::toString);
                     };
                     var10.setFeatureSeed(var11, var42, var17);

                     try {
                        var1.setCurrentlyGenerating(var26);
                        var25.placeWithBiomeCheck(var1, this, var10, var6);
                     } catch (Exception var30) {
                        CrashReport var28 = CrashReport.forThrowable(var30, "Feature placement");
                        var33 = var28.addCategory("Feature");
                        Objects.requireNonNull(var26);
                        var33.setDetail("Description", var26::get);
                        throw new ReportedException(var28);
                     }
                  }
               }
            }

            var1.setCurrentlyGenerating((Supplier)null);
         } catch (Exception var31) {
            CrashReport var16 = CrashReport.forThrowable(var31, "Biome decoration");
            var16.addCategory("Generation").setDetail("CenterX", (Object)var4.x).setDetail("CenterZ", (Object)var4.z).setDetail("Seed", (Object)var11);
            throw new ReportedException(var16);
         }
      }
   }

   public boolean hasFeatureChunkInRange(ResourceKey<StructureSet> var1, long var2, int var4, int var5, int var6) {
      StructureSet var7 = (StructureSet)this.structureSets.get(var1);
      if (var7 == null) {
         return false;
      } else {
         StructurePlacement var8 = var7.placement();

         for(int var9 = var4 - var6; var9 <= var4 + var6; ++var9) {
            for(int var10 = var5 - var6; var10 <= var5 + var6; ++var10) {
               if (var8.isFeatureChunk(this, var2, var9, var10)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private static BoundingBox getWritableArea(ChunkAccess var0) {
      ChunkPos var1 = var0.getPos();
      int var2 = var1.getMinBlockX();
      int var3 = var1.getMinBlockZ();
      LevelHeightAccessor var4 = var0.getHeightAccessorForGeneration();
      int var5 = var4.getMinBuildHeight() + 1;
      int var6 = var4.getMaxBuildHeight() - 1;
      return new BoundingBox(var2, var5, var3, var2 + 15, var6, var3 + 15);
   }

   public abstract void buildSurface(WorldGenRegion var1, StructureFeatureManager var2, ChunkAccess var3);

   public abstract void spawnOriginalMobs(WorldGenRegion var1);

   public int getSpawnHeight(LevelHeightAccessor var1) {
      return 64;
   }

   public BiomeSource getBiomeSource() {
      return this.runtimeBiomeSource;
   }

   public abstract int getGenDepth();

   public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(Holder<Biome> var1, StructureFeatureManager var2, MobCategory var3, BlockPos var4) {
      Map var5 = var2.getAllStructuresAt(var4);
      Iterator var6 = var5.entrySet().iterator();

      while(var6.hasNext()) {
         Entry var7 = (Entry)var6.next();
         ConfiguredStructureFeature var8 = (ConfiguredStructureFeature)var7.getKey();
         StructureSpawnOverride var9 = (StructureSpawnOverride)var8.spawnOverrides.get(var3);
         if (var9 != null) {
            MutableBoolean var10 = new MutableBoolean(false);
            Predicate var11 = var9.boundingBox() == StructureSpawnOverride.BoundingBoxType.PIECE ? (var2x) -> {
               return var2.structureHasPieceAt(var4, var2x);
            } : (var1x) -> {
               return var1x.getBoundingBox().isInside(var4);
            };
            var2.fillStartsForFeature(var8, (LongSet)var7.getValue(), (var2x) -> {
               if (var10.isFalse() && var11.test(var2x)) {
                  var10.setTrue();
               }

            });
            if (var10.isTrue()) {
               return var9.spawns();
            }
         }
      }

      return ((Biome)var1.value()).getMobSettings().getMobs(var3);
   }

   public static Stream<ConfiguredStructureFeature<?, ?>> allConfigurations(Registry<ConfiguredStructureFeature<?, ?>> var0, StructureFeature<?> var1) {
      return var0.stream().filter((var1x) -> {
         return var1x.feature == var1;
      });
   }

   public void createStructures(RegistryAccess var1, StructureFeatureManager var2, ChunkAccess var3, StructureManager var4, long var5) {
      ChunkPos var7 = var3.getPos();
      SectionPos var8 = SectionPos.bottomOf(var3);
      this.possibleStructureSets().forEach((var9) -> {
         StructurePlacement var10 = ((StructureSet)var9.value()).placement();
         List var11 = ((StructureSet)var9.value()).structures();
         Iterator var12 = var11.iterator();

         while(var12.hasNext()) {
            StructureSet.StructureSelectionEntry var13 = (StructureSet.StructureSelectionEntry)var12.next();
            StructureStart var14 = var2.getStartForFeature(var8, (ConfiguredStructureFeature)var13.structure().value(), var3);
            if (var14 != null && var14.isValid()) {
               return;
            }
         }

         if (var10.isFeatureChunk(this, var5, var7.x, var7.z)) {
            if (var11.size() == 1) {
               this.tryGenerateStructure((StructureSet.StructureSelectionEntry)var11.get(0), var2, var1, var4, var5, var3, var7, var8);
            } else {
               ArrayList var19 = new ArrayList(var11.size());
               var19.addAll(var11);
               WorldgenRandom var20 = new WorldgenRandom(new LegacyRandomSource(0L));
               var20.setLargeFeatureSeed(var5, var7.x, var7.z);
               int var21 = 0;

               StructureSet.StructureSelectionEntry var16;
               for(Iterator var15 = var19.iterator(); var15.hasNext(); var21 += var16.weight()) {
                  var16 = (StructureSet.StructureSelectionEntry)var15.next();
               }

               while(!var19.isEmpty()) {
                  int var22 = var20.nextInt(var21);
                  int var23 = 0;

                  for(Iterator var17 = var19.iterator(); var17.hasNext(); ++var23) {
                     StructureSet.StructureSelectionEntry var18 = (StructureSet.StructureSelectionEntry)var17.next();
                     var22 -= var18.weight();
                     if (var22 < 0) {
                        break;
                     }
                  }

                  StructureSet.StructureSelectionEntry var24 = (StructureSet.StructureSelectionEntry)var19.get(var23);
                  if (this.tryGenerateStructure(var24, var2, var1, var4, var5, var3, var7, var8)) {
                     return;
                  }

                  var19.remove(var23);
                  var21 -= var24.weight();
               }

            }
         }
      });
   }

   private boolean tryGenerateStructure(StructureSet.StructureSelectionEntry var1, StructureFeatureManager var2, RegistryAccess var3, StructureManager var4, long var5, ChunkAccess var7, ChunkPos var8, SectionPos var9) {
      ConfiguredStructureFeature var10 = (ConfiguredStructureFeature)var1.structure().value();
      int var11 = fetchReferences(var2, var7, var9, var10);
      HolderSet var12 = var10.biomes();
      Predicate var13 = (var2x) -> {
         return var12.contains(this.adjustBiome(var2x));
      };
      StructureStart var14 = var10.generate(var3, this, this.biomeSource, var4, var5, var8, var11, var7, var13);
      if (var14.isValid()) {
         var2.setStartForFeature(var9, var10, var14, var7);
         return true;
      } else {
         return false;
      }
   }

   private static int fetchReferences(StructureFeatureManager var0, ChunkAccess var1, SectionPos var2, ConfiguredStructureFeature<?, ?> var3) {
      StructureStart var4 = var0.getStartForFeature(var2, var3, var1);
      return var4 != null ? var4.getReferences() : 0;
   }

   protected Holder<Biome> adjustBiome(Holder<Biome> var1) {
      return var1;
   }

   public void createReferences(WorldGenLevel var1, StructureFeatureManager var2, ChunkAccess var3) {
      boolean var4 = true;
      ChunkPos var5 = var3.getPos();
      int var6 = var5.x;
      int var7 = var5.z;
      int var8 = var5.getMinBlockX();
      int var9 = var5.getMinBlockZ();
      SectionPos var10 = SectionPos.bottomOf(var3);

      for(int var11 = var6 - 8; var11 <= var6 + 8; ++var11) {
         for(int var12 = var7 - 8; var12 <= var7 + 8; ++var12) {
            long var13 = ChunkPos.asLong(var11, var12);
            Iterator var15 = var1.getChunk(var11, var12).getAllStarts().values().iterator();

            while(var15.hasNext()) {
               StructureStart var16 = (StructureStart)var15.next();

               try {
                  if (var16.isValid() && var16.getBoundingBox().intersects(var8, var9, var8 + 15, var9 + 15)) {
                     var2.addReferenceForFeature(var10, var16.getFeature(), var13, var3);
                     DebugPackets.sendStructurePacket(var1, var16);
                  }
               } catch (Exception var21) {
                  CrashReport var18 = CrashReport.forThrowable(var21, "Generating structure reference");
                  CrashReportCategory var19 = var18.addCategory("Structure");
                  Optional var20 = var1.registryAccess().registry(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
                  var19.setDetail("Id", () -> {
                     return (String)var20.map((var1) -> {
                        return var1.getKey(var16.getFeature()).toString();
                     }).orElse("UNKNOWN");
                  });
                  var19.setDetail("Name", () -> {
                     return Registry.STRUCTURE_FEATURE.getKey(var16.getFeature().feature).toString();
                  });
                  var19.setDetail("Class", () -> {
                     return var16.getFeature().getClass().getCanonicalName();
                  });
                  throw new ReportedException(var18);
               }
            }
         }
      }

   }

   public abstract CompletableFuture<ChunkAccess> fillFromNoise(Executor var1, Blender var2, StructureFeatureManager var3, ChunkAccess var4);

   public abstract int getSeaLevel();

   public abstract int getMinY();

   public abstract int getBaseHeight(int var1, int var2, Heightmap.Types var3, LevelHeightAccessor var4);

   public abstract NoiseColumn getBaseColumn(int var1, int var2, LevelHeightAccessor var3);

   public int getFirstFreeHeight(int var1, int var2, Heightmap.Types var3, LevelHeightAccessor var4) {
      return this.getBaseHeight(var1, var2, var3, var4);
   }

   public int getFirstOccupiedHeight(int var1, int var2, Heightmap.Types var3, LevelHeightAccessor var4) {
      return this.getBaseHeight(var1, var2, var3, var4) - 1;
   }

   public void ensureStructuresGenerated() {
      if (!this.hasGeneratedPositions) {
         this.generatePositions();
         this.hasGeneratedPositions = true;
      }

   }

   @Nullable
   public List<ChunkPos> getRingPositionsFor(ConcentricRingsStructurePlacement var1) {
      this.ensureStructuresGenerated();
      CompletableFuture var2 = (CompletableFuture)this.ringPositions.get(var1);
      return var2 != null ? (List)var2.join() : null;
   }

   private List<StructurePlacement> getPlacementsForFeature(Holder<ConfiguredStructureFeature<?, ?>> var1) {
      this.ensureStructuresGenerated();
      return (List)this.placementsForFeature.getOrDefault(var1.value(), List.of());
   }

   public abstract void addDebugScreenInfo(List<String> var1, BlockPos var2);

   static {
      Registry.register(Registry.CHUNK_GENERATOR, (String)"noise", NoiseBasedChunkGenerator.CODEC);
      Registry.register(Registry.CHUNK_GENERATOR, (String)"flat", FlatLevelSource.CODEC);
      Registry.register(Registry.CHUNK_GENERATOR, (String)"debug", DebugLevelSource.CODEC);
      LOGGER = LogUtils.getLogger();
      CODEC = Registry.CHUNK_GENERATOR.byNameCodec().dispatchStable(ChunkGenerator::codec, Function.identity());
   }
}
