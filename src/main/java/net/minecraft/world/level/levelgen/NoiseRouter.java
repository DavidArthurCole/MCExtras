package net.minecraft.world.level.levelgen;

import java.util.List;
import net.minecraft.world.level.biome.Climate;

public record NoiseRouter(DensityFunction a, DensityFunction b, DensityFunction c, DensityFunction d, PositionalRandomFactory e, PositionalRandomFactory f, DensityFunction g, DensityFunction h, DensityFunction i, DensityFunction j, DensityFunction k, DensityFunction l, DensityFunction m, DensityFunction n, DensityFunction o, DensityFunction p, DensityFunction q, List<Climate.ParameterPoint> r) {
   private final DensityFunction barrierNoise;
   private final DensityFunction fluidLevelFloodednessNoise;
   private final DensityFunction fluidLevelSpreadNoise;
   private final DensityFunction lavaNoise;
   private final PositionalRandomFactory aquiferPositionalRandomFactory;
   private final PositionalRandomFactory oreVeinsPositionalRandomFactory;
   private final DensityFunction temperature;
   private final DensityFunction humidity;
   private final DensityFunction continents;
   private final DensityFunction erosion;
   private final DensityFunction depth;
   private final DensityFunction ridges;
   private final DensityFunction initialDensityWithoutJaggedness;
   private final DensityFunction finalDensity;
   private final DensityFunction veinToggle;
   private final DensityFunction veinRidged;
   private final DensityFunction veinGap;
   private final List<Climate.ParameterPoint> spawnTarget;

   public NoiseRouter(DensityFunction var1, DensityFunction var2, DensityFunction var3, DensityFunction var4, PositionalRandomFactory var5, PositionalRandomFactory var6, DensityFunction var7, DensityFunction var8, DensityFunction var9, DensityFunction var10, DensityFunction var11, DensityFunction var12, DensityFunction var13, DensityFunction var14, DensityFunction var15, DensityFunction var16, DensityFunction var17, List<Climate.ParameterPoint> var18) {
      this.barrierNoise = var1;
      this.fluidLevelFloodednessNoise = var2;
      this.fluidLevelSpreadNoise = var3;
      this.lavaNoise = var4;
      this.aquiferPositionalRandomFactory = var5;
      this.oreVeinsPositionalRandomFactory = var6;
      this.temperature = var7;
      this.humidity = var8;
      this.continents = var9;
      this.erosion = var10;
      this.depth = var11;
      this.ridges = var12;
      this.initialDensityWithoutJaggedness = var13;
      this.finalDensity = var14;
      this.veinToggle = var15;
      this.veinRidged = var16;
      this.veinGap = var17;
      this.spawnTarget = var18;
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

   public PositionalRandomFactory aquiferPositionalRandomFactory() {
      return this.aquiferPositionalRandomFactory;
   }

   public PositionalRandomFactory oreVeinsPositionalRandomFactory() {
      return this.oreVeinsPositionalRandomFactory;
   }

   public DensityFunction temperature() {
      return this.temperature;
   }

   public DensityFunction humidity() {
      return this.humidity;
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

   public List<Climate.ParameterPoint> spawnTarget() {
      return this.spawnTarget;
   }
}
