package net.minecraft.world.level.block.grower;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class DarkOakTreeGrower extends AbstractMegaTreeGrower {
   public DarkOakTreeGrower() {
   }

   @Nullable
   protected Holder<? extends ConfiguredFeature<?, ?>> getConfiguredFeature(Random var1, boolean var2) {
      return null;
   }

   @Nullable
   protected Holder<? extends ConfiguredFeature<?, ?>> getConfiguredMegaFeature(Random var1) {
      return TreeFeatures.DARK_OAK;
   }
}
