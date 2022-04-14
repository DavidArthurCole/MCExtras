package net.minecraft.world.level.levelgen;

import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Random;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class WorldGenSettings {
   public static final Codec<WorldGenSettings> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(Codec.LONG.fieldOf("seed").stable().forGetter(WorldGenSettings::seed), Codec.BOOL.fieldOf("generate_features").orElse(true).stable().forGetter(WorldGenSettings::generateFeatures), Codec.BOOL.fieldOf("bonus_chest").orElse(false).stable().forGetter(WorldGenSettings::generateBonusChest), RegistryCodecs.dataPackAwareCodec(Registry.LEVEL_STEM_REGISTRY, Lifecycle.stable(), LevelStem.CODEC).xmap(LevelStem::sortMap, Function.identity()).fieldOf("dimensions").forGetter(WorldGenSettings::dimensions), Codec.STRING.optionalFieldOf("legacy_custom_options").stable().forGetter((var0x) -> {
         return var0x.legacyCustomOptions;
      })).apply(var0, var0.stable(WorldGenSettings::new));
   }).comapFlatMap(WorldGenSettings::guardExperimental, Function.identity());
   private static final Logger LOGGER = LogUtils.getLogger();
   private final long seed;
   private final boolean generateFeatures;
   private final boolean generateBonusChest;
   private final Registry<LevelStem> dimensions;
   private final Optional<String> legacyCustomOptions;

   private DataResult<WorldGenSettings> guardExperimental() {
      LevelStem var1 = (LevelStem)this.dimensions.get(LevelStem.OVERWORLD);
      if (var1 == null) {
         return DataResult.error("Overworld settings missing");
      } else {
         return this.stable() ? DataResult.success(this, Lifecycle.stable()) : DataResult.success(this);
      }
   }

   private boolean stable() {
      return LevelStem.stable(this.seed, this.dimensions);
   }

   public WorldGenSettings(long var1, boolean var3, boolean var4, Registry<LevelStem> var5) {
      this(var1, var3, var4, var5, Optional.empty());
      LevelStem var6 = (LevelStem)var5.get(LevelStem.OVERWORLD);
      if (var6 == null) {
         throw new IllegalStateException("Overworld settings missing");
      }
   }

   private WorldGenSettings(long var1, boolean var3, boolean var4, Registry<LevelStem> var5, Optional<String> var6) {
      this.seed = var1;
      this.generateFeatures = var3;
      this.generateBonusChest = var4;
      this.dimensions = var5;
      this.legacyCustomOptions = var6;
   }

   public static WorldGenSettings demoSettings(RegistryAccess var0) {
      int var1 = "North Carolina".hashCode();
      return new WorldGenSettings((long)var1, true, true, withOverworld(var0.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY), (Registry)DimensionType.defaultDimensions(var0, (long)var1), makeDefaultOverworld(var0, (long)var1)));
   }

   public static WorldGenSettings makeDefault(RegistryAccess var0) {
      long var1 = (new Random()).nextLong();
      return new WorldGenSettings(var1, true, false, withOverworld(var0.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY), (Registry)DimensionType.defaultDimensions(var0, var1), makeDefaultOverworld(var0, var1)));
   }

   public static NoiseBasedChunkGenerator makeDefaultOverworld(RegistryAccess var0, long var1) {
      return makeDefaultOverworld(var0, var1, true);
   }

   public static NoiseBasedChunkGenerator makeDefaultOverworld(RegistryAccess var0, long var1, boolean var3) {
      return makeOverworld(var0, var1, NoiseGeneratorSettings.OVERWORLD, var3);
   }

   public static NoiseBasedChunkGenerator makeOverworld(RegistryAccess var0, long var1, ResourceKey<NoiseGeneratorSettings> var3) {
      return makeOverworld(var0, var1, var3, true);
   }

   public static NoiseBasedChunkGenerator makeOverworld(RegistryAccess var0, long var1, ResourceKey<NoiseGeneratorSettings> var3, boolean var4) {
      Registry var5 = var0.registryOrThrow(Registry.BIOME_REGISTRY);
      Registry var6 = var0.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
      Registry var7 = var0.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY);
      Registry var8 = var0.registryOrThrow(Registry.NOISE_REGISTRY);
      return new NoiseBasedChunkGenerator(var6, var8, MultiNoiseBiomeSource.Preset.OVERWORLD.biomeSource(var5, var4), var1, var7.getOrCreateHolder(var3));
   }

   public long seed() {
      return this.seed;
   }

   public boolean generateFeatures() {
      return this.generateFeatures;
   }

   public boolean generateBonusChest() {
      return this.generateBonusChest;
   }

   public static Registry<LevelStem> withOverworld(Registry<DimensionType> var0, Registry<LevelStem> var1, ChunkGenerator var2) {
      LevelStem var3 = (LevelStem)var1.get(LevelStem.OVERWORLD);
      Holder var4 = var3 == null ? var0.getOrCreateHolder(DimensionType.OVERWORLD_LOCATION) : var3.typeHolder();
      return withOverworld(var1, var4, var2);
   }

   public static Registry<LevelStem> withOverworld(Registry<LevelStem> var0, Holder<DimensionType> var1, ChunkGenerator var2) {
      MappedRegistry var3 = new MappedRegistry(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental(), (Function)null);
      var3.register(LevelStem.OVERWORLD, new LevelStem(var1, var2), Lifecycle.stable());
      Iterator var4 = var0.entrySet().iterator();

      while(var4.hasNext()) {
         Entry var5 = (Entry)var4.next();
         ResourceKey var6 = (ResourceKey)var5.getKey();
         if (var6 != LevelStem.OVERWORLD) {
            var3.register(var6, (LevelStem)var5.getValue(), var0.lifecycle((LevelStem)var5.getValue()));
         }
      }

      return var3;
   }

   public Registry<LevelStem> dimensions() {
      return this.dimensions;
   }

   public ChunkGenerator overworld() {
      LevelStem var1 = (LevelStem)this.dimensions.get(LevelStem.OVERWORLD);
      if (var1 == null) {
         throw new IllegalStateException("Overworld settings missing");
      } else {
         return var1.generator();
      }
   }

   public ImmutableSet<ResourceKey<Level>> levels() {
      return (ImmutableSet)this.dimensions().entrySet().stream().map(Entry::getKey).map(WorldGenSettings::levelStemToLevel).collect(ImmutableSet.toImmutableSet());
   }

   public static ResourceKey<Level> levelStemToLevel(ResourceKey<LevelStem> var0) {
      return ResourceKey.create(Registry.DIMENSION_REGISTRY, var0.location());
   }

   public static ResourceKey<LevelStem> levelToLevelStem(ResourceKey<Level> var0) {
      return ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, var0.location());
   }

   public boolean isDebug() {
      return this.overworld() instanceof DebugLevelSource;
   }

   public boolean isFlatWorld() {
      return this.overworld() instanceof FlatLevelSource;
   }

   public boolean isOldCustomizedWorld() {
      return this.legacyCustomOptions.isPresent();
   }

   public WorldGenSettings withBonusChest() {
      return new WorldGenSettings(this.seed, this.generateFeatures, true, this.dimensions, this.legacyCustomOptions);
   }

   public WorldGenSettings withFeaturesToggled() {
      return new WorldGenSettings(this.seed, !this.generateFeatures, this.generateBonusChest, this.dimensions);
   }

   public WorldGenSettings withBonusChestToggled() {
      return new WorldGenSettings(this.seed, this.generateFeatures, !this.generateBonusChest, this.dimensions);
   }

   public static WorldGenSettings create(RegistryAccess var0, DedicatedServerProperties.WorldGenProperties var1) {
      long var2 = parseSeed(var1.levelSeed()).orElse((new Random()).nextLong());
      Registry var4 = var0.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
      Registry var5 = var0.registryOrThrow(Registry.BIOME_REGISTRY);
      Registry var6 = var0.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
      Registry var7 = DimensionType.defaultDimensions(var0, var2);
      String var8 = var1.levelType();
      byte var9 = -1;
      switch(var8.hashCode()) {
      case -1100099890:
         if (var8.equals("largebiomes")) {
            var9 = 3;
         }
         break;
      case 3145593:
         if (var8.equals("flat")) {
            var9 = 0;
         }
         break;
      case 1045526590:
         if (var8.equals("debug_all_block_states")) {
            var9 = 1;
         }
         break;
      case 1271599715:
         if (var8.equals("amplified")) {
            var9 = 2;
         }
      }

      switch(var9) {
      case 0:
         Dynamic var10 = new Dynamic(JsonOps.INSTANCE, var1.generatorSettings());
         boolean var10003 = var1.generateStructures();
         DataResult var10010 = FlatLevelGeneratorSettings.CODEC.parse(var10);
         Logger var10011 = LOGGER;
         Objects.requireNonNull(var10011);
         return new WorldGenSettings(var2, var10003, false, withOverworld(var4, (Registry)var7, new FlatLevelSource(var6, (FlatLevelGeneratorSettings)var10010.resultOrPartial(var10011::error).orElseGet(() -> {
            return FlatLevelGeneratorSettings.getDefault(var5, var6);
         }))));
      case 1:
         return new WorldGenSettings(var2, var1.generateStructures(), false, withOverworld(var4, (Registry)var7, new DebugLevelSource(var6, var5)));
      case 2:
         return new WorldGenSettings(var2, var1.generateStructures(), false, withOverworld(var4, (Registry)var7, makeOverworld(var0, var2, NoiseGeneratorSettings.AMPLIFIED)));
      case 3:
         return new WorldGenSettings(var2, var1.generateStructures(), false, withOverworld(var4, (Registry)var7, makeOverworld(var0, var2, NoiseGeneratorSettings.LARGE_BIOMES)));
      default:
         return new WorldGenSettings(var2, var1.generateStructures(), false, withOverworld(var4, (Registry)var7, makeDefaultOverworld(var0, var2)));
      }
   }

   public WorldGenSettings withSeed(boolean var1, OptionalLong var2) {
      long var4 = var2.orElse(this.seed);
      Object var6;
      if (var2.isPresent()) {
         MappedRegistry var7 = new MappedRegistry(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental(), (Function)null);
         long var8 = var2.getAsLong();
         Iterator var10 = this.dimensions.entrySet().iterator();

         while(var10.hasNext()) {
            Entry var11 = (Entry)var10.next();
            ResourceKey var12 = (ResourceKey)var11.getKey();
            var7.register(var12, new LevelStem(((LevelStem)var11.getValue()).typeHolder(), ((LevelStem)var11.getValue()).generator().withSeed(var8)), this.dimensions.lifecycle((LevelStem)var11.getValue()));
         }

         var6 = var7;
      } else {
         var6 = this.dimensions;
      }

      WorldGenSettings var3;
      if (this.isDebug()) {
         var3 = new WorldGenSettings(var4, false, false, (Registry)var6);
      } else {
         var3 = new WorldGenSettings(var4, this.generateFeatures(), this.generateBonusChest() && !var1, (Registry)var6);
      }

      return var3;
   }

   public static OptionalLong parseSeed(String var0) {
      var0 = var0.trim();
      if (StringUtils.isEmpty(var0)) {
         return OptionalLong.empty();
      } else {
         try {
            return OptionalLong.of(Long.parseLong(var0));
         } catch (NumberFormatException var2) {
            return OptionalLong.of((long)var0.hashCode());
         }
      }
   }
}
