package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.TridentModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownTrident;

public class ThrownTridentRenderer extends EntityRenderer<ThrownTrident> {
   public static final ResourceLocation TRIDENT_LOCATION = new ResourceLocation("textures/entity/trident.png");
   private final TridentModel model;

   public ThrownTridentRenderer(EntityRendererProvider.Context var1) {
      super(var1);
      this.model = new TridentModel(var1.bakeLayer(ModelLayers.TRIDENT));
   }

   public void render(ThrownTrident var1, float var2, float var3, PoseStack var4, MultiBufferSource var5, int var6) {
      var4.pushPose();
      var4.mulPose(Vector3f.YP.rotationDegrees(Mth.lerp(var3, var1.yRotO, var1.getYRot()) - 90.0F));
      var4.mulPose(Vector3f.ZP.rotationDegrees(Mth.lerp(var3, var1.xRotO, var1.getXRot()) + 90.0F));
      VertexConsumer var7 = ItemRenderer.getFoilBufferDirect(var5, this.model.renderType(this.getTextureLocation(var1)), false, var1.isFoil());
      this.model.renderToBuffer(var4, var7, var6, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
      var4.popPose();
      super.render(var1, var2, var3, var4, var5, var6);
   }

   public ResourceLocation getTextureLocation(ThrownTrident var1) {
      return TRIDENT_LOCATION;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Entity var1) {
      return this.getTextureLocation((ThrownTrident)var1);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void render(Entity var1, float var2, float var3, PoseStack var4, MultiBufferSource var5, int var6) {
      this.render((ThrownTrident)var1, var2, var3, var4, var5, var6);
   }
}
