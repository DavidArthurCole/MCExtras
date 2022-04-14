package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.MineshaftConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OceanRuinConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RangeConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RuinedPortalConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ShipwreckConfiguration;
import net.minecraft.world.level.levelgen.structure.NetherFossilFeature;
import net.minecraft.world.level.levelgen.structure.OceanRuinFeature;
import net.minecraft.world.level.levelgen.structure.PostPlacementProcessor;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import org.slf4j.Logger;

public class StructureFeature<C extends FeatureConfiguration> {
   private static final Map<StructureFeature<?>, GenerationStep.Decoration> STEP = Maps.newHashMap();
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final StructureFeature<JigsawConfiguration> PILLAGER_OUTPOST;
   public static final StructureFeature<MineshaftConfiguration> MINESHAFT;
   public static final StructureFeature<NoneFeatureConfiguration> WOODLAND_MANSION;
   public static final StructureFeature<NoneFeatureConfiguration> JUNGLE_TEMPLE;
   public static final StructureFeature<NoneFeatureConfiguration> DESERT_PYRAMID;
   public static final StructureFeature<NoneFeatureConfiguration> IGLOO;
   public static final StructureFeature<RuinedPortalConfiguration> RUINED_PORTAL;
   public static final StructureFeature<ShipwreckConfiguration> SHIPWRECK;
   public static final StructureFeature<NoneFeatureConfiguration> SWAMP_HUT;
   public static final StructureFeature<NoneFeatureConfiguration> STRONGHOLD;
   public static final StructureFeature<NoneFeatureConfiguration> OCEAN_MONUMENT;
   public static final StructureFeature<OceanRuinConfiguration> OCEAN_RUIN;
   public static final StructureFeature<NoneFeatureConfiguration> FORTRESS;
   public static final StructureFeature<NoneFeatureConfiguration> END_CITY;
   public static final StructureFeature<ProbabilityFeatureConfiguration> BURIED_TREASURE;
   public static final StructureFeature<JigsawConfiguration> VILLAGE;
   public static final StructureFeature<RangeConfiguration> NETHER_FOSSIL;
   public static final StructureFeature<JigsawConfiguration> BASTION_REMNANT;
   public static final int MAX_STRUCTURE_RANGE = 8;
   private final Codec<ConfiguredStructureFeature<C, StructureFeature<C>>> configuredStructureCodec;
   private final PieceGeneratorSupplier<C> pieceGenerator;
   private final PostPlacementProcessor postPlacementProcessor;

   private static <F extends StructureFeature<?>> F register(String var0, F var1, GenerationStep.Decoration var2) {
      STEP.put(var1, var2);
      return (StructureFeature)Registry.register(Registry.STRUCTURE_FEATURE, (String)var0, var1);
   }

   public StructureFeature(Codec<C> var1, PieceGeneratorSupplier<C> var2) {
      this(var1, var2, PostPlacementProcessor.NONE);
   }

   public StructureFeature(Codec<C> var1, PieceGeneratorSupplier<C> var2, PostPlacementProcessor var3) {
      this.configuredStructureCodec = RecordCodecBuilder.create((var2x) -> {
         return var2x.group(var1.fieldOf("config").forGetter((var0) -> {
            return var0.config;
         }), RegistryCodecs.homogeneousList(Registry.BIOME_REGISTRY).fieldOf("biomes").forGetter(ConfiguredStructureFeature::biomes), Codec.BOOL.optionalFieldOf("adapt_noise", false).forGetter((var0) -> {
            return var0.adaptNoise;
         }), Codec.simpleMap(MobCategory.CODEC, StructureSpawnOverride.CODEC, StringRepresentable.keys(MobCategory.values())).fieldOf("spawn_overrides").forGetter((var0) -> {
            return var0.spawnOverrides;
         })).apply(var2x, (var1x, var2, var3, var4) -> {
            return new ConfiguredStructureFeature(this, var1x, var2, var3, var4);
         });
      });
      this.pieceGenerator = var2;
      this.postPlacementProcessor = var3;
   }

   public GenerationStep.Decoration step() {
      return (GenerationStep.Decoration)STEP.get(this);
   }

   public static void bootstrap() {
   }

   @Nullable
   public static StructureStart loadStaticStart(StructurePieceSerializationContext var0, CompoundTag var1, long var2) {
      String var4 = var1.getString("id");
      if ("INVALID".equals(var4)) {
         return StructureStart.INVALID_START;
      } else {
         Registry var5 = var0.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
         ConfiguredStructureFeature var6 = (ConfiguredStructureFeature)var5.get(new ResourceLocation(var4));
         if (var6 == null) {
            LOGGER.error("Unknown feature id: {}", var4);
            return null;
         } else {
            ChunkPos var7 = new ChunkPos(var1.getInt("ChunkX"), var1.getInt("ChunkZ"));
            int var8 = var1.getInt("references");
            ListTag var9 = var1.getList("Children", 10);

            try {
               PiecesContainer var10 = PiecesContainer.load(var9, var0);
               if (var6.feature == OCEAN_MONUMENT) {
                  var10 = OceanMonumentFeature.regeneratePiecesAfterLoad(var7, var2, var10);
               }

               return new StructureStart(var6, var7, var8, var10);
            } catch (Exception var11) {
               LOGGER.error("Failed Start with id {}", var4, var11);
               return null;
            }
         }
      }
   }

   public Codec<ConfiguredStructureFeature<C, StructureFeature<C>>> configuredStructureCodec() {
      return this.configuredStructureCodec;
   }

   public ConfiguredStructureFeature<C, ? extends StructureFeature<C>> configured(C var1, TagKey<Biome> var2) {
      return this.configured(var1, var2, false);
   }

   public ConfiguredStructureFeature<C, ? extends StructureFeature<C>> configured(C var1, TagKey<Biome> var2, boolean var3) {
      return new ConfiguredStructureFeature(this, var1, BuiltinRegistries.BIOME.getOrCreateTag(var2), var3, Map.of());
   }

   public ConfiguredStructureFeature<C, ? extends StructureFeature<C>> configured(C var1, TagKey<Biome> var2, Map<MobCategory, StructureSpawnOverride> var3) {
      return new ConfiguredStructureFeature(this, var1, BuiltinRegistries.BIOME.getOrCreateTag(var2), false, var3);
   }

   public ConfiguredStructureFeature<C, ? extends StructureFeature<C>> configured(C var1, TagKey<Biome> var2, boolean var3, Map<MobCategory, StructureSpawnOverride> var4) {
      return new ConfiguredStructureFeature(this, var1, BuiltinRegistries.BIOME.getOrCreateTag(var2), var3, var4);
   }

   public static BlockPos getLocatePos(RandomSpreadStructurePlacement var0, ChunkPos var1) {
      return (new BlockPos(var1.getMinBlockX(), 0, var1.getMinBlockZ())).offset(var0.locateOffset());
   }

   public boolean canGenerate(RegistryAccess var1, ChunkGenerator var2, BiomeSource var3, StructureManager var4, long var5, ChunkPos var7, C var8, LevelHeightAccessor var9, Predicate<Holder<Biome>> var10) {
      return this.pieceGenerator.createGenerator(new PieceGeneratorSupplier.Context(var2, var3, var5, var7, var8, var9, var10, var4, var1)).isPresent();
   }

   public PieceGeneratorSupplier<C> pieceGeneratorSupplier() {
      return this.pieceGenerator;
   }

   public PostPlacementProcessor getPostPlacementProcessor() {
      return this.postPlacementProcessor;
   }

   static {
      PILLAGER_OUTPOST = register("pillager_outpost", new PillagerOutpostFeature(JigsawConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
      MINESHAFT = register("mineshaft", new MineshaftFeature(MineshaftConfiguration.CODEC), GenerationStep.Decoration.UNDERGROUND_STRUCTURES);
      WOODLAND_MANSION = register("mansion", new WoodlandMansionFeature(NoneFeatureConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
      JUNGLE_TEMPLE = register("jungle_pyramid", new JunglePyramidFeature(NoneFeatureConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
      DESERT_PYRAMID = register("desert_pyramid", new DesertPyramidFeature(NoneFeatureConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
      IGLOO = register("igloo", new IglooFeature(NoneFeatureConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
      RUINED_PORTAL = register("ruined_portal", new RuinedPortalFeature(RuinedPortalConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
      SHIPWRECK = register("shipwreck", new ShipwreckFeature(ShipwreckConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
      SWAMP_HUT = register("swamp_hut", new SwamplandHutFeature(NoneFeatureConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
      STRONGHOLD = register("stronghold", new StrongholdFeature(NoneFeatureConfiguration.CODEC), GenerationStep.Decoration.STRONGHOLDS);
      OCEAN_MONUMENT = register("monument", new OceanMonumentFeature(NoneFeatureConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
      OCEAN_RUIN = register("ocean_ruin", new OceanRuinFeature(OceanRuinConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
      FORTRESS = register("fortress", new NetherFortressFeature(NoneFeatureConfiguration.CODEC), GenerationStep.Decoration.UNDERGROUND_DECORATION);
      END_CITY = register("endcity", new EndCityFeature(NoneFeatureConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
      BURIED_TREASURE = register("buried_treasure", new BuriedTreasureFeature(ProbabilityFeatureConfiguration.CODEC), GenerationStep.Decoration.UNDERGROUND_STRUCTURES);
      VILLAGE = register("village", new VillageFeature(JigsawConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
      NETHER_FOSSIL = register("nether_fossil", new NetherFossilFeature(RangeConfiguration.CODEC), GenerationStep.Decoration.UNDERGROUND_DECORATION);
      BASTION_REMNANT = register("bastion_remnant", new BastionFeature(JigsawConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
   }
}
