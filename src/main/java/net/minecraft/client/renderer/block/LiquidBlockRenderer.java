package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Iterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LiquidBlockRenderer {
   private static final float MAX_FLUID_HEIGHT = 0.8888889F;
   private final TextureAtlasSprite[] lavaIcons = new TextureAtlasSprite[2];
   private final TextureAtlasSprite[] waterIcons = new TextureAtlasSprite[2];
   private TextureAtlasSprite waterOverlay;

   public LiquidBlockRenderer() {
   }

   protected void setupSprites() {
      this.lavaIcons[0] = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(Blocks.LAVA.defaultBlockState()).getParticleIcon();
      this.lavaIcons[1] = ModelBakery.LAVA_FLOW.sprite();
      this.waterIcons[0] = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(Blocks.WATER.defaultBlockState()).getParticleIcon();
      this.waterIcons[1] = ModelBakery.WATER_FLOW.sprite();
      this.waterOverlay = ModelBakery.WATER_OVERLAY.sprite();
   }

   private static boolean isNeighborSameFluid(FluidState var0, FluidState var1) {
      return var1.getType().isSame(var0.getType());
   }

   private static boolean isFaceOccludedByState(BlockGetter var0, Direction var1, float var2, BlockPos var3, BlockState var4) {
      if (var4.canOcclude()) {
         VoxelShape var5 = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, (double)var2, 1.0D);
         VoxelShape var6 = var4.getOcclusionShape(var0, var3);
         return Shapes.blockOccudes(var5, var6, var1);
      } else {
         return false;
      }
   }

   private static boolean isFaceOccludedByNeighbor(BlockGetter var0, BlockPos var1, Direction var2, float var3, BlockState var4) {
      return isFaceOccludedByState(var0, var2, var3, var1.relative(var2), var4);
   }

   private static boolean isFaceOccludedBySelf(BlockGetter var0, BlockPos var1, BlockState var2, Direction var3) {
      return isFaceOccludedByState(var0, var3.getOpposite(), 1.0F, var1, var2);
   }

   public static boolean shouldRenderFace(BlockAndTintGetter var0, BlockPos var1, FluidState var2, BlockState var3, Direction var4, FluidState var5) {
      return !isFaceOccludedBySelf(var0, var1, var3, var4) && !isNeighborSameFluid(var2, var5);
   }

   public boolean tesselate(BlockAndTintGetter var1, BlockPos var2, VertexConsumer var3, BlockState var4, FluidState var5) {
      boolean var6 = var5.is(FluidTags.LAVA);
      TextureAtlasSprite[] var7 = var6 ? this.lavaIcons : this.waterIcons;
      int var8 = var6 ? 16777215 : BiomeColors.getAverageWaterColor(var1, var2);
      float var9 = (float)(var8 >> 16 & 255) / 255.0F;
      float var10 = (float)(var8 >> 8 & 255) / 255.0F;
      float var11 = (float)(var8 & 255) / 255.0F;
      BlockState var12 = var1.getBlockState(var2.relative(Direction.DOWN));
      FluidState var13 = var12.getFluidState();
      BlockState var14 = var1.getBlockState(var2.relative(Direction.UP));
      FluidState var15 = var14.getFluidState();
      BlockState var16 = var1.getBlockState(var2.relative(Direction.NORTH));
      FluidState var17 = var16.getFluidState();
      BlockState var18 = var1.getBlockState(var2.relative(Direction.SOUTH));
      FluidState var19 = var18.getFluidState();
      BlockState var20 = var1.getBlockState(var2.relative(Direction.WEST));
      FluidState var21 = var20.getFluidState();
      BlockState var22 = var1.getBlockState(var2.relative(Direction.EAST));
      FluidState var23 = var22.getFluidState();
      boolean var24 = !isNeighborSameFluid(var5, var15);
      boolean var25 = shouldRenderFace(var1, var2, var5, var4, Direction.DOWN, var13) && !isFaceOccludedByNeighbor(var1, var2, Direction.DOWN, 0.8888889F, var12);
      boolean var26 = shouldRenderFace(var1, var2, var5, var4, Direction.NORTH, var17);
      boolean var27 = shouldRenderFace(var1, var2, var5, var4, Direction.SOUTH, var19);
      boolean var28 = shouldRenderFace(var1, var2, var5, var4, Direction.WEST, var21);
      boolean var29 = shouldRenderFace(var1, var2, var5, var4, Direction.EAST, var23);
      if (!var24 && !var25 && !var29 && !var28 && !var26 && !var27) {
         return false;
      } else {
         boolean var30 = false;
         float var31 = var1.getShade(Direction.DOWN, true);
         float var32 = var1.getShade(Direction.UP, true);
         float var33 = var1.getShade(Direction.NORTH, true);
         float var34 = var1.getShade(Direction.WEST, true);
         Fluid var35 = var5.getType();
         float var40 = this.getHeight(var1, var35, var2, var4, var5);
         float var36;
         float var37;
         float var38;
         float var39;
         if (var40 >= 1.0F) {
            var36 = 1.0F;
            var37 = 1.0F;
            var38 = 1.0F;
            var39 = 1.0F;
         } else {
            float var41 = this.getHeight(var1, var35, var2.north(), var16, var17);
            float var42 = this.getHeight(var1, var35, var2.south(), var18, var19);
            float var43 = this.getHeight(var1, var35, var2.east(), var22, var23);
            float var44 = this.getHeight(var1, var35, var2.west(), var20, var21);
            var36 = this.calculateAverageHeight(var1, var35, var40, var41, var43, var2.relative(Direction.NORTH).relative(Direction.EAST));
            var37 = this.calculateAverageHeight(var1, var35, var40, var41, var44, var2.relative(Direction.NORTH).relative(Direction.WEST));
            var38 = this.calculateAverageHeight(var1, var35, var40, var42, var43, var2.relative(Direction.SOUTH).relative(Direction.EAST));
            var39 = this.calculateAverageHeight(var1, var35, var40, var42, var44, var2.relative(Direction.SOUTH).relative(Direction.WEST));
         }

         double var74 = (double)(var2.getX() & 15);
         double var75 = (double)(var2.getY() & 15);
         double var45 = (double)(var2.getZ() & 15);
         float var47 = 0.001F;
         float var48 = var25 ? 0.001F : 0.0F;
         float var49;
         float var50;
         float var51;
         float var52;
         float var53;
         float var54;
         float var55;
         float var56;
         float var65;
         float var66;
         if (var24 && !isFaceOccludedByNeighbor(var1, var2, Direction.UP, Math.min(Math.min(var37, var39), Math.min(var38, var36)), var14)) {
            var30 = true;
            var37 -= 0.001F;
            var39 -= 0.001F;
            var38 -= 0.001F;
            var36 -= 0.001F;
            Vec3 var57 = var5.getFlow(var1, var2);
            TextureAtlasSprite var58;
            float var59;
            float var60;
            float var61;
            float var62;
            if (var57.x == 0.0D && var57.z == 0.0D) {
               var58 = var7[0];
               var49 = var58.getU(0.0D);
               var53 = var58.getV(0.0D);
               var50 = var49;
               var54 = var58.getV(16.0D);
               var51 = var58.getU(16.0D);
               var55 = var54;
               var52 = var51;
               var56 = var53;
            } else {
               var58 = var7[1];
               var59 = (float)Mth.atan2(var57.z, var57.x) - 1.5707964F;
               var60 = Mth.sin(var59) * 0.25F;
               var61 = Mth.cos(var59) * 0.25F;
               var62 = 8.0F;
               var49 = var58.getU((double)(8.0F + (-var61 - var60) * 16.0F));
               var53 = var58.getV((double)(8.0F + (-var61 + var60) * 16.0F));
               var50 = var58.getU((double)(8.0F + (-var61 + var60) * 16.0F));
               var54 = var58.getV((double)(8.0F + (var61 + var60) * 16.0F));
               var51 = var58.getU((double)(8.0F + (var61 + var60) * 16.0F));
               var55 = var58.getV((double)(8.0F + (var61 - var60) * 16.0F));
               var52 = var58.getU((double)(8.0F + (var61 - var60) * 16.0F));
               var56 = var58.getV((double)(8.0F + (-var61 - var60) * 16.0F));
            }

            float var82 = (var49 + var50 + var51 + var52) / 4.0F;
            var59 = (var53 + var54 + var55 + var56) / 4.0F;
            var60 = (float)var7[0].getWidth() / (var7[0].getU1() - var7[0].getU0());
            var61 = (float)var7[0].getHeight() / (var7[0].getV1() - var7[0].getV0());
            var62 = 4.0F / Math.max(var61, var60);
            var49 = Mth.lerp(var62, var49, var82);
            var50 = Mth.lerp(var62, var50, var82);
            var51 = Mth.lerp(var62, var51, var82);
            var52 = Mth.lerp(var62, var52, var82);
            var53 = Mth.lerp(var62, var53, var59);
            var54 = Mth.lerp(var62, var54, var59);
            var55 = Mth.lerp(var62, var55, var59);
            var56 = Mth.lerp(var62, var56, var59);
            int var63 = this.getLightColor(var1, var2);
            float var64 = var32 * var9;
            var65 = var32 * var10;
            var66 = var32 * var11;
            this.vertex(var3, var74 + 0.0D, var75 + (double)var37, var45 + 0.0D, var64, var65, var66, var49, var53, var63);
            this.vertex(var3, var74 + 0.0D, var75 + (double)var39, var45 + 1.0D, var64, var65, var66, var50, var54, var63);
            this.vertex(var3, var74 + 1.0D, var75 + (double)var38, var45 + 1.0D, var64, var65, var66, var51, var55, var63);
            this.vertex(var3, var74 + 1.0D, var75 + (double)var36, var45 + 0.0D, var64, var65, var66, var52, var56, var63);
            if (var5.shouldRenderBackwardUpFace(var1, var2.above())) {
               this.vertex(var3, var74 + 0.0D, var75 + (double)var37, var45 + 0.0D, var64, var65, var66, var49, var53, var63);
               this.vertex(var3, var74 + 1.0D, var75 + (double)var36, var45 + 0.0D, var64, var65, var66, var52, var56, var63);
               this.vertex(var3, var74 + 1.0D, var75 + (double)var38, var45 + 1.0D, var64, var65, var66, var51, var55, var63);
               this.vertex(var3, var74 + 0.0D, var75 + (double)var39, var45 + 1.0D, var64, var65, var66, var50, var54, var63);
            }
         }

         if (var25) {
            var49 = var7[0].getU0();
            var50 = var7[0].getU1();
            var51 = var7[0].getV0();
            var52 = var7[0].getV1();
            int var79 = this.getLightColor(var1, var2.below());
            var54 = var31 * var9;
            var55 = var31 * var10;
            var56 = var31 * var11;
            this.vertex(var3, var74, var75 + (double)var48, var45 + 1.0D, var54, var55, var56, var49, var52, var79);
            this.vertex(var3, var74, var75 + (double)var48, var45, var54, var55, var56, var49, var51, var79);
            this.vertex(var3, var74 + 1.0D, var75 + (double)var48, var45, var54, var55, var56, var50, var51, var79);
            this.vertex(var3, var74 + 1.0D, var75 + (double)var48, var45 + 1.0D, var54, var55, var56, var50, var52, var79);
            var30 = true;
         }

         int var76 = this.getLightColor(var1, var2);
         Iterator var77 = Direction.Plane.HORIZONTAL.iterator();

         while(true) {
            Direction var78;
            double var80;
            double var81;
            double var83;
            double var84;
            boolean var85;
            do {
               do {
                  if (!var77.hasNext()) {
                     return var30;
                  }

                  var78 = (Direction)var77.next();
                  switch(var78) {
                  case NORTH:
                     var52 = var37;
                     var53 = var36;
                     var80 = var74;
                     var83 = var74 + 1.0D;
                     var81 = var45 + 0.0010000000474974513D;
                     var84 = var45 + 0.0010000000474974513D;
                     var85 = var26;
                     break;
                  case SOUTH:
                     var52 = var38;
                     var53 = var39;
                     var80 = var74 + 1.0D;
                     var83 = var74;
                     var81 = var45 + 1.0D - 0.0010000000474974513D;
                     var84 = var45 + 1.0D - 0.0010000000474974513D;
                     var85 = var27;
                     break;
                  case WEST:
                     var52 = var39;
                     var53 = var37;
                     var80 = var74 + 0.0010000000474974513D;
                     var83 = var74 + 0.0010000000474974513D;
                     var81 = var45 + 1.0D;
                     var84 = var45;
                     var85 = var28;
                     break;
                  default:
                     var52 = var36;
                     var53 = var38;
                     var80 = var74 + 1.0D - 0.0010000000474974513D;
                     var83 = var74 + 1.0D - 0.0010000000474974513D;
                     var81 = var45;
                     var84 = var45 + 1.0D;
                     var85 = var29;
                  }
               } while(!var85);
            } while(isFaceOccludedByNeighbor(var1, var2, var78, Math.max(var52, var53), var1.getBlockState(var2.relative(var78))));

            var30 = true;
            BlockPos var86 = var2.relative(var78);
            TextureAtlasSprite var87 = var7[1];
            if (!var6) {
               Block var88 = var1.getBlockState(var86).getBlock();
               if (var88 instanceof HalfTransparentBlock || var88 instanceof LeavesBlock) {
                  var87 = this.waterOverlay;
               }
            }

            var65 = var87.getU(0.0D);
            var66 = var87.getU(8.0D);
            float var67 = var87.getV((double)((1.0F - var52) * 16.0F * 0.5F));
            float var68 = var87.getV((double)((1.0F - var53) * 16.0F * 0.5F));
            float var69 = var87.getV(8.0D);
            float var70 = var78.getAxis() == Direction.Axis.Z ? var33 : var34;
            float var71 = var32 * var70 * var9;
            float var72 = var32 * var70 * var10;
            float var73 = var32 * var70 * var11;
            this.vertex(var3, var80, var75 + (double)var52, var81, var71, var72, var73, var65, var67, var76);
            this.vertex(var3, var83, var75 + (double)var53, var84, var71, var72, var73, var66, var68, var76);
            this.vertex(var3, var83, var75 + (double)var48, var84, var71, var72, var73, var66, var69, var76);
            this.vertex(var3, var80, var75 + (double)var48, var81, var71, var72, var73, var65, var69, var76);
            if (var87 != this.waterOverlay) {
               this.vertex(var3, var80, var75 + (double)var48, var81, var71, var72, var73, var65, var69, var76);
               this.vertex(var3, var83, var75 + (double)var48, var84, var71, var72, var73, var66, var69, var76);
               this.vertex(var3, var83, var75 + (double)var53, var84, var71, var72, var73, var66, var68, var76);
               this.vertex(var3, var80, var75 + (double)var52, var81, var71, var72, var73, var65, var67, var76);
            }
         }
      }
   }

   private float calculateAverageHeight(BlockAndTintGetter var1, Fluid var2, float var3, float var4, float var5, BlockPos var6) {
      if (!(var5 >= 1.0F) && !(var4 >= 1.0F)) {
         float[] var7 = new float[2];
         if (var5 > 0.0F || var4 > 0.0F) {
            float var8 = this.getHeight(var1, var2, var6);
            if (var8 >= 1.0F) {
               return 1.0F;
            }

            this.addWeightedHeight(var7, var8);
         }

         this.addWeightedHeight(var7, var3);
         this.addWeightedHeight(var7, var5);
         this.addWeightedHeight(var7, var4);
         return var7[0] / var7[1];
      } else {
         return 1.0F;
      }
   }

   private void addWeightedHeight(float[] var1, float var2) {
      if (var2 >= 0.8F) {
         var1[0] += var2 * 10.0F;
         var1[1] += 10.0F;
      } else if (var2 >= 0.0F) {
         var1[0] += var2;
         int var10002 = var1[1]++;
      }

   }

   private float getHeight(BlockAndTintGetter var1, Fluid var2, BlockPos var3) {
      BlockState var4 = var1.getBlockState(var3);
      return this.getHeight(var1, var2, var3, var4, var4.getFluidState());
   }

   private float getHeight(BlockAndTintGetter var1, Fluid var2, BlockPos var3, BlockState var4, FluidState var5) {
      if (var2.isSame(var5.getType())) {
         BlockState var6 = var1.getBlockState(var3.above());
         return var2.isSame(var6.getFluidState().getType()) ? 1.0F : var5.getOwnHeight();
      } else {
         return !var4.getMaterial().isSolid() ? 0.0F : -1.0F;
      }
   }

   private void vertex(VertexConsumer var1, double var2, double var4, double var6, float var8, float var9, float var10, float var11, float var12, int var13) {
      var1.vertex(var2, var4, var6).color(var8, var9, var10, 1.0F).uv(var11, var12).uv2(var13).normal(0.0F, 1.0F, 0.0F).endVertex();
   }

   private int getLightColor(BlockAndTintGetter var1, BlockPos var2) {
      int var3 = LevelRenderer.getLightColor(var1, var2);
      int var4 = LevelRenderer.getLightColor(var1, var2.above());
      int var5 = var3 & 255;
      int var6 = var4 & 255;
      int var7 = var3 >> 16 & 255;
      int var8 = var4 >> 16 & 255;
      return (var5 > var6 ? var5 : var6) | (var7 > var8 ? var7 : var8) << 16;
   }
}
