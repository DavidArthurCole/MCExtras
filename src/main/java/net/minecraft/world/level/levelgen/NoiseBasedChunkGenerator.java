package net.minecraft.world.level.levelgen;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TerrainShaper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class NoiseBasedChunkGenerator extends ChunkGenerator {
   public static final Codec<NoiseBasedChunkGenerator> CODEC = RecordCodecBuilder.create((var0) -> {
      return commonCodec(var0).and(var0.group(RegistryOps.retrieveRegistry(Registry.NOISE_REGISTRY).forGetter((var0x) -> {
         return var0x.noises;
      }), BiomeSource.CODEC.fieldOf("biome_source").forGetter((var0x) -> {
         return var0x.biomeSource;
      }), Codec.LONG.fieldOf("seed").stable().forGetter((var0x) -> {
         return var0x.seed;
      }), NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter((var0x) -> {
         return var0x.settings;
      }))).apply(var0, var0.stable(NoiseBasedChunkGenerator::new));
   });
   private static final BlockState AIR;
   private static final BlockState[] EMPTY_COLUMN;
   protected final BlockState defaultBlock;
   private final Registry<NormalNoise.NoiseParameters> noises;
   private final long seed;
   protected final Holder<NoiseGeneratorSettings> settings;
   private final NoiseRouter router;
   private final Climate.Sampler sampler;
   private final SurfaceSystem surfaceSystem;
   private final Aquifer.FluidPicker globalFluidPicker;

   public NoiseBasedChunkGenerator(Registry<StructureSet> var1, Registry<NormalNoise.NoiseParameters> var2, BiomeSource var3, long var4, Holder<NoiseGeneratorSettings> var6) {
      this(var1, var2, var3, var3, var4, var6);
   }

   private NoiseBasedChunkGenerator(Registry<StructureSet> var1, Registry<NormalNoise.NoiseParameters> var2, BiomeSource var3, BiomeSource var4, long var5, Holder<NoiseGeneratorSettings> var7) {
      super(var1, Optional.empty(), var3, var4, var5);
      this.noises = var2;
      this.seed = var5;
      this.settings = var7;
      NoiseGeneratorSettings var8 = (NoiseGeneratorSettings)this.settings.value();
      this.defaultBlock = var8.defaultBlock();
      NoiseSettings var9 = var8.noiseSettings();
      this.router = var8.createNoiseRouter(var2, var5);
      this.sampler = new Climate.Sampler(this.router.temperature(), this.router.humidity(), this.router.continents(), this.router.erosion(), this.router.depth(), this.router.ridges(), this.router.spawnTarget());
      Aquifer.FluidStatus var10 = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
      int var11 = var8.seaLevel();
      Aquifer.FluidStatus var12 = new Aquifer.FluidStatus(var11, var8.defaultFluid());
      Aquifer.FluidStatus var13 = new Aquifer.FluidStatus(var9.minY() - 1, Blocks.AIR.defaultBlockState());
      this.globalFluidPicker = (var4x, var5x, var6) -> {
         return var5x < Math.min(-54, var11) ? var10 : var12;
      };
      this.surfaceSystem = new SurfaceSystem(var2, this.defaultBlock, var11, var5, var8.getRandomSource());
   }

   public CompletableFuture<ChunkAccess> createBiomes(Registry<Biome> var1, Executor var2, Blender var3, StructureFeatureManager var4, ChunkAccess var5) {
      return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("init_biomes", () -> {
         this.doCreateBiomes(var3, var4, var5);
         return var5;
      }), Util.backgroundExecutor());
   }

   private void doCreateBiomes(Blender var1, StructureFeatureManager var2, ChunkAccess var3) {
      NoiseChunk var4 = var3.getOrCreateNoiseChunk(this.router, () -> {
         return new Beardifier(var2, var3);
      }, (NoiseGeneratorSettings)this.settings.value(), this.globalFluidPicker, var1);
      BiomeResolver var5 = BelowZeroRetrogen.getBiomeResolver(var1.getBiomeResolver(this.runtimeBiomeSource), var3);
      var3.fillBiomesFromNoise(var5, var4.cachedClimateSampler(this.router));
   }

   @VisibleForDebug
   public NoiseRouter router() {
      return this.router;
   }

   public Climate.Sampler climateSampler() {
      return this.sampler;
   }

   protected Codec<? extends ChunkGenerator> codec() {
      return CODEC;
   }

   public ChunkGenerator withSeed(long var1) {
      return new NoiseBasedChunkGenerator(this.structureSets, this.noises, this.biomeSource.withSeed(var1), var1, this.settings);
   }

   public boolean stable(long var1, ResourceKey<NoiseGeneratorSettings> var3) {
      return this.seed == var1 && this.settings.is(var3);
   }

   public int getBaseHeight(int var1, int var2, Heightmap.Types var3, LevelHeightAccessor var4) {
      NoiseSettings var5 = ((NoiseGeneratorSettings)this.settings.value()).noiseSettings();
      int var6 = Math.max(var5.minY(), var4.getMinBuildHeight());
      int var7 = Math.min(var5.minY() + var5.height(), var4.getMaxBuildHeight());
      int var8 = Mth.intFloorDiv(var6, var5.getCellHeight());
      int var9 = Mth.intFloorDiv(var7 - var6, var5.getCellHeight());
      return var9 <= 0 ? var4.getMinBuildHeight() : this.iterateNoiseColumn(var1, var2, (BlockState[])null, var3.isOpaque(), var8, var9).orElse(var4.getMinBuildHeight());
   }

   public NoiseColumn getBaseColumn(int var1, int var2, LevelHeightAccessor var3) {
      NoiseSettings var4 = ((NoiseGeneratorSettings)this.settings.value()).noiseSettings();
      int var5 = Math.max(var4.minY(), var3.getMinBuildHeight());
      int var6 = Math.min(var4.minY() + var4.height(), var3.getMaxBuildHeight());
      int var7 = Mth.intFloorDiv(var5, var4.getCellHeight());
      int var8 = Mth.intFloorDiv(var6 - var5, var4.getCellHeight());
      if (var8 <= 0) {
         return new NoiseColumn(var5, EMPTY_COLUMN);
      } else {
         BlockState[] var9 = new BlockState[var8 * var4.getCellHeight()];
         this.iterateNoiseColumn(var1, var2, var9, (Predicate)null, var7, var8);
         return new NoiseColumn(var5, var9);
      }
   }

   public void addDebugScreenInfo(List<String> var1, BlockPos var2) {
      DecimalFormat var3 = new DecimalFormat("0.000");
      DensityFunction.SinglePointContext var4 = new DensityFunction.SinglePointContext(var2.getX(), var2.getY(), var2.getZ());
      double var5 = this.router.ridges().compute(var4);
      String var10001 = var3.format(this.router.temperature().compute(var4));
      var1.add("NoiseRouter T: " + var10001 + " H: " + var3.format(this.router.humidity().compute(var4)) + " C: " + var3.format(this.router.continents().compute(var4)) + " E: " + var3.format(this.router.erosion().compute(var4)) + " D: " + var3.format(this.router.depth().compute(var4)) + " W: " + var3.format(var5) + " PV: " + var3.format((double)TerrainShaper.peaksAndValleys((float)var5)) + " AS: " + var3.format(this.router.initialDensityWithoutJaggedness().compute(var4)) + " N: " + var3.format(this.router.finalDensity().compute(var4)));
   }

   private OptionalInt iterateNoiseColumn(int var1, int var2, @Nullable BlockState[] var3, @Nullable Predicate<BlockState> var4, int var5, int var6) {
      NoiseSettings var7 = ((NoiseGeneratorSettings)this.settings.value()).noiseSettings();
      int var8 = var7.getCellWidth();
      int var9 = var7.getCellHeight();
      int var10 = Math.floorDiv(var1, var8);
      int var11 = Math.floorDiv(var2, var8);
      int var12 = Math.floorMod(var1, var8);
      int var13 = Math.floorMod(var2, var8);
      int var14 = var10 * var8;
      int var15 = var11 * var8;
      double var16 = (double)var12 / (double)var8;
      double var18 = (double)var13 / (double)var8;
      NoiseChunk var20 = NoiseChunk.forColumn(var14, var15, var5, var6, this.router, (NoiseGeneratorSettings)this.settings.value(), this.globalFluidPicker);
      var20.initializeForFirstCellX();
      var20.advanceCellX(0);

      for(int var21 = var6 - 1; var21 >= 0; --var21) {
         var20.selectCellYZ(var21, 0);

         for(int var22 = var9 - 1; var22 >= 0; --var22) {
            int var23 = (var5 + var21) * var9 + var22;
            double var24 = (double)var22 / (double)var9;
            var20.updateForY(var23, var24);
            var20.updateForX(var1, var16);
            var20.updateForZ(var2, var18);
            BlockState var26 = var20.getInterpolatedState();
            BlockState var27 = var26 == null ? this.defaultBlock : var26;
            if (var3 != null) {
               int var28 = var21 * var9 + var22;
               var3[var28] = var27;
            }

            if (var4 != null && var4.test(var27)) {
               var20.stopInterpolation();
               return OptionalInt.of(var23 + 1);
            }
         }
      }

      var20.stopInterpolation();
      return OptionalInt.empty();
   }

   public void buildSurface(WorldGenRegion var1, StructureFeatureManager var2, ChunkAccess var3) {
      if (!SharedConstants.debugVoidTerrain(var3.getPos())) {
         WorldGenerationContext var4 = new WorldGenerationContext(this, var1);
         NoiseGeneratorSettings var5 = (NoiseGeneratorSettings)this.settings.value();
         NoiseChunk var6 = var3.getOrCreateNoiseChunk(this.router, () -> {
            return new Beardifier(var2, var3);
         }, var5, this.globalFluidPicker, Blender.of(var1));
         this.surfaceSystem.buildSurface(var1.getBiomeManager(), var1.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), var5.useLegacyRandomSource(), var4, var3, var6, var5.surfaceRule());
      }
   }

   public void applyCarvers(WorldGenRegion var1, long var2, BiomeManager var4, StructureFeatureManager var5, ChunkAccess var6, GenerationStep.Carving var7) {
      BiomeManager var8 = var4.withDifferentSource((var1x, var2x, var3) -> {
         return this.biomeSource.getNoiseBiome(var1x, var2x, var3, this.climateSampler());
      });
      WorldgenRandom var9 = new WorldgenRandom(new LegacyRandomSource(RandomSupport.seedUniquifier()));
      boolean var10 = true;
      ChunkPos var11 = var6.getPos();
      NoiseChunk var12 = var6.getOrCreateNoiseChunk(this.router, () -> {
         return new Beardifier(var5, var6);
      }, (NoiseGeneratorSettings)this.settings.value(), this.globalFluidPicker, Blender.of(var1));
      Aquifer var13 = var12.aquifer();
      CarvingContext var14 = new CarvingContext(this, var1.registryAccess(), var6.getHeightAccessorForGeneration(), var12);
      CarvingMask var15 = ((ProtoChunk)var6).getOrCreateCarvingMask(var7);

      for(int var16 = -8; var16 <= 8; ++var16) {
         for(int var17 = -8; var17 <= 8; ++var17) {
            ChunkPos var18 = new ChunkPos(var11.x + var16, var11.z + var17);
            ChunkAccess var19 = var1.getChunk(var18.x, var18.z);
            BiomeGenerationSettings var20 = ((Biome)var19.carverBiome(() -> {
               return this.biomeSource.getNoiseBiome(QuartPos.fromBlock(var18.getMinBlockX()), 0, QuartPos.fromBlock(var18.getMinBlockZ()), this.climateSampler());
            }).value()).getGenerationSettings();
            Iterable var21 = var20.getCarvers(var7);
            int var22 = 0;

            for(Iterator var23 = var21.iterator(); var23.hasNext(); ++var22) {
               Holder var24 = (Holder)var23.next();
               ConfiguredWorldCarver var25 = (ConfiguredWorldCarver)var24.value();
               var9.setLargeFeatureSeed(var2 + (long)var22, var18.x, var18.z);
               if (var25.isStartChunk(var9)) {
                  Objects.requireNonNull(var8);
                  var25.carve(var14, var6, var8::getBiome, var9, var13, var18, var15);
               }
            }
         }
      }

   }

   public CompletableFuture<ChunkAccess> fillFromNoise(Executor var1, Blender var2, StructureFeatureManager var3, ChunkAccess var4) {
      NoiseSettings var5 = ((NoiseGeneratorSettings)this.settings.value()).noiseSettings();
      LevelHeightAccessor var6 = var4.getHeightAccessorForGeneration();
      int var7 = Math.max(var5.minY(), var6.getMinBuildHeight());
      int var8 = Math.min(var5.minY() + var5.height(), var6.getMaxBuildHeight());
      int var9 = Mth.intFloorDiv(var7, var5.getCellHeight());
      int var10 = Mth.intFloorDiv(var8 - var7, var5.getCellHeight());
      if (var10 <= 0) {
         return CompletableFuture.completedFuture(var4);
      } else {
         int var11 = var4.getSectionIndex(var10 * var5.getCellHeight() - 1 + var7);
         int var12 = var4.getSectionIndex(var7);
         HashSet var13 = Sets.newHashSet();

         for(int var14 = var11; var14 >= var12; --var14) {
            LevelChunkSection var15 = var4.getSection(var14);
            var15.acquire();
            var13.add(var15);
         }

         return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("wgen_fill_noise", () -> {
            return this.doFill(var2, var3, var4, var9, var10);
         }), Util.backgroundExecutor()).whenCompleteAsync((var1x, var2x) -> {
            Iterator var3 = var13.iterator();

            while(var3.hasNext()) {
               LevelChunkSection var4 = (LevelChunkSection)var3.next();
               var4.release();
            }

         }, var1);
      }
   }

   private ChunkAccess doFill(Blender var1, StructureFeatureManager var2, ChunkAccess var3, int var4, int var5) {
      NoiseGeneratorSettings var6 = (NoiseGeneratorSettings)this.settings.value();
      NoiseChunk var7 = var3.getOrCreateNoiseChunk(this.router, () -> {
         return new Beardifier(var2, var3);
      }, var6, this.globalFluidPicker, var1);
      Heightmap var8 = var3.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
      Heightmap var9 = var3.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
      ChunkPos var10 = var3.getPos();
      int var11 = var10.getMinBlockX();
      int var12 = var10.getMinBlockZ();
      Aquifer var13 = var7.aquifer();
      var7.initializeForFirstCellX();
      BlockPos.MutableBlockPos var14 = new BlockPos.MutableBlockPos();
      NoiseSettings var15 = var6.noiseSettings();
      int var16 = var15.getCellWidth();
      int var17 = var15.getCellHeight();
      int var18 = 16 / var16;
      int var19 = 16 / var16;

      for(int var20 = 0; var20 < var18; ++var20) {
         var7.advanceCellX(var20);

         for(int var21 = 0; var21 < var19; ++var21) {
            LevelChunkSection var22 = var3.getSection(var3.getSectionsCount() - 1);

            for(int var23 = var5 - 1; var23 >= 0; --var23) {
               var7.selectCellYZ(var23, var21);

               for(int var24 = var17 - 1; var24 >= 0; --var24) {
                  int var25 = (var4 + var23) * var17 + var24;
                  int var26 = var25 & 15;
                  int var27 = var3.getSectionIndex(var25);
                  if (var3.getSectionIndex(var22.bottomBlockY()) != var27) {
                     var22 = var3.getSection(var27);
                  }

                  double var28 = (double)var24 / (double)var17;
                  var7.updateForY(var25, var28);

                  for(int var30 = 0; var30 < var16; ++var30) {
                     int var31 = var11 + var20 * var16 + var30;
                     int var32 = var31 & 15;
                     double var33 = (double)var30 / (double)var16;
                     var7.updateForX(var31, var33);

                     for(int var35 = 0; var35 < var16; ++var35) {
                        int var36 = var12 + var21 * var16 + var35;
                        int var37 = var36 & 15;
                        double var38 = (double)var35 / (double)var16;
                        var7.updateForZ(var36, var38);
                        BlockState var40 = var7.getInterpolatedState();
                        if (var40 == null) {
                           var40 = this.defaultBlock;
                        }

                        var40 = this.debugPreliminarySurfaceLevel(var7, var31, var25, var36, var40);
                        if (var40 != AIR && !SharedConstants.debugVoidTerrain(var3.getPos())) {
                           if (var40.getLightEmission() != 0 && var3 instanceof ProtoChunk) {
                              var14.set(var31, var25, var36);
                              ((ProtoChunk)var3).addLight(var14);
                           }

                           var22.setBlockState(var32, var26, var37, var40, false);
                           var8.update(var32, var25, var37, var40);
                           var9.update(var32, var25, var37, var40);
                           if (var13.shouldScheduleFluidUpdate() && !var40.getFluidState().isEmpty()) {
                              var14.set(var31, var25, var36);
                              var3.markPosForPostprocessing(var14);
                           }
                        }
                     }
                  }
               }
            }
         }

         var7.swapSlices();
      }

      var7.stopInterpolation();
      return var3;
   }

   private BlockState debugPreliminarySurfaceLevel(NoiseChunk var1, int var2, int var3, int var4, BlockState var5) {
      return var5;
   }

   public int getGenDepth() {
      return ((NoiseGeneratorSettings)this.settings.value()).noiseSettings().height();
   }

   public int getSeaLevel() {
      return ((NoiseGeneratorSettings)this.settings.value()).seaLevel();
   }

   public int getMinY() {
      return ((NoiseGeneratorSettings)this.settings.value()).noiseSettings().minY();
   }

   public void spawnOriginalMobs(WorldGenRegion var1) {
      if (!((NoiseGeneratorSettings)this.settings.value()).disableMobGeneration()) {
         ChunkPos var2 = var1.getCenter();
         Holder var3 = var1.getBiome(var2.getWorldPosition().atY(var1.getMaxBuildHeight() - 1));
         WorldgenRandom var4 = new WorldgenRandom(new LegacyRandomSource(RandomSupport.seedUniquifier()));
         var4.setDecorationSeed(var1.getSeed(), var2.getMinBlockX(), var2.getMinBlockZ());
         NaturalSpawner.spawnMobsForChunkGeneration(var1, var3, var2, var4);
      }
   }

   /** @deprecated */
   @Deprecated
   public Optional<BlockState> topMaterial(CarvingContext var1, Function<BlockPos, Holder<Biome>> var2, ChunkAccess var3, NoiseChunk var4, BlockPos var5, boolean var6) {
      return this.surfaceSystem.topMaterial(((NoiseGeneratorSettings)this.settings.value()).surfaceRule(), var1, var2, var3, var4, var5, var6);
   }

   static {
      AIR = Blocks.AIR.defaultBlockState();
      EMPTY_COLUMN = new BlockState[0];
   }
}
