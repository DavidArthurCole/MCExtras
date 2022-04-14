package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.core.QuartPos;
import net.minecraft.data.worldgen.TerrainProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.TerrainShaper;
import net.minecraft.world.level.dimension.DimensionType;

public record NoiseSettings(int f, int g, NoiseSamplingSettings h, NoiseSlider i, NoiseSlider j, int k, int l, TerrainShaper m) {
   private final int minY;
   private final int height;
   private final NoiseSamplingSettings noiseSamplingSettings;
   private final NoiseSlider topSlideSettings;
   private final NoiseSlider bottomSlideSettings;
   private final int noiseSizeHorizontal;
   private final int noiseSizeVertical;
   private final TerrainShaper terrainShaper;
   public static final Codec<NoiseSettings> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y).fieldOf("min_y").forGetter(NoiseSettings::minY), Codec.intRange(0, DimensionType.Y_SIZE).fieldOf("height").forGetter(NoiseSettings::height), NoiseSamplingSettings.CODEC.fieldOf("sampling").forGetter(NoiseSettings::noiseSamplingSettings), NoiseSlider.CODEC.fieldOf("top_slide").forGetter(NoiseSettings::topSlideSettings), NoiseSlider.CODEC.fieldOf("bottom_slide").forGetter(NoiseSettings::bottomSlideSettings), Codec.intRange(1, 4).fieldOf("size_horizontal").forGetter(NoiseSettings::noiseSizeHorizontal), Codec.intRange(1, 4).fieldOf("size_vertical").forGetter(NoiseSettings::noiseSizeVertical), TerrainShaper.CODEC.fieldOf("terrain_shaper").forGetter(NoiseSettings::terrainShaper)).apply(var0, NoiseSettings::new);
   }).comapFlatMap(NoiseSettings::guardY, Function.identity());
   static final NoiseSettings NETHER_NOISE_SETTINGS = create(0, 128, new NoiseSamplingSettings(1.0D, 3.0D, 80.0D, 60.0D), new NoiseSlider(0.9375D, 3, 0), new NoiseSlider(2.5D, 4, -1), 1, 2, TerrainProvider.nether());
   static final NoiseSettings END_NOISE_SETTINGS = create(0, 128, new NoiseSamplingSettings(2.0D, 1.0D, 80.0D, 160.0D), new NoiseSlider(-23.4375D, 64, -46), new NoiseSlider(-0.234375D, 7, 1), 2, 1, TerrainProvider.end());
   static final NoiseSettings CAVES_NOISE_SETTINGS = create(-64, 192, new NoiseSamplingSettings(1.0D, 3.0D, 80.0D, 60.0D), new NoiseSlider(0.9375D, 3, 0), new NoiseSlider(2.5D, 4, -1), 1, 2, TerrainProvider.caves());
   static final NoiseSettings FLOATING_ISLANDS_NOISE_SETTINGS = create(0, 256, new NoiseSamplingSettings(2.0D, 1.0D, 80.0D, 160.0D), new NoiseSlider(-23.4375D, 64, -46), new NoiseSlider(-0.234375D, 7, 1), 2, 1, TerrainProvider.floatingIslands());

   public NoiseSettings(int var1, int var2, NoiseSamplingSettings var3, NoiseSlider var4, NoiseSlider var5, int var6, int var7, TerrainShaper var8) {
      this.minY = var1;
      this.height = var2;
      this.noiseSamplingSettings = var3;
      this.topSlideSettings = var4;
      this.bottomSlideSettings = var5;
      this.noiseSizeHorizontal = var6;
      this.noiseSizeVertical = var7;
      this.terrainShaper = var8;
   }

   private static DataResult<NoiseSettings> guardY(NoiseSettings var0) {
      if (var0.minY() + var0.height() > DimensionType.MAX_Y + 1) {
         return DataResult.error("min_y + height cannot be higher than: " + (DimensionType.MAX_Y + 1));
      } else if (var0.height() % 16 != 0) {
         return DataResult.error("height has to be a multiple of 16");
      } else {
         return var0.minY() % 16 != 0 ? DataResult.error("min_y has to be a multiple of 16") : DataResult.success(var0);
      }
   }

   public static NoiseSettings create(int var0, int var1, NoiseSamplingSettings var2, NoiseSlider var3, NoiseSlider var4, int var5, int var6, TerrainShaper var7) {
      NoiseSettings var8 = new NoiseSettings(var0, var1, var2, var3, var4, var5, var6, var7);
      guardY(var8).error().ifPresent((var0x) -> {
         throw new IllegalStateException(var0x.message());
      });
      return var8;
   }

   static NoiseSettings overworldNoiseSettings(boolean var0) {
      return create(-64, 384, new NoiseSamplingSettings(1.0D, 1.0D, 80.0D, 160.0D), new NoiseSlider(-0.078125D, 2, var0 ? 0 : 8), new NoiseSlider(var0 ? 0.4D : 0.1171875D, 3, 0), 1, 2, TerrainProvider.overworld(var0));
   }

   public int getCellHeight() {
      return QuartPos.toBlock(this.noiseSizeVertical());
   }

   public int getCellWidth() {
      return QuartPos.toBlock(this.noiseSizeHorizontal());
   }

   public int getCellCountY() {
      return this.height() / this.getCellHeight();
   }

   public int getMinCellY() {
      return Mth.intFloorDiv(this.minY(), this.getCellHeight());
   }

   public int minY() {
      return this.minY;
   }

   public int height() {
      return this.height;
   }

   public NoiseSamplingSettings noiseSamplingSettings() {
      return this.noiseSamplingSettings;
   }

   public NoiseSlider topSlideSettings() {
      return this.topSlideSettings;
   }

   public NoiseSlider bottomSlideSettings() {
      return this.bottomSlideSettings;
   }

   public int noiseSizeHorizontal() {
      return this.noiseSizeHorizontal;
   }

   public int noiseSizeVertical() {
      return this.noiseSizeVertical;
   }

   public TerrainShaper terrainShaper() {
      return this.terrainShaper;
   }
}
