package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;

public class BeehiveDecorator extends TreeDecorator {
   public static final Codec<BeehiveDecorator> CODEC = Codec.floatRange(0.0F, 1.0F).fieldOf("probability").xmap(BeehiveDecorator::new, (var0) -> {
      return var0.probability;
   }).codec();
   private static final Direction WORLDGEN_FACING;
   private static final Direction[] SPAWN_DIRECTIONS;
   private final float probability;

   public BeehiveDecorator(float var1) {
      this.probability = var1;
   }

   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.BEEHIVE;
   }

   public void place(LevelSimulatedReader var1, BiConsumer<BlockPos, BlockState> var2, Random var3, List<BlockPos> var4, List<BlockPos> var5) {
      if (!(var3.nextFloat() >= this.probability)) {
         int var6 = !var5.isEmpty() ? Math.max(((BlockPos)var5.get(0)).getY() - 1, ((BlockPos)var4.get(0)).getY() + 1) : Math.min(((BlockPos)var4.get(0)).getY() + 1 + var3.nextInt(3), ((BlockPos)var4.get(var4.size() - 1)).getY());
         List var7 = (List)var4.stream().filter((var1x) -> {
            return var1x.getY() == var6;
         }).flatMap((var0) -> {
            Stream var10000 = Stream.of(SPAWN_DIRECTIONS);
            Objects.requireNonNull(var0);
            return var10000.map(var0::relative);
         }).collect(Collectors.toList());
         if (!var7.isEmpty()) {
            Collections.shuffle(var7);
            Optional var8 = var7.stream().filter((var1x) -> {
               return Feature.isAir(var1, var1x) && Feature.isAir(var1, var1x.relative(WORLDGEN_FACING));
            }).findFirst();
            if (!var8.isEmpty()) {
               var2.accept((BlockPos)var8.get(), (BlockState)Blocks.BEE_NEST.defaultBlockState().setValue(BeehiveBlock.FACING, WORLDGEN_FACING));
               var1.getBlockEntity((BlockPos)var8.get(), BlockEntityType.BEEHIVE).ifPresent((var1x) -> {
                  int var2 = 2 + var3.nextInt(2);

                  for(int var3x = 0; var3x < var2; ++var3x) {
                     CompoundTag var4 = new CompoundTag();
                     var4.putString("id", Registry.ENTITY_TYPE.getKey(EntityType.BEE).toString());
                     var1x.storeBee(var4, var3.nextInt(599), false);
                  }

               });
            }
         }
      }
   }

   static {
      WORLDGEN_FACING = Direction.SOUTH;
      SPAWN_DIRECTIONS = (Direction[])Direction.Plane.HORIZONTAL.stream().filter((var0) -> {
         return var0 != WORLDGEN_FACING.getOpposite();
      }).toArray((var0) -> {
         return new Direction[var0];
      });
   }
}
