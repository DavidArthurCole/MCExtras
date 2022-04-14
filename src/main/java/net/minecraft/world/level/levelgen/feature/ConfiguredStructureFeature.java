package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public class ConfiguredStructureFeature<FC extends FeatureConfiguration, F extends StructureFeature<FC>> {
   public static final Codec<ConfiguredStructureFeature<?, ?>> DIRECT_CODEC;
   public static final Codec<Holder<ConfiguredStructureFeature<?, ?>>> CODEC;
   public static final Codec<HolderSet<ConfiguredStructureFeature<?, ?>>> LIST_CODEC;
   public final F feature;
   public final FC config;
   public final HolderSet<Biome> biomes;
   public final Map<MobCategory, StructureSpawnOverride> spawnOverrides;
   public final boolean adaptNoise;

   public ConfiguredStructureFeature(F var1, FC var2, HolderSet<Biome> var3, boolean var4, Map<MobCategory, StructureSpawnOverride> var5) {
      this.feature = var1;
      this.config = var2;
      this.biomes = var3;
      this.adaptNoise = var4;
      this.spawnOverrides = var5;
   }

   public StructureStart generate(RegistryAccess var1, ChunkGenerator var2, BiomeSource var3, StructureManager var4, long var5, ChunkPos var7, int var8, LevelHeightAccessor var9, Predicate<Holder<Biome>> var10) {
      Optional var11 = this.feature.pieceGeneratorSupplier().createGenerator(new PieceGeneratorSupplier.Context(var2, var3, var5, var7, this.config, var9, var10, var4, var1));
      if (var11.isPresent()) {
         StructurePiecesBuilder var12 = new StructurePiecesBuilder();
         WorldgenRandom var13 = new WorldgenRandom(new LegacyRandomSource(0L));
         var13.setLargeFeatureSeed(var5, var7.x, var7.z);
         ((PieceGenerator)var11.get()).generatePieces(var12, new PieceGenerator.Context(this.config, var2, var4, var7, var9, var13, var5));
         StructureStart var14 = new StructureStart(this, var7, var8, var12.build());
         if (var14.isValid()) {
            return var14;
         }
      }

      return StructureStart.INVALID_START;
   }

   public HolderSet<Biome> biomes() {
      return this.biomes;
   }

   public BoundingBox adjustBoundingBox(BoundingBox var1) {
      return this.adaptNoise ? var1.inflatedBy(12) : var1;
   }

   static {
      DIRECT_CODEC = Registry.STRUCTURE_FEATURE.byNameCodec().dispatch((var0) -> {
         return var0.feature;
      }, StructureFeature::configuredStructureCodec);
      CODEC = RegistryFileCodec.create(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, DIRECT_CODEC);
      LIST_CODEC = RegistryCodecs.homogeneousList(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, DIRECT_CODEC);
   }
}
