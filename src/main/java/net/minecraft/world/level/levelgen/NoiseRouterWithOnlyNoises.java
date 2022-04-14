package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;

public record NoiseRouterWithOnlyNoises(DensityFunction b, DensityFunction c, DensityFunction d, DensityFunction e, DensityFunction f, DensityFunction g, DensityFunction h, DensityFunction i, DensityFunction j, DensityFunction k, DensityFunction l, DensityFunction m, DensityFunction n, DensityFunction o, DensityFunction p) {
   private final DensityFunction barrierNoise;
   private final DensityFunction fluidLevelFloodednessNoise;
   private final DensityFunction fluidLevelSpreadNoise;
   private final DensityFunction lavaNoise;
   private final DensityFunction temperature;
   private final DensityFunction vegetation;
   private final DensityFunction continents;
   private final DensityFunction erosion;
   private final DensityFunction depth;
   private final DensityFunction ridges;
   private final DensityFunction initialDensityWithoutJaggedness;
   private final DensityFunction finalDensity;
   private final DensityFunction veinToggle;
   private final DensityFunction veinRidged;
   private final DensityFunction veinGap;
   public static final Codec<NoiseRouterWithOnlyNoises> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(field("barrier", NoiseRouterWithOnlyNoises::barrierNoise), field("fluid_level_floodedness", NoiseRouterWithOnlyNoises::fluidLevelFloodednessNoise), field("fluid_level_spread", NoiseRouterWithOnlyNoises::fluidLevelSpreadNoise), field("lava", NoiseRouterWithOnlyNoises::lavaNoise), field("temperature", NoiseRouterWithOnlyNoises::temperature), field("vegetation", NoiseRouterWithOnlyNoises::vegetation), field("continents", NoiseRouterWithOnlyNoises::continents), field("erosion", NoiseRouterWithOnlyNoises::erosion), field("depth", NoiseRouterWithOnlyNoises::depth), field("ridges", NoiseRouterWithOnlyNoises::ridges), field("initial_density_without_jaggedness", NoiseRouterWithOnlyNoises::initialDensityWithoutJaggedness), field("final_density", NoiseRouterWithOnlyNoises::finalDensity), field("vein_toggle", NoiseRouterWithOnlyNoises::veinToggle), field("vein_ridged", NoiseRouterWithOnlyNoises::veinRidged), field("vein_gap", NoiseRouterWithOnlyNoises::veinGap)).apply(var0, NoiseRouterWithOnlyNoises::new);
   });

   public NoiseRouterWithOnlyNoises(DensityFunction var1, DensityFunction var2, DensityFunction var3, DensityFunction var4, DensityFunction var5, DensityFunction var6, DensityFunction var7, DensityFunction var8, DensityFunction var9, DensityFunction var10, DensityFunction var11, DensityFunction var12, DensityFunction var13, DensityFunction var14, DensityFunction var15) {
      this.barrierNoise = var1;
      this.fluidLevelFloodednessNoise = var2;
      this.fluidLevelSpreadNoise = var3;
      this.lavaNoise = var4;
      this.temperature = var5;
      this.vegetation = var6;
      this.continents = var7;
      this.erosion = var8;
      this.depth = var9;
      this.ridges = var10;
      this.initialDensityWithoutJaggedness = var11;
      this.finalDensity = var12;
      this.veinToggle = var13;
      this.veinRidged = var14;
      this.veinGap = var15;
   }

   private static RecordCodecBuilder<NoiseRouterWithOnlyNoises, DensityFunction> field(String var0, Function<NoiseRouterWithOnlyNoises, DensityFunction> var1) {
      return DensityFunction.HOLDER_HELPER_CODEC.fieldOf(var0).forGetter(var1);
   }

   public NoiseRouterWithOnlyNoises mapAll(DensityFunction.Visitor var1) {
      return new NoiseRouterWithOnlyNoises(this.barrierNoise.mapAll(var1), this.fluidLevelFloodednessNoise.mapAll(var1), this.fluidLevelSpreadNoise.mapAll(var1), this.lavaNoise.mapAll(var1), this.temperature.mapAll(var1), this.vegetation.mapAll(var1), this.continents.mapAll(var1), this.erosion.mapAll(var1), this.depth.mapAll(var1), this.ridges.mapAll(var1), this.initialDensityWithoutJaggedness.mapAll(var1), this.finalDensity.mapAll(var1), this.veinToggle.mapAll(var1), this.veinRidged.mapAll(var1), this.veinGap.mapAll(var1));
   }

   public DensityFunction barrierNoise() {
      return this.barrierNoise;
   }

   public DensityFunction fluidLevelFloodednessNoise() {
      return this.fluidLevelFloodednessNoise;
   }

   public DensityFunction fluidLevelSpreadNoise() {
      return this.fluidLevelSpreadNoise;
   }

   public DensityFunction lavaNoise() {
      return this.lavaNoise;
   }

   public DensityFunction temperature() {
      return this.temperature;
   }

   public DensityFunction vegetation() {
      return this.vegetation;
   }

   public DensityFunction continents() {
      return this.continents;
   }

   public DensityFunction erosion() {
      return this.erosion;
   }

   public DensityFunction depth() {
      return this.depth;
   }

   public DensityFunction ridges() {
      return this.ridges;
   }

   public DensityFunction initialDensityWithoutJaggedness() {
      return this.initialDensityWithoutJaggedness;
   }

   public DensityFunction finalDensity() {
      return this.finalDensity;
   }

   public DensityFunction veinToggle() {
      return this.veinToggle;
   }

   public DensityFunction veinRidged() {
      return this.veinRidged;
   }

   public DensityFunction veinGap() {
      return this.veinGap;
   }
}
