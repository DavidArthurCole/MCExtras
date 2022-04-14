package net.minecraft.data.worldgen;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.placement.VillagePlacements;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class PlainVillagePools {
   public static final Holder<StructureTemplatePool> START;

   public PlainVillagePools() {
   }

   public static void bootstrap() {
   }

   static {
      START = Pools.register(new StructureTemplatePool(new ResourceLocation("village/plains/town_centers"), new ResourceLocation("empty"), ImmutableList.of(Pair.of(StructurePoolElement.legacy("village/plains/town_centers/plains_fountain_01", ProcessorLists.MOSSIFY_20_PERCENT), 50), Pair.of(StructurePoolElement.legacy("village/plains/town_centers/plains_meeting_point_1", ProcessorLists.MOSSIFY_20_PERCENT), 50), Pair.of(StructurePoolElement.legacy("village/plains/town_centers/plains_meeting_point_2"), 50), Pair.of(StructurePoolElement.legacy("village/plains/town_centers/plains_meeting_point_3", ProcessorLists.MOSSIFY_70_PERCENT), 50), Pair.of(StructurePoolElement.legacy("village/plains/zombie/town_centers/plains_fountain_01", ProcessorLists.ZOMBIE_PLAINS), 1), Pair.of(StructurePoolElement.legacy("village/plains/zombie/town_centers/plains_meeting_point_1", ProcessorLists.ZOMBIE_PLAINS), 1), Pair.of(StructurePoolElement.legacy("village/plains/zombie/town_centers/plains_meeting_point_2", ProcessorLists.ZOMBIE_PLAINS), 1), Pair.of(StructurePoolElement.legacy("village/plains/zombie/town_centers/plains_meeting_point_3", ProcessorLists.ZOMBIE_PLAINS), 1)), StructureTemplatePool.Projection.RIGID));
      Pools.register(new StructureTemplatePool(new ResourceLocation("village/plains/streets"), new ResourceLocation("village/plains/terminators"), ImmutableList.of(Pair.of(StructurePoolElement.legacy("village/plains/streets/corner_01", ProcessorLists.STREET_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/streets/corner_02", ProcessorLists.STREET_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/streets/corner_03", ProcessorLists.STREET_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/streets/straight_01", ProcessorLists.STREET_PLAINS), 4), Pair.of(StructurePoolElement.legacy("village/plains/streets/straight_02", ProcessorLists.STREET_PLAINS), 4), Pair.of(StructurePoolElement.legacy("village/plains/streets/straight_03", ProcessorLists.STREET_PLAINS), 7), Pair.of(StructurePoolElement.legacy("village/plains/streets/straight_04", ProcessorLists.STREET_PLAINS), 7), Pair.of(StructurePoolElement.legacy("village/plains/streets/straight_05", ProcessorLists.STREET_PLAINS), 3), Pair.of(StructurePoolElement.legacy("village/plains/streets/straight_06", ProcessorLists.STREET_PLAINS), 4), Pair.of(StructurePoolElement.legacy("village/plains/streets/crossroad_01", ProcessorLists.STREET_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/streets/crossroad_02", ProcessorLists.STREET_PLAINS), 1), Pair.of(StructurePoolElement.legacy("village/plains/streets/crossroad_03", ProcessorLists.STREET_PLAINS), 2), new Pair[]{Pair.of(StructurePoolElement.legacy("village/plains/streets/crossroad_04", ProcessorLists.STREET_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/streets/crossroad_05", ProcessorLists.STREET_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/streets/crossroad_06", ProcessorLists.STREET_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/streets/turn_01", ProcessorLists.STREET_PLAINS), 3)}), StructureTemplatePool.Projection.TERRAIN_MATCHING));
      Pools.register(new StructureTemplatePool(new ResourceLocation("village/plains/zombie/streets"), new ResourceLocation("village/plains/terminators"), ImmutableList.of(Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/corner_01", ProcessorLists.STREET_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/corner_02", ProcessorLists.STREET_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/corner_03", ProcessorLists.STREET_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/straight_01", ProcessorLists.STREET_PLAINS), 4), Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/straight_02", ProcessorLists.STREET_PLAINS), 4), Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/straight_03", ProcessorLists.STREET_PLAINS), 7), Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/straight_04", ProcessorLists.STREET_PLAINS), 7), Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/straight_05", ProcessorLists.STREET_PLAINS), 3), Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/straight_06", ProcessorLists.STREET_PLAINS), 4), Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/crossroad_01", ProcessorLists.STREET_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/crossroad_02", ProcessorLists.STREET_PLAINS), 1), Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/crossroad_03", ProcessorLists.STREET_PLAINS), 2), new Pair[]{Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/crossroad_04", ProcessorLists.STREET_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/crossroad_05", ProcessorLists.STREET_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/crossroad_06", ProcessorLists.STREET_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/turn_01", ProcessorLists.STREET_PLAINS), 3)}), StructureTemplatePool.Projection.TERRAIN_MATCHING));
      Pools.register(new StructureTemplatePool(new ResourceLocation("village/plains/houses"), new ResourceLocation("village/plains/terminators"), ImmutableList.of(Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_small_house_1", ProcessorLists.MOSSIFY_10_PERCENT), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_small_house_2", ProcessorLists.MOSSIFY_10_PERCENT), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_small_house_3", ProcessorLists.MOSSIFY_10_PERCENT), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_small_house_4", ProcessorLists.MOSSIFY_10_PERCENT), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_small_house_5", ProcessorLists.MOSSIFY_10_PERCENT), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_small_house_6", ProcessorLists.MOSSIFY_10_PERCENT), 1), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_small_house_7", ProcessorLists.MOSSIFY_10_PERCENT), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_small_house_8", ProcessorLists.MOSSIFY_10_PERCENT), 3), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_medium_house_1", ProcessorLists.MOSSIFY_10_PERCENT), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_medium_house_2", ProcessorLists.MOSSIFY_10_PERCENT), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_big_house_1", ProcessorLists.MOSSIFY_10_PERCENT), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_butcher_shop_1", ProcessorLists.MOSSIFY_10_PERCENT), 2), new Pair[]{Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_butcher_shop_2", ProcessorLists.MOSSIFY_10_PERCENT), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_tool_smith_1", ProcessorLists.MOSSIFY_10_PERCENT), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_fletcher_house_1", ProcessorLists.MOSSIFY_10_PERCENT), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_shepherds_house_1"), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_armorer_house_1", ProcessorLists.MOSSIFY_10_PERCENT), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_fisher_cottage_1", ProcessorLists.MOSSIFY_10_PERCENT), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_tannery_1", ProcessorLists.MOSSIFY_10_PERCENT), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_cartographer_1", ProcessorLists.MOSSIFY_10_PERCENT), 1), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_library_1", ProcessorLists.MOSSIFY_10_PERCENT), 5), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_library_2", ProcessorLists.MOSSIFY_10_PERCENT), 1), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_masons_house_1", ProcessorLists.MOSSIFY_10_PERCENT), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_weaponsmith_1", ProcessorLists.MOSSIFY_10_PERCENT), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_temple_3", ProcessorLists.MOSSIFY_10_PERCENT), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_temple_4", ProcessorLists.MOSSIFY_10_PERCENT), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_stable_1", ProcessorLists.MOSSIFY_10_PERCENT), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_stable_2"), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_large_farm_1", ProcessorLists.FARM_PLAINS), 4), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_small_farm_1", ProcessorLists.FARM_PLAINS), 4), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_animal_pen_1"), 1), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_animal_pen_2"), 1), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_animal_pen_3"), 5), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_accessory_1"), 1), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_meeting_point_4", ProcessorLists.MOSSIFY_70_PERCENT), 3), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_meeting_point_5"), 1), Pair.of(StructurePoolElement.empty(), 10)}), StructureTemplatePool.Projection.RIGID));
      Pools.register(new StructureTemplatePool(new ResourceLocation("village/plains/zombie/houses"), new ResourceLocation("village/plains/terminators"), ImmutableList.of(Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_small_house_1", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_small_house_2", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_small_house_3", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_small_house_4", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_small_house_5", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_small_house_6", ProcessorLists.ZOMBIE_PLAINS), 1), Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_small_house_7", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_small_house_8", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_medium_house_1", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_medium_house_2", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_big_house_1", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_butcher_shop_1", ProcessorLists.ZOMBIE_PLAINS), 2), new Pair[]{Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_butcher_shop_2", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_tool_smith_1", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_fletcher_house_1", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_shepherds_house_1", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_armorer_house_1", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_fisher_cottage_1", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_tannery_1", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_cartographer_1", ProcessorLists.ZOMBIE_PLAINS), 1), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_library_1", ProcessorLists.ZOMBIE_PLAINS), 3), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_library_2", ProcessorLists.ZOMBIE_PLAINS), 1), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_masons_house_1", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_weaponsmith_1", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_temple_3", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_temple_4", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_stable_1", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_stable_2", ProcessorLists.ZOMBIE_PLAINS), 2), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_large_farm_1", ProcessorLists.ZOMBIE_PLAINS), 4), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_small_farm_1", ProcessorLists.ZOMBIE_PLAINS), 4), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_animal_pen_1", ProcessorLists.ZOMBIE_PLAINS), 1), Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_animal_pen_2", ProcessorLists.ZOMBIE_PLAINS), 1), Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_animal_pen_3", ProcessorLists.ZOMBIE_PLAINS), 5), Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_meeting_point_4", ProcessorLists.ZOMBIE_PLAINS), 3), Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_meeting_point_5", ProcessorLists.ZOMBIE_PLAINS), 1), Pair.of(StructurePoolElement.empty(), 10)}), StructureTemplatePool.Projection.RIGID));
      Pools.register(new StructureTemplatePool(new ResourceLocation("village/plains/terminators"), new ResourceLocation("empty"), ImmutableList.of(Pair.of(StructurePoolElement.legacy("village/plains/terminators/terminator_01", ProcessorLists.STREET_PLAINS), 1), Pair.of(StructurePoolElement.legacy("village/plains/terminators/terminator_02", ProcessorLists.STREET_PLAINS), 1), Pair.of(StructurePoolElement.legacy("village/plains/terminators/terminator_03", ProcessorLists.STREET_PLAINS), 1), Pair.of(StructurePoolElement.legacy("village/plains/terminators/terminator_04", ProcessorLists.STREET_PLAINS), 1)), StructureTemplatePool.Projection.TERRAIN_MATCHING));
      Pools.register(new StructureTemplatePool(new ResourceLocation("village/plains/trees"), new ResourceLocation("empty"), ImmutableList.of(Pair.of(StructurePoolElement.feature(VillagePlacements.OAK_VILLAGE), 1)), StructureTemplatePool.Projection.RIGID));
      Pools.register(new StructureTemplatePool(new ResourceLocation("village/plains/decor"), new ResourceLocation("empty"), ImmutableList.of(Pair.of(StructurePoolElement.legacy("village/plains/plains_lamp_1"), 2), Pair.of(StructurePoolElement.feature(VillagePlacements.OAK_VILLAGE), 1), Pair.of(StructurePoolElement.feature(VillagePlacements.FLOWER_PLAIN_VILLAGE), 1), Pair.of(StructurePoolElement.feature(VillagePlacements.PILE_HAY_VILLAGE), 1), Pair.of(StructurePoolElement.empty(), 2)), StructureTemplatePool.Projection.RIGID));
      Pools.register(new StructureTemplatePool(new ResourceLocation("village/plains/zombie/decor"), new ResourceLocation("empty"), ImmutableList.of(Pair.of(StructurePoolElement.legacy("village/plains/plains_lamp_1", ProcessorLists.ZOMBIE_PLAINS), 1), Pair.of(StructurePoolElement.feature(VillagePlacements.OAK_VILLAGE), 1), Pair.of(StructurePoolElement.feature(VillagePlacements.FLOWER_PLAIN_VILLAGE), 1), Pair.of(StructurePoolElement.feature(VillagePlacements.PILE_HAY_VILLAGE), 1), Pair.of(StructurePoolElement.empty(), 2)), StructureTemplatePool.Projection.RIGID));
      Pools.register(new StructureTemplatePool(new ResourceLocation("village/plains/villagers"), new ResourceLocation("empty"), ImmutableList.of(Pair.of(StructurePoolElement.legacy("village/plains/villagers/nitwit"), 1), Pair.of(StructurePoolElement.legacy("village/plains/villagers/baby"), 1), Pair.of(StructurePoolElement.legacy("village/plains/villagers/unemployed"), 10)), StructureTemplatePool.Projection.RIGID));
      Pools.register(new StructureTemplatePool(new ResourceLocation("village/plains/zombie/villagers"), new ResourceLocation("empty"), ImmutableList.of(Pair.of(StructurePoolElement.legacy("village/plains/zombie/villagers/nitwit"), 1), Pair.of(StructurePoolElement.legacy("village/plains/zombie/villagers/unemployed"), 10)), StructureTemplatePool.Projection.RIGID));
      Pools.register(new StructureTemplatePool(new ResourceLocation("village/common/animals"), new ResourceLocation("empty"), ImmutableList.of(Pair.of(StructurePoolElement.legacy("village/common/animals/cows_1"), 7), Pair.of(StructurePoolElement.legacy("village/common/animals/pigs_1"), 7), Pair.of(StructurePoolElement.legacy("village/common/animals/horses_1"), 1), Pair.of(StructurePoolElement.legacy("village/common/animals/horses_2"), 1), Pair.of(StructurePoolElement.legacy("village/common/animals/horses_3"), 1), Pair.of(StructurePoolElement.legacy("village/common/animals/horses_4"), 1), Pair.of(StructurePoolElement.legacy("village/common/animals/horses_5"), 1), Pair.of(StructurePoolElement.legacy("village/common/animals/sheep_1"), 1), Pair.of(StructurePoolElement.legacy("village/common/animals/sheep_2"), 1), Pair.of(StructurePoolElement.empty(), 5)), StructureTemplatePool.Projection.RIGID));
      Pools.register(new StructureTemplatePool(new ResourceLocation("village/common/sheep"), new ResourceLocation("empty"), ImmutableList.of(Pair.of(StructurePoolElement.legacy("village/common/animals/sheep_1"), 1), Pair.of(StructurePoolElement.legacy("village/common/animals/sheep_2"), 1)), StructureTemplatePool.Projection.RIGID));
      Pools.register(new StructureTemplatePool(new ResourceLocation("village/common/cats"), new ResourceLocation("empty"), ImmutableList.of(Pair.of(StructurePoolElement.legacy("village/common/animals/cat_black"), 1), Pair.of(StructurePoolElement.legacy("village/common/animals/cat_british"), 1), Pair.of(StructurePoolElement.legacy("village/common/animals/cat_calico"), 1), Pair.of(StructurePoolElement.legacy("village/common/animals/cat_persian"), 1), Pair.of(StructurePoolElement.legacy("village/common/animals/cat_ragdoll"), 1), Pair.of(StructurePoolElement.legacy("village/common/animals/cat_red"), 1), Pair.of(StructurePoolElement.legacy("village/common/animals/cat_siamese"), 1), Pair.of(StructurePoolElement.legacy("village/common/animals/cat_tabby"), 1), Pair.of(StructurePoolElement.legacy("village/common/animals/cat_white"), 1), Pair.of(StructurePoolElement.legacy("village/common/animals/cat_jellie"), 1), Pair.of(StructurePoolElement.empty(), 3)), StructureTemplatePool.Projection.RIGID));
      Pools.register(new StructureTemplatePool(new ResourceLocation("village/common/butcher_animals"), new ResourceLocation("empty"), ImmutableList.of(Pair.of(StructurePoolElement.legacy("village/common/animals/cows_1"), 3), Pair.of(StructurePoolElement.legacy("village/common/animals/pigs_1"), 3), Pair.of(StructurePoolElement.legacy("village/common/animals/sheep_1"), 1), Pair.of(StructurePoolElement.legacy("village/common/animals/sheep_2"), 1)), StructureTemplatePool.Projection.RIGID));
      Pools.register(new StructureTemplatePool(new ResourceLocation("village/common/iron_golem"), new ResourceLocation("empty"), ImmutableList.of(Pair.of(StructurePoolElement.legacy("village/common/iron_golem"), 1)), StructureTemplatePool.Projection.RIGID));
      Pools.register(new StructureTemplatePool(new ResourceLocation("village/common/well_bottoms"), new ResourceLocation("empty"), ImmutableList.of(Pair.of(StructurePoolElement.legacy("village/common/well_bottom"), 1)), StructureTemplatePool.Projection.RIGID));
   }
}
