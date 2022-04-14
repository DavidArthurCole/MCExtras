package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

public class HumanoidMobRenderer<T extends Mob, M extends HumanoidModel<T>> extends MobRenderer<T, M> {
   private static final ResourceLocation DEFAULT_LOCATION = new ResourceLocation("textures/entity/steve.png");

   public HumanoidMobRenderer(EntityRendererProvider.Context var1, M var2, float var3) {
      this(var1, var2, var3, 1.0F, 1.0F, 1.0F);
   }

   public HumanoidMobRenderer(EntityRendererProvider.Context var1, M var2, float var3, float var4, float var5, float var6) {
      super(var1, var2, var3);
      this.addLayer(new CustomHeadLayer(this, var1.getModelSet(), var4, var5, var6));
      this.addLayer(new ElytraLayer(this, var1.getModelSet()));
      this.addLayer(new ItemInHandLayer(this));
   }

   public ResourceLocation getTextureLocation(T var1) {
      return DEFAULT_LOCATION;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Entity var1) {
      return this.getTextureLocation((Mob)var1);
   }
}