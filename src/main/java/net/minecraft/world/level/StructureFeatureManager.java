package net.minecraft.world.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.FeatureAccess;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public class StructureFeatureManager {
   private final LevelAccessor level;
   private final WorldGenSettings worldGenSettings;
   private final StructureCheck structureCheck;

   public StructureFeatureManager(LevelAccessor var1, WorldGenSettings var2, StructureCheck var3) {
      this.level = var1;
      this.worldGenSettings = var2;
      this.structureCheck = var3;
   }

   public StructureFeatureManager forWorldGenRegion(WorldGenRegion var1) {
      if (var1.getLevel() != this.level) {
         ServerLevel var10002 = var1.getLevel();
         throw new IllegalStateException("Using invalid feature manager (source level: " + var10002 + ", region: " + var1);
      } else {
         return new StructureFeatureManager(var1, this.worldGenSettings, this.structureCheck);
      }
   }

   public List<StructureStart> startsForFeature(SectionPos var1, Predicate<ConfiguredStructureFeature<?, ?>> var2) {
      Map var3 = this.level.getChunk(var1.x(), var1.z(), ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
      Builder var4 = ImmutableList.builder();
      Iterator var5 = var3.entrySet().iterator();

      while(var5.hasNext()) {
         Entry var6 = (Entry)var5.next();
         ConfiguredStructureFeature var7 = (ConfiguredStructureFeature)var6.getKey();
         if (var2.test(var7)) {
            LongSet var10002 = (LongSet)var6.getValue();
            Objects.requireNonNull(var4);
            this.fillStartsForFeature(var7, var10002, var4::add);
         }
      }

      return var4.build();
   }

   public List<StructureStart> startsForFeature(SectionPos var1, ConfiguredStructureFeature<?, ?> var2) {
      LongSet var3 = this.level.getChunk(var1.x(), var1.z(), ChunkStatus.STRUCTURE_REFERENCES).getReferencesForFeature(var2);
      Builder var4 = ImmutableList.builder();
      Objects.requireNonNull(var4);
      this.fillStartsForFeature(var2, var3, var4::add);
      return var4.build();
   }

   public void fillStartsForFeature(ConfiguredStructureFeature<?, ?> var1, LongSet var2, Consumer<StructureStart> var3) {
      LongIterator var4 = var2.iterator();

      while(var4.hasNext()) {
         long var5 = (Long)var4.next();
         SectionPos var7 = SectionPos.of(new ChunkPos(var5), this.level.getMinSection());
         StructureStart var8 = this.getStartForFeature(var7, var1, this.level.getChunk(var7.x(), var7.z(), ChunkStatus.STRUCTURE_STARTS));
         if (var8 != null && var8.isValid()) {
            var3.accept(var8);
         }
      }

   }

   @Nullable
   public StructureStart getStartForFeature(SectionPos var1, ConfiguredStructureFeature<?, ?> var2, FeatureAccess var3) {
      return var3.getStartForFeature(var2);
   }

   public void setStartForFeature(SectionPos var1, ConfiguredStructureFeature<?, ?> var2, StructureStart var3, FeatureAccess var4) {
      var4.setStartForFeature(var2, var3);
   }

   public void addReferenceForFeature(SectionPos var1, ConfiguredStructureFeature<?, ?> var2, long var3, FeatureAccess var5) {
      var5.addReferenceForFeature(var2, var3);
   }

   public boolean shouldGenerateFeatures() {
      return this.worldGenSettings.generateFeatures();
   }

   public StructureStart getStructureAt(BlockPos var1, ConfiguredStructureFeature<?, ?> var2) {
      Iterator var3 = this.startsForFeature(SectionPos.of(var1), var2).iterator();

      StructureStart var4;
      do {
         if (!var3.hasNext()) {
            return StructureStart.INVALID_START;
         }

         var4 = (StructureStart)var3.next();
      } while(!var4.getBoundingBox().isInside(var1));

      return var4;
   }

   public StructureStart getStructureWithPieceAt(BlockPos var1, ResourceKey<ConfiguredStructureFeature<?, ?>> var2) {
      ConfiguredStructureFeature var3 = (ConfiguredStructureFeature)this.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).get(var2);
      return var3 == null ? StructureStart.INVALID_START : this.getStructureWithPieceAt(var1, var3);
   }

   public StructureStart getStructureWithPieceAt(BlockPos var1, ConfiguredStructureFeature<?, ?> var2) {
      Iterator var3 = this.startsForFeature(SectionPos.of(var1), var2).iterator();

      StructureStart var4;
      do {
         if (!var3.hasNext()) {
            return StructureStart.INVALID_START;
         }

         var4 = (StructureStart)var3.next();
      } while(!this.structureHasPieceAt(var1, var4));

      return var4;
   }

   public boolean structureHasPieceAt(BlockPos var1, StructureStart var2) {
      Iterator var3 = var2.getPieces().iterator();

      StructurePiece var4;
      do {
         if (!var3.hasNext()) {
            return false;
         }

         var4 = (StructurePiece)var3.next();
      } while(!var4.getBoundingBox().isInside(var1));

      return true;
   }

   public boolean hasAnyStructureAt(BlockPos var1) {
      SectionPos var2 = SectionPos.of(var1);
      return this.level.getChunk(var2.x(), var2.z(), ChunkStatus.STRUCTURE_REFERENCES).hasAnyStructureReferences();
   }

   public Map<ConfiguredStructureFeature<?, ?>, LongSet> getAllStructuresAt(BlockPos var1) {
      SectionPos var2 = SectionPos.of(var1);
      return this.level.getChunk(var2.x(), var2.z(), ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
   }

   public StructureCheckResult checkStructurePresence(ChunkPos var1, ConfiguredStructureFeature<?, ?> var2, boolean var3) {
      return this.structureCheck.checkStart(var1, var2, var3);
   }

   public void addReference(StructureStart var1) {
      var1.addReference();
      this.structureCheck.incrementReference(var1.getChunkPos(), var1.getFeature());
   }

   public RegistryAccess registryAccess() {
      return this.level.registryAccess();
   }
}
