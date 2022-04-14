package net.minecraft.world.level.levelgen;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.biome.TerrainShaper;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class NoiseRouterData {
   private static final float ORE_THICKNESS = 0.08F;
   private static final double VEININESS_FREQUENCY = 1.5D;
   private static final double NOODLE_SPACING_AND_STRAIGHTNESS = 1.5D;
   private static final double SURFACE_DENSITY_THRESHOLD = 1.5625D;
   private static final DensityFunction BLENDING_FACTOR = DensityFunctions.constant(10.0D);
   private static final DensityFunction BLENDING_JAGGEDNESS = DensityFunctions.zero();
   private static final ResourceKey<DensityFunction> ZERO = createKey("zero");
   private static final ResourceKey<DensityFunction> Y = createKey("y");
   private static final ResourceKey<DensityFunction> SHIFT_X = createKey("shift_x");
   private static final ResourceKey<DensityFunction> SHIFT_Z = createKey("shift_z");
   private static final ResourceKey<DensityFunction> BASE_3D_NOISE = createKey("overworld/base_3d_noise");
   private static final ResourceKey<DensityFunction> CONTINENTS = createKey("overworld/continents");
   private static final ResourceKey<DensityFunction> EROSION = createKey("overworld/erosion");
   private static final ResourceKey<DensityFunction> RIDGES = createKey("overworld/ridges");
   private static final ResourceKey<DensityFunction> FACTOR = createKey("overworld/factor");
   private static final ResourceKey<DensityFunction> DEPTH = createKey("overworld/depth");
   private static final ResourceKey<DensityFunction> SLOPED_CHEESE = createKey("overworld/sloped_cheese");
   private static final ResourceKey<DensityFunction> CONTINENTS_LARGE = createKey("overworld_large_biomes/continents");
   private static final ResourceKey<DensityFunction> EROSION_LARGE = createKey("overworld_large_biomes/erosion");
   private static final ResourceKey<DensityFunction> FACTOR_LARGE = createKey("overworld_large_biomes/factor");
   private static final ResourceKey<DensityFunction> DEPTH_LARGE = createKey("overworld_large_biomes/depth");
   private static final ResourceKey<DensityFunction> SLOPED_CHEESE_LARGE = createKey("overworld_large_biomes/sloped_cheese");
   private static final ResourceKey<DensityFunction> SLOPED_CHEESE_END = createKey("end/sloped_cheese");
   private static final ResourceKey<DensityFunction> SPAGHETTI_ROUGHNESS_FUNCTION = createKey("overworld/caves/spaghetti_roughness_function");
   private static final ResourceKey<DensityFunction> ENTRANCES = createKey("overworld/caves/entrances");
   private static final ResourceKey<DensityFunction> NOODLE = createKey("overworld/caves/noodle");
   private static final ResourceKey<DensityFunction> PILLARS = createKey("overworld/caves/pillars");
   private static final ResourceKey<DensityFunction> SPAGHETTI_2D_THICKNESS_MODULATOR = createKey("overworld/caves/spaghetti_2d_thickness_modulator");
   private static final ResourceKey<DensityFunction> SPAGHETTI_2D = createKey("overworld/caves/spaghetti_2d");

   public NoiseRouterData() {
   }

   protected static NoiseRouterWithOnlyNoises overworld(NoiseSettings var0, boolean var1) {
      return overworldWithNewCaves(var0, var1);
   }

   private static ResourceKey<DensityFunction> createKey(String var0) {
      return ResourceKey.create(Registry.DENSITY_FUNCTION_REGISTRY, new ResourceLocation(var0));
   }

   public static Holder<? extends DensityFunction> bootstrap() {
      register(ZERO, DensityFunctions.zero());
      int var0 = DimensionType.MIN_Y * 2;
      int var1 = DimensionType.MAX_Y * 2;
      register(Y, DensityFunctions.yClampedGradient(var0, var1, (double)var0, (double)var1));
      DensityFunction var2 = register(SHIFT_X, DensityFunctions.flatCache(DensityFunctions.cache2d(DensityFunctions.shiftA(getNoise(Noises.SHIFT)))));
      DensityFunction var3 = register(SHIFT_Z, DensityFunctions.flatCache(DensityFunctions.cache2d(DensityFunctions.shiftB(getNoise(Noises.SHIFT)))));
      register(BASE_3D_NOISE, BlendedNoise.UNSEEDED);
      DensityFunction var4 = register(CONTINENTS, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(var2, var3, 0.25D, getNoise(Noises.CONTINENTALNESS))));
      DensityFunction var5 = register(EROSION, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(var2, var3, 0.25D, getNoise(Noises.EROSION))));
      DensityFunction var6 = register(RIDGES, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(var2, var3, 0.25D, getNoise(Noises.RIDGE))));
      DensityFunction var7 = DensityFunctions.noise(getNoise(Noises.JAGGED), 1500.0D, 0.0D);
      DensityFunction var8 = splineWithBlending(var4, var5, var6, DensityFunctions.TerrainShaperSpline.SplineType.OFFSET, -0.81D, 2.5D, DensityFunctions.blendOffset());
      DensityFunction var9 = register(FACTOR, splineWithBlending(var4, var5, var6, DensityFunctions.TerrainShaperSpline.SplineType.FACTOR, 0.0D, 8.0D, BLENDING_FACTOR));
      DensityFunction var10 = register(DEPTH, DensityFunctions.add(DensityFunctions.yClampedGradient(-64, 320, 1.5D, -1.5D), var8));
      register(SLOPED_CHEESE, slopedCheese(var4, var5, var6, var9, var10, var7));
      DensityFunction var11 = register(CONTINENTS_LARGE, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(var2, var3, 0.25D, getNoise(Noises.CONTINENTALNESS_LARGE))));
      DensityFunction var12 = register(EROSION_LARGE, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(var2, var3, 0.25D, getNoise(Noises.EROSION_LARGE))));
      DensityFunction var13 = splineWithBlending(var11, var12, var6, DensityFunctions.TerrainShaperSpline.SplineType.OFFSET, -0.81D, 2.5D, DensityFunctions.blendOffset());
      DensityFunction var14 = register(FACTOR_LARGE, splineWithBlending(var11, var12, var6, DensityFunctions.TerrainShaperSpline.SplineType.FACTOR, 0.0D, 8.0D, BLENDING_FACTOR));
      DensityFunction var15 = register(DEPTH_LARGE, DensityFunctions.add(DensityFunctions.yClampedGradient(-64, 320, 1.5D, -1.5D), var13));
      register(SLOPED_CHEESE_LARGE, slopedCheese(var11, var12, var6, var14, var15, var7));
      register(SLOPED_CHEESE_END, DensityFunctions.add(DensityFunctions.endIslands(0L), getFunction(BASE_3D_NOISE)));
      register(SPAGHETTI_ROUGHNESS_FUNCTION, spaghettiRoughnessFunction());
      register(SPAGHETTI_2D_THICKNESS_MODULATOR, DensityFunctions.cacheOnce(DensityFunctions.mappedNoise(getNoise(Noises.SPAGHETTI_2D_THICKNESS), 2.0D, 1.0D, -0.6D, -1.3D)));
      register(SPAGHETTI_2D, spaghetti2D());
      register(ENTRANCES, entrances());
      register(NOODLE, noodle());
      register(PILLARS, pillars());
      return (Holder)BuiltinRegistries.DENSITY_FUNCTION.holders().iterator().next();
   }

   private static DensityFunction register(ResourceKey<DensityFunction> var0, DensityFunction var1) {
      return new DensityFunctions.HolderHolder(BuiltinRegistries.register(BuiltinRegistries.DENSITY_FUNCTION, (ResourceKey)var0, var1));
   }

   private static Holder<NormalNoise.NoiseParameters> getNoise(ResourceKey<NormalNoise.NoiseParameters> var0) {
      return BuiltinRegistries.NOISE.getHolderOrThrow(var0);
   }

   private static DensityFunction getFunction(ResourceKey<DensityFunction> var0) {
      return new DensityFunctions.HolderHolder(BuiltinRegistries.DENSITY_FUNCTION.getHolderOrThrow(var0));
   }

   private static DensityFunction slopedCheese(DensityFunction var0, DensityFunction var1, DensityFunction var2, DensityFunction var3, DensityFunction var4, DensityFunction var5) {
      DensityFunction var6 = splineWithBlending(var0, var1, var2, DensityFunctions.TerrainShaperSpline.SplineType.JAGGEDNESS, 0.0D, 1.28D, BLENDING_JAGGEDNESS);
      DensityFunction var7 = DensityFunctions.mul(var6, var5.halfNegative());
      DensityFunction var8 = noiseGradientDensity(var3, DensityFunctions.add(var4, var7));
      return DensityFunctions.add(var8, getFunction(BASE_3D_NOISE));
   }

   private static DensityFunction spaghettiRoughnessFunction() {
      DensityFunction var0 = DensityFunctions.noise(getNoise(Noises.SPAGHETTI_ROUGHNESS));
      DensityFunction var1 = DensityFunctions.mappedNoise(getNoise(Noises.SPAGHETTI_ROUGHNESS_MODULATOR), 0.0D, -0.1D);
      return DensityFunctions.cacheOnce(DensityFunctions.mul(var1, DensityFunctions.add(var0.abs(), DensityFunctions.constant(-0.4D))));
   }

   private static DensityFunction entrances() {
      DensityFunction var0 = DensityFunctions.cacheOnce(DensityFunctions.noise(getNoise(Noises.SPAGHETTI_3D_RARITY), 2.0D, 1.0D));
      DensityFunction var1 = DensityFunctions.mappedNoise(getNoise(Noises.SPAGHETTI_3D_THICKNESS), -0.065D, -0.088D);
      DensityFunction var2 = DensityFunctions.weirdScaledSampler(var0, getNoise(Noises.SPAGHETTI_3D_1), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE1);
      DensityFunction var3 = DensityFunctions.weirdScaledSampler(var0, getNoise(Noises.SPAGHETTI_3D_2), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE1);
      DensityFunction var4 = DensityFunctions.add(DensityFunctions.max(var2, var3), var1).clamp(-1.0D, 1.0D);
      DensityFunction var5 = getFunction(SPAGHETTI_ROUGHNESS_FUNCTION);
      DensityFunction var6 = DensityFunctions.noise(getNoise(Noises.CAVE_ENTRANCE), 0.75D, 0.5D);
      DensityFunction var7 = DensityFunctions.add(DensityFunctions.add(var6, DensityFunctions.constant(0.37D)), DensityFunctions.yClampedGradient(-10, 30, 0.3D, 0.0D));
      return DensityFunctions.cacheOnce(DensityFunctions.min(var7, DensityFunctions.add(var5, var4)));
   }

   private static DensityFunction noodle() {
      DensityFunction var0 = getFunction(Y);
      boolean var1 = true;
      boolean var2 = true;
      boolean var3 = true;
      DensityFunction var4 = yLimitedInterpolatable(var0, DensityFunctions.noise(getNoise(Noises.NOODLE), 1.0D, 1.0D), -60, 320, -1);
      DensityFunction var5 = yLimitedInterpolatable(var0, DensityFunctions.mappedNoise(getNoise(Noises.NOODLE_THICKNESS), 1.0D, 1.0D, -0.05D, -0.1D), -60, 320, 0);
      double var6 = 2.6666666666666665D;
      DensityFunction var8 = yLimitedInterpolatable(var0, DensityFunctions.noise(getNoise(Noises.NOODLE_RIDGE_A), 2.6666666666666665D, 2.6666666666666665D), -60, 320, 0);
      DensityFunction var9 = yLimitedInterpolatable(var0, DensityFunctions.noise(getNoise(Noises.NOODLE_RIDGE_B), 2.6666666666666665D, 2.6666666666666665D), -60, 320, 0);
      DensityFunction var10 = DensityFunctions.mul(DensityFunctions.constant(1.5D), DensityFunctions.max(var8.abs(), var9.abs()));
      return DensityFunctions.rangeChoice(var4, -1000000.0D, 0.0D, DensityFunctions.constant(64.0D), DensityFunctions.add(var5, var10));
   }

   private static DensityFunction pillars() {
      double var0 = 25.0D;
      double var2 = 0.3D;
      DensityFunction var4 = DensityFunctions.noise(getNoise(Noises.PILLAR), 25.0D, 0.3D);
      DensityFunction var5 = DensityFunctions.mappedNoise(getNoise(Noises.PILLAR_RARENESS), 0.0D, -2.0D);
      DensityFunction var6 = DensityFunctions.mappedNoise(getNoise(Noises.PILLAR_THICKNESS), 0.0D, 1.1D);
      DensityFunction var7 = DensityFunctions.add(DensityFunctions.mul(var4, DensityFunctions.constant(2.0D)), var5);
      return DensityFunctions.cacheOnce(DensityFunctions.mul(var7, var6.cube()));
   }

   private static DensityFunction spaghetti2D() {
      DensityFunction var0 = DensityFunctions.noise(getNoise(Noises.SPAGHETTI_2D_MODULATOR), 2.0D, 1.0D);
      DensityFunction var1 = DensityFunctions.weirdScaledSampler(var0, getNoise(Noises.SPAGHETTI_2D), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE2);
      DensityFunction var2 = DensityFunctions.mappedNoise(getNoise(Noises.SPAGHETTI_2D_ELEVATION), 0.0D, (double)Math.floorDiv(-64, 8), 8.0D);
      DensityFunction var3 = getFunction(SPAGHETTI_2D_THICKNESS_MODULATOR);
      DensityFunction var4 = DensityFunctions.add(var2, DensityFunctions.yClampedGradient(-64, 320, 8.0D, -40.0D)).abs();
      DensityFunction var5 = DensityFunctions.add(var4, var3).cube();
      double var6 = 0.083D;
      DensityFunction var8 = DensityFunctions.add(var1, DensityFunctions.mul(DensityFunctions.constant(0.083D), var3));
      return DensityFunctions.max(var8, var5).clamp(-1.0D, 1.0D);
   }

   private static DensityFunction underground(DensityFunction var0) {
      DensityFunction var1 = getFunction(SPAGHETTI_2D);
      DensityFunction var2 = getFunction(SPAGHETTI_ROUGHNESS_FUNCTION);
      DensityFunction var3 = DensityFunctions.noise(getNoise(Noises.CAVE_LAYER), 8.0D);
      DensityFunction var4 = DensityFunctions.mul(DensityFunctions.constant(4.0D), var3.square());
      DensityFunction var5 = DensityFunctions.noise(getNoise(Noises.CAVE_CHEESE), 0.6666666666666666D);
      DensityFunction var6 = DensityFunctions.add(DensityFunctions.add(DensityFunctions.constant(0.27D), var5).clamp(-1.0D, 1.0D), DensityFunctions.add(DensityFunctions.constant(1.5D), DensityFunctions.mul(DensityFunctions.constant(-0.64D), var0)).clamp(0.0D, 0.5D));
      DensityFunction var7 = DensityFunctions.add(var4, var6);
      DensityFunction var8 = DensityFunctions.min(DensityFunctions.min(var7, getFunction(ENTRANCES)), DensityFunctions.add(var1, var2));
      DensityFunction var9 = getFunction(PILLARS);
      DensityFunction var10 = DensityFunctions.rangeChoice(var9, -1000000.0D, 0.03D, DensityFunctions.constant(-1000000.0D), var9);
      return DensityFunctions.max(var8, var10);
   }

   private static DensityFunction postProcess(NoiseSettings var0, DensityFunction var1) {
      DensityFunction var2 = DensityFunctions.slide(var0, var1);
      DensityFunction var3 = DensityFunctions.blendDensity(var2);
      return DensityFunctions.mul(DensityFunctions.interpolated(var3), DensityFunctions.constant(0.64D)).squeeze();
   }

   private static NoiseRouterWithOnlyNoises overworldWithNewCaves(NoiseSettings var0, boolean var1) {
      DensityFunction var2 = DensityFunctions.noise(getNoise(Noises.AQUIFER_BARRIER), 0.5D);
      DensityFunction var3 = DensityFunctions.noise(getNoise(Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS), 0.67D);
      DensityFunction var4 = DensityFunctions.noise(getNoise(Noises.AQUIFER_FLUID_LEVEL_SPREAD), 0.7142857142857143D);
      DensityFunction var5 = DensityFunctions.noise(getNoise(Noises.AQUIFER_LAVA));
      DensityFunction var6 = getFunction(SHIFT_X);
      DensityFunction var7 = getFunction(SHIFT_Z);
      DensityFunction var8 = DensityFunctions.shiftedNoise2d(var6, var7, 0.25D, getNoise(var1 ? Noises.TEMPERATURE_LARGE : Noises.TEMPERATURE));
      DensityFunction var9 = DensityFunctions.shiftedNoise2d(var6, var7, 0.25D, getNoise(var1 ? Noises.VEGETATION_LARGE : Noises.VEGETATION));
      DensityFunction var10 = getFunction(var1 ? FACTOR_LARGE : FACTOR);
      DensityFunction var11 = getFunction(var1 ? DEPTH_LARGE : DEPTH);
      DensityFunction var12 = noiseGradientDensity(DensityFunctions.cache2d(var10), var11);
      DensityFunction var13 = getFunction(var1 ? SLOPED_CHEESE_LARGE : SLOPED_CHEESE);
      DensityFunction var14 = DensityFunctions.min(var13, DensityFunctions.mul(DensityFunctions.constant(5.0D), getFunction(ENTRANCES)));
      DensityFunction var15 = DensityFunctions.rangeChoice(var13, -1000000.0D, 1.5625D, var14, underground(var13));
      DensityFunction var16 = DensityFunctions.min(postProcess(var0, var15), getFunction(NOODLE));
      DensityFunction var17 = getFunction(Y);
      int var18 = var0.minY();
      int var19 = Stream.of(OreVeinifier.VeinType.values()).mapToInt((var0x) -> {
         return var0x.minY;
      }).min().orElse(var18);
      int var20 = Stream.of(OreVeinifier.VeinType.values()).mapToInt((var0x) -> {
         return var0x.maxY;
      }).max().orElse(var18);
      DensityFunction var21 = yLimitedInterpolatable(var17, DensityFunctions.noise(getNoise(Noises.ORE_VEININESS), 1.5D, 1.5D), var19, var20, 0);
      float var22 = 4.0F;
      DensityFunction var23 = yLimitedInterpolatable(var17, DensityFunctions.noise(getNoise(Noises.ORE_VEIN_A), 4.0D, 4.0D), var19, var20, 0).abs();
      DensityFunction var24 = yLimitedInterpolatable(var17, DensityFunctions.noise(getNoise(Noises.ORE_VEIN_B), 4.0D, 4.0D), var19, var20, 0).abs();
      DensityFunction var25 = DensityFunctions.add(DensityFunctions.constant(-0.07999999821186066D), DensityFunctions.max(var23, var24));
      DensityFunction var26 = DensityFunctions.noise(getNoise(Noises.ORE_GAP));
      return new NoiseRouterWithOnlyNoises(var2, var3, var4, var5, var8, var9, getFunction(var1 ? CONTINENTS_LARGE : CONTINENTS), getFunction(var1 ? EROSION_LARGE : EROSION), getFunction(var1 ? DEPTH_LARGE : DEPTH), getFunction(RIDGES), var12, var16, var21, var25, var26);
   }

   private static NoiseRouterWithOnlyNoises noNewCaves(NoiseSettings var0) {
      DensityFunction var1 = getFunction(SHIFT_X);
      DensityFunction var2 = getFunction(SHIFT_Z);
      DensityFunction var3 = DensityFunctions.shiftedNoise2d(var1, var2, 0.25D, getNoise(Noises.TEMPERATURE));
      DensityFunction var4 = DensityFunctions.shiftedNoise2d(var1, var2, 0.25D, getNoise(Noises.VEGETATION));
      DensityFunction var5 = noiseGradientDensity(DensityFunctions.cache2d(getFunction(FACTOR)), getFunction(DEPTH));
      DensityFunction var6 = postProcess(var0, getFunction(SLOPED_CHEESE));
      return new NoiseRouterWithOnlyNoises(DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), var3, var4, getFunction(CONTINENTS), getFunction(EROSION), getFunction(DEPTH), getFunction(RIDGES), var5, var6, DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero());
   }

   protected static NoiseRouterWithOnlyNoises overworldWithoutCaves(NoiseSettings var0) {
      return noNewCaves(var0);
   }

   protected static NoiseRouterWithOnlyNoises nether(NoiseSettings var0) {
      return noNewCaves(var0);
   }

   protected static NoiseRouterWithOnlyNoises end(NoiseSettings var0) {
      DensityFunction var1 = DensityFunctions.cache2d(DensityFunctions.endIslands(0L));
      DensityFunction var2 = postProcess(var0, getFunction(SLOPED_CHEESE_END));
      return new NoiseRouterWithOnlyNoises(DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), var1, var2, DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero());
   }

   private static NormalNoise seedNoise(PositionalRandomFactory var0, Registry<NormalNoise.NoiseParameters> var1, Holder<NormalNoise.NoiseParameters> var2) {
      Optional var10001 = var2.unwrapKey();
      Objects.requireNonNull(var1);
      return Noises.instantiate(var0, (Holder)var10001.flatMap(var1::getHolder).orElse(var2));
   }

   public static NoiseRouter createNoiseRouter(NoiseSettings var0, long var1, Registry<NormalNoise.NoiseParameters> var3, WorldgenRandom.Algorithm var4, NoiseRouterWithOnlyNoises var5) {
      boolean var6 = var4 == WorldgenRandom.Algorithm.LEGACY;
      PositionalRandomFactory var7 = var4.newInstance(var1).forkPositional();
      HashMap var8 = new HashMap();
      DensityFunction.Visitor var9 = (var7x) -> {
         Holder var15;
         if (var7x instanceof DensityFunctions.Noise) {
            DensityFunctions.Noise var16 = (DensityFunctions.Noise)var7x;
            var15 = var16.noiseData();
            return new DensityFunctions.Noise(var15, seedNoise(var7, var3, var15), var16.xzScale(), var16.yScale());
         } else if (var7x instanceof DensityFunctions.ShiftNoise) {
            DensityFunctions.ShiftNoise var14 = (DensityFunctions.ShiftNoise)var7x;
            Holder var18 = var14.noiseData();
            NormalNoise var17;
            if (var6) {
               var17 = NormalNoise.create(var7.fromHashOf(Noises.SHIFT.location()), new NormalNoise.NoiseParameters(0, 0.0D, new double[0]));
            } else {
               var17 = seedNoise(var7, var3, var18);
            }

            return var14.withNewNoise(var17);
         } else if (var7x instanceof DensityFunctions.ShiftedNoise) {
            DensityFunctions.ShiftedNoise var13 = (DensityFunctions.ShiftedNoise)var7x;
            if (var6) {
               var15 = var13.noiseData();
               NormalNoise var10;
               if (Objects.equals(var15.unwrapKey(), Optional.of(Noises.TEMPERATURE))) {
                  var10 = NormalNoise.createLegacyNetherBiome(var4.newInstance(var1), new NormalNoise.NoiseParameters(-7, 1.0D, new double[]{1.0D}));
                  return new DensityFunctions.ShiftedNoise(var13.shiftX(), var13.shiftY(), var13.shiftZ(), var13.xzScale(), var13.yScale(), var15, var10);
               }

               if (Objects.equals(var15.unwrapKey(), Optional.of(Noises.VEGETATION))) {
                  var10 = NormalNoise.createLegacyNetherBiome(var4.newInstance(var1 + 1L), new NormalNoise.NoiseParameters(-7, 1.0D, new double[]{1.0D}));
                  return new DensityFunctions.ShiftedNoise(var13.shiftX(), var13.shiftY(), var13.shiftZ(), var13.xzScale(), var13.yScale(), var15, var10);
               }
            }

            var15 = var13.noiseData();
            return new DensityFunctions.ShiftedNoise(var13.shiftX(), var13.shiftY(), var13.shiftZ(), var13.xzScale(), var13.yScale(), var15, seedNoise(var7, var3, var15));
         } else if (var7x instanceof DensityFunctions.WeirdScaledSampler) {
            DensityFunctions.WeirdScaledSampler var12 = (DensityFunctions.WeirdScaledSampler)var7x;
            return new DensityFunctions.WeirdScaledSampler(var12.input(), var12.noiseData(), seedNoise(var7, var3, var12.noiseData()), var12.rarityValueMapper());
         } else if (var7x instanceof BlendedNoise) {
            return var6 ? new BlendedNoise(var4.newInstance(var1), var0.noiseSamplingSettings(), var0.getCellWidth(), var0.getCellHeight()) : new BlendedNoise(var7.fromHashOf(new ResourceLocation("terrain")), var0.noiseSamplingSettings(), var0.getCellWidth(), var0.getCellHeight());
         } else if (var7x instanceof DensityFunctions.EndIslandDensityFunction) {
            return new DensityFunctions.EndIslandDensityFunction(var1);
         } else if (var7x instanceof DensityFunctions.TerrainShaperSpline) {
            DensityFunctions.TerrainShaperSpline var11 = (DensityFunctions.TerrainShaperSpline)var7x;
            TerrainShaper var9 = var0.terrainShaper();
            return new DensityFunctions.TerrainShaperSpline(var11.continentalness(), var11.erosion(), var11.weirdness(), var9, var11.spline(), var11.minValue(), var11.maxValue());
         } else if (var7x instanceof DensityFunctions.Slide) {
            DensityFunctions.Slide var8 = (DensityFunctions.Slide)var7x;
            return new DensityFunctions.Slide(var0, var8.input());
         } else {
            return var7x;
         }
      };
      DensityFunction.Visitor var10 = (var2) -> {
         return (DensityFunction)var8.computeIfAbsent(var2, var9);
      };
      NoiseRouterWithOnlyNoises var11 = var5.mapAll(var10);
      PositionalRandomFactory var12 = var7.fromHashOf(new ResourceLocation("aquifer")).forkPositional();
      PositionalRandomFactory var13 = var7.fromHashOf(new ResourceLocation("ore")).forkPositional();
      return new NoiseRouter(var11.barrierNoise(), var11.fluidLevelFloodednessNoise(), var11.fluidLevelSpreadNoise(), var11.lavaNoise(), var12, var13, var11.temperature(), var11.vegetation(), var11.continents(), var11.erosion(), var11.depth(), var11.ridges(), var11.initialDensityWithoutJaggedness(), var11.finalDensity(), var11.veinToggle(), var11.veinRidged(), var11.veinGap(), (new OverworldBiomeBuilder()).spawnTarget());
   }

   private static DensityFunction splineWithBlending(DensityFunction var0, DensityFunction var1, DensityFunction var2, DensityFunctions.TerrainShaperSpline.SplineType var3, double var4, double var6, DensityFunction var8) {
      DensityFunction var9 = DensityFunctions.terrainShaperSpline(var0, var1, var2, var3, var4, var6);
      DensityFunction var10 = DensityFunctions.lerp(DensityFunctions.blendAlpha(), var8, var9);
      return DensityFunctions.flatCache(DensityFunctions.cache2d(var10));
   }

   private static DensityFunction noiseGradientDensity(DensityFunction var0, DensityFunction var1) {
      DensityFunction var2 = DensityFunctions.mul(var1, var0);
      return DensityFunctions.mul(DensityFunctions.constant(4.0D), var2.quarterNegative());
   }

   private static DensityFunction yLimitedInterpolatable(DensityFunction var0, DensityFunction var1, int var2, int var3, int var4) {
      return DensityFunctions.interpolated(DensityFunctions.rangeChoice(var0, (double)var2, (double)(var3 + 1), var1, DensityFunctions.constant((double)var4)));
   }

   protected static double applySlide(NoiseSettings var0, double var1, double var3) {
      double var5 = (double)((int)var3 / var0.getCellHeight() - var0.getMinCellY());
      var1 = var0.topSlideSettings().applySlide(var1, (double)var0.getCellCountY() - var5);
      var1 = var0.bottomSlideSettings().applySlide(var1, var5);
      return var1;
   }

   protected static double computePreliminarySurfaceLevelScanning(NoiseSettings var0, DensityFunction var1, int var2, int var3) {
      for(int var4 = var0.getMinCellY() + var0.getCellCountY(); var4 >= var0.getMinCellY(); --var4) {
         int var5 = var4 * var0.getCellHeight();
         double var6 = -0.703125D;
         double var8 = var1.compute(new DensityFunction.SinglePointContext(var2, var5, var3)) + -0.703125D;
         double var10 = Mth.clamp(var8, -64.0D, 64.0D);
         var10 = applySlide(var0, var10, (double)var5);
         if (var10 > 0.390625D) {
            return (double)var5;
         }
      }

      return 2.147483647E9D;
   }

   protected static final class QuantizedSpaghettiRarity {
      protected QuantizedSpaghettiRarity() {
      }

      protected static double getSphaghettiRarity2D(double var0) {
         if (var0 < -0.75D) {
            return 0.5D;
         } else if (var0 < -0.5D) {
            return 0.75D;
         } else if (var0 < 0.5D) {
            return 1.0D;
         } else {
            return var0 < 0.75D ? 2.0D : 3.0D;
         }
      }

      protected static double getSpaghettiRarity3D(double var0) {
         if (var0 < -0.5D) {
            return 0.75D;
         } else if (var0 < 0.0D) {
            return 1.0D;
         } else {
            return var0 < 0.5D ? 1.5D : 2.0D;
         }
      }
   }
}
