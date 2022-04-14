package net.minecraft.client.gui.screens.worldselection;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.DataResult.PartialResult;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;

public class WorldGenSettingsComponent implements Widget {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component CUSTOM_WORLD_DESCRIPTION = new TranslatableComponent("generator.custom");
   private static final Component AMPLIFIED_HELP_TEXT = new TranslatableComponent("generator.amplified.info");
   private static final Component MAP_FEATURES_INFO = new TranslatableComponent("selectWorld.mapFeatures.info");
   private static final Component SELECT_FILE_PROMPT = new TranslatableComponent("selectWorld.import_worldgen_settings.select_file");
   private MultiLineLabel amplifiedWorldInfo;
   private Font font;
   private int width;
   private EditBox seedEdit;
   private CycleButton<Boolean> featuresButton;
   private CycleButton<Boolean> bonusItemsButton;
   private CycleButton<WorldPreset> typeButton;
   private Button customWorldDummyButton;
   private Button customizeTypeButton;
   private Button importSettingsButton;
   private RegistryAccess.Frozen registryHolder;
   private WorldGenSettings settings;
   private Optional<WorldPreset> preset;
   private OptionalLong seed;

   public WorldGenSettingsComponent(RegistryAccess.Frozen var1, WorldGenSettings var2, Optional<WorldPreset> var3, OptionalLong var4) {
      this.amplifiedWorldInfo = MultiLineLabel.EMPTY;
      this.registryHolder = var1;
      this.settings = var2;
      this.preset = var3;
      this.seed = var4;
   }

   public void init(CreateWorldScreen var1, Minecraft var2, Font var3) {
      this.font = var3;
      this.width = var1.width;
      this.seedEdit = new EditBox(this.font, this.width / 2 - 100, 60, 200, 20, new TranslatableComponent("selectWorld.enterSeed"));
      this.seedEdit.setValue(toString(this.seed));
      this.seedEdit.setResponder((var1x) -> {
         this.seed = WorldGenSettings.parseSeed(this.seedEdit.getValue());
      });
      var1.addWidget(this.seedEdit);
      int var4 = this.width / 2 - 155;
      int var5 = this.width / 2 + 5;
      this.featuresButton = (CycleButton)var1.addRenderableWidget(CycleButton.onOffBuilder(this.settings.generateFeatures()).withCustomNarration((var0) -> {
         return CommonComponents.joinForNarration(var0.createDefaultNarrationMessage(), new TranslatableComponent("selectWorld.mapFeatures.info"));
      }).create(var4, 100, 150, 20, new TranslatableComponent("selectWorld.mapFeatures"), (var1x, var2x) -> {
         this.settings = this.settings.withFeaturesToggled();
      }));
      this.featuresButton.visible = false;
      this.typeButton = (CycleButton)var1.addRenderableWidget(CycleButton.builder(WorldPreset::description).withValues((List)WorldPreset.PRESETS.stream().filter(WorldPreset::isVisibleByDefault).collect(Collectors.toList()), WorldPreset.PRESETS).withCustomNarration((var0) -> {
         return var0.getValue() == WorldPreset.AMPLIFIED ? CommonComponents.joinForNarration(var0.createDefaultNarrationMessage(), AMPLIFIED_HELP_TEXT) : var0.createDefaultNarrationMessage();
      }).create(var5, 100, 150, 20, new TranslatableComponent("selectWorld.mapType"), (var2x, var3x) -> {
         this.preset = Optional.of(var3x);
         this.settings = var3x.create(this.registryHolder, this.settings.seed(), this.settings.generateFeatures(), this.settings.generateBonusChest());
         var1.refreshWorldGenSettingsVisibility();
      }));
      Optional var10000 = this.preset;
      CycleButton var10001 = this.typeButton;
      Objects.requireNonNull(var10001);
      var10000.ifPresent(var10001::setValue);
      this.typeButton.visible = false;
      this.customWorldDummyButton = (Button)var1.addRenderableWidget(new Button(var5, 100, 150, 20, CommonComponents.optionNameValue(new TranslatableComponent("selectWorld.mapType"), CUSTOM_WORLD_DESCRIPTION), (var0) -> {
      }));
      this.customWorldDummyButton.active = false;
      this.customWorldDummyButton.visible = false;
      this.customizeTypeButton = (Button)var1.addRenderableWidget(new Button(var5, 120, 150, 20, new TranslatableComponent("selectWorld.customizeType"), (var3x) -> {
         WorldPreset.PresetEditor var4 = (WorldPreset.PresetEditor)WorldPreset.EDITORS.get(this.preset);
         if (var4 != null) {
            var2.setScreen(var4.createEditScreen(var1, this.settings));
         }

      }));
      this.customizeTypeButton.visible = false;
      this.bonusItemsButton = (CycleButton)var1.addRenderableWidget(CycleButton.onOffBuilder(this.settings.generateBonusChest() && !var1.hardCore).create(var4, 151, 150, 20, new TranslatableComponent("selectWorld.bonusItems"), (var1x, var2x) -> {
         this.settings = this.settings.withBonusChestToggled();
      }));
      this.bonusItemsButton.visible = false;
      this.importSettingsButton = (Button)var1.addRenderableWidget(new Button(var4, 185, 150, 20, new TranslatableComponent("selectWorld.import_worldgen_settings"), (var3x) -> {
         String var4 = TinyFileDialogs.tinyfd_openFileDialog(SELECT_FILE_PROMPT.getString(), (CharSequence)null, (PointerBuffer)null, (CharSequence)null, false);
         if (var4 != null) {
            RegistryAccess.Writable var5 = RegistryAccess.builtinCopy();
            PackRepository var7 = new PackRepository(PackType.SERVER_DATA, new RepositorySource[]{new ServerPacksSource(), new FolderRepositorySource(var1.getTempDataPackDir().toFile(), PackSource.WORLD)});

            label74: {
               DataResult var6;
               try {
                  MinecraftServer.configurePackRepository(var7, var1.dataPacks, false);
                  MultiPackResourceManager var8 = new MultiPackResourceManager(PackType.SERVER_DATA, var7.openAllSelected());

                  label70: {
                     try {
                        RegistryOps var9 = RegistryOps.createAndLoad(JsonOps.INSTANCE, var5, (ResourceManager)var8);

                        try {
                           BufferedReader var10 = Files.newBufferedReader(Paths.get(var4));

                           try {
                              JsonElement var11 = JsonParser.parseReader(var10);
                              var6 = WorldGenSettings.CODEC.parse(var9, var11);
                           } catch (Throwable var16) {
                              if (var10 != null) {
                                 try {
                                    var10.close();
                                 } catch (Throwable var15) {
                                    var16.addSuppressed(var15);
                                 }
                              }

                              throw var16;
                           }

                           if (var10 != null) {
                              var10.close();
                           }
                        } catch (Exception var17) {
                           var6 = DataResult.error("Failed to parse file: " + var17.getMessage());
                        }

                        if (!var6.error().isPresent()) {
                           break label70;
                        }

                        TranslatableComponent var21 = new TranslatableComponent("selectWorld.import_worldgen_settings.failure");
                        String var22 = ((PartialResult)var6.error().get()).message();
                        LOGGER.error("Error parsing world settings: {}", var22);
                        TextComponent var12 = new TextComponent(var22);
                        var2.getToasts().addToast(SystemToast.multiline(var2, SystemToast.SystemToastIds.WORLD_GEN_SETTINGS_TRANSFER, var21, var12));
                     } catch (Throwable var18) {
                        try {
                           var8.close();
                        } catch (Throwable var14) {
                           var18.addSuppressed(var14);
                        }

                        throw var18;
                     }

                     var8.close();
                     break label74;
                  }

                  var8.close();
               } catch (Throwable var19) {
                  try {
                     var7.close();
                  } catch (Throwable var13) {
                     var19.addSuppressed(var13);
                  }

                  throw var19;
               }

               var7.close();
               Lifecycle var20 = var6.lifecycle();
               Logger var10001 = LOGGER;
               Objects.requireNonNull(var10001);
               var6.resultOrPartial(var10001::error).ifPresent((var5x) -> {
                  BooleanConsumer var6 = (var5xx) -> {
                     var2.setScreen(var1);
                     if (var5xx) {
                        this.importSettings(var5.freeze(), var5x);
                     }

                  };
                  if (var20 == Lifecycle.stable()) {
                     this.importSettings(var5.freeze(), var5x);
                  } else if (var20 == Lifecycle.experimental()) {
                     var2.setScreen(new ConfirmScreen(var6, new TranslatableComponent("selectWorld.import_worldgen_settings.experimental.title"), new TranslatableComponent("selectWorld.import_worldgen_settings.experimental.question")));
                  } else {
                     var2.setScreen(new ConfirmScreen(var6, new TranslatableComponent("selectWorld.import_worldgen_settings.deprecated.title"), new TranslatableComponent("selectWorld.import_worldgen_settings.deprecated.question")));
                  }

               });
               return;
            }

            var7.close();
         }
      }));
      this.importSettingsButton.visible = false;
      this.amplifiedWorldInfo = MultiLineLabel.create(var3, AMPLIFIED_HELP_TEXT, this.typeButton.getWidth());
   }

   private void importSettings(RegistryAccess.Frozen var1, WorldGenSettings var2) {
      this.registryHolder = var1;
      this.settings = var2;
      this.preset = WorldPreset.of(var2);
      this.selectWorldTypeButton(true);
      this.seed = OptionalLong.of(var2.seed());
      this.seedEdit.setValue(toString(this.seed));
   }

   public void tick() {
      this.seedEdit.tick();
   }

   public void render(PoseStack var1, int var2, int var3, float var4) {
      if (this.featuresButton.visible) {
         this.font.drawShadow(var1, MAP_FEATURES_INFO, (float)(this.width / 2 - 150), 122.0F, -6250336);
      }

      this.seedEdit.render(var1, var2, var3, var4);
      if (this.preset.equals(Optional.of(WorldPreset.AMPLIFIED))) {
         MultiLineLabel var10000 = this.amplifiedWorldInfo;
         int var10002 = this.typeButton.x + 2;
         int var10003 = this.typeButton.y + 22;
         Objects.requireNonNull(this.font);
         var10000.renderLeftAligned(var1, var10002, var10003, 9, 10526880);
      }

   }

   protected void updateSettings(WorldGenSettings var1) {
      this.settings = var1;
   }

   private static String toString(OptionalLong var0) {
      return var0.isPresent() ? Long.toString(var0.getAsLong()) : "";
   }

   public WorldGenSettings makeSettings(boolean var1) {
      OptionalLong var2 = WorldGenSettings.parseSeed(this.seedEdit.getValue());
      return this.settings.withSeed(var1, var2);
   }

   public boolean isDebug() {
      return this.settings.isDebug();
   }

   public void setVisibility(boolean var1) {
      this.selectWorldTypeButton(var1);
      if (this.settings.isDebug()) {
         this.featuresButton.visible = false;
         this.bonusItemsButton.visible = false;
         this.customizeTypeButton.visible = false;
         this.importSettingsButton.visible = false;
      } else {
         this.featuresButton.visible = var1;
         this.bonusItemsButton.visible = var1;
         this.customizeTypeButton.visible = var1 && WorldPreset.EDITORS.containsKey(this.preset);
         this.importSettingsButton.visible = var1;
      }

      this.seedEdit.setVisible(var1);
   }

   private void selectWorldTypeButton(boolean var1) {
      if (this.preset.isPresent()) {
         this.typeButton.visible = var1;
         this.customWorldDummyButton.visible = false;
      } else {
         this.typeButton.visible = false;
         this.customWorldDummyButton.visible = var1;
      }

   }

   public RegistryAccess registryHolder() {
      return this.registryHolder;
   }

   void updateDataPacks(WorldStem var1) {
      this.settings = var1.worldData().worldGenSettings();
      this.registryHolder = var1.registryAccess();
   }

   public void switchToHardcore() {
      this.bonusItemsButton.active = false;
      this.bonusItemsButton.setValue(false);
   }

   public void switchOutOfHardcode() {
      this.bonusItemsButton.active = true;
      this.bonusItemsButton.setValue(this.settings.generateBonusChest());
   }
}
