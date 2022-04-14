package net.minecraft.client.gui.screens;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.slf4j.Logger;

public class PresetFlatWorldScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SLOT_TEX_SIZE = 128;
   private static final int SLOT_BG_SIZE = 18;
   private static final int SLOT_STAT_HEIGHT = 20;
   private static final int SLOT_BG_X = 1;
   private static final int SLOT_BG_Y = 1;
   private static final int SLOT_FG_X = 2;
   private static final int SLOT_FG_Y = 2;
   static final List<PresetFlatWorldScreen.PresetInfo> PRESETS = Lists.newArrayList();
   private static final ResourceKey<Biome> DEFAULT_BIOME;
   final CreateFlatWorldScreen parent;
   private Component shareText;
   private Component listText;
   private PresetFlatWorldScreen.PresetsList list;
   private Button selectButton;
   EditBox export;
   FlatLevelGeneratorSettings settings;

   public PresetFlatWorldScreen(CreateFlatWorldScreen var1) {
      super(new TranslatableComponent("createWorld.customize.presets.title"));
      this.parent = var1;
   }

   @Nullable
   private static FlatLayerInfo getLayerInfoFromString(String var0, int var1) {
      String[] var2 = var0.split("\\*", 2);
      int var3;
      if (var2.length == 2) {
         try {
            var3 = Math.max(Integer.parseInt(var2[0]), 0);
         } catch (NumberFormatException var10) {
            LOGGER.error("Error while parsing flat world string => {}", var10.getMessage());
            return null;
         }
      } else {
         var3 = 1;
      }

      int var4 = Math.min(var1 + var3, DimensionType.Y_SIZE);
      int var5 = var4 - var1;
      String var7 = var2[var2.length - 1];

      Block var6;
      try {
         var6 = (Block)Registry.BLOCK.getOptional(new ResourceLocation(var7)).orElse((Object)null);
      } catch (Exception var9) {
         LOGGER.error("Error while parsing flat world string => {}", var9.getMessage());
         return null;
      }

      if (var6 == null) {
         LOGGER.error("Error while parsing flat world string => Unknown block, {}", var7);
         return null;
      } else {
         return new FlatLayerInfo(var5, var6);
      }
   }

   private static List<FlatLayerInfo> getLayersInfoFromString(String var0) {
      ArrayList var1 = Lists.newArrayList();
      String[] var2 = var0.split(",");
      int var3 = 0;
      String[] var4 = var2;
      int var5 = var2.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         String var7 = var4[var6];
         FlatLayerInfo var8 = getLayerInfoFromString(var7, var3);
         if (var8 == null) {
            return Collections.emptyList();
         }

         var1.add(var8);
         var3 += var8.getHeight();
      }

      return var1;
   }

   public static FlatLevelGeneratorSettings fromString(Registry<Biome> var0, Registry<StructureSet> var1, String var2, FlatLevelGeneratorSettings var3) {
      Iterator var4 = Splitter.on(';').split(var2).iterator();
      if (!var4.hasNext()) {
         return FlatLevelGeneratorSettings.getDefault(var0, var1);
      } else {
         List var5 = getLayersInfoFromString((String)var4.next());
         if (var5.isEmpty()) {
            return FlatLevelGeneratorSettings.getDefault(var0, var1);
         } else {
            FlatLevelGeneratorSettings var6 = var3.withLayers(var5, var3.structureOverrides());
            ResourceKey var7 = DEFAULT_BIOME;
            if (var4.hasNext()) {
               try {
                  ResourceLocation var8 = new ResourceLocation((String)var4.next());
                  var7 = ResourceKey.create(Registry.BIOME_REGISTRY, var8);
                  var0.getOptional(var7).orElseThrow(() -> {
                     return new IllegalArgumentException("Invalid Biome: " + var8);
                  });
               } catch (Exception var9) {
                  LOGGER.error("Error while parsing flat world string => {}", var9.getMessage());
                  var7 = DEFAULT_BIOME;
               }
            }

            var6.setBiome(var0.getOrCreateHolder(var7));
            return var6;
         }
      }
   }

   static String save(FlatLevelGeneratorSettings var0) {
      StringBuilder var1 = new StringBuilder();

      for(int var2 = 0; var2 < var0.getLayersInfo().size(); ++var2) {
         if (var2 > 0) {
            var1.append(",");
         }

         var1.append(var0.getLayersInfo().get(var2));
      }

      var1.append(";");
      var1.append(var0.getBiome().unwrapKey().map(ResourceKey::location).orElseThrow(() -> {
         return new IllegalStateException("Biome not registered");
      }));
      return var1.toString();
   }

   protected void init() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
      this.shareText = new TranslatableComponent("createWorld.customize.presets.share");
      this.listText = new TranslatableComponent("createWorld.customize.presets.list");
      this.export = new EditBox(this.font, 50, 40, this.width - 100, 20, this.shareText);
      this.export.setMaxLength(1230);
      RegistryAccess var1 = this.parent.parent.worldGenSettingsComponent.registryHolder();
      Registry var2 = var1.registryOrThrow(Registry.BIOME_REGISTRY);
      Registry var3 = var1.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
      this.export.setValue(save(this.parent.settings()));
      this.settings = this.parent.settings();
      this.addWidget(this.export);
      this.list = new PresetFlatWorldScreen.PresetsList();
      this.addWidget(this.list);
      this.selectButton = (Button)this.addRenderableWidget(new Button(this.width / 2 - 155, this.height - 28, 150, 20, new TranslatableComponent("createWorld.customize.presets.select"), (var3x) -> {
         FlatLevelGeneratorSettings var4 = fromString(var2, var3, this.export.getValue(), this.settings);
         this.parent.setConfig(var4);
         this.minecraft.setScreen(this.parent);
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 5, this.height - 28, 150, 20, CommonComponents.GUI_CANCEL, (var1x) -> {
         this.minecraft.setScreen(this.parent);
      }));
      this.updateButtonValidity(this.list.getSelected() != null);
   }

   public boolean mouseScrolled(double var1, double var3, double var5) {
      return this.list.mouseScrolled(var1, var3, var5);
   }

   public void resize(Minecraft var1, int var2, int var3) {
      String var4 = this.export.getValue();
      this.init(var1, var2, var3);
      this.export.setValue(var4);
   }

   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   public void removed() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
   }

   public void render(PoseStack var1, int var2, int var3, float var4) {
      this.renderBackground(var1);
      this.list.render(var1, var2, var3, var4);
      var1.pushPose();
      var1.translate(0.0D, 0.0D, 400.0D);
      drawCenteredString(var1, this.font, this.title, this.width / 2, 8, 16777215);
      drawString(var1, this.font, this.shareText, 50, 30, 10526880);
      drawString(var1, this.font, this.listText, 50, 70, 10526880);
      var1.popPose();
      this.export.render(var1, var2, var3, var4);
      super.render(var1, var2, var3, var4);
   }

   public void tick() {
      this.export.tick();
      super.tick();
   }

   public void updateButtonValidity(boolean var1) {
      this.selectButton.active = var1 || this.export.getValue().length() > 1;
   }

   private static void preset(Component var0, ItemLike var1, ResourceKey<Biome> var2, Set<ResourceKey<StructureSet>> var3, boolean var4, boolean var5, FlatLayerInfo... var6) {
      PRESETS.add(new PresetFlatWorldScreen.PresetInfo(var1.asItem(), var0, (var5x) -> {
         Registry var6x = var5x.registryOrThrow(Registry.BIOME_REGISTRY);
         Registry var7 = var5x.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
         HolderSet.Direct var8 = HolderSet.direct((List)var3.stream().flatMap((var1) -> {
            return var7.getHolder(var1).stream();
         }).collect(Collectors.toList()));
         FlatLevelGeneratorSettings var9 = new FlatLevelGeneratorSettings(Optional.of(var8), var6x);
         if (var4) {
            var9.setDecoration();
         }

         if (var5) {
            var9.setAddLakes();
         }

         for(int var10 = var6.length - 1; var10 >= 0; --var10) {
            var9.getLayersInfo().add(var6[var10]);
         }

         var9.setBiome(var6x.getOrCreateHolder(var2));
         var9.updateLayers();
         return var9;
      }));
   }

   static {
      DEFAULT_BIOME = Biomes.PLAINS;
      preset(new TranslatableComponent("createWorld.customize.preset.classic_flat"), Blocks.GRASS_BLOCK, Biomes.PLAINS, Set.of(BuiltinStructureSets.VILLAGES), false, false, new FlatLayerInfo(1, Blocks.GRASS_BLOCK), new FlatLayerInfo(2, Blocks.DIRT), new FlatLayerInfo(1, Blocks.BEDROCK));
      preset(new TranslatableComponent("createWorld.customize.preset.tunnelers_dream"), Blocks.STONE, Biomes.WINDSWEPT_HILLS, Set.of(BuiltinStructureSets.MINESHAFTS, BuiltinStructureSets.STRONGHOLDS), true, false, new FlatLayerInfo(1, Blocks.GRASS_BLOCK), new FlatLayerInfo(5, Blocks.DIRT), new FlatLayerInfo(230, Blocks.STONE), new FlatLayerInfo(1, Blocks.BEDROCK));
      preset(new TranslatableComponent("createWorld.customize.preset.water_world"), Items.WATER_BUCKET, Biomes.DEEP_OCEAN, Set.of(BuiltinStructureSets.OCEAN_RUINS, BuiltinStructureSets.SHIPWRECKS, BuiltinStructureSets.OCEAN_MONUMENTS), false, false, new FlatLayerInfo(90, Blocks.WATER), new FlatLayerInfo(5, Blocks.GRAVEL), new FlatLayerInfo(5, Blocks.DIRT), new FlatLayerInfo(5, Blocks.STONE), new FlatLayerInfo(64, Blocks.DEEPSLATE), new FlatLayerInfo(1, Blocks.BEDROCK));
      preset(new TranslatableComponent("createWorld.customize.preset.overworld"), Blocks.GRASS, Biomes.PLAINS, Set.of(BuiltinStructureSets.VILLAGES, BuiltinStructureSets.MINESHAFTS, BuiltinStructureSets.PILLAGER_OUTPOSTS, BuiltinStructureSets.RUINED_PORTALS, BuiltinStructureSets.STRONGHOLDS), true, true, new FlatLayerInfo(1, Blocks.GRASS_BLOCK), new FlatLayerInfo(3, Blocks.DIRT), new FlatLayerInfo(59, Blocks.STONE), new FlatLayerInfo(1, Blocks.BEDROCK));
      preset(new TranslatableComponent("createWorld.customize.preset.snowy_kingdom"), Blocks.SNOW, Biomes.SNOWY_PLAINS, Set.of(BuiltinStructureSets.VILLAGES, BuiltinStructureSets.IGLOOS), false, false, new FlatLayerInfo(1, Blocks.SNOW), new FlatLayerInfo(1, Blocks.GRASS_BLOCK), new FlatLayerInfo(3, Blocks.DIRT), new FlatLayerInfo(59, Blocks.STONE), new FlatLayerInfo(1, Blocks.BEDROCK));
      preset(new TranslatableComponent("createWorld.customize.preset.bottomless_pit"), Items.FEATHER, Biomes.PLAINS, Set.of(BuiltinStructureSets.VILLAGES), false, false, new FlatLayerInfo(1, Blocks.GRASS_BLOCK), new FlatLayerInfo(3, Blocks.DIRT), new FlatLayerInfo(2, Blocks.COBBLESTONE));
      preset(new TranslatableComponent("createWorld.customize.preset.desert"), Blocks.SAND, Biomes.DESERT, Set.of(BuiltinStructureSets.VILLAGES, BuiltinStructureSets.DESERT_PYRAMIDS, BuiltinStructureSets.MINESHAFTS, BuiltinStructureSets.STRONGHOLDS), true, false, new FlatLayerInfo(8, Blocks.SAND), new FlatLayerInfo(52, Blocks.SANDSTONE), new FlatLayerInfo(3, Blocks.STONE), new FlatLayerInfo(1, Blocks.BEDROCK));
      preset(new TranslatableComponent("createWorld.customize.preset.redstone_ready"), Items.REDSTONE, Biomes.DESERT, Set.of(), false, false, new FlatLayerInfo(116, Blocks.SANDSTONE), new FlatLayerInfo(3, Blocks.STONE), new FlatLayerInfo(1, Blocks.BEDROCK));
      preset(new TranslatableComponent("createWorld.customize.preset.the_void"), Blocks.BARRIER, Biomes.THE_VOID, Set.of(), true, false, new FlatLayerInfo(1, Blocks.AIR));
   }

   private class PresetsList extends ObjectSelectionList<PresetFlatWorldScreen.PresetsList.Entry> {
      public PresetsList() {
         super(PresetFlatWorldScreen.this.minecraft, PresetFlatWorldScreen.this.width, PresetFlatWorldScreen.this.height, 80, PresetFlatWorldScreen.this.height - 37, 24);
         Iterator var2 = PresetFlatWorldScreen.PRESETS.iterator();

         while(var2.hasNext()) {
            PresetFlatWorldScreen.PresetInfo var3 = (PresetFlatWorldScreen.PresetInfo)var2.next();
            this.addEntry(new PresetFlatWorldScreen.PresetsList.Entry(var3));
         }

      }

      public void setSelected(@Nullable PresetFlatWorldScreen.PresetsList.Entry var1) {
         super.setSelected(var1);
         PresetFlatWorldScreen.this.updateButtonValidity(var1 != null);
      }

      protected boolean isFocused() {
         return PresetFlatWorldScreen.this.getFocused() == this;
      }

      public boolean keyPressed(int var1, int var2, int var3) {
         if (super.keyPressed(var1, var2, var3)) {
            return true;
         } else {
            if ((var1 == 257 || var1 == 335) && this.getSelected() != null) {
               ((PresetFlatWorldScreen.PresetsList.Entry)this.getSelected()).select();
            }

            return false;
         }
      }

      // $FF: synthetic method
      // $FF: bridge method
      public void setSelected(@Nullable AbstractSelectionList.Entry var1) {
         this.setSelected((PresetFlatWorldScreen.PresetsList.Entry)var1);
      }

      public class Entry extends ObjectSelectionList.Entry<PresetFlatWorldScreen.PresetsList.Entry> {
         private final PresetFlatWorldScreen.PresetInfo preset;

         public Entry(PresetFlatWorldScreen.PresetInfo var2) {
            this.preset = var2;
         }

         public void render(PoseStack var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
            this.blitSlot(var1, var4, var3, this.preset.icon);
            PresetFlatWorldScreen.this.font.draw(var1, this.preset.name, (float)(var4 + 18 + 5), (float)(var3 + 6), 16777215);
         }

         public boolean mouseClicked(double var1, double var3, int var5) {
            if (var5 == 0) {
               this.select();
            }

            return false;
         }

         void select() {
            PresetsList.this.setSelected(this);
            PresetFlatWorldScreen.this.settings = (FlatLevelGeneratorSettings)this.preset.settings.apply(PresetFlatWorldScreen.this.parent.parent.worldGenSettingsComponent.registryHolder());
            PresetFlatWorldScreen.this.export.setValue(PresetFlatWorldScreen.save(PresetFlatWorldScreen.this.settings));
            PresetFlatWorldScreen.this.export.moveCursorToStart();
         }

         private void blitSlot(PoseStack var1, int var2, int var3, Item var4) {
            this.blitSlotBg(var1, var2 + 1, var3 + 1);
            PresetFlatWorldScreen.this.itemRenderer.renderGuiItem(new ItemStack(var4), var2 + 2, var3 + 2);
         }

         private void blitSlotBg(PoseStack var1, int var2, int var3) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, GuiComponent.STATS_ICON_LOCATION);
            GuiComponent.blit(var1, var2, var3, PresetFlatWorldScreen.this.getBlitOffset(), 0.0F, 0.0F, 18, 18, 128, 128);
         }

         public Component getNarration() {
            return new TranslatableComponent("narrator.select", new Object[]{this.preset.getName()});
         }
      }
   }

   static class PresetInfo {
      public final Item icon;
      public final Component name;
      public final Function<RegistryAccess, FlatLevelGeneratorSettings> settings;

      public PresetInfo(Item var1, Component var2, Function<RegistryAccess, FlatLevelGeneratorSettings> var3) {
         this.icon = var1;
         this.name = var2;
         this.settings = var3;
      }

      public Component getName() {
         return this.name;
      }
   }
}
