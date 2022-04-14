package net.minecraft.world.level.levelgen;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.Double2DoubleFunction;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.ToFloatFunction;
import net.minecraft.world.level.biome.TerrainShaper;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.slf4j.Logger;

public final class DensityFunctions {
   private static final Codec<DensityFunction> CODEC;
   protected static final double MAX_REASONABLE_NOISE_VALUE = 1000000.0D;
   static final Codec<Double> NOISE_VALUE_CODEC;
   public static final Codec<DensityFunction> DIRECT_CODEC;

   public static Codec<? extends DensityFunction> bootstrap(Registry<Codec<? extends DensityFunction>> var0) {
      register(var0, "blend_alpha", DensityFunctions.BlendAlpha.CODEC);
      register(var0, "blend_offset", DensityFunctions.BlendOffset.CODEC);
      register(var0, "beardifier", DensityFunctions.BeardifierMarker.CODEC);
      register(var0, "old_blended_noise", BlendedNoise.CODEC);
      DensityFunctions.Marker.Type[] var1 = DensityFunctions.Marker.Type.values();
      int var2 = var1.length;

      int var3;
      for(var3 = 0; var3 < var2; ++var3) {
         DensityFunctions.Marker.Type var4 = var1[var3];
         register(var0, var4.getSerializedName(), var4.codec);
      }

      register(var0, "noise", DensityFunctions.Noise.CODEC);
      register(var0, "end_islands", DensityFunctions.EndIslandDensityFunction.CODEC);
      register(var0, "weird_scaled_sampler", DensityFunctions.WeirdScaledSampler.CODEC);
      register(var0, "shifted_noise", DensityFunctions.ShiftedNoise.CODEC);
      register(var0, "range_choice", DensityFunctions.RangeChoice.CODEC);
      register(var0, "shift_a", DensityFunctions.ShiftA.CODEC);
      register(var0, "shift_b", DensityFunctions.ShiftB.CODEC);
      register(var0, "shift", DensityFunctions.Shift.CODEC);
      register(var0, "blend_density", DensityFunctions.BlendDensity.CODEC);
      register(var0, "clamp", DensityFunctions.Clamp.CODEC);
      DensityFunctions.Mapped.Type[] var5 = DensityFunctions.Mapped.Type.values();
      var2 = var5.length;

      for(var3 = 0; var3 < var2; ++var3) {
         DensityFunctions.Mapped.Type var7 = var5[var3];
         register(var0, var7.getSerializedName(), var7.codec);
      }

      register(var0, "slide", DensityFunctions.Slide.CODEC);
      DensityFunctions.TwoArgumentSimpleFunction.Type[] var6 = DensityFunctions.TwoArgumentSimpleFunction.Type.values();
      var2 = var6.length;

      for(var3 = 0; var3 < var2; ++var3) {
         DensityFunctions.TwoArgumentSimpleFunction.Type var8 = var6[var3];
         register(var0, var8.getSerializedName(), var8.codec);
      }

      register(var0, "spline", DensityFunctions.Spline.CODEC);
      register(var0, "terrain_shaper_spline", DensityFunctions.TerrainShaperSpline.CODEC);
      register(var0, "constant", DensityFunctions.Constant.CODEC);
      return register(var0, "y_clamped_gradient", DensityFunctions.YClampedGradient.CODEC);
   }

   private static Codec<? extends DensityFunction> register(Registry<Codec<? extends DensityFunction>> var0, String var1, Codec<? extends DensityFunction> var2) {
      return (Codec)Registry.register(var0, (String)var1, var2);
   }

   static <A, O> Codec<O> singleArgumentCodec(Codec<A> var0, Function<A, O> var1, Function<O, A> var2) {
      return var0.fieldOf("argument").xmap(var1, var2).codec();
   }

   static <O> Codec<O> singleFunctionArgumentCodec(Function<DensityFunction, O> var0, Function<O, DensityFunction> var1) {
      return singleArgumentCodec(DensityFunction.HOLDER_HELPER_CODEC, var0, var1);
   }

   static <O> Codec<O> doubleFunctionArgumentCodec(BiFunction<DensityFunction, DensityFunction, O> var0, Function<O, DensityFunction> var1, Function<O, DensityFunction> var2) {
      return RecordCodecBuilder.create((var3) -> {
         return var3.group(DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument1").forGetter(var1), DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument2").forGetter(var2)).apply(var3, var0);
      });
   }

   static <O> Codec<O> makeCodec(MapCodec<O> var0) {
      return var0.codec();
   }

   private DensityFunctions() {
   }

   public static DensityFunction interpolated(DensityFunction var0) {
      return new DensityFunctions.Marker(DensityFunctions.Marker.Type.Interpolated, var0);
   }

   public static DensityFunction flatCache(DensityFunction var0) {
      return new DensityFunctions.Marker(DensityFunctions.Marker.Type.FlatCache, var0);
   }

   public static DensityFunction cache2d(DensityFunction var0) {
      return new DensityFunctions.Marker(DensityFunctions.Marker.Type.Cache2D, var0);
   }

   public static DensityFunction cacheOnce(DensityFunction var0) {
      return new DensityFunctions.Marker(DensityFunctions.Marker.Type.CacheOnce, var0);
   }

   public static DensityFunction cacheAllInCell(DensityFunction var0) {
      return new DensityFunctions.Marker(DensityFunctions.Marker.Type.CacheAllInCell, var0);
   }

   public static DensityFunction mappedNoise(Holder<NormalNoise.NoiseParameters> var0, @Deprecated double var1, double var3, double var5, double var7) {
      return mapFromUnitTo(new DensityFunctions.Noise(var0, (NormalNoise)null, var1, var3), var5, var7);
   }

   public static DensityFunction mappedNoise(Holder<NormalNoise.NoiseParameters> var0, double var1, double var3, double var5) {
      return mappedNoise(var0, 1.0D, var1, var3, var5);
   }

   public static DensityFunction mappedNoise(Holder<NormalNoise.NoiseParameters> var0, double var1, double var3) {
      return mappedNoise(var0, 1.0D, 1.0D, var1, var3);
   }

   public static DensityFunction shiftedNoise2d(DensityFunction var0, DensityFunction var1, double var2, Holder<NormalNoise.NoiseParameters> var4) {
      return new DensityFunctions.ShiftedNoise(var0, zero(), var1, var2, 0.0D, var4, (NormalNoise)null);
   }

   public static DensityFunction noise(Holder<NormalNoise.NoiseParameters> var0) {
      return noise(var0, 1.0D, 1.0D);
   }

   public static DensityFunction noise(Holder<NormalNoise.NoiseParameters> var0, double var1, double var3) {
      return new DensityFunctions.Noise(var0, (NormalNoise)null, var1, var3);
   }

   public static DensityFunction noise(Holder<NormalNoise.NoiseParameters> var0, double var1) {
      return noise(var0, 1.0D, var1);
   }

   public static DensityFunction rangeChoice(DensityFunction var0, double var1, double var3, DensityFunction var5, DensityFunction var6) {
      return new DensityFunctions.RangeChoice(var0, var1, var3, var5, var6);
   }

   public static DensityFunction shiftA(Holder<NormalNoise.NoiseParameters> var0) {
      return new DensityFunctions.ShiftA(var0, (NormalNoise)null);
   }

   public static DensityFunction shiftB(Holder<NormalNoise.NoiseParameters> var0) {
      return new DensityFunctions.ShiftB(var0, (NormalNoise)null);
   }

   public static DensityFunction shift(Holder<NormalNoise.NoiseParameters> var0) {
      return new DensityFunctions.Shift(var0, (NormalNoise)null);
   }

   public static DensityFunction blendDensity(DensityFunction var0) {
      return new DensityFunctions.BlendDensity(var0);
   }

   public static DensityFunction endIslands(long var0) {
      return new DensityFunctions.EndIslandDensityFunction(var0);
   }

   public static DensityFunction weirdScaledSampler(DensityFunction var0, Holder<NormalNoise.NoiseParameters> var1, DensityFunctions.WeirdScaledSampler.RarityValueMapper var2) {
      return new DensityFunctions.WeirdScaledSampler(var0, var1, (NormalNoise)null, var2);
   }

   public static DensityFunction slide(NoiseSettings var0, DensityFunction var1) {
      return new DensityFunctions.Slide(var0, var1);
   }

   public static DensityFunction add(DensityFunction var0, DensityFunction var1) {
      return DensityFunctions.TwoArgumentSimpleFunction.create(DensityFunctions.TwoArgumentSimpleFunction.Type.ADD, var0, var1);
   }

   public static DensityFunction mul(DensityFunction var0, DensityFunction var1) {
      return DensityFunctions.TwoArgumentSimpleFunction.create(DensityFunctions.TwoArgumentSimpleFunction.Type.MUL, var0, var1);
   }

   public static DensityFunction min(DensityFunction var0, DensityFunction var1) {
      return DensityFunctions.TwoArgumentSimpleFunction.create(DensityFunctions.TwoArgumentSimpleFunction.Type.MIN, var0, var1);
   }

   public static DensityFunction max(DensityFunction var0, DensityFunction var1) {
      return DensityFunctions.TwoArgumentSimpleFunction.create(DensityFunctions.TwoArgumentSimpleFunction.Type.MAX, var0, var1);
   }

   public static DensityFunction terrainShaperSpline(DensityFunction var0, DensityFunction var1, DensityFunction var2, DensityFunctions.TerrainShaperSpline.SplineType var3, double var4, double var6) {
      return new DensityFunctions.TerrainShaperSpline(var0, var1, var2, (TerrainShaper)null, var3, var4, var6);
   }

   public static DensityFunction zero() {
      return DensityFunctions.Constant.ZERO;
   }

   public static DensityFunction constant(double var0) {
      return new DensityFunctions.Constant(var0);
   }

   public static DensityFunction yClampedGradient(int var0, int var1, double var2, double var4) {
      return new DensityFunctions.YClampedGradient(var0, var1, var2, var4);
   }

   public static DensityFunction map(DensityFunction var0, DensityFunctions.Mapped.Type var1) {
      return DensityFunctions.Mapped.create(var1, var0);
   }

   private static DensityFunction mapFromUnitTo(DensityFunction var0, double var1, double var3) {
      double var5 = (var1 + var3) * 0.5D;
      double var7 = (var3 - var1) * 0.5D;
      return add(constant(var5), mul(constant(var7), var0));
   }

   public static DensityFunction blendAlpha() {
      return DensityFunctions.BlendAlpha.INSTANCE;
   }

   public static DensityFunction blendOffset() {
      return DensityFunctions.BlendOffset.INSTANCE;
   }

   public static DensityFunction lerp(DensityFunction var0, DensityFunction var1, DensityFunction var2) {
      DensityFunction var3 = cacheOnce(var0);
      DensityFunction var4 = add(mul(var3, constant(-1.0D)), constant(1.0D));
      return add(mul(var1, var4), mul(var2, var3));
   }

   static {
      CODEC = Registry.DENSITY_FUNCTION_TYPES.byNameCodec().dispatch(DensityFunction::codec, Function.identity());
      NOISE_VALUE_CODEC = Codec.doubleRange(-1000000.0D, 1000000.0D);
      DIRECT_CODEC = Codec.either(NOISE_VALUE_CODEC, CODEC).xmap((var0) -> {
         return (DensityFunction)var0.map(DensityFunctions::constant, Function.identity());
      }, (var0) -> {
         if (var0 instanceof DensityFunctions.Constant) {
            DensityFunctions.Constant var1 = (DensityFunctions.Constant)var0;
            return Either.left(var1.value());
         } else {
            return Either.right(var0);
         }
      });
   }

   protected static enum BlendAlpha implements DensityFunction.SimpleFunction {
      INSTANCE;

      public static final Codec<DensityFunction> CODEC = Codec.unit(INSTANCE);

      private BlendAlpha() {
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return 1.0D;
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         Arrays.fill(var1, 1.0D);
      }

      public double minValue() {
         return 1.0D;
      }

      public double maxValue() {
         return 1.0D;
      }

      public Codec<? extends DensityFunction> codec() {
         return CODEC;
      }

      // $FF: synthetic method
      private static DensityFunctions.BlendAlpha[] $values() {
         return new DensityFunctions.BlendAlpha[]{INSTANCE};
      }
   }

   protected static enum BlendOffset implements DensityFunction.SimpleFunction {
      INSTANCE;

      public static final Codec<DensityFunction> CODEC = Codec.unit(INSTANCE);

      private BlendOffset() {
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return 0.0D;
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         Arrays.fill(var1, 0.0D);
      }

      public double minValue() {
         return 0.0D;
      }

      public double maxValue() {
         return 0.0D;
      }

      public Codec<? extends DensityFunction> codec() {
         return CODEC;
      }

      // $FF: synthetic method
      private static DensityFunctions.BlendOffset[] $values() {
         return new DensityFunctions.BlendOffset[]{INSTANCE};
      }
   }

   protected static enum BeardifierMarker implements DensityFunctions.BeardifierOrMarker {
      INSTANCE;

      private BeardifierMarker() {
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return 0.0D;
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         Arrays.fill(var1, 0.0D);
      }

      public double minValue() {
         return 0.0D;
      }

      public double maxValue() {
         return 0.0D;
      }

      // $FF: synthetic method
      private static DensityFunctions.BeardifierMarker[] $values() {
         return new DensityFunctions.BeardifierMarker[]{INSTANCE};
      }
   }

   protected static record Marker(DensityFunctions.Marker.Type a, DensityFunction e) implements DensityFunctions.MarkerOrMarked {
      private final DensityFunctions.Marker.Type type;
      private final DensityFunction wrapped;

      protected Marker(DensityFunctions.Marker.Type var1, DensityFunction var2) {
         this.type = var1;
         this.wrapped = var2;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return this.wrapped.compute(var1);
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         this.wrapped.fillArray(var1, var2);
      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return (DensityFunction)var1.apply(new DensityFunctions.Marker(this.type, this.wrapped.mapAll(var1)));
      }

      public double minValue() {
         return this.wrapped.minValue();
      }

      public double maxValue() {
         return this.wrapped.maxValue();
      }

      public DensityFunctions.Marker.Type type() {
         return this.type;
      }

      public DensityFunction wrapped() {
         return this.wrapped;
      }

      static enum Type implements StringRepresentable {
         Interpolated("interpolated"),
         FlatCache("flat_cache"),
         Cache2D("cache_2d"),
         CacheOnce("cache_once"),
         CacheAllInCell("cache_all_in_cell");

         private final String name;
         final Codec<DensityFunctions.MarkerOrMarked> codec = DensityFunctions.singleFunctionArgumentCodec((var1x) -> {
            return new DensityFunctions.Marker(this, var1x);
         }, DensityFunctions.MarkerOrMarked::wrapped);

         private Type(String var3) {
            this.name = var3;
         }

         public String getSerializedName() {
            return this.name;
         }

         // $FF: synthetic method
         private static DensityFunctions.Marker.Type[] $values() {
            return new DensityFunctions.Marker.Type[]{Interpolated, FlatCache, Cache2D, CacheOnce, CacheAllInCell};
         }
      }
   }

   protected static record Noise(Holder<NormalNoise.NoiseParameters> f, @Nullable NormalNoise g, double h, double i) implements DensityFunction.SimpleFunction {
      private final Holder<NormalNoise.NoiseParameters> noiseData;
      @Nullable
      private final NormalNoise noise;
      /** @deprecated */
      @Deprecated
      private final double xzScale;
      private final double yScale;
      public static final MapCodec<DensityFunctions.Noise> DATA_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(NormalNoise.NoiseParameters.CODEC.fieldOf("noise").forGetter(DensityFunctions.Noise::noiseData), Codec.DOUBLE.fieldOf("xz_scale").forGetter(DensityFunctions.Noise::xzScale), Codec.DOUBLE.fieldOf("y_scale").forGetter(DensityFunctions.Noise::yScale)).apply(var0, DensityFunctions.Noise::createUnseeded);
      });
      public static final Codec<DensityFunctions.Noise> CODEC;

      protected Noise(Holder<NormalNoise.NoiseParameters> var1, @Nullable NormalNoise var2, @Deprecated double var3, double var5) {
         this.noiseData = var1;
         this.noise = var2;
         this.xzScale = var3;
         this.yScale = var5;
      }

      public static DensityFunctions.Noise createUnseeded(Holder<NormalNoise.NoiseParameters> var0, @Deprecated double var1, double var3) {
         return new DensityFunctions.Noise(var0, (NormalNoise)null, var1, var3);
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return this.noise == null ? 0.0D : this.noise.getValue((double)var1.blockX() * this.xzScale, (double)var1.blockY() * this.yScale, (double)var1.blockZ() * this.xzScale);
      }

      public double minValue() {
         return -this.maxValue();
      }

      public double maxValue() {
         return this.noise == null ? 2.0D : this.noise.maxValue();
      }

      public Codec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public Holder<NormalNoise.NoiseParameters> noiseData() {
         return this.noiseData;
      }

      @Nullable
      public NormalNoise noise() {
         return this.noise;
      }

      /** @deprecated */
      @Deprecated
      public double xzScale() {
         return this.xzScale;
      }

      public double yScale() {
         return this.yScale;
      }

      static {
         CODEC = DensityFunctions.makeCodec(DATA_CODEC);
      }
   }

   protected static final class EndIslandDensityFunction implements DensityFunction.SimpleFunction {
      public static final Codec<DensityFunctions.EndIslandDensityFunction> CODEC = Codec.unit(new DensityFunctions.EndIslandDensityFunction(0L));
      final SimplexNoise islandNoise;

      public EndIslandDensityFunction(long var1) {
         LegacyRandomSource var3 = new LegacyRandomSource(var1);
         var3.consumeCount(17292);
         this.islandNoise = new SimplexNoise(var3);
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return ((double)TheEndBiomeSource.getHeightValue(this.islandNoise, var1.blockX() / 8, var1.blockZ() / 8) - 8.0D) / 128.0D;
      }

      public double minValue() {
         return -0.84375D;
      }

      public double maxValue() {
         return 0.5625D;
      }

      public Codec<? extends DensityFunction> codec() {
         return CODEC;
      }
   }

   protected static record WeirdScaledSampler(DensityFunction e, Holder<NormalNoise.NoiseParameters> f, @Nullable NormalNoise g, DensityFunctions.WeirdScaledSampler.RarityValueMapper h) implements DensityFunctions.TransformerWithContext {
      private final DensityFunction input;
      private final Holder<NormalNoise.NoiseParameters> noiseData;
      @Nullable
      private final NormalNoise noise;
      private final DensityFunctions.WeirdScaledSampler.RarityValueMapper rarityValueMapper;
      private static final MapCodec<DensityFunctions.WeirdScaledSampler> DATA_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(DensityFunctions.WeirdScaledSampler::input), NormalNoise.NoiseParameters.CODEC.fieldOf("noise").forGetter(DensityFunctions.WeirdScaledSampler::noiseData), DensityFunctions.WeirdScaledSampler.RarityValueMapper.CODEC.fieldOf("rarity_value_mapper").forGetter(DensityFunctions.WeirdScaledSampler::rarityValueMapper)).apply(var0, DensityFunctions.WeirdScaledSampler::createUnseeded);
      });
      public static final Codec<DensityFunctions.WeirdScaledSampler> CODEC;

      protected WeirdScaledSampler(DensityFunction var1, Holder<NormalNoise.NoiseParameters> var2, @Nullable NormalNoise var3, DensityFunctions.WeirdScaledSampler.RarityValueMapper var4) {
         this.input = var1;
         this.noiseData = var2;
         this.noise = var3;
         this.rarityValueMapper = var4;
      }

      public static DensityFunctions.WeirdScaledSampler createUnseeded(DensityFunction var0, Holder<NormalNoise.NoiseParameters> var1, DensityFunctions.WeirdScaledSampler.RarityValueMapper var2) {
         return new DensityFunctions.WeirdScaledSampler(var0, var1, (NormalNoise)null, var2);
      }

      public double transform(DensityFunction.FunctionContext var1, double var2) {
         if (this.noise == null) {
            return 0.0D;
         } else {
            double var4 = this.rarityValueMapper.mapper.get(var2);
            return var4 * Math.abs(this.noise.getValue((double)var1.blockX() / var4, (double)var1.blockY() / var4, (double)var1.blockZ() / var4));
         }
      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         this.input.mapAll(var1);
         return (DensityFunction)var1.apply(new DensityFunctions.WeirdScaledSampler(this.input.mapAll(var1), this.noiseData, this.noise, this.rarityValueMapper));
      }

      public double minValue() {
         return 0.0D;
      }

      public double maxValue() {
         return this.rarityValueMapper.maxRarity * (this.noise == null ? 2.0D : this.noise.maxValue());
      }

      public Codec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public DensityFunction input() {
         return this.input;
      }

      public Holder<NormalNoise.NoiseParameters> noiseData() {
         return this.noiseData;
      }

      @Nullable
      public NormalNoise noise() {
         return this.noise;
      }

      public DensityFunctions.WeirdScaledSampler.RarityValueMapper rarityValueMapper() {
         return this.rarityValueMapper;
      }

      static {
         CODEC = DensityFunctions.makeCodec(DATA_CODEC);
      }

      public static enum RarityValueMapper implements StringRepresentable {
         TYPE1("type_1", NoiseRouterData.QuantizedSpaghettiRarity::getSpaghettiRarity3D, 2.0D),
         TYPE2("type_2", NoiseRouterData.QuantizedSpaghettiRarity::getSphaghettiRarity2D, 3.0D);

         private static final Map<String, DensityFunctions.WeirdScaledSampler.RarityValueMapper> BY_NAME = (Map)Arrays.stream(values()).collect(Collectors.toMap(DensityFunctions.WeirdScaledSampler.RarityValueMapper::getSerializedName, (var0) -> {
            return var0;
         }));
         public static final Codec<DensityFunctions.WeirdScaledSampler.RarityValueMapper> CODEC;
         private final String name;
         final Double2DoubleFunction mapper;
         final double maxRarity;

         private RarityValueMapper(String var3, Double2DoubleFunction var4, double var5) {
            this.name = var3;
            this.mapper = var4;
            this.maxRarity = var5;
         }

         public String getSerializedName() {
            return this.name;
         }

         // $FF: synthetic method
         private static DensityFunctions.WeirdScaledSampler.RarityValueMapper[] $values() {
            return new DensityFunctions.WeirdScaledSampler.RarityValueMapper[]{TYPE1, TYPE2};
         }

         static {
            Supplier var10000 = DensityFunctions.WeirdScaledSampler.RarityValueMapper::values;
            Map var10001 = BY_NAME;
            Objects.requireNonNull(var10001);
            CODEC = StringRepresentable.fromEnum(var10000, var10001::get);
         }
      }
   }

   protected static record ShiftedNoise(DensityFunction e, DensityFunction f, DensityFunction g, double h, double i, Holder<NormalNoise.NoiseParameters> j, @Nullable NormalNoise k) implements DensityFunction {
      private final DensityFunction shiftX;
      private final DensityFunction shiftY;
      private final DensityFunction shiftZ;
      private final double xzScale;
      private final double yScale;
      private final Holder<NormalNoise.NoiseParameters> noiseData;
      @Nullable
      private final NormalNoise noise;
      private static final MapCodec<DensityFunctions.ShiftedNoise> DATA_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(DensityFunction.HOLDER_HELPER_CODEC.fieldOf("shift_x").forGetter(DensityFunctions.ShiftedNoise::shiftX), DensityFunction.HOLDER_HELPER_CODEC.fieldOf("shift_y").forGetter(DensityFunctions.ShiftedNoise::shiftY), DensityFunction.HOLDER_HELPER_CODEC.fieldOf("shift_z").forGetter(DensityFunctions.ShiftedNoise::shiftZ), Codec.DOUBLE.fieldOf("xz_scale").forGetter(DensityFunctions.ShiftedNoise::xzScale), Codec.DOUBLE.fieldOf("y_scale").forGetter(DensityFunctions.ShiftedNoise::yScale), NormalNoise.NoiseParameters.CODEC.fieldOf("noise").forGetter(DensityFunctions.ShiftedNoise::noiseData)).apply(var0, DensityFunctions.ShiftedNoise::createUnseeded);
      });
      public static final Codec<DensityFunctions.ShiftedNoise> CODEC;

      protected ShiftedNoise(DensityFunction var1, DensityFunction var2, DensityFunction var3, double var4, double var6, Holder<NormalNoise.NoiseParameters> var8, @Nullable NormalNoise var9) {
         this.shiftX = var1;
         this.shiftY = var2;
         this.shiftZ = var3;
         this.xzScale = var4;
         this.yScale = var6;
         this.noiseData = var8;
         this.noise = var9;
      }

      public static DensityFunctions.ShiftedNoise createUnseeded(DensityFunction var0, DensityFunction var1, DensityFunction var2, double var3, double var5, Holder<NormalNoise.NoiseParameters> var7) {
         return new DensityFunctions.ShiftedNoise(var0, var1, var2, var3, var5, var7, (NormalNoise)null);
      }

      public double compute(DensityFunction.FunctionContext var1) {
         if (this.noise == null) {
            return 0.0D;
         } else {
            double var2 = (double)var1.blockX() * this.xzScale + this.shiftX.compute(var1);
            double var4 = (double)var1.blockY() * this.yScale + this.shiftY.compute(var1);
            double var6 = (double)var1.blockZ() * this.xzScale + this.shiftZ.compute(var1);
            return this.noise.getValue(var2, var4, var6);
         }
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         var2.fillAllDirectly(var1, this);
      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return (DensityFunction)var1.apply(new DensityFunctions.ShiftedNoise(this.shiftX.mapAll(var1), this.shiftY.mapAll(var1), this.shiftZ.mapAll(var1), this.xzScale, this.yScale, this.noiseData, this.noise));
      }

      public double minValue() {
         return -this.maxValue();
      }

      public double maxValue() {
         return this.noise == null ? 2.0D : this.noise.maxValue();
      }

      public Codec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public DensityFunction shiftX() {
         return this.shiftX;
      }

      public DensityFunction shiftY() {
         return this.shiftY;
      }

      public DensityFunction shiftZ() {
         return this.shiftZ;
      }

      public double xzScale() {
         return this.xzScale;
      }

      public double yScale() {
         return this.yScale;
      }

      public Holder<NormalNoise.NoiseParameters> noiseData() {
         return this.noiseData;
      }

      @Nullable
      public NormalNoise noise() {
         return this.noise;
      }

      static {
         CODEC = DensityFunctions.makeCodec(DATA_CODEC);
      }
   }

   static record RangeChoice(DensityFunction f, double g, double h, DensityFunction i, DensityFunction j) implements DensityFunction {
      private final DensityFunction input;
      private final double minInclusive;
      private final double maxExclusive;
      private final DensityFunction whenInRange;
      private final DensityFunction whenOutOfRange;
      public static final MapCodec<DensityFunctions.RangeChoice> DATA_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(DensityFunctions.RangeChoice::input), DensityFunctions.NOISE_VALUE_CODEC.fieldOf("min_inclusive").forGetter(DensityFunctions.RangeChoice::minInclusive), DensityFunctions.NOISE_VALUE_CODEC.fieldOf("max_exclusive").forGetter(DensityFunctions.RangeChoice::maxExclusive), DensityFunction.HOLDER_HELPER_CODEC.fieldOf("when_in_range").forGetter(DensityFunctions.RangeChoice::whenInRange), DensityFunction.HOLDER_HELPER_CODEC.fieldOf("when_out_of_range").forGetter(DensityFunctions.RangeChoice::whenOutOfRange)).apply(var0, DensityFunctions.RangeChoice::new);
      });
      public static final Codec<DensityFunctions.RangeChoice> CODEC;

      RangeChoice(DensityFunction var1, double var2, double var4, DensityFunction var6, DensityFunction var7) {
         this.input = var1;
         this.minInclusive = var2;
         this.maxExclusive = var4;
         this.whenInRange = var6;
         this.whenOutOfRange = var7;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         double var2 = this.input.compute(var1);
         return var2 >= this.minInclusive && var2 < this.maxExclusive ? this.whenInRange.compute(var1) : this.whenOutOfRange.compute(var1);
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         this.input.fillArray(var1, var2);

         for(int var3 = 0; var3 < var1.length; ++var3) {
            double var4 = var1[var3];
            if (var4 >= this.minInclusive && var4 < this.maxExclusive) {
               var1[var3] = this.whenInRange.compute(var2.forIndex(var3));
            } else {
               var1[var3] = this.whenOutOfRange.compute(var2.forIndex(var3));
            }
         }

      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return (DensityFunction)var1.apply(new DensityFunctions.RangeChoice(this.input.mapAll(var1), this.minInclusive, this.maxExclusive, this.whenInRange.mapAll(var1), this.whenOutOfRange.mapAll(var1)));
      }

      public double minValue() {
         return Math.min(this.whenInRange.minValue(), this.whenOutOfRange.minValue());
      }

      public double maxValue() {
         return Math.max(this.whenInRange.maxValue(), this.whenOutOfRange.maxValue());
      }

      public Codec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public DensityFunction input() {
         return this.input;
      }

      public double minInclusive() {
         return this.minInclusive;
      }

      public double maxExclusive() {
         return this.maxExclusive;
      }

      public DensityFunction whenInRange() {
         return this.whenInRange;
      }

      public DensityFunction whenOutOfRange() {
         return this.whenOutOfRange;
      }

      static {
         CODEC = DensityFunctions.makeCodec(DATA_CODEC);
      }
   }

   protected static record ShiftA(Holder<NormalNoise.NoiseParameters> a, @Nullable NormalNoise e) implements DensityFunctions.ShiftNoise {
      private final Holder<NormalNoise.NoiseParameters> noiseData;
      @Nullable
      private final NormalNoise offsetNoise;
      static final Codec<DensityFunctions.ShiftA> CODEC;

      protected ShiftA(Holder<NormalNoise.NoiseParameters> var1, @Nullable NormalNoise var2) {
         this.noiseData = var1;
         this.offsetNoise = var2;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return this.compute((double)var1.blockX(), 0.0D, (double)var1.blockZ());
      }

      public DensityFunctions.ShiftNoise withNewNoise(NormalNoise var1) {
         return new DensityFunctions.ShiftA(this.noiseData, var1);
      }

      public Codec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public Holder<NormalNoise.NoiseParameters> noiseData() {
         return this.noiseData;
      }

      @Nullable
      public NormalNoise offsetNoise() {
         return this.offsetNoise;
      }

      static {
         CODEC = DensityFunctions.singleArgumentCodec(NormalNoise.NoiseParameters.CODEC, (var0) -> {
            return new DensityFunctions.ShiftA(var0, (NormalNoise)null);
         }, DensityFunctions.ShiftA::noiseData);
      }
   }

   protected static record ShiftB(Holder<NormalNoise.NoiseParameters> a, @Nullable NormalNoise e) implements DensityFunctions.ShiftNoise {
      private final Holder<NormalNoise.NoiseParameters> noiseData;
      @Nullable
      private final NormalNoise offsetNoise;
      static final Codec<DensityFunctions.ShiftB> CODEC;

      protected ShiftB(Holder<NormalNoise.NoiseParameters> var1, @Nullable NormalNoise var2) {
         this.noiseData = var1;
         this.offsetNoise = var2;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return this.compute((double)var1.blockZ(), (double)var1.blockX(), 0.0D);
      }

      public DensityFunctions.ShiftNoise withNewNoise(NormalNoise var1) {
         return new DensityFunctions.ShiftB(this.noiseData, var1);
      }

      public Codec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public Holder<NormalNoise.NoiseParameters> noiseData() {
         return this.noiseData;
      }

      @Nullable
      public NormalNoise offsetNoise() {
         return this.offsetNoise;
      }

      static {
         CODEC = DensityFunctions.singleArgumentCodec(NormalNoise.NoiseParameters.CODEC, (var0) -> {
            return new DensityFunctions.ShiftB(var0, (NormalNoise)null);
         }, DensityFunctions.ShiftB::noiseData);
      }
   }

   static record Shift(Holder<NormalNoise.NoiseParameters> a, @Nullable NormalNoise e) implements DensityFunctions.ShiftNoise {
      private final Holder<NormalNoise.NoiseParameters> noiseData;
      @Nullable
      private final NormalNoise offsetNoise;
      static final Codec<DensityFunctions.Shift> CODEC;

      Shift(Holder<NormalNoise.NoiseParameters> var1, @Nullable NormalNoise var2) {
         this.noiseData = var1;
         this.offsetNoise = var2;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return this.compute((double)var1.blockX(), (double)var1.blockY(), (double)var1.blockZ());
      }

      public DensityFunctions.ShiftNoise withNewNoise(NormalNoise var1) {
         return new DensityFunctions.Shift(this.noiseData, var1);
      }

      public Codec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public Holder<NormalNoise.NoiseParameters> noiseData() {
         return this.noiseData;
      }

      @Nullable
      public NormalNoise offsetNoise() {
         return this.offsetNoise;
      }

      static {
         CODEC = DensityFunctions.singleArgumentCodec(NormalNoise.NoiseParameters.CODEC, (var0) -> {
            return new DensityFunctions.Shift(var0, (NormalNoise)null);
         }, DensityFunctions.Shift::noiseData);
      }
   }

   static record BlendDensity(DensityFunction a) implements DensityFunctions.TransformerWithContext {
      private final DensityFunction input;
      static final Codec<DensityFunctions.BlendDensity> CODEC = DensityFunctions.singleFunctionArgumentCodec(DensityFunctions.BlendDensity::new, DensityFunctions.BlendDensity::input);

      BlendDensity(DensityFunction var1) {
         this.input = var1;
      }

      public double transform(DensityFunction.FunctionContext var1, double var2) {
         return var1.getBlender().blendDensity(var1, var2);
      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return (DensityFunction)var1.apply(new DensityFunctions.BlendDensity(this.input.mapAll(var1)));
      }

      public double minValue() {
         return Double.NEGATIVE_INFINITY;
      }

      public double maxValue() {
         return Double.POSITIVE_INFINITY;
      }

      public Codec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public DensityFunction input() {
         return this.input;
      }
   }

   protected static record Clamp(DensityFunction e, double f, double g) implements DensityFunctions.PureTransformer {
      private final DensityFunction input;
      private final double minValue;
      private final double maxValue;
      private static final MapCodec<DensityFunctions.Clamp> DATA_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(DensityFunction.DIRECT_CODEC.fieldOf("input").forGetter(DensityFunctions.Clamp::input), DensityFunctions.NOISE_VALUE_CODEC.fieldOf("min").forGetter(DensityFunctions.Clamp::minValue), DensityFunctions.NOISE_VALUE_CODEC.fieldOf("max").forGetter(DensityFunctions.Clamp::maxValue)).apply(var0, DensityFunctions.Clamp::new);
      });
      public static final Codec<DensityFunctions.Clamp> CODEC;

      protected Clamp(DensityFunction var1, double var2, double var4) {
         this.input = var1;
         this.minValue = var2;
         this.maxValue = var4;
      }

      public double transform(double var1) {
         return Mth.clamp(var1, this.minValue, this.maxValue);
      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return new DensityFunctions.Clamp(this.input.mapAll(var1), this.minValue, this.maxValue);
      }

      public Codec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public DensityFunction input() {
         return this.input;
      }

      public double minValue() {
         return this.minValue;
      }

      public double maxValue() {
         return this.maxValue;
      }

      static {
         CODEC = DensityFunctions.makeCodec(DATA_CODEC);
      }
   }

   protected static record Mapped(DensityFunctions.Mapped.Type a, DensityFunction e, double f, double g) implements DensityFunctions.PureTransformer {
      private final DensityFunctions.Mapped.Type type;
      private final DensityFunction input;
      private final double minValue;
      private final double maxValue;

      protected Mapped(DensityFunctions.Mapped.Type var1, DensityFunction var2, double var3, double var5) {
         this.type = var1;
         this.input = var2;
         this.minValue = var3;
         this.maxValue = var5;
      }

      public static DensityFunctions.Mapped create(DensityFunctions.Mapped.Type var0, DensityFunction var1) {
         double var2 = var1.minValue();
         double var4 = transform(var0, var2);
         double var6 = transform(var0, var1.maxValue());
         return var0 != DensityFunctions.Mapped.Type.ABS && var0 != DensityFunctions.Mapped.Type.SQUARE ? new DensityFunctions.Mapped(var0, var1, var4, var6) : new DensityFunctions.Mapped(var0, var1, Math.max(0.0D, var2), Math.max(var4, var6));
      }

      private static double transform(DensityFunctions.Mapped.Type var0, double var1) {
         double var10000;
         switch(var0) {
         case ABS:
            var10000 = Math.abs(var1);
            break;
         case SQUARE:
            var10000 = var1 * var1;
            break;
         case CUBE:
            var10000 = var1 * var1 * var1;
            break;
         case HALF_NEGATIVE:
            var10000 = var1 > 0.0D ? var1 : var1 * 0.5D;
            break;
         case QUARTER_NEGATIVE:
            var10000 = var1 > 0.0D ? var1 : var1 * 0.25D;
            break;
         case SQUEEZE:
            double var3 = Mth.clamp(var1, -1.0D, 1.0D);
            var10000 = var3 / 2.0D - var3 * var3 * var3 / 24.0D;
            break;
         default:
            throw new IncompatibleClassChangeError();
         }

         return var10000;
      }

      public double transform(double var1) {
         return transform(this.type, var1);
      }

      public DensityFunctions.Mapped mapAll(DensityFunction.Visitor var1) {
         return create(this.type, this.input.mapAll(var1));
      }

      public Codec<? extends DensityFunction> codec() {
         return this.type.codec;
      }

      public DensityFunctions.Mapped.Type type() {
         return this.type;
      }

      public DensityFunction input() {
         return this.input;
      }

      public double minValue() {
         return this.minValue;
      }

      public double maxValue() {
         return this.maxValue;
      }

      // $FF: synthetic method
      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return this.mapAll(var1);
      }

      static enum Type implements StringRepresentable {
         ABS("abs"),
         SQUARE("square"),
         CUBE("cube"),
         HALF_NEGATIVE("half_negative"),
         QUARTER_NEGATIVE("quarter_negative"),
         SQUEEZE("squeeze");

         private final String name;
         final Codec<DensityFunctions.Mapped> codec = DensityFunctions.singleFunctionArgumentCodec((var1x) -> {
            return DensityFunctions.Mapped.create(this, var1x);
         }, DensityFunctions.Mapped::input);

         private Type(String var3) {
            this.name = var3;
         }

         public String getSerializedName() {
            return this.name;
         }

         // $FF: synthetic method
         private static DensityFunctions.Mapped.Type[] $values() {
            return new DensityFunctions.Mapped.Type[]{ABS, SQUARE, CUBE, HALF_NEGATIVE, QUARTER_NEGATIVE, SQUEEZE};
         }
      }
   }

   protected static record Slide(@Nullable NoiseSettings e, DensityFunction f) implements DensityFunctions.TransformerWithContext {
      @Nullable
      private final NoiseSettings settings;
      private final DensityFunction input;
      public static final Codec<DensityFunctions.Slide> CODEC = DensityFunctions.singleFunctionArgumentCodec((var0) -> {
         return new DensityFunctions.Slide((NoiseSettings)null, var0);
      }, DensityFunctions.Slide::input);

      protected Slide(@Nullable NoiseSettings var1, DensityFunction var2) {
         this.settings = var1;
         this.input = var2;
      }

      public double transform(DensityFunction.FunctionContext var1, double var2) {
         return this.settings == null ? var2 : NoiseRouterData.applySlide(this.settings, var2, (double)var1.blockY());
      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return (DensityFunction)var1.apply(new DensityFunctions.Slide(this.settings, this.input.mapAll(var1)));
      }

      public double minValue() {
         return this.settings == null ? this.input.minValue() : Math.min(this.input.minValue(), Math.min(this.settings.bottomSlideSettings().target(), this.settings.topSlideSettings().target()));
      }

      public double maxValue() {
         return this.settings == null ? this.input.maxValue() : Math.max(this.input.maxValue(), Math.max(this.settings.bottomSlideSettings().target(), this.settings.topSlideSettings().target()));
      }

      public Codec<? extends DensityFunction> codec() {
         return CODEC;
      }

      @Nullable
      public NoiseSettings settings() {
         return this.settings;
      }

      public DensityFunction input() {
         return this.input;
      }
   }

   interface TwoArgumentSimpleFunction extends DensityFunction {
      Logger LOGGER = LogUtils.getLogger();

      static DensityFunctions.TwoArgumentSimpleFunction create(DensityFunctions.TwoArgumentSimpleFunction.Type var0, DensityFunction var1, DensityFunction var2) {
         double var3 = var1.minValue();
         double var5 = var2.minValue();
         double var7 = var1.maxValue();
         double var9 = var2.maxValue();
         if (var0 == DensityFunctions.TwoArgumentSimpleFunction.Type.MIN || var0 == DensityFunctions.TwoArgumentSimpleFunction.Type.MAX) {
            boolean var11 = var3 >= var9;
            boolean var12 = var5 >= var7;
            if (var11 || var12) {
               LOGGER.warn("Creating a " + var0 + " function between two non-overlapping inputs: " + var1 + " and " + var2);
            }
         }

         double var10000;
         switch(var0) {
         case ADD:
            var10000 = var3 + var5;
            break;
         case MAX:
            var10000 = Math.max(var3, var5);
            break;
         case MIN:
            var10000 = Math.min(var3, var5);
            break;
         case MUL:
            var10000 = var3 > 0.0D && var5 > 0.0D ? var3 * var5 : (var7 < 0.0D && var9 < 0.0D ? var7 * var9 : Math.min(var3 * var9, var7 * var5));
            break;
         default:
            throw new IncompatibleClassChangeError();
         }

         double var16 = var10000;
         switch(var0) {
         case ADD:
            var10000 = var7 + var9;
            break;
         case MAX:
            var10000 = Math.max(var7, var9);
            break;
         case MIN:
            var10000 = Math.min(var7, var9);
            break;
         case MUL:
            var10000 = var3 > 0.0D && var5 > 0.0D ? var7 * var9 : (var7 < 0.0D && var9 < 0.0D ? var3 * var5 : Math.max(var3 * var5, var7 * var9));
            break;
         default:
            throw new IncompatibleClassChangeError();
         }

         double var13 = var10000;
         if (var0 == DensityFunctions.TwoArgumentSimpleFunction.Type.MUL || var0 == DensityFunctions.TwoArgumentSimpleFunction.Type.ADD) {
            DensityFunctions.Constant var15;
            if (var1 instanceof DensityFunctions.Constant) {
               var15 = (DensityFunctions.Constant)var1;
               return new DensityFunctions.MulOrAdd(var0 == DensityFunctions.TwoArgumentSimpleFunction.Type.ADD ? DensityFunctions.MulOrAdd.Type.ADD : DensityFunctions.MulOrAdd.Type.MUL, var2, var16, var13, var15.value);
            }

            if (var2 instanceof DensityFunctions.Constant) {
               var15 = (DensityFunctions.Constant)var2;
               return new DensityFunctions.MulOrAdd(var0 == DensityFunctions.TwoArgumentSimpleFunction.Type.ADD ? DensityFunctions.MulOrAdd.Type.ADD : DensityFunctions.MulOrAdd.Type.MUL, var1, var16, var13, var15.value);
            }
         }

         return new DensityFunctions.Ap2(var0, var1, var2, var16, var13);
      }

      DensityFunctions.TwoArgumentSimpleFunction.Type type();

      DensityFunction argument1();

      DensityFunction argument2();

      default Codec<? extends DensityFunction> codec() {
         return this.type().codec;
      }

      public static enum Type implements StringRepresentable {
         ADD("add"),
         MUL("mul"),
         MIN("min"),
         MAX("max");

         final Codec<DensityFunctions.TwoArgumentSimpleFunction> codec = DensityFunctions.doubleFunctionArgumentCodec((var1x, var2x) -> {
            return DensityFunctions.TwoArgumentSimpleFunction.create(this, var1x, var2x);
         }, DensityFunctions.TwoArgumentSimpleFunction::argument1, DensityFunctions.TwoArgumentSimpleFunction::argument2);
         private final String name;

         private Type(String var3) {
            this.name = var3;
         }

         public String getSerializedName() {
            return this.name;
         }

         // $FF: synthetic method
         private static DensityFunctions.TwoArgumentSimpleFunction.Type[] $values() {
            return new DensityFunctions.TwoArgumentSimpleFunction.Type[]{ADD, MUL, MIN, MAX};
         }
      }
   }

   public static record Spline(CubicSpline<TerrainShaper.PointCustom> e, double f, double g) implements DensityFunction {
      private final CubicSpline<TerrainShaper.PointCustom> spline;
      private final double minValue;
      private final double maxValue;
      private static final MapCodec<DensityFunctions.Spline> DATA_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(TerrainShaper.SPLINE_CUSTOM_CODEC.fieldOf("spline").forGetter(DensityFunctions.Spline::spline), DensityFunctions.NOISE_VALUE_CODEC.fieldOf("min_value").forGetter(DensityFunctions.Spline::minValue), DensityFunctions.NOISE_VALUE_CODEC.fieldOf("max_value").forGetter(DensityFunctions.Spline::maxValue)).apply(var0, DensityFunctions.Spline::new);
      });
      public static final Codec<DensityFunctions.Spline> CODEC;

      public Spline(CubicSpline<TerrainShaper.PointCustom> var1, double var2, double var4) {
         this.spline = var1;
         this.minValue = var2;
         this.maxValue = var4;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return Mth.clamp((double)this.spline.apply(TerrainShaper.makePoint(var1)), this.minValue, this.maxValue);
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         var2.fillAllDirectly(var1, this);
      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return (DensityFunction)var1.apply(new DensityFunctions.Spline(this.spline.mapAll((var1x) -> {
            Object var10000;
            if (var1x instanceof TerrainShaper.CoordinateCustom) {
               TerrainShaper.CoordinateCustom var2 = (TerrainShaper.CoordinateCustom)var1x;
               var10000 = var2.mapAll(var1);
            } else {
               var10000 = var1x;
            }

            return (ToFloatFunction)var10000;
         }), this.minValue, this.maxValue));
      }

      public Codec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public CubicSpline<TerrainShaper.PointCustom> spline() {
         return this.spline;
      }

      public double minValue() {
         return this.minValue;
      }

      public double maxValue() {
         return this.maxValue;
      }

      static {
         CODEC = DensityFunctions.makeCodec(DATA_CODEC);
      }
   }

   /** @deprecated */
   @Deprecated
   public static record TerrainShaperSpline(DensityFunction e, DensityFunction f, DensityFunction g, @Nullable TerrainShaper h, DensityFunctions.TerrainShaperSpline.SplineType i, double j, double k) implements DensityFunction {
      private final DensityFunction continentalness;
      private final DensityFunction erosion;
      private final DensityFunction weirdness;
      @Nullable
      private final TerrainShaper shaper;
      private final DensityFunctions.TerrainShaperSpline.SplineType spline;
      private final double minValue;
      private final double maxValue;
      private static final MapCodec<DensityFunctions.TerrainShaperSpline> DATA_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(DensityFunction.HOLDER_HELPER_CODEC.fieldOf("continentalness").forGetter(DensityFunctions.TerrainShaperSpline::continentalness), DensityFunction.HOLDER_HELPER_CODEC.fieldOf("erosion").forGetter(DensityFunctions.TerrainShaperSpline::erosion), DensityFunction.HOLDER_HELPER_CODEC.fieldOf("weirdness").forGetter(DensityFunctions.TerrainShaperSpline::weirdness), DensityFunctions.TerrainShaperSpline.SplineType.CODEC.fieldOf("spline").forGetter(DensityFunctions.TerrainShaperSpline::spline), DensityFunctions.NOISE_VALUE_CODEC.fieldOf("min_value").forGetter(DensityFunctions.TerrainShaperSpline::minValue), DensityFunctions.NOISE_VALUE_CODEC.fieldOf("max_value").forGetter(DensityFunctions.TerrainShaperSpline::maxValue)).apply(var0, DensityFunctions.TerrainShaperSpline::createUnseeded);
      });
      public static final Codec<DensityFunctions.TerrainShaperSpline> CODEC;

      public TerrainShaperSpline(DensityFunction var1, DensityFunction var2, DensityFunction var3, @Nullable TerrainShaper var4, DensityFunctions.TerrainShaperSpline.SplineType var5, double var6, double var8) {
         this.continentalness = var1;
         this.erosion = var2;
         this.weirdness = var3;
         this.shaper = var4;
         this.spline = var5;
         this.minValue = var6;
         this.maxValue = var8;
      }

      public static DensityFunctions.TerrainShaperSpline createUnseeded(DensityFunction var0, DensityFunction var1, DensityFunction var2, DensityFunctions.TerrainShaperSpline.SplineType var3, double var4, double var6) {
         return new DensityFunctions.TerrainShaperSpline(var0, var1, var2, (TerrainShaper)null, var3, var4, var6);
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return this.shaper == null ? 0.0D : Mth.clamp((double)this.spline.spline.apply(this.shaper, TerrainShaper.makePoint((float)this.continentalness.compute(var1), (float)this.erosion.compute(var1), (float)this.weirdness.compute(var1))), this.minValue, this.maxValue);
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         for(int var3 = 0; var3 < var1.length; ++var3) {
            var1[var3] = this.compute(var2.forIndex(var3));
         }

      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return (DensityFunction)var1.apply(new DensityFunctions.TerrainShaperSpline(this.continentalness.mapAll(var1), this.erosion.mapAll(var1), this.weirdness.mapAll(var1), this.shaper, this.spline, this.minValue, this.maxValue));
      }

      public Codec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public DensityFunction continentalness() {
         return this.continentalness;
      }

      public DensityFunction erosion() {
         return this.erosion;
      }

      public DensityFunction weirdness() {
         return this.weirdness;
      }

      @Nullable
      public TerrainShaper shaper() {
         return this.shaper;
      }

      public DensityFunctions.TerrainShaperSpline.SplineType spline() {
         return this.spline;
      }

      public double minValue() {
         return this.minValue;
      }

      public double maxValue() {
         return this.maxValue;
      }

      static {
         CODEC = DensityFunctions.makeCodec(DATA_CODEC);
      }

      public static enum SplineType implements StringRepresentable {
         OFFSET("offset", TerrainShaper::offset),
         FACTOR("factor", TerrainShaper::factor),
         JAGGEDNESS("jaggedness", TerrainShaper::jaggedness);

         private static final Map<String, DensityFunctions.TerrainShaperSpline.SplineType> BY_NAME = (Map)Arrays.stream(values()).collect(Collectors.toMap(DensityFunctions.TerrainShaperSpline.SplineType::getSerializedName, (var0) -> {
            return var0;
         }));
         public static final Codec<DensityFunctions.TerrainShaperSpline.SplineType> CODEC;
         private final String name;
         final DensityFunctions.TerrainShaperSpline.Spline spline;

         private SplineType(String var3, DensityFunctions.TerrainShaperSpline.Spline var4) {
            this.name = var3;
            this.spline = var4;
         }

         public String getSerializedName() {
            return this.name;
         }

         // $FF: synthetic method
         private static DensityFunctions.TerrainShaperSpline.SplineType[] $values() {
            return new DensityFunctions.TerrainShaperSpline.SplineType[]{OFFSET, FACTOR, JAGGEDNESS};
         }

         static {
            Supplier var10000 = DensityFunctions.TerrainShaperSpline.SplineType::values;
            Map var10001 = BY_NAME;
            Objects.requireNonNull(var10001);
            CODEC = StringRepresentable.fromEnum(var10000, var10001::get);
         }
      }

      interface Spline {
         float apply(TerrainShaper var1, TerrainShaper.Point var2);
      }
   }

   static record Constant(double a) implements DensityFunction.SimpleFunction {
      final double value;
      static final Codec<DensityFunctions.Constant> CODEC;
      static final DensityFunctions.Constant ZERO;

      Constant(double var1) {
         this.value = var1;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return this.value;
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         Arrays.fill(var1, this.value);
      }

      public double minValue() {
         return this.value;
      }

      public double maxValue() {
         return this.value;
      }

      public Codec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public double value() {
         return this.value;
      }

      static {
         CODEC = DensityFunctions.singleArgumentCodec(DensityFunctions.NOISE_VALUE_CODEC, DensityFunctions.Constant::new, DensityFunctions.Constant::value);
         ZERO = new DensityFunctions.Constant(0.0D);
      }
   }

   static record YClampedGradient(int e, int f, double g, double h) implements DensityFunction.SimpleFunction {
      private final int fromY;
      private final int toY;
      private final double fromValue;
      private final double toValue;
      private static final MapCodec<DensityFunctions.YClampedGradient> DATA_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(Codec.intRange(DimensionType.MIN_Y * 2, DimensionType.MAX_Y * 2).fieldOf("from_y").forGetter(DensityFunctions.YClampedGradient::fromY), Codec.intRange(DimensionType.MIN_Y * 2, DimensionType.MAX_Y * 2).fieldOf("to_y").forGetter(DensityFunctions.YClampedGradient::toY), DensityFunctions.NOISE_VALUE_CODEC.fieldOf("from_value").forGetter(DensityFunctions.YClampedGradient::fromValue), DensityFunctions.NOISE_VALUE_CODEC.fieldOf("to_value").forGetter(DensityFunctions.YClampedGradient::toValue)).apply(var0, DensityFunctions.YClampedGradient::new);
      });
      public static final Codec<DensityFunctions.YClampedGradient> CODEC;

      YClampedGradient(int var1, int var2, double var3, double var5) {
         this.fromY = var1;
         this.toY = var2;
         this.fromValue = var3;
         this.toValue = var5;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return Mth.clampedMap((double)var1.blockY(), (double)this.fromY, (double)this.toY, this.fromValue, this.toValue);
      }

      public double minValue() {
         return Math.min(this.fromValue, this.toValue);
      }

      public double maxValue() {
         return Math.max(this.fromValue, this.toValue);
      }

      public Codec<? extends DensityFunction> codec() {
         return CODEC;
      }

      public int fromY() {
         return this.fromY;
      }

      public int toY() {
         return this.toY;
      }

      public double fromValue() {
         return this.fromValue;
      }

      public double toValue() {
         return this.toValue;
      }

      static {
         CODEC = DensityFunctions.makeCodec(DATA_CODEC);
      }
   }

   static record Ap2(DensityFunctions.TwoArgumentSimpleFunction.Type e, DensityFunction f, DensityFunction g, double h, double i) implements DensityFunctions.TwoArgumentSimpleFunction {
      private final DensityFunctions.TwoArgumentSimpleFunction.Type type;
      private final DensityFunction argument1;
      private final DensityFunction argument2;
      private final double minValue;
      private final double maxValue;

      Ap2(DensityFunctions.TwoArgumentSimpleFunction.Type var1, DensityFunction var2, DensityFunction var3, double var4, double var6) {
         this.type = var1;
         this.argument1 = var2;
         this.argument2 = var3;
         this.minValue = var4;
         this.maxValue = var6;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         double var2 = this.argument1.compute(var1);
         double var10000;
         switch(this.type) {
         case ADD:
            var10000 = var2 + this.argument2.compute(var1);
            break;
         case MAX:
            var10000 = var2 > this.argument2.maxValue() ? var2 : Math.max(var2, this.argument2.compute(var1));
            break;
         case MIN:
            var10000 = var2 < this.argument2.minValue() ? var2 : Math.min(var2, this.argument2.compute(var1));
            break;
         case MUL:
            var10000 = var2 == 0.0D ? 0.0D : var2 * this.argument2.compute(var1);
            break;
         default:
            throw new IncompatibleClassChangeError();
         }

         return var10000;
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         this.argument1.fillArray(var1, var2);
         int var5;
         double var6;
         double var8;
         switch(this.type) {
         case ADD:
            double[] var9 = new double[var1.length];
            this.argument2.fillArray(var9, var2);

            for(int var10 = 0; var10 < var1.length; ++var10) {
               var1[var10] += var9[var10];
            }

            return;
         case MAX:
            var8 = this.argument2.maxValue();

            for(var5 = 0; var5 < var1.length; ++var5) {
               var6 = var1[var5];
               var1[var5] = var6 > var8 ? var6 : Math.max(var6, this.argument2.compute(var2.forIndex(var5)));
            }

            return;
         case MIN:
            var8 = this.argument2.minValue();

            for(var5 = 0; var5 < var1.length; ++var5) {
               var6 = var1[var5];
               var1[var5] = var6 < var8 ? var6 : Math.min(var6, this.argument2.compute(var2.forIndex(var5)));
            }

            return;
         case MUL:
            for(int var3 = 0; var3 < var1.length; ++var3) {
               double var4 = var1[var3];
               var1[var3] = var4 == 0.0D ? 0.0D : var4 * this.argument2.compute(var2.forIndex(var3));
            }
         }

      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return (DensityFunction)var1.apply(DensityFunctions.TwoArgumentSimpleFunction.create(this.type, this.argument1.mapAll(var1), this.argument2.mapAll(var1)));
      }

      public double minValue() {
         return this.minValue;
      }

      public double maxValue() {
         return this.maxValue;
      }

      public DensityFunctions.TwoArgumentSimpleFunction.Type type() {
         return this.type;
      }

      public DensityFunction argument1() {
         return this.argument1;
      }

      public DensityFunction argument2() {
         return this.argument2;
      }
   }

   private static record MulOrAdd(DensityFunctions.MulOrAdd.Type e, DensityFunction f, double g, double h, double i) implements DensityFunctions.TwoArgumentSimpleFunction, DensityFunctions.PureTransformer {
      private final DensityFunctions.MulOrAdd.Type specificType;
      private final DensityFunction input;
      private final double minValue;
      private final double maxValue;
      private final double argument;

      MulOrAdd(DensityFunctions.MulOrAdd.Type var1, DensityFunction var2, double var3, double var5, double var7) {
         this.specificType = var1;
         this.input = var2;
         this.minValue = var3;
         this.maxValue = var5;
         this.argument = var7;
      }

      public DensityFunctions.TwoArgumentSimpleFunction.Type type() {
         return this.specificType == DensityFunctions.MulOrAdd.Type.MUL ? DensityFunctions.TwoArgumentSimpleFunction.Type.MUL : DensityFunctions.TwoArgumentSimpleFunction.Type.ADD;
      }

      public DensityFunction argument1() {
         return DensityFunctions.constant(this.argument);
      }

      public DensityFunction argument2() {
         return this.input;
      }

      public double transform(double var1) {
         double var10000;
         switch(this.specificType) {
         case MUL:
            var10000 = var1 * this.argument;
            break;
         case ADD:
            var10000 = var1 + this.argument;
            break;
         default:
            throw new IncompatibleClassChangeError();
         }

         return var10000;
      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         DensityFunction var2 = this.input.mapAll(var1);
         double var3 = var2.minValue();
         double var5 = var2.maxValue();
         double var7;
         double var9;
         if (this.specificType == DensityFunctions.MulOrAdd.Type.ADD) {
            var7 = var3 + this.argument;
            var9 = var5 + this.argument;
         } else if (this.argument >= 0.0D) {
            var7 = var3 * this.argument;
            var9 = var5 * this.argument;
         } else {
            var7 = var5 * this.argument;
            var9 = var3 * this.argument;
         }

         return new DensityFunctions.MulOrAdd(this.specificType, var2, var7, var9, this.argument);
      }

      public DensityFunctions.MulOrAdd.Type specificType() {
         return this.specificType;
      }

      public DensityFunction input() {
         return this.input;
      }

      public double minValue() {
         return this.minValue;
      }

      public double maxValue() {
         return this.maxValue;
      }

      public double argument() {
         return this.argument;
      }

      static enum Type {
         MUL,
         ADD;

         private Type() {
         }

         // $FF: synthetic method
         private static DensityFunctions.MulOrAdd.Type[] $values() {
            return new DensityFunctions.MulOrAdd.Type[]{MUL, ADD};
         }
      }
   }

   interface ShiftNoise extends DensityFunction.SimpleFunction {
      Holder<NormalNoise.NoiseParameters> noiseData();

      @Nullable
      NormalNoise offsetNoise();

      default double minValue() {
         return -this.maxValue();
      }

      default double maxValue() {
         NormalNoise var1 = this.offsetNoise();
         return (var1 == null ? 2.0D : var1.maxValue()) * 4.0D;
      }

      default double compute(double var1, double var3, double var5) {
         NormalNoise var7 = this.offsetNoise();
         return var7 == null ? 0.0D : var7.getValue(var1 * 0.25D, var3 * 0.25D, var5 * 0.25D) * 4.0D;
      }

      DensityFunctions.ShiftNoise withNewNoise(NormalNoise var1);
   }

   public interface MarkerOrMarked extends DensityFunction {
      DensityFunctions.Marker.Type type();

      DensityFunction wrapped();

      default Codec<? extends DensityFunction> codec() {
         return this.type().codec;
      }
   }

   protected static record HolderHolder(Holder<DensityFunction> a) implements DensityFunction {
      private final Holder<DensityFunction> function;

      protected HolderHolder(Holder<DensityFunction> var1) {
         this.function = var1;
      }

      public double compute(DensityFunction.FunctionContext var1) {
         return ((DensityFunction)this.function.value()).compute(var1);
      }

      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         ((DensityFunction)this.function.value()).fillArray(var1, var2);
      }

      public DensityFunction mapAll(DensityFunction.Visitor var1) {
         return (DensityFunction)var1.apply(new DensityFunctions.HolderHolder(new Holder.Direct(((DensityFunction)this.function.value()).mapAll(var1))));
      }

      public double minValue() {
         return ((DensityFunction)this.function.value()).minValue();
      }

      public double maxValue() {
         return ((DensityFunction)this.function.value()).maxValue();
      }

      public Codec<? extends DensityFunction> codec() {
         throw new UnsupportedOperationException("Calling .codec() on HolderHolder");
      }

      public Holder<DensityFunction> function() {
         return this.function;
      }
   }

   public interface BeardifierOrMarker extends DensityFunction.SimpleFunction {
      Codec<DensityFunction> CODEC = Codec.unit(DensityFunctions.BeardifierMarker.INSTANCE);

      default Codec<? extends DensityFunction> codec() {
         return CODEC;
      }
   }

   private interface PureTransformer extends DensityFunction {
      DensityFunction input();

      default double compute(DensityFunction.FunctionContext var1) {
         return this.transform(this.input().compute(var1));
      }

      default void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         this.input().fillArray(var1, var2);

         for(int var3 = 0; var3 < var1.length; ++var3) {
            var1[var3] = this.transform(var1[var3]);
         }

      }

      double transform(double var1);
   }

   private interface TransformerWithContext extends DensityFunction {
      DensityFunction input();

      default double compute(DensityFunction.FunctionContext var1) {
         return this.transform(var1, this.input().compute(var1));
      }

      default void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         this.input().fillArray(var1, var2);

         for(int var3 = 0; var3 < var1.length; ++var3) {
            var1[var3] = this.transform(var2.forIndex(var3), var1[var3]);
         }

      }

      double transform(DensityFunction.FunctionContext var1, double var2);
   }
}
