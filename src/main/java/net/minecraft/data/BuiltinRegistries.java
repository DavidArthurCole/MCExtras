package net.minecraft.data;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.data.worldgen.NoiseData;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.data.worldgen.StructureFeatures;
import net.minecraft.data.worldgen.StructureSets;
import net.minecraft.data.worldgen.biome.Biomes;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.slf4j.Logger;

public class BuiltinRegistries {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<ResourceLocation, Supplier<? extends Holder<?>>> LOADERS = Maps.newLinkedHashMap();
   private static final WritableRegistry<WritableRegistry<?>> WRITABLE_REGISTRY = new MappedRegistry(ResourceKey.createRegistryKey(new ResourceLocation("root")), Lifecycle.experimental(), (Function)null);
   public static final Registry<? extends Registry<?>> REGISTRY;
   public static final Registry<ConfiguredWorldCarver<?>> CONFIGURED_CARVER;
   public static final Registry<ConfiguredFeature<?, ?>> CONFIGURED_FEATURE;
   public static final Registry<PlacedFeature> PLACED_FEATURE;
   public static final Registry<ConfiguredStructureFeature<?, ?>> CONFIGURED_STRUCTURE_FEATURE;
   public static final Registry<StructureSet> STRUCTURE_SETS;
   public static final Registry<StructureProcessorList> PROCESSOR_LIST;
   public static final Registry<StructureTemplatePool> TEMPLATE_POOL;
   public static final Registry<Biome> BIOME;
   public static final Registry<NormalNoise.NoiseParameters> NOISE;
   public static final Registry<DensityFunction> DENSITY_FUNCTION;
   public static final Registry<NoiseGeneratorSettings> NOISE_GENERATOR_SETTINGS;
   public static final RegistryAccess ACCESS;

   public BuiltinRegistries() {
   }

   private static <T> Registry<T> registerSimple(ResourceKey<? extends Registry<T>> var0, Supplier<? extends Holder<? extends T>> var1) {
      return registerSimple(var0, Lifecycle.stable(), var1);
   }

   private static <T> Registry<T> registerSimple(ResourceKey<? extends Registry<T>> var0, Lifecycle var1, Supplier<? extends Holder<? extends T>> var2) {
      return internalRegister(var0, new MappedRegistry(var0, var1, (Function)null), var2, var1);
   }

   private static <T, R extends WritableRegistry<T>> R internalRegister(ResourceKey<? extends Registry<T>> var0, R var1, Supplier<? extends Holder<? extends T>> var2, Lifecycle var3) {
      ResourceLocation var4 = var0.location();
      LOADERS.put(var4, var2);
      WRITABLE_REGISTRY.register(var0, var1, var3);
      return var1;
   }

   public static <V extends T, T> Holder<V> registerExact(Registry<T> var0, String var1, V var2) {
      Holder var3 = register(var0, new ResourceLocation(var1), var2);
      return var3;
   }

   public static <T> Holder<T> register(Registry<T> var0, String var1, T var2) {
      return register(var0, new ResourceLocation(var1), var2);
   }

   public static <T> Holder<T> register(Registry<T> var0, ResourceLocation var1, T var2) {
      return register(var0, ResourceKey.create(var0.key(), var1), var2);
   }

   public static <T> Holder<T> register(Registry<T> var0, ResourceKey<T> var1, T var2) {
      return ((WritableRegistry)var0).register(var1, var2, Lifecycle.stable());
   }

   public static void bootstrap() {
   }

   static {
      REGISTRY = WRITABLE_REGISTRY;
      CONFIGURED_CARVER = registerSimple(Registry.CONFIGURED_CARVER_REGISTRY, () -> {
         return Carvers.CAVE;
      });
      CONFIGURED_FEATURE = registerSimple(Registry.CONFIGURED_FEATURE_REGISTRY, FeatureUtils::bootstrap);
      PLACED_FEATURE = registerSimple(Registry.PLACED_FEATURE_REGISTRY, PlacementUtils::bootstrap);
      CONFIGURED_STRUCTURE_FEATURE = registerSimple(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, StructureFeatures::bootstrap);
      STRUCTURE_SETS = registerSimple(Registry.STRUCTURE_SET_REGISTRY, StructureSets::bootstrap);
      PROCESSOR_LIST = registerSimple(Registry.PROCESSOR_LIST_REGISTRY, () -> {
         return ProcessorLists.ZOMBIE_PLAINS;
      });
      TEMPLATE_POOL = registerSimple(Registry.TEMPLATE_POOL_REGISTRY, Pools::bootstrap);
      BIOME = registerSimple(Registry.BIOME_REGISTRY, Biomes::bootstrap);
      NOISE = registerSimple(Registry.NOISE_REGISTRY, NoiseData::bootstrap);
      DENSITY_FUNCTION = registerSimple(Registry.DENSITY_FUNCTION_REGISTRY, NoiseRouterData::bootstrap);
      NOISE_GENERATOR_SETTINGS = registerSimple(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, NoiseGeneratorSettings::bootstrap);
      LOADERS.forEach((var0, var1) -> {
         if (!((Holder)var1.get()).isBound()) {
            LOGGER.error("Unable to bootstrap registry '{}'", var0);
         }

      });
      Registry.checkRegistry(WRITABLE_REGISTRY);
      ACCESS = RegistryAccess.fromRegistryOfRegistries(REGISTRY);
   }
}
