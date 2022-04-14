package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public record NoiseGeneratorSettings(NoiseSettings j, BlockState k, BlockState l, NoiseRouterWithOnlyNoises m, SurfaceRules.RuleSource n, int o, boolean p, boolean q, boolean r, boolean s) {
   private final NoiseSettings noiseSettings;
   private final BlockState defaultBlock;
   private final BlockState defaultFluid;
   private final NoiseRouterWithOnlyNoises noiseRouter;
   private final SurfaceRules.RuleSource surfaceRule;
   private final int seaLevel;
   private final boolean disableMobGeneration;
   private final boolean aquifersEnabled;
   private final boolean oreVeinsEnabled;
   private final boolean useLegacyRandomSource;
   public static final Codec<NoiseGeneratorSettings> DIRECT_CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(NoiseSettings.CODEC.fieldOf("noise").forGetter(NoiseGeneratorSettings::noiseSettings), BlockState.CODEC.fieldOf("default_block").forGetter(NoiseGeneratorSettings::defaultBlock), BlockState.CODEC.fieldOf("default_fluid").forGetter(NoiseGeneratorSettings::defaultFluid), NoiseRouterWithOnlyNoises.CODEC.fieldOf("noise_router").forGetter(NoiseGeneratorSettings::noiseRouter), SurfaceRules.RuleSource.CODEC.fieldOf("surface_rule").forGetter(NoiseGeneratorSettings::surfaceRule), Codec.INT.fieldOf("sea_level").forGetter(NoiseGeneratorSettings::seaLevel), Codec.BOOL.fieldOf("disable_mob_generation").forGetter(NoiseGeneratorSettings::disableMobGeneration), Codec.BOOL.fieldOf("aquifers_enabled").forGetter(NoiseGeneratorSettings::isAquifersEnabled), Codec.BOOL.fieldOf("ore_veins_enabled").forGetter(NoiseGeneratorSettings::oreVeinsEnabled), Codec.BOOL.fieldOf("legacy_random_source").forGetter(NoiseGeneratorSettings::useLegacyRandomSource)).apply(var0, NoiseGeneratorSettings::new);
   });
   public static final Codec<Holder<NoiseGeneratorSettings>> CODEC;
   public static final ResourceKey<NoiseGeneratorSettings> OVERWORLD;
   public static final ResourceKey<NoiseGeneratorSettings> LARGE_BIOMES;
   public static final ResourceKey<NoiseGeneratorSettings> AMPLIFIED;
   public static final ResourceKey<NoiseGeneratorSettings> NETHER;
   public static final ResourceKey<NoiseGeneratorSettings> END;
   public static final ResourceKey<NoiseGeneratorSettings> CAVES;
   public static final ResourceKey<NoiseGeneratorSettings> FLOATING_ISLANDS;

   public NoiseGeneratorSettings(NoiseSettings var1, BlockState var2, BlockState var3, NoiseRouterWithOnlyNoises var4, SurfaceRules.RuleSource var5, int var6, boolean var7, boolean var8, boolean var9, boolean var10) {
      this.noiseSettings = var1;
      this.defaultBlock = var2;
      this.defaultFluid = var3;
      this.noiseRouter = var4;
      this.surfaceRule = var5;
      this.seaLevel = var6;
      this.disableMobGeneration = var7;
      this.aquifersEnabled = var8;
      this.oreVeinsEnabled = var9;
      this.useLegacyRandomSource = var10;
   }

   /** @deprecated */
   @Deprecated
   public boolean disableMobGeneration() {
      return this.disableMobGeneration;
   }

   public boolean isAquifersEnabled() {
      return this.aquifersEnabled;
   }

   public boolean oreVeinsEnabled() {
      return this.oreVeinsEnabled;
   }

   public WorldgenRandom.Algorithm getRandomSource() {
      return this.useLegacyRandomSource ? WorldgenRandom.Algorithm.LEGACY : WorldgenRandom.Algorithm.XOROSHIRO;
   }

   public NoiseRouter createNoiseRouter(Registry<NormalNoise.NoiseParameters> var1, long var2) {
      return NoiseRouterData.createNoiseRouter(this.noiseSettings, var2, var1, this.getRandomSource(), this.noiseRouter);
   }

   private static void register(ResourceKey<NoiseGeneratorSettings> var0, NoiseGeneratorSettings var1) {
      BuiltinRegistries.register(BuiltinRegistries.NOISE_GENERATOR_SETTINGS, (ResourceLocation)var0.location(), var1);
   }

   public static Holder<NoiseGeneratorSettings> bootstrap() {
      return (Holder)BuiltinRegistries.NOISE_GENERATOR_SETTINGS.holders().iterator().next();
   }

   private static NoiseGeneratorSettings end() {
      return new NoiseGeneratorSettings(NoiseSettings.END_NOISE_SETTINGS, Blocks.END_STONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), NoiseRouterData.end(NoiseSettings.END_NOISE_SETTINGS), SurfaceRuleData.end(), 0, true, false, false, true);
   }

   private static NoiseGeneratorSettings nether() {
      return new NoiseGeneratorSettings(NoiseSettings.NETHER_NOISE_SETTINGS, Blocks.NETHERRACK.defaultBlockState(), Blocks.LAVA.defaultBlockState(), NoiseRouterData.nether(NoiseSettings.NETHER_NOISE_SETTINGS), SurfaceRuleData.nether(), 32, false, false, false, true);
   }

   private static NoiseGeneratorSettings overworld(boolean var0, boolean var1) {
      NoiseSettings var2 = NoiseSettings.overworldNoiseSettings(var0);
      return new NoiseGeneratorSettings(var2, Blocks.STONE.defaultBlockState(), Blocks.WATER.defaultBlockState(), NoiseRouterData.overworld(var2, var1), SurfaceRuleData.overworld(), 63, false, true, true, false);
   }

   private static NoiseGeneratorSettings caves() {
      return new NoiseGeneratorSettings(NoiseSettings.CAVES_NOISE_SETTINGS, Blocks.STONE.defaultBlockState(), Blocks.WATER.defaultBlockState(), NoiseRouterData.overworldWithoutCaves(NoiseSettings.CAVES_NOISE_SETTINGS), SurfaceRuleData.overworldLike(false, true, true), 32, false, false, false, true);
   }

   private static NoiseGeneratorSettings floatingIslands() {
      return new NoiseGeneratorSettings(NoiseSettings.FLOATING_ISLANDS_NOISE_SETTINGS, Blocks.STONE.defaultBlockState(), Blocks.WATER.defaultBlockState(), NoiseRouterData.overworldWithoutCaves(NoiseSettings.FLOATING_ISLANDS_NOISE_SETTINGS), SurfaceRuleData.overworldLike(false, false, false), -64, false, false, false, true);
   }

   public NoiseSettings noiseSettings() {
      return this.noiseSettings;
   }

   public BlockState defaultBlock() {
      return this.defaultBlock;
   }

   public BlockState defaultFluid() {
      return this.defaultFluid;
   }

   public NoiseRouterWithOnlyNoises noiseRouter() {
      return this.noiseRouter;
   }

   public SurfaceRules.RuleSource surfaceRule() {
      return this.surfaceRule;
   }

   public int seaLevel() {
      return this.seaLevel;
   }

   public boolean aquifersEnabled() {
      return this.aquifersEnabled;
   }

   public boolean useLegacyRandomSource() {
      return this.useLegacyRandomSource;
   }

   static {
      CODEC = RegistryFileCodec.create(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, DIRECT_CODEC);
      OVERWORLD = ResourceKey.create(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, new ResourceLocation("overworld"));
      LARGE_BIOMES = ResourceKey.create(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, new ResourceLocation("large_biomes"));
      AMPLIFIED = ResourceKey.create(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, new ResourceLocation("amplified"));
      NETHER = ResourceKey.create(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, new ResourceLocation("nether"));
      END = ResourceKey.create(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, new ResourceLocation("end"));
      CAVES = ResourceKey.create(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, new ResourceLocation("caves"));
      FLOATING_ISLANDS = ResourceKey.create(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, new ResourceLocation("floating_islands"));
      register(OVERWORLD, overworld(false, false));
      register(LARGE_BIOMES, overworld(false, true));
      register(AMPLIFIED, overworld(true, false));
      register(NETHER, nether());
      register(END, end());
      register(CAVES, caves());
      register(FLOATING_ISLANDS, floatingIslands());
   }
}
