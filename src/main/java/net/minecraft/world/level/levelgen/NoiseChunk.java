package net.minecraft.world.level.levelgen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.material.MaterialRuleList;

public class NoiseChunk implements DensityFunction.ContextProvider, DensityFunction.FunctionContext {
   private final NoiseSettings noiseSettings;
   final int cellCountXZ;
   final int cellCountY;
   final int cellNoiseMinY;
   private final int firstCellX;
   private final int firstCellZ;
   final int firstNoiseX;
   final int firstNoiseZ;
   final List<NoiseChunk.NoiseInterpolator> interpolators;
   final List<NoiseChunk.CacheAllInCell> cellCaches;
   private final Map<DensityFunction, DensityFunction> wrapped = new HashMap();
   private final Long2IntMap preliminarySurfaceLevel = new Long2IntOpenHashMap();
   private final Aquifer aquifer;
   private final DensityFunction initialDensityNoJaggedness;
   private final NoiseChunk.BlockStateFiller blockStateRule;
   private final Blender blender;
   private final NoiseChunk.FlatCache blendAlpha;
   private final NoiseChunk.FlatCache blendOffset;
   private final DensityFunctions.BeardifierOrMarker beardifier;
   private long lastBlendingDataPos;
   private Blender.BlendingOutput lastBlendingOutput;
   final int noiseSizeXZ;
   final int cellWidth;
   final int cellHeight;
   boolean interpolating;
   boolean fillingCell;
   private int cellStartBlockX;
   int cellStartBlockY;
   private int cellStartBlockZ;
   int inCellX;
   int inCellY;
   int inCellZ;
   long interpolationCounter;
   long arrayInterpolationCounter;
   int arrayIndex;
   private final DensityFunction.ContextProvider sliceFillingContextProvider;

   public static NoiseChunk forChunk(ChunkAccess var0, NoiseRouter var1, Supplier<DensityFunctions.BeardifierOrMarker> var2, NoiseGeneratorSettings var3, Aquifer.FluidPicker var4, Blender var5) {
      ChunkPos var6 = var0.getPos();
      NoiseSettings var7 = var3.noiseSettings();
      int var8 = Math.max(var7.minY(), var0.getMinBuildHeight());
      int var9 = Math.min(var7.minY() + var7.height(), var0.getMaxBuildHeight());
      int var10 = Mth.intFloorDiv(var8, var7.getCellHeight());
      int var11 = Mth.intFloorDiv(var9 - var8, var7.getCellHeight());
      return new NoiseChunk(16 / var7.getCellWidth(), var11, var10, var1, var6.getMinBlockX(), var6.getMinBlockZ(), (DensityFunctions.BeardifierOrMarker)var2.get(), var3, var4, var5);
   }

   public static NoiseChunk forColumn(int var0, int var1, int var2, int var3, NoiseRouter var4, NoiseGeneratorSettings var5, Aquifer.FluidPicker var6) {
      return new NoiseChunk(1, var3, var2, var4, var0, var1, DensityFunctions.BeardifierMarker.INSTANCE, var5, var6, Blender.empty());
   }

   private NoiseChunk(int var1, int var2, int var3, NoiseRouter var4, int var5, int var6, DensityFunctions.BeardifierOrMarker var7, NoiseGeneratorSettings var8, Aquifer.FluidPicker var9, Blender var10) {
      this.lastBlendingDataPos = ChunkPos.INVALID_CHUNK_POS;
      this.lastBlendingOutput = new Blender.BlendingOutput(1.0D, 0.0D);
      this.sliceFillingContextProvider = new DensityFunction.ContextProvider() {
         public DensityFunction.FunctionContext forIndex(int var1) {
            NoiseChunk.this.cellStartBlockY = (var1 + NoiseChunk.this.cellNoiseMinY) * NoiseChunk.this.cellHeight;
            ++NoiseChunk.this.interpolationCounter;
            NoiseChunk.this.inCellY = 0;
            NoiseChunk.this.arrayIndex = var1;
            return NoiseChunk.this;
         }

         public void fillAllDirectly(double[] var1, DensityFunction var2) {
            for(int var3 = 0; var3 < NoiseChunk.this.cellCountY + 1; ++var3) {
               NoiseChunk.this.cellStartBlockY = (var3 + NoiseChunk.this.cellNoiseMinY) * NoiseChunk.this.cellHeight;
               ++NoiseChunk.this.interpolationCounter;
               NoiseChunk.this.inCellY = 0;
               NoiseChunk.this.arrayIndex = var3;
               var1[var3] = var2.compute(NoiseChunk.this);
            }

         }
      };
      this.noiseSettings = var8.noiseSettings();
      this.cellCountXZ = var1;
      this.cellCountY = var2;
      this.cellNoiseMinY = var3;
      this.cellWidth = this.noiseSettings.getCellWidth();
      this.cellHeight = this.noiseSettings.getCellHeight();
      this.firstCellX = Math.floorDiv(var5, this.cellWidth);
      this.firstCellZ = Math.floorDiv(var6, this.cellWidth);
      this.interpolators = Lists.newArrayList();
      this.cellCaches = Lists.newArrayList();
      this.firstNoiseX = QuartPos.fromBlock(var5);
      this.firstNoiseZ = QuartPos.fromBlock(var6);
      this.noiseSizeXZ = QuartPos.fromBlock(var1 * this.cellWidth);
      this.blender = var10;
      this.beardifier = var7;
      this.blendAlpha = new NoiseChunk.FlatCache(new NoiseChunk.BlendAlpha(), false);
      this.blendOffset = new NoiseChunk.FlatCache(new NoiseChunk.BlendOffset(), false);

      int var11;
      int var12;
      for(var11 = 0; var11 <= this.noiseSizeXZ; ++var11) {
         var12 = this.firstNoiseX + var11;
         int var13 = QuartPos.toBlock(var12);

         for(int var14 = 0; var14 <= this.noiseSizeXZ; ++var14) {
            int var15 = this.firstNoiseZ + var14;
            int var16 = QuartPos.toBlock(var15);
            Blender.BlendingOutput var17 = var10.blendOffsetAndFactor(var13, var16);
            this.blendAlpha.values[var11][var14] = var17.alpha();
            this.blendOffset.values[var11][var14] = var17.blendingOffset();
         }
      }

      if (!var8.isAquifersEnabled()) {
         this.aquifer = Aquifer.createDisabled(var9);
      } else {
         var11 = SectionPos.blockToSectionCoord(var5);
         var12 = SectionPos.blockToSectionCoord(var6);
         this.aquifer = Aquifer.create(this, new ChunkPos(var11, var12), var4.barrierNoise(), var4.fluidLevelFloodednessNoise(), var4.fluidLevelSpreadNoise(), var4.lavaNoise(), var4.aquiferPositionalRandomFactory(), var3 * this.cellHeight, var2 * this.cellHeight, var9);
      }

      Builder var18 = ImmutableList.builder();
      DensityFunction var19 = DensityFunctions.cacheAllInCell(DensityFunctions.add(var4.finalDensity(), DensityFunctions.BeardifierMarker.INSTANCE)).mapAll(this::wrap);
      var18.add((var2x) -> {
         return this.aquifer.computeSubstance(var2x, var19.compute(var2x));
      });
      if (var8.oreVeinsEnabled()) {
         var18.add(OreVeinifier.create(var4.veinToggle().mapAll(this::wrap), var4.veinRidged().mapAll(this::wrap), var4.veinGap().mapAll(this::wrap), var4.oreVeinsPositionalRandomFactory()));
      }

      this.blockStateRule = new MaterialRuleList(var18.build());
      this.initialDensityNoJaggedness = var4.initialDensityWithoutJaggedness().mapAll(this::wrap);
   }

   protected Climate.Sampler cachedClimateSampler(NoiseRouter var1) {
      return new Climate.Sampler(var1.temperature().mapAll(this::wrap), var1.humidity().mapAll(this::wrap), var1.continents().mapAll(this::wrap), var1.erosion().mapAll(this::wrap), var1.depth().mapAll(this::wrap), var1.ridges().mapAll(this::wrap), var1.spawnTarget());
   }

   @Nullable
   protected BlockState getInterpolatedState() {
      return this.blockStateRule.calculate(this);
   }

   public int blockX() {
      return this.cellStartBlockX + this.inCellX;
   }

   public int blockY() {
      return this.cellStartBlockY + this.inCellY;
   }

   public int blockZ() {
      return this.cellStartBlockZ + this.inCellZ;
   }

   public int preliminarySurfaceLevel(int var1, int var2) {
      return this.preliminarySurfaceLevel.computeIfAbsent(ChunkPos.asLong(QuartPos.fromBlock(var1), QuartPos.fromBlock(var2)), this::computePreliminarySurfaceLevel);
   }

   private int computePreliminarySurfaceLevel(long var1) {
      int var3 = ChunkPos.getX(var1);
      int var4 = ChunkPos.getZ(var1);
      return (int)NoiseRouterData.computePreliminarySurfaceLevelScanning(this.noiseSettings, this.initialDensityNoJaggedness, QuartPos.toBlock(var3), QuartPos.toBlock(var4));
   }

   public Blender getBlender() {
      return this.blender;
   }

   private void fillSlice(boolean var1, int var2) {
      this.cellStartBlockX = var2 * this.cellWidth;
      this.inCellX = 0;

      for(int var3 = 0; var3 < this.cellCountXZ + 1; ++var3) {
         int var4 = this.firstCellZ + var3;
         this.cellStartBlockZ = var4 * this.cellWidth;
         this.inCellZ = 0;
         ++this.arrayInterpolationCounter;
         Iterator var5 = this.interpolators.iterator();

         while(var5.hasNext()) {
            NoiseChunk.NoiseInterpolator var6 = (NoiseChunk.NoiseInterpolator)var5.next();
            double[] var7 = (var1 ? var6.slice0 : var6.slice1)[var3];
            var6.fillArray(var7, this.sliceFillingContextProvider);
         }
      }

      ++this.arrayInterpolationCounter;
   }

   public void initializeForFirstCellX() {
      if (this.interpolating) {
         throw new IllegalStateException("Staring interpolation twice");
      } else {
         this.interpolating = true;
         this.interpolationCounter = 0L;
         this.fillSlice(true, this.firstCellX);
      }
   }

   public void advanceCellX(int var1) {
      this.fillSlice(false, this.firstCellX + var1 + 1);
      this.cellStartBlockX = (this.firstCellX + var1) * this.cellWidth;
   }

   public NoiseChunk forIndex(int var1) {
      int var2 = Math.floorMod(var1, this.cellWidth);
      int var3 = Math.floorDiv(var1, this.cellWidth);
      int var4 = Math.floorMod(var3, this.cellWidth);
      int var5 = this.cellHeight - 1 - Math.floorDiv(var3, this.cellWidth);
      this.inCellX = var4;
      this.inCellY = var5;
      this.inCellZ = var2;
      this.arrayIndex = var1;
      return this;
   }

   public void fillAllDirectly(double[] var1, DensityFunction var2) {
      this.arrayIndex = 0;

      for(int var3 = this.cellHeight - 1; var3 >= 0; --var3) {
         this.inCellY = var3;

         for(int var4 = 0; var4 < this.cellWidth; ++var4) {
            this.inCellX = var4;

            for(int var5 = 0; var5 < this.cellWidth; ++var5) {
               this.inCellZ = var5;
               var1[this.arrayIndex++] = var2.compute(this);
            }
         }
      }

   }

   public void selectCellYZ(int var1, int var2) {
      this.interpolators.forEach((var2x) -> {
         var2x.selectCellYZ(var1, var2);
      });
      this.fillingCell = true;
      this.cellStartBlockY = (var1 + this.cellNoiseMinY) * this.cellHeight;
      this.cellStartBlockZ = (this.firstCellZ + var2) * this.cellWidth;
      ++this.arrayInterpolationCounter;
      Iterator var3 = this.cellCaches.iterator();

      while(var3.hasNext()) {
         NoiseChunk.CacheAllInCell var4 = (NoiseChunk.CacheAllInCell)var3.next();
         var4.noiseFiller.fillArray(var4.values, this);
      }

      ++this.arrayInterpolationCounter;
      this.fillingCell = false;
   }

   public void updateForY(int var1, double var2) {
      this.inCellY = var1 - this.cellStartBlockY;
      this.interpolators.forEach((var2x) -> {
         var2x.updateForY(var2);
      });
   }

   public void updateForX(int var1, double var2) {
      this.inCellX = var1 - this.cellStartBlockX;
      this.interpolators.forEach((var2x) -> {
         var2x.updateForX(var2);
      });
   }

   public void updateForZ(int var1, double var2) {
      this.inCellZ = var1 - this.cellStartBlockZ;
      ++this.interpolationCounter;
      this.interpolators.forEach((var2x) -> {
         var2x.updateForZ(var2);
      });
   }

   public void stopInterpolation() {
      if (!this.interpolating) {
         throw new IllegalStateException("Staring interpolation twice");
      } else {
         this.interpolating = false;
      }
   }

   public void swapSlices() {
      this.interpolators.forEach(NoiseChunk.NoiseInterpolator::swapSlices);
   }

   public Aquifer aquifer() {
      return this.aquifer;
   }

   Blender.BlendingOutput getOrComputeBlendingOutput(int var1, int var2) {
      long var3 = ChunkPos.asLong(var1, var2);
      if (this.lastBlendingDataPos == var3) {
         return this.lastBlendingOutput;
      } else {
         this.lastBlendingDataPos = var3;
         Blender.BlendingOutput var5 = this.blender.blendOffsetAndFactor(var1, var2);
         this.lastBlendingOutput = var5;
         return var5;
      }
   }

   protected DensityFunction wrap(DensityFunction var1) {
      return (DensityFunction)this.wrapped.computeIfAbsent(var1, this::wrapNew);
   }

   private DensityFunction wrapNew(DensityFunction var1) {
      if (var1 instanceof DensityFunctions.Marker) {
         DensityFunctions.Marker var3 = (DensityFunctions.Marker)var1;
         Object var10000;
         switch(var3.type()) {
         case Interpolated:
            var10000 = new NoiseChunk.NoiseInterpolator(var3.wrapped());
            break;
         case FlatCache:
            var10000 = new NoiseChunk.FlatCache(var3.wrapped(), true);
            break;
         case Cache2D:
            var10000 = new NoiseChunk.Cache2D(var3.wrapped());
            break;
         case CacheOnce:
            var10000 = new NoiseChunk.CacheOnce(var3.wrapped());
            break;
         case CacheAllInCell:
            var10000 = new NoiseChunk.CacheAllInCell(var3.wrapped());
            break;
         default:
            throw new IncompatibleClassChangeError();
         }

         return (DensityFunction)var10000;
      } else {
         if (this.blender != Blender.empty()) {
            if (var1 == DensityFunctions.BlendAlpha.INSTANCE) {
               return this.blendAlpha;
            }

            if (var1 == DensityFunctions.BlendOffset.INSTANCE) {
               return this.blendOffset;
            }
         }

         if (var1 == DensityFunctions.BeardifierMarker.INSTANCE) {
            return this.beardifier;
         } else if (var1 instanceof DensityFunctions.HolderHolder) {
            DensityFunctions.HolderHolder var2 = (DensityFunctions.HolderHolder)var1;
            return (DensityFunction)var2.function().value();
         } else {
            return var1;
         }
      }
   }

   // $FF: synthetic method
   public DensityFunction.FunctionContext forIndex(int var1) {
      return this.forIndex(var1);
   }

   private class FlatCache implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
      private final DensityFunction noiseFiller;
      final double[][] values;

      FlatCache(DensityFunction var2, boolean var3) {
         this.noiseFiller = var2;
         this.values = new double[NoiseChunk.this.noiseSizeXZ + 1][NoiseChunk.this.noiseSizeXZ + 1];
         if (var3) {
            for(int var4 = 0; var4 <= NoiseChunk.this.noiseSizeXZ; ++var4) {
               int var5 = NoiseChunk.this.firstNoiseX + var4;
               int var6 = QuartPos.toBlock(var5);

               for(int var7 = 0; var7 <= NoiseChunk.this.noiseSizeXZ; ++var7) {
                  int var8 = NoiseChunk.this.firstNoiseZ + var7;
                  int var9 = QuartPos.toBlock(var8);
                  this.values[var4][var7] = var2.compute(new DensityFunction.SinglePointContext(var6, 0, var9));
               }
            }
         }

      }

      public double compute(DensityFunction.FunctionContext var1) {
         int var2 = QuartPos.fromBlock(var1.blockX());
         int var3 = QuartPos.fromBlock(var1.blockZ());
         int var4 = var2 - NoiseChunk.this.firstNoiseX;
         int var5 = var3 - NoiseChunk.this.firstNoiseZ;
         int var6 = this.values.length;
         return var4 >= 0 && var5 >= 0 && var4 < var6 && var5 < var6 ? this.values[var4][var5] : this.noiseFiller.compute(var1);
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         var2.fillAllDirectly(var1, this);
      }

      public DensityFunction wrapped() {
         return this.noiseFiller;
      }

      public DensityFunctions.Marker.Type type() {
         return DensityFunctions.Marker.Type.FlatCache;
      }
   }

   private class BlendAlpha implements NoiseChunk.NoiseChunkDensityFunction {
      BlendAlpha() {
      }

      public DensityFunction wrapped() {
         return DensityFunctions.BlendAlpha.INSTANCE;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return NoiseChunk.this.getOrComputeBlendingOutput(var1.blockX(), var1.blockZ()).alpha();
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         var2.fillAllDirectly(var1, this);
      }

      public double minValue() {
         return 0.0D;
      }

      public double maxValue() {
         return 1.0D;
      }

      public Codec<? extends DensityFunction> codec() {
         return DensityFunctions.BlendAlpha.CODEC;
      }
   }

   class BlendOffset implements NoiseChunk.NoiseChunkDensityFunction {
      BlendOffset() {
      }

      public DensityFunction wrapped() {
         return DensityFunctions.BlendOffset.INSTANCE;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return NoiseChunk.this.getOrComputeBlendingOutput(var1.blockX(), var1.blockZ()).blendingOffset();
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         var2.fillAllDirectly(var1, this);
      }

      public double minValue() {
         return Double.NEGATIVE_INFINITY;
      }

      public double maxValue() {
         return Double.POSITIVE_INFINITY;
      }

      public Codec<? extends DensityFunction> codec() {
         return DensityFunctions.BlendOffset.CODEC;
      }
   }

   @FunctionalInterface
   public interface BlockStateFiller {
      @Nullable
      BlockState calculate(DensityFunction.FunctionContext var1);
   }

   public class NoiseInterpolator implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
      double[][] slice0;
      double[][] slice1;
      private final DensityFunction noiseFiller;
      private double noise000;
      private double noise001;
      private double noise100;
      private double noise101;
      private double noise010;
      private double noise011;
      private double noise110;
      private double noise111;
      private double valueXZ00;
      private double valueXZ10;
      private double valueXZ01;
      private double valueXZ11;
      private double valueZ0;
      private double valueZ1;
      private double value;

      NoiseInterpolator(DensityFunction var2) {
         this.noiseFiller = var2;
         this.slice0 = this.allocateSlice(NoiseChunk.this.cellCountY, NoiseChunk.this.cellCountXZ);
         this.slice1 = this.allocateSlice(NoiseChunk.this.cellCountY, NoiseChunk.this.cellCountXZ);
         NoiseChunk.this.interpolators.add(this);
      }

      private double[][] allocateSlice(int var1, int var2) {
         int var3 = var2 + 1;
         int var4 = var1 + 1;
         double[][] var5 = new double[var3][var4];

         for(int var6 = 0; var6 < var3; ++var6) {
            var5[var6] = new double[var4];
         }

         return var5;
      }

      void selectCellYZ(int var1, int var2) {
         this.noise000 = this.slice0[var2][var1];
         this.noise001 = this.slice0[var2 + 1][var1];
         this.noise100 = this.slice1[var2][var1];
         this.noise101 = this.slice1[var2 + 1][var1];
         this.noise010 = this.slice0[var2][var1 + 1];
         this.noise011 = this.slice0[var2 + 1][var1 + 1];
         this.noise110 = this.slice1[var2][var1 + 1];
         this.noise111 = this.slice1[var2 + 1][var1 + 1];
      }

      void updateForY(double var1) {
         this.valueXZ00 = Mth.lerp(var1, this.noise000, this.noise010);
         this.valueXZ10 = Mth.lerp(var1, this.noise100, this.noise110);
         this.valueXZ01 = Mth.lerp(var1, this.noise001, this.noise011);
         this.valueXZ11 = Mth.lerp(var1, this.noise101, this.noise111);
      }

      void updateForX(double var1) {
         this.valueZ0 = Mth.lerp(var1, this.valueXZ00, this.valueXZ10);
         this.valueZ1 = Mth.lerp(var1, this.valueXZ01, this.valueXZ11);
      }

      void updateForZ(double var1) {
         this.value = Mth.lerp(var1, this.valueZ0, this.valueZ1);
      }

      public double compute(DensityFunction.FunctionContext var1) {
         if (var1 != NoiseChunk.this) {
            return this.noiseFiller.compute(var1);
         } else if (!NoiseChunk.this.interpolating) {
            throw new IllegalStateException("Trying to sample interpolator outside the interpolation loop");
         } else {
            return NoiseChunk.this.fillingCell ? Mth.lerp3((double)NoiseChunk.this.inCellX / (double)NoiseChunk.this.cellWidth, (double)NoiseChunk.this.inCellY / (double)NoiseChunk.this.cellHeight, (double)NoiseChunk.this.inCellZ / (double)NoiseChunk.this.cellWidth, this.noise000, this.noise100, this.noise010, this.noise110, this.noise001, this.noise101, this.noise011, this.noise111) : this.value;
         }
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         if (NoiseChunk.this.fillingCell) {
            var2.fillAllDirectly(var1, this);
         } else {
            this.wrapped().fillArray(var1, var2);
         }
      }

      public DensityFunction wrapped() {
         return this.noiseFiller;
      }

      private void swapSlices() {
         double[][] var1 = this.slice0;
         this.slice0 = this.slice1;
         this.slice1 = var1;
      }

      public DensityFunctions.Marker.Type type() {
         return DensityFunctions.Marker.Type.Interpolated;
      }
   }

   class CacheAllInCell implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
      final DensityFunction noiseFiller;
      final double[] values;

      CacheAllInCell(DensityFunction var2) {
         this.noiseFiller = var2;
         this.values = new double[NoiseChunk.this.cellWidth * NoiseChunk.this.cellWidth * NoiseChunk.this.cellHeight];
         NoiseChunk.this.cellCaches.add(this);
      }

      public double compute(DensityFunction.FunctionContext var1) {
         if (var1 != NoiseChunk.this) {
            return this.noiseFiller.compute(var1);
         } else if (!NoiseChunk.this.interpolating) {
            throw new IllegalStateException("Trying to sample interpolator outside the interpolation loop");
         } else {
            int var2 = NoiseChunk.this.inCellX;
            int var3 = NoiseChunk.this.inCellY;
            int var4 = NoiseChunk.this.inCellZ;
            return var2 >= 0 && var3 >= 0 && var4 >= 0 && var2 < NoiseChunk.this.cellWidth && var3 < NoiseChunk.this.cellHeight && var4 < NoiseChunk.this.cellWidth ? this.values[((NoiseChunk.this.cellHeight - 1 - var3) * NoiseChunk.this.cellWidth + var2) * NoiseChunk.this.cellWidth + var4] : this.noiseFiller.compute(var1);
         }
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         var2.fillAllDirectly(var1, this);
      }

      public DensityFunction wrapped() {
         return this.noiseFiller;
      }

      public DensityFunctions.Marker.Type type() {
         return DensityFunctions.Marker.Type.CacheAllInCell;
      }
   }

   private static class Cache2D implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
      private final DensityFunction function;
      private long lastPos2D;
      private double lastValue;

      Cache2D(DensityFunction var1) {
         this.lastPos2D = ChunkPos.INVALID_CHUNK_POS;
         this.function = var1;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         int var2 = var1.blockX();
         int var3 = var1.blockZ();
         long var4 = ChunkPos.asLong(var2, var3);
         if (this.lastPos2D == var4) {
            return this.lastValue;
         } else {
            this.lastPos2D = var4;
            double var6 = this.function.compute(var1);
            this.lastValue = var6;
            return var6;
         }
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         this.function.fillArray(var1, var2);
      }

      public DensityFunction wrapped() {
         return this.function;
      }

      public DensityFunctions.Marker.Type type() {
         return DensityFunctions.Marker.Type.Cache2D;
      }
   }

   private class CacheOnce implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
      private final DensityFunction function;
      private long lastCounter;
      private long lastArrayCounter;
      private double lastValue;
      @Nullable
      private double[] lastArray;

      CacheOnce(DensityFunction var2) {
         this.function = var2;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         if (var1 != NoiseChunk.this) {
            return this.function.compute(var1);
         } else if (this.lastArray != null && this.lastArrayCounter == NoiseChunk.this.arrayInterpolationCounter) {
            return this.lastArray[NoiseChunk.this.arrayIndex];
         } else if (this.lastCounter == NoiseChunk.this.interpolationCounter) {
            return this.lastValue;
         } else {
            this.lastCounter = NoiseChunk.this.interpolationCounter;
            double var2 = this.function.compute(var1);
            this.lastValue = var2;
            return var2;
         }
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         if (this.lastArray != null && this.lastArrayCounter == NoiseChunk.this.arrayInterpolationCounter) {
            System.arraycopy(this.lastArray, 0, var1, 0, var1.length);
         } else {
            this.wrapped().fillArray(var1, var2);
            if (this.lastArray != null && this.lastArray.length == var1.length) {
               System.arraycopy(var1, 0, this.lastArray, 0, var1.length);
            } else {
               this.lastArray = (double[])var1.clone();
            }

            this.lastArrayCounter = NoiseChunk.this.arrayInterpolationCounter;
         }
      }

      public DensityFunction wrapped() {
         return this.function;
      }

      public DensityFunctions.Marker.Type type() {
         return DensityFunctions.Marker.Type.CacheOnce;
      }
   }

   private interface NoiseChunkDensityFunction extends DensityFunction {
      DensityFunction wrapped();

      default DensityFunction mapAll(DensityFunction.Visitor var1) {
         return this.wrapped().mapAll(var1);
      }

      default double minValue() {
         return this.wrapped().minValue();
      }

      default double maxValue() {
         return this.wrapped().maxValue();
      }
   }
}
