package net.minecraft.server;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;

public record WorldStem(CloseableResourceManager a, ReloadableServerResources b, RegistryAccess.Frozen c, WorldData d) implements AutoCloseable {
   private final CloseableResourceManager resourceManager;
   private final ReloadableServerResources dataPackResources;
   private final RegistryAccess.Frozen registryAccess;
   private final WorldData worldData;

   public WorldStem(CloseableResourceManager var1, ReloadableServerResources var2, RegistryAccess.Frozen var3, WorldData var4) {
      this.resourceManager = var1;
      this.dataPackResources = var2;
      this.registryAccess = var3;
      this.worldData = var4;
   }

   public static CompletableFuture<WorldStem> load(WorldStem.InitConfig var0, WorldStem.DataPackConfigSupplier var1, WorldStem.WorldDataSupplier var2, Executor var3, Executor var4) {
      try {
         DataPackConfig var5 = (DataPackConfig)var1.get();
         DataPackConfig var6 = MinecraftServer.configurePackRepository(var0.packRepository(), var5, var0.safeMode());
         List var7 = var0.packRepository().openAllSelected();
         MultiPackResourceManager var8 = new MultiPackResourceManager(PackType.SERVER_DATA, var7);
         Pair var9 = var2.get(var8, var6);
         WorldData var10 = (WorldData)var9.getFirst();
         RegistryAccess.Frozen var11 = (RegistryAccess.Frozen)var9.getSecond();
         return ReloadableServerResources.loadResources(var8, var11, var0.commandSelection(), var0.functionCompilationLevel(), var3, var4).whenComplete((var1x, var2x) -> {
            if (var2x != null) {
               var8.close();
            }

         }).thenApply((var3x) -> {
            return new WorldStem(var8, var3x, var11, var10);
         });
      } catch (Exception var12) {
         return CompletableFuture.failedFuture(var12);
      }
   }

   public void close() {
      this.resourceManager.close();
   }

   public void updateGlobals() {
      this.dataPackResources.updateRegistryTags(this.registryAccess);
   }

   public CloseableResourceManager resourceManager() {
      return this.resourceManager;
   }

   public ReloadableServerResources dataPackResources() {
      return this.dataPackResources;
   }

   public RegistryAccess.Frozen registryAccess() {
      return this.registryAccess;
   }

   public WorldData worldData() {
      return this.worldData;
   }

   @FunctionalInterface
   public interface DataPackConfigSupplier extends Supplier<DataPackConfig> {
      static WorldStem.DataPackConfigSupplier loadFromWorld(LevelStorageSource.LevelStorageAccess var0) {
         return () -> {
            DataPackConfig var1 = var0.getDataPacks();
            if (var1 == null) {
               throw new IllegalStateException("Failed to load data pack config");
            } else {
               return var1;
            }
         };
      }
   }

   public static record InitConfig(PackRepository a, Commands.CommandSelection b, int c, boolean d) {
      private final PackRepository packRepository;
      private final Commands.CommandSelection commandSelection;
      private final int functionCompilationLevel;
      private final boolean safeMode;

      public InitConfig(PackRepository var1, Commands.CommandSelection var2, int var3, boolean var4) {
         this.packRepository = var1;
         this.commandSelection = var2;
         this.functionCompilationLevel = var3;
         this.safeMode = var4;
      }

      public PackRepository packRepository() {
         return this.packRepository;
      }

      public Commands.CommandSelection commandSelection() {
         return this.commandSelection;
      }

      public int functionCompilationLevel() {
         return this.functionCompilationLevel;
      }

      public boolean safeMode() {
         return this.safeMode;
      }
   }

   @FunctionalInterface
   public interface WorldDataSupplier {
      Pair<WorldData, RegistryAccess.Frozen> get(ResourceManager var1, DataPackConfig var2);

      static WorldStem.WorldDataSupplier loadFromWorld(LevelStorageSource.LevelStorageAccess var0) {
         return (var1, var2) -> {
            RegistryAccess.Writable var3 = RegistryAccess.builtinCopy();
            RegistryOps var4 = RegistryOps.createAndLoad(NbtOps.INSTANCE, var3, (ResourceManager)var1);
            WorldData var5 = var0.getDataTag(var4, var2, var3.allElementsLifecycle());
            if (var5 == null) {
               throw new IllegalStateException("Failed to load world");
            } else {
               return Pair.of(var5, var3.freeze());
            }
         };
      }
   }
}
