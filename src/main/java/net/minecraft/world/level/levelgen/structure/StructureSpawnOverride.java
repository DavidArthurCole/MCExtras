package net.minecraft.world.level.levelgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.level.biome.MobSpawnSettings;

public record StructureSpawnOverride(StructureSpawnOverride.BoundingBoxType b, WeightedRandomList<MobSpawnSettings.SpawnerData> c) {
   private final StructureSpawnOverride.BoundingBoxType boundingBox;
   private final WeightedRandomList<MobSpawnSettings.SpawnerData> spawns;
   public static final Codec<StructureSpawnOverride> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(StructureSpawnOverride.BoundingBoxType.CODEC.fieldOf("bounding_box").forGetter(StructureSpawnOverride::boundingBox), WeightedRandomList.codec(MobSpawnSettings.SpawnerData.CODEC).fieldOf("spawns").forGetter(StructureSpawnOverride::spawns)).apply(var0, StructureSpawnOverride::new);
   });

   public StructureSpawnOverride(StructureSpawnOverride.BoundingBoxType var1, WeightedRandomList<MobSpawnSettings.SpawnerData> var2) {
      this.boundingBox = var1;
      this.spawns = var2;
   }

   public StructureSpawnOverride.BoundingBoxType boundingBox() {
      return this.boundingBox;
   }

   public WeightedRandomList<MobSpawnSettings.SpawnerData> spawns() {
      return this.spawns;
   }

   public static enum BoundingBoxType implements StringRepresentable {
      PIECE("piece"),
      STRUCTURE("full");

      public static final StructureSpawnOverride.BoundingBoxType[] VALUES = values();
      public static final Codec<StructureSpawnOverride.BoundingBoxType> CODEC = StringRepresentable.fromEnum(() -> {
         return VALUES;
      }, StructureSpawnOverride.BoundingBoxType::byName);
      private final String id;

      private BoundingBoxType(String var3) {
         this.id = var3;
      }

      public String getSerializedName() {
         return this.id;
      }

      @Nullable
      public static StructureSpawnOverride.BoundingBoxType byName(@Nullable String var0) {
         if (var0 == null) {
            return null;
         } else {
            StructureSpawnOverride.BoundingBoxType[] var1 = VALUES;
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
               StructureSpawnOverride.BoundingBoxType var4 = var1[var3];
               if (var4.id.equals(var0)) {
                  return var4;
               }
            }

            return null;
         }
      }

      // $FF: synthetic method
      private static StructureSpawnOverride.BoundingBoxType[] $values() {
         return new StructureSpawnOverride.BoundingBoxType[]{PIECE, STRUCTURE};
      }
   }
}
