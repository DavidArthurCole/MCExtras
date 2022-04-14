package net.minecraft.world.level.storage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.nbt.visitors.SkipFields;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.DirectoryLock;
import net.minecraft.util.MemoryReserve;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.slf4j.Logger;

public class LevelStorageSource {
   static final Logger LOGGER = LogUtils.getLogger();
   static final DateTimeFormatter FORMATTER;
   private static final String ICON_FILENAME = "icon.png";
   private static final ImmutableList<String> OLD_SETTINGS_KEYS;
   private static final String TAG_DATA = "Data";
   final Path baseDir;
   private final Path backupDir;
   final DataFixer fixerUpper;

   public LevelStorageSource(Path var1, Path var2, DataFixer var3) {
      this.fixerUpper = var3;

      try {
         Files.createDirectories(Files.exists(var1, new LinkOption[0]) ? var1.toRealPath() : var1);
      } catch (IOException var5) {
         throw new RuntimeException(var5);
      }

      this.baseDir = var1;
      this.backupDir = var2;
   }

   public static LevelStorageSource createDefault(Path var0) {
      return new LevelStorageSource(var0, var0.resolve("../backups"), DataFixers.getDataFixer());
   }

   private static <T> Pair<WorldGenSettings, Lifecycle> readWorldGenSettings(Dynamic<T> var0, DataFixer var1, int var2) {
      Dynamic var3 = var0.get("WorldGenSettings").orElseEmptyMap();
      UnmodifiableIterator var4 = OLD_SETTINGS_KEYS.iterator();

      while(var4.hasNext()) {
         String var5 = (String)var4.next();
         Optional var6 = var0.get(var5).result();
         if (var6.isPresent()) {
            var3 = var3.set(var5, (Dynamic)var6.get());
         }
      }

      Dynamic var8 = var1.update(References.WORLD_GEN_SETTINGS, var3, var2, SharedConstants.getCurrentVersion().getWorldVersion());
      DataResult var7 = WorldGenSettings.CODEC.parse(var8);
      Logger var10002 = LOGGER;
      Objects.requireNonNull(var10002);
      return Pair.of((WorldGenSettings)var7.resultOrPartial(Util.prefix("WorldGenSettings: ", var10002::error)).orElseGet(() -> {
         RegistryAccess var1 = RegistryAccess.readFromDisk(var8);
         return WorldGenSettings.makeDefault(var1);
      }), var7.lifecycle());
   }

   private static DataPackConfig readDataPackConfig(Dynamic<?> var0) {
      DataResult var10000 = DataPackConfig.CODEC.parse(var0);
      Logger var10001 = LOGGER;
      Objects.requireNonNull(var10001);
      return (DataPackConfig)var10000.resultOrPartial(var10001::error).orElse(DataPackConfig.DEFAULT);
   }

   public String getName() {
      return "Anvil";
   }

   public List<LevelSummary> getLevelList() throws LevelStorageException {
      if (!Files.isDirectory(this.baseDir, new LinkOption[0])) {
         throw new LevelStorageException((new TranslatableComponent("selectWorld.load_folder_access")).getString());
      } else {
         ArrayList var1 = Lists.newArrayList();
         File[] var2 = this.baseDir.toFile().listFiles();
         File[] var3 = var2;
         int var4 = var2.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            File var6 = var3[var5];
            if (var6.isDirectory()) {
               boolean var7;
               try {
                  var7 = DirectoryLock.isLocked(var6.toPath());
               } catch (Exception var14) {
                  LOGGER.warn("Failed to read {} lock", var6, var14);
                  continue;
               }

               try {
                  LevelSummary var8 = (LevelSummary)this.readLevelData(var6, this.levelSummaryReader(var6, var7));
                  if (var8 != null) {
                     var1.add(var8);
                  }
               } catch (OutOfMemoryError var12) {
                  MemoryReserve.release();
                  System.gc();
                  LOGGER.error(LogUtils.FATAL_MARKER, "Ran out of memory trying to read summary of {}", var6);
                  throw var12;
               } catch (StackOverflowError var13) {
                  LOGGER.error(LogUtils.FATAL_MARKER, "Ran out of stack trying to read summary of {}. Assuming corruption; attempting to restore from from level.dat_old.", var6);
                  File var9 = new File(var6, "level.dat");
                  File var10 = new File(var6, "level.dat_old");
                  LocalDateTime var10003 = LocalDateTime.now();
                  File var11 = new File(var6, "level.dat_corrupted_" + var10003.format(FORMATTER));
                  Util.safeReplaceOrMoveFile(var9, var10, var11, true);
                  throw var13;
               }
            }
         }

         return var1;
      }
   }

   private int getStorageVersion() {
      return 19133;
   }

   @Nullable
   <T> T readLevelData(File var1, BiFunction<File, DataFixer, T> var2) {
      if (!var1.exists()) {
         return null;
      } else {
         File var3 = new File(var1, "level.dat");
         if (var3.exists()) {
            Object var4 = var2.apply(var3, this.fixerUpper);
            if (var4 != null) {
               return var4;
            }
         }

         var3 = new File(var1, "level.dat_old");
         return var3.exists() ? var2.apply(var3, this.fixerUpper) : null;
      }
   }

   @Nullable
   private static DataPackConfig getDataPacks(File var0, DataFixer var1) {
      try {
         Tag var2 = readLightweightData(var0);
         if (var2 instanceof CompoundTag) {
            CompoundTag var3 = (CompoundTag)var2;
            CompoundTag var4 = var3.getCompound("Data");
            int var5 = var4.contains("DataVersion", 99) ? var4.getInt("DataVersion") : -1;
            Dynamic var6 = var1.update(DataFixTypes.LEVEL.getType(), new Dynamic(NbtOps.INSTANCE, var4), var5, SharedConstants.getCurrentVersion().getWorldVersion());
            return (DataPackConfig)var6.get("DataPacks").result().map(LevelStorageSource::readDataPackConfig).orElse(DataPackConfig.DEFAULT);
         }
      } catch (Exception var7) {
         LOGGER.error("Exception reading {}", var0, var7);
      }

      return null;
   }

   static BiFunction<File, DataFixer, PrimaryLevelData> getLevelData(DynamicOps<Tag> var0, DataPackConfig var1, Lifecycle var2) {
      return (var3, var4) -> {
         try {
            CompoundTag var5 = NbtIo.readCompressed(var3);
            CompoundTag var6 = var5.getCompound("Data");
            CompoundTag var7 = var6.contains("Player", 10) ? var6.getCompound("Player") : null;
            var6.remove("Player");
            int var8 = var6.contains("DataVersion", 99) ? var6.getInt("DataVersion") : -1;
            Dynamic var9 = var4.update(DataFixTypes.LEVEL.getType(), new Dynamic(var0, var6), var8, SharedConstants.getCurrentVersion().getWorldVersion());
            Pair var10 = readWorldGenSettings(var9, var4, var8);
            LevelVersion var11 = LevelVersion.parse(var9);
            LevelSettings var12 = LevelSettings.parse(var9, var1);
            Lifecycle var13 = ((Lifecycle)var10.getSecond()).add(var2);
            return PrimaryLevelData.parse(var9, var4, var8, var7, var12, var11, (WorldGenSettings)var10.getFirst(), var13);
         } catch (Exception var14) {
            LOGGER.error("Exception reading {}", var3, var14);
            return null;
         }
      };
   }

   BiFunction<File, DataFixer, LevelSummary> levelSummaryReader(File var1, boolean var2) {
      return (var3, var4) -> {
         try {
            Tag var5 = readLightweightData(var3);
            if (var5 instanceof CompoundTag) {
               CompoundTag var6 = (CompoundTag)var5;
               CompoundTag var7 = var6.getCompound("Data");
               int var8 = var7.contains("DataVersion", 99) ? var7.getInt("DataVersion") : -1;
               Dynamic var9 = var4.update(DataFixTypes.LEVEL.getType(), new Dynamic(NbtOps.INSTANCE, var7), var8, SharedConstants.getCurrentVersion().getWorldVersion());
               LevelVersion var10 = LevelVersion.parse(var9);
               int var11 = var10.levelDataVersion();
               if (var11 == 19132 || var11 == 19133) {
                  boolean var12 = var11 != this.getStorageVersion();
                  File var13 = new File(var1, "icon.png");
                  DataPackConfig var14 = (DataPackConfig)var9.get("DataPacks").result().map(LevelStorageSource::readDataPackConfig).orElse(DataPackConfig.DEFAULT);
                  LevelSettings var15 = LevelSettings.parse(var9, var14);
                  return new LevelSummary(var15, var10, var1.getName(), var12, var2, var13);
               }
            } else {
               LOGGER.warn("Invalid root tag in {}", var3);
            }

            return null;
         } catch (Exception var16) {
            LOGGER.error("Exception reading {}", var3, var16);
            return null;
         }
      };
   }

   @Nullable
   private static Tag readLightweightData(File var0) throws IOException {
      SkipFields var1 = new SkipFields(new FieldSelector[]{new FieldSelector("Data", CompoundTag.TYPE, "Player"), new FieldSelector("Data", CompoundTag.TYPE, "WorldGenSettings")});
      NbtIo.parseCompressed((File)var0, var1);
      return var1.getResult();
   }

   public boolean isNewLevelIdAcceptable(String var1) {
      try {
         Path var2 = this.baseDir.resolve(var1);
         Files.createDirectory(var2);
         Files.deleteIfExists(var2);
         return true;
      } catch (IOException var3) {
         return false;
      }
   }

   public boolean levelExists(String var1) {
      return Files.isDirectory(this.baseDir.resolve(var1), new LinkOption[0]);
   }

   public Path getBaseDir() {
      return this.baseDir;
   }

   public Path getBackupPath() {
      return this.backupDir;
   }

   public LevelStorageSource.LevelStorageAccess createAccess(String var1) throws IOException {
      return new LevelStorageSource.LevelStorageAccess(var1);
   }

   static {
      FORMATTER = (new DateTimeFormatterBuilder()).appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD).appendLiteral('-').appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral('-').appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral('_').appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral('-').appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral('-').appendValue(ChronoField.SECOND_OF_MINUTE, 2).toFormatter();
      OLD_SETTINGS_KEYS = ImmutableList.of("RandomSeed", "generatorName", "generatorOptions", "generatorVersion", "legacy_custom_options", "MapFeatures", "BonusChest");
   }

   public class LevelStorageAccess implements AutoCloseable {
      final DirectoryLock lock;
      final Path levelPath;
      private final String levelId;
      private final Map<LevelResource, Path> resources = Maps.newHashMap();

      public LevelStorageAccess(String var2) throws IOException {
         this.levelId = var2;
         this.levelPath = LevelStorageSource.this.baseDir.resolve(var2);
         this.lock = DirectoryLock.create(this.levelPath);
      }

      public String getLevelId() {
         return this.levelId;
      }

      public Path getLevelPath(LevelResource var1) {
         return (Path)this.resources.computeIfAbsent(var1, (var1x) -> {
            return this.levelPath.resolve(var1x.getId());
         });
      }

      public Path getDimensionPath(ResourceKey<Level> var1) {
         return DimensionType.getStorageFolder(var1, this.levelPath);
      }

      private void checkLock() {
         if (!this.lock.isValid()) {
            throw new IllegalStateException("Lock is no longer valid");
         }
      }

      public PlayerDataStorage createPlayerStorage() {
         this.checkLock();
         return new PlayerDataStorage(this, LevelStorageSource.this.fixerUpper);
      }

      @Nullable
      public LevelSummary getSummary() {
         this.checkLock();
         return (LevelSummary)LevelStorageSource.this.readLevelData(this.levelPath.toFile(), LevelStorageSource.this.levelSummaryReader(this.levelPath.toFile(), false));
      }

      @Nullable
      public WorldData getDataTag(DynamicOps<Tag> var1, DataPackConfig var2, Lifecycle var3) {
         this.checkLock();
         return (WorldData)LevelStorageSource.this.readLevelData(this.levelPath.toFile(), LevelStorageSource.getLevelData(var1, var2, var3));
      }

      @Nullable
      public DataPackConfig getDataPacks() {
         this.checkLock();
         return (DataPackConfig)LevelStorageSource.this.readLevelData(this.levelPath.toFile(), LevelStorageSource::getDataPacks);
      }

      public void saveDataTag(RegistryAccess var1, WorldData var2) {
         this.saveDataTag(var1, var2, (CompoundTag)null);
      }

      public void saveDataTag(RegistryAccess var1, WorldData var2, @Nullable CompoundTag var3) {
         File var4 = this.levelPath.toFile();
         CompoundTag var5 = var2.createTag(var1, var3);
         CompoundTag var6 = new CompoundTag();
         var6.put("Data", var5);

         try {
            File var7 = File.createTempFile("level", ".dat", var4);
            NbtIo.writeCompressed(var6, var7);
            File var8 = new File(var4, "level.dat_old");
            File var9 = new File(var4, "level.dat");
            Util.safeReplaceFile(var9, var7, var8);
         } catch (Exception var10) {
            LevelStorageSource.LOGGER.error("Failed to save level {}", var4, var10);
         }

      }

      public Optional<Path> getIconFile() {
         return !this.lock.isValid() ? Optional.empty() : Optional.of(this.levelPath.resolve("icon.png"));
      }

      public void deleteLevel() throws IOException {
         this.checkLock();
         final Path var1 = this.levelPath.resolve("session.lock");
         LevelStorageSource.LOGGER.info("Deleting level {}", this.levelId);
         int var2 = 1;

         while(var2 <= 5) {
            LevelStorageSource.LOGGER.info("Attempt {}...", var2);

            try {
               Files.walkFileTree(this.levelPath, new SimpleFileVisitor<Path>() {
                  public FileVisitResult visitFile(Path var1x, BasicFileAttributes var2) throws IOException {
                     if (!var1x.equals(var1)) {
                        LevelStorageSource.LOGGER.debug("Deleting {}", var1x);
                        Files.delete(var1x);
                     }

                     return FileVisitResult.CONTINUE;
                  }

                  public FileVisitResult postVisitDirectory(Path var1x, IOException var2) throws IOException {
                     if (var2 != null) {
                        throw var2;
                     } else {
                        if (var1x.equals(LevelStorageAccess.this.levelPath)) {
                           LevelStorageAccess.this.lock.close();
                           Files.deleteIfExists(var1);
                        }

                        Files.delete(var1x);
                        return FileVisitResult.CONTINUE;
                     }
                  }

                  // $FF: synthetic method
                  public FileVisitResult postVisitDirectory(Object var1x, IOException var2) throws IOException {
                     return this.postVisitDirectory((Path)var1x, var2);
                  }

                  // $FF: synthetic method
                  public FileVisitResult visitFile(Object var1x, BasicFileAttributes var2) throws IOException {
                     return this.visitFile((Path)var1x, var2);
                  }
               });
               break;
            } catch (IOException var6) {
               if (var2 >= 5) {
                  throw var6;
               }

               LevelStorageSource.LOGGER.warn("Failed to delete {}", this.levelPath, var6);

               try {
                  Thread.sleep(500L);
               } catch (InterruptedException var5) {
               }

               ++var2;
            }
         }

      }

      public void renameLevel(String var1) throws IOException {
         this.checkLock();
         File var2 = new File(LevelStorageSource.this.baseDir.toFile(), this.levelId);
         if (var2.exists()) {
            File var3 = new File(var2, "level.dat");
            if (var3.exists()) {
               CompoundTag var4 = NbtIo.readCompressed(var3);
               CompoundTag var5 = var4.getCompound("Data");
               var5.putString("LevelName", var1);
               NbtIo.writeCompressed(var4, var3);
            }

         }
      }

      public long makeWorldBackup() throws IOException {
         this.checkLock();
         String var10000 = LocalDateTime.now().format(LevelStorageSource.FORMATTER);
         String var1 = var10000 + "_" + this.levelId;
         Path var2 = LevelStorageSource.this.getBackupPath();

         try {
            Files.createDirectories(Files.exists(var2, new LinkOption[0]) ? var2.toRealPath() : var2);
         } catch (IOException var9) {
            throw new RuntimeException(var9);
         }

         Path var3 = var2.resolve(FileUtil.findAvailableName(var2, var1, ".zip"));
         final ZipOutputStream var4 = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(var3)));

         try {
            final Path var5 = Paths.get(this.levelId);
            Files.walkFileTree(this.levelPath, new SimpleFileVisitor<Path>() {
               public FileVisitResult visitFile(Path var1, BasicFileAttributes var2) throws IOException {
                  if (var1.endsWith("session.lock")) {
                     return FileVisitResult.CONTINUE;
                  } else {
                     String var3 = var5.resolve(LevelStorageAccess.this.levelPath.relativize(var1)).toString().replace('\\', '/');
                     ZipEntry var4x = new ZipEntry(var3);
                     var4.putNextEntry(var4x);
                     com.google.common.io.Files.asByteSource(var1.toFile()).copyTo(var4);
                     var4.closeEntry();
                     return FileVisitResult.CONTINUE;
                  }
               }

               // $FF: synthetic method
               public FileVisitResult visitFile(Object var1, BasicFileAttributes var2) throws IOException {
                  return this.visitFile((Path)var1, var2);
               }
            });
         } catch (Throwable var8) {
            try {
               var4.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }

            throw var8;
         }

         var4.close();
         return Files.size(var3);
      }

      public void close() throws IOException {
         this.lock.close();
      }
   }
}