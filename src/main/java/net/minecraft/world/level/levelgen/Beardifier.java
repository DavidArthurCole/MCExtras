package net.minecraft.world.level.levelgen;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Iterator;
import net.minecraft.Util;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.NoiseEffect;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class Beardifier implements DensityFunctions.BeardifierOrMarker {
   public static final int BEARD_KERNEL_RADIUS = 12;
   private static final int BEARD_KERNEL_SIZE = 24;
   private static final float[] BEARD_KERNEL = (float[])Util.make(new float[13824], (var0) -> {
      for(int var1 = 0; var1 < 24; ++var1) {
         for(int var2 = 0; var2 < 24; ++var2) {
            for(int var3 = 0; var3 < 24; ++var3) {
               var0[var1 * 24 * 24 + var2 * 24 + var3] = (float)computeBeardContribution(var2 - 12, var3 - 12, var1 - 12);
            }
         }
      }

   });
   private final ObjectList<StructurePiece> rigids;
   private final ObjectList<JigsawJunction> junctions;
   private final ObjectListIterator<StructurePiece> pieceIterator;
   private final ObjectListIterator<JigsawJunction> junctionIterator;

   protected Beardifier(StructureFeatureManager var1, ChunkAccess var2) {
      ChunkPos var3 = var2.getPos();
      int var4 = var3.getMinBlockX();
      int var5 = var3.getMinBlockZ();
      this.junctions = new ObjectArrayList(32);
      this.rigids = new ObjectArrayList(10);
      var1.startsForFeature(SectionPos.bottomOf(var2), (var0) -> {
         return var0.adaptNoise;
      }).forEach((var4x) -> {
         Iterator var5x = var4x.getPieces().iterator();

         while(true) {
            while(true) {
               StructurePiece var6;
               do {
                  if (!var5x.hasNext()) {
                     return;
                  }

                  var6 = (StructurePiece)var5x.next();
               } while(!var6.isCloseToChunk(var3, 12));

               if (var6 instanceof PoolElementStructurePiece) {
                  PoolElementStructurePiece var7 = (PoolElementStructurePiece)var6;
                  StructureTemplatePool.Projection var8 = var7.getElement().getProjection();
                  if (var8 == StructureTemplatePool.Projection.RIGID) {
                     this.rigids.add(var7);
                  }

                  Iterator var9 = var7.getJunctions().iterator();

                  while(var9.hasNext()) {
                     JigsawJunction var10 = (JigsawJunction)var9.next();
                     int var11 = var10.getSourceX();
                     int var12 = var10.getSourceZ();
                     if (var11 > var4 - 12 && var12 > var5 - 12 && var11 < var4 + 15 + 12 && var12 < var5 + 15 + 12) {
                        this.junctions.add(var10);
                     }
                  }
               } else {
                  this.rigids.add(var6);
               }
            }
         }
      });
      this.pieceIterator = this.rigids.iterator();
      this.junctionIterator = this.junctions.iterator();
   }

   public double compute(DensityFunction.FunctionContext var1) {
      int var2 = var1.blockX();
      int var3 = var1.blockY();
      int var4 = var1.blockZ();
      double var5 = 0.0D;

      int var9;
      int var10;
      while(this.pieceIterator.hasNext()) {
         StructurePiece var7 = (StructurePiece)this.pieceIterator.next();
         BoundingBox var8 = var7.getBoundingBox();
         var9 = Math.max(0, Math.max(var8.minX() - var2, var2 - var8.maxX()));
         var10 = var3 - (var8.minY() + (var7 instanceof PoolElementStructurePiece ? ((PoolElementStructurePiece)var7).getGroundLevelDelta() : 0));
         int var11 = Math.max(0, Math.max(var8.minZ() - var4, var4 - var8.maxZ()));
         NoiseEffect var12 = var7.getNoiseEffect();
         if (var12 == NoiseEffect.BURY) {
            var5 += getBuryContribution(var9, var10, var11);
         } else if (var12 == NoiseEffect.BEARD) {
            var5 += getBeardContribution(var9, var10, var11) * 0.8D;
         }
      }

      this.pieceIterator.back(this.rigids.size());

      while(this.junctionIterator.hasNext()) {
         JigsawJunction var13 = (JigsawJunction)this.junctionIterator.next();
         int var14 = var2 - var13.getSourceX();
         var9 = var3 - var13.getSourceGroundY();
         var10 = var4 - var13.getSourceZ();
         var5 += getBeardContribution(var14, var9, var10) * 0.4D;
      }

      this.junctionIterator.back(this.junctions.size());
      return var5;
   }

   public double minValue() {
      return Double.NEGATIVE_INFINITY;
   }

   public double maxValue() {
      return Double.POSITIVE_INFINITY;
   }

   private static double getBuryContribution(int var0, int var1, int var2) {
      double var3 = Mth.length((double)var0, (double)var1 / 2.0D, (double)var2);
      return Mth.clampedMap(var3, 0.0D, 6.0D, 1.0D, 0.0D);
   }

   private static double getBeardContribution(int var0, int var1, int var2) {
      int var3 = var0 + 12;
      int var4 = var1 + 12;
      int var5 = var2 + 12;
      if (var3 >= 0 && var3 < 24) {
         if (var4 >= 0 && var4 < 24) {
            return var5 >= 0 && var5 < 24 ? (double)BEARD_KERNEL[var5 * 24 * 24 + var3 * 24 + var4] : 0.0D;
         } else {
            return 0.0D;
         }
      } else {
         return 0.0D;
      }
   }

   private static double computeBeardContribution(int var0, int var1, int var2) {
      double var3 = (double)(var0 * var0 + var2 * var2);
      double var5 = (double)var1 + 0.5D;
      double var7 = var5 * var5;
      double var9 = Math.pow(2.718281828459045D, -(var7 / 16.0D + var3 / 16.0D));
      double var11 = -var5 * Mth.fastInvSqrt(var7 / 2.0D + var3 / 2.0D) / 2.0D;
      return var11 * var9;
   }
}