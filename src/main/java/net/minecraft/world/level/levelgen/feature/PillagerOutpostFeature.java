package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;

public class PillagerOutpostFeature extends JigsawFeature {
   public PillagerOutpostFeature(Codec<JigsawConfiguration> var1) {
      super(var1, 0, true, true, PillagerOutpostFeature::checkLocation);
   }

   private static boolean checkLocation(PieceGeneratorSupplier.Context<JigsawConfiguration> var0) {
      ChunkPos var1 = var0.chunkPos();
      int var2 = var1.x >> 4;
      int var3 = var1.z >> 4;
      WorldgenRandom var4 = new WorldgenRandom(new LegacyRandomSource(0L));
      var4.setSeed((long)(var2 ^ var3 << 4) ^ var0.seed());
      var4.nextInt();
      if (var4.nextInt(5) != 0) {
         return false;
      } else {
         return !var0.chunkGenerator().hasFeatureChunkInRange(BuiltinStructureSets.VILLAGES, var0.seed(), var1.x, var1.z, 10);
      }
   }
}
