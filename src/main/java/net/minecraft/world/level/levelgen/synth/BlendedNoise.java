package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import java.util.stream.IntStream;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseSamplingSettings;
import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

public class BlendedNoise implements DensityFunction.SimpleFunction {
   public static final BlendedNoise UNSEEDED = new BlendedNoise(new XoroshiroRandomSource(0L), new NoiseSamplingSettings(1.0D, 1.0D, 80.0D, 160.0D), 4, 8);
   public static final Codec<BlendedNoise> CODEC;
   private final PerlinNoise minLimitNoise;
   private final PerlinNoise maxLimitNoise;
   private final PerlinNoise mainNoise;
   private final double xzScale;
   private final double yScale;
   private final double xzMainScale;
   private final double yMainScale;
   private final int cellWidth;
   private final int cellHeight;
   private final double maxValue;

   private BlendedNoise(PerlinNoise var1, PerlinNoise var2, PerlinNoise var3, NoiseSamplingSettings var4, int var5, int var6) {
      this.minLimitNoise = var1;
      this.maxLimitNoise = var2;
      this.mainNoise = var3;
      this.xzScale = 684.412D * var4.xzScale();
      this.yScale = 684.412D * var4.yScale();
      this.xzMainScale = this.xzScale / var4.xzFactor();
      this.yMainScale = this.yScale / var4.yFactor();
      this.cellWidth = var5;
      this.cellHeight = var6;
      this.maxValue = var1.maxBrokenValue(this.yScale);
   }

   public BlendedNoise(RandomSource var1, NoiseSamplingSettings var2, int var3, int var4) {
      this(PerlinNoise.createLegacyForBlendedNoise(var1, IntStream.rangeClosed(-15, 0)), PerlinNoise.createLegacyForBlendedNoise(var1, IntStream.rangeClosed(-15, 0)), PerlinNoise.createLegacyForBlendedNoise(var1, IntStream.rangeClosed(-7, 0)), var2, var3, var4);
   }

   public double compute(DensityFunction.FunctionContext var1) {
      int var2 = Math.floorDiv(var1.blockX(), this.cellWidth);
      int var3 = Math.floorDiv(var1.blockY(), this.cellHeight);
      int var4 = Math.floorDiv(var1.blockZ(), this.cellWidth);
      double var5 = 0.0D;
      double var7 = 0.0D;
      double var9 = 0.0D;
      boolean var11 = true;
      double var12 = 1.0D;

      for(int var14 = 0; var14 < 8; ++var14) {
         ImprovedNoise var15 = this.mainNoise.getOctaveNoise(var14);
         if (var15 != null) {
            var9 += var15.noise(PerlinNoise.wrap((double)var2 * this.xzMainScale * var12), PerlinNoise.wrap((double)var3 * this.yMainScale * var12), PerlinNoise.wrap((double)var4 * this.xzMainScale * var12), this.yMainScale * var12, (double)var3 * this.yMainScale * var12) / var12;
         }

         var12 /= 2.0D;
      }

      double var28 = (var9 / 10.0D + 1.0D) / 2.0D;
      boolean var16 = var28 >= 1.0D;
      boolean var17 = var28 <= 0.0D;
      var12 = 1.0D;

      for(int var18 = 0; var18 < 16; ++var18) {
         double var19 = PerlinNoise.wrap((double)var2 * this.xzScale * var12);
         double var21 = PerlinNoise.wrap((double)var3 * this.yScale * var12);
         double var23 = PerlinNoise.wrap((double)var4 * this.xzScale * var12);
         double var25 = this.yScale * var12;
         ImprovedNoise var27;
         if (!var16) {
            var27 = this.minLimitNoise.getOctaveNoise(var18);
            if (var27 != null) {
               var5 += var27.noise(var19, var21, var23, var25, (double)var3 * var25) / var12;
            }
         }

         if (!var17) {
            var27 = this.maxLimitNoise.getOctaveNoise(var18);
            if (var27 != null) {
               var7 += var27.noise(var19, var21, var23, var25, (double)var3 * var25) / var12;
            }
         }

         var12 /= 2.0D;
      }

      return Mth.clampedLerp(var5 / 512.0D, var7 / 512.0D, var28) / 128.0D;
   }

   public double minValue() {
      return -this.maxValue();
   }

   public double maxValue() {
      return this.maxValue;
   }

   @VisibleForTesting
   public void parityConfigString(StringBuilder var1) {
      var1.append("BlendedNoise{minLimitNoise=");
      this.minLimitNoise.parityConfigString(var1);
      var1.append(", maxLimitNoise=");
      this.maxLimitNoise.parityConfigString(var1);
      var1.append(", mainNoise=");
      this.mainNoise.parityConfigString(var1);
      var1.append(String.format(", xzScale=%.3f, yScale=%.3f, xzMainScale=%.3f, yMainScale=%.3f, cellWidth=%d, cellHeight=%d", this.xzScale, this.yScale, this.xzMainScale, this.yMainScale, this.cellWidth, this.cellHeight)).append('}');
   }

   public Codec<? extends DensityFunction> codec() {
      return CODEC;
   }

   static {
      CODEC = Codec.unit(UNSEEDED);
   }
}
