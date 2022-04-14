package net.minecraft.world.level.levelgen.structure;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import org.slf4j.Logger;

public class StructureCheck {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int NO_STRUCTURE = -1;
   private final ChunkScanAccess storageAccess;
   private final RegistryAccess registryAccess;
   private final Registry<Biome> biomes;
   private final Registry<ConfiguredStructureFeature<?, ?>> structureConfigs;
   private final StructureManager structureManager;
   private final ResourceKey<Level> dimension;
   private final ChunkGenerator chunkGenerator;
   private final LevelHeightAccessor heightAccessor;
   private final BiomeSource biomeSource;
   private final long seed;
   private final DataFixer fixerUpper;
   private final Long2ObjectMap<Object2IntMap<ConfiguredStructureFeature<?, ?>>> loadedChunks = new Long2ObjectOpenHashMap();
   private final Map<ConfiguredStructureFeature<?, ?>, Long2BooleanMap> featureChecks = new HashMap();

   public StructureCheck(ChunkScanAccess var1, RegistryAccess var2, StructureManager var3, ResourceKey<Level> var4, ChunkGenerator var5, LevelHeightAccessor var6, BiomeSource var7, long var8, DataFixer var10) {
      this.storageAccess = var1;
      this.registryAccess = var2;
      this.structureManager = var3;
      this.dimension = var4;
      this.chunkGenerator = var5;
      this.heightAccessor = var6;
      this.biomeSource = var7;
      this.seed = var8;
      this.fixerUpper = var10;
      this.biomes = var2.ownedRegistryOrThrow(Registry.BIOME_REGISTRY);
      this.structureConfigs = var2.ownedRegistryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
   }

   public StructureCheckResult checkStart(ChunkPos var1, ConfiguredStructureFeature<?, ?> var2, boolean var3) {
      long var4 = var1.toLong();
      Object2IntMap var6 = (Object2IntMap)this.loadedChunks.get(var4);
      if (var6 != null) {
         return this.checkStructureInfo(var6, var2, var3);
      } else {
         StructureCheckResult var7 = this.tryLoadFromStorage(var1, var2, var3, var4);
         if (var7 != null) {
            return var7;
         } else {
            boolean var8 = ((Long2BooleanMap)this.featureChecks.computeIfAbsent(var2, (var0) -> {
               return new Long2BooleanOpenHashMap();
            })).computeIfAbsent(var4, (var3x) -> {
               return this.canCreateStructure(var1, var2);
            });
            return !var8 ? StructureCheckResult.START_NOT_PRESENT : StructureCheckResult.CHUNK_LOAD_NEEDED;
         }
      }
   }

   private <FC extends FeatureConfiguration, F extends StructureFeature<FC>> boolean canCreateStructure(ChunkPos var1, ConfiguredStructureFeature<FC, F> var2) {
      StructureFeature var10000 = var2.feature;
      RegistryAccess var10001 = this.registryAccess;
      ChunkGenerator var10002 = this.chunkGenerator;
      BiomeSource var10003 = this.biomeSource;
      StructureManager var10004 = this.structureManager;
      long var10005 = this.seed;
      FeatureConfiguration var10007 = var2.config;
      LevelHeightAccessor var10008 = this.heightAccessor;
      HolderSet var10009 = var2.biomes();
      Objects.requireNonNull(var10009);
      return var10000.canGenerate(var10001, var10002, var10003, var10004, var10005, var1, var10007, var10008, var10009::contains);
   }

   @Nullable
   private StructureCheckResult tryLoadFromStorage(ChunkPos var1, ConfiguredStructureFeature<?, ?> var2, boolean var3, long var4) {
      CollectFields var6 = new CollectFields(new FieldSelector[]{new FieldSelector(IntTag.TYPE, "DataVersion"), new FieldSelector("Level", "Structures", CompoundTag.TYPE, "Starts"), new FieldSelector("structures", CompoundTag.TYPE, "starts")});

      try {
         this.storageAccess.scanChunk(var1, var6).join();
      } catch (Exception var13) {
         LOGGER.warn("Failed to read chunk {}", var1, var13);
         return StructureCheckResult.CHUNK_LOAD_NEEDED;
      }

      Tag var7 = var6.getResult();
      if (!(var7 instanceof CompoundTag)) {
         return null;
      } else {
         CompoundTag var8 = (CompoundTag)var7;
         int var9 = ChunkStorage.getVersion(var8);
         if (var9 <= 1493) {
            return StructureCheckResult.CHUNK_LOAD_NEEDED;
         } else {
            ChunkStorage.injectDatafixingContext(var8, this.dimension, this.chunkGenerator.getTypeNameForDataFixer());

            CompoundTag var10;
            try {
               var10 = NbtUtils.update(this.fixerUpper, DataFixTypes.CHUNK, var8, var9);
            } catch (Exception var12) {
               LOGGER.warn("Failed to partially datafix chunk {}", var1, var12);
               return StructureCheckResult.CHUNK_LOAD_NEEDED;
            }

            Object2IntMap var11 = this.loadStructures(var10);
            if (var11 == null) {
               return null;
            } else {
               this.storeFullResults(var4, var11);
               return this.checkStructureInfo(var11, var2, var3);
            }
         }
      }
   }

   @Nullable
   private Object2IntMap<ConfiguredStructureFeature<?, ?>> loadStructures(CompoundTag var1) {
      if (!var1.contains("structures", 10)) {
         return null;
      } else {
         CompoundTag var2 = var1.getCompound("structures");
         if (!var2.contains("starts", 10)) {
            return null;
         } else {
            CompoundTag var3 = var2.getCompound("starts");
            if (var3.isEmpty()) {
               return Object2IntMaps.emptyMap();
            } else {
               Object2IntOpenHashMap var4 = new Object2IntOpenHashMap();
               Registry var5 = this.registryAccess.registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
               Iterator var6 = var3.getAllKeys().iterator();

               while(var6.hasNext()) {
                  String var7 = (String)var6.next();
                  ResourceLocation var8 = ResourceLocation.tryParse(var7);
                  if (var8 != null) {
                     ConfiguredStructureFeature var9 = (ConfiguredStructureFeature)var5.get(var8);
                     if (var9 != null) {
                        CompoundTag var10 = var3.getCompound(var7);
                        if (!var10.isEmpty()) {
                           String var11 = var10.getString("id");
                           if (!"INVALID".equals(var11)) {
                              int var12 = var10.getInt("references");
                              var4.put(var9, var12);
                           }
                        }
                     }
                  }
               }

               return var4;
            }
         }
      }
   }

   private static Object2IntMap<ConfiguredStructureFeature<?, ?>> deduplicateEmptyMap(Object2IntMap<ConfiguredStructureFeature<?, ?>> var0) {
      return var0.isEmpty() ? Object2IntMaps.emptyMap() : var0;
   }

   private StructureCheckResult checkStructureInfo(Object2IntMap<ConfiguredStructureFeature<?, ?>> var1, ConfiguredStructureFeature<?, ?> var2, boolean var3) {
      int var4 = var1.getOrDefault(var2, -1);
      return var4 == -1 || var3 && var4 != 0 ? StructureCheckResult.START_NOT_PRESENT : StructureCheckResult.START_PRESENT;
   }

   public void onStructureLoad(ChunkPos var1, Map<ConfiguredStructureFeature<?, ?>, StructureStart> var2) {
      long var3 = var1.toLong();
      Object2IntOpenHashMap var5 = new Object2IntOpenHashMap();
      var2.forEach((var1x, var2x) -> {
         if (var2x.isValid()) {
            var5.put(var1x, var2x.getReferences());
         }

      });
      this.storeFullResults(var3, var5);
   }

   private void storeFullResults(long var1, Object2IntMap<ConfiguredStructureFeature<?, ?>> var3) {
      this.loadedChunks.put(var1, deduplicateEmptyMap(var3));
      this.featureChecks.values().forEach((var2) -> {
         var2.remove(var1);
      });
   }

   public void incrementReference(ChunkPos var1, ConfiguredStructureFeature<?, ?> var2) {
      this.loadedChunks.compute(var1.toLong(), (var1x, var2x) -> {
         if (var2x == null || ((Object2IntMap)var2x).isEmpty()) {
            var2x = new Object2IntOpenHashMap();
         }

         ((Object2IntMap)var2x).computeInt(var2, (var0, var1) -> {
            return var1 == null ? 1 : var1 + 1;
         });
         return (Object2IntMap)var2x;
      });
   }
}
