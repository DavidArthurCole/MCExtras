package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.item.ItemEntity;

public class StopAdmiringIfItemTooFarAway<E extends Piglin> extends Behavior<E> {
   private final int maxDistanceToItem;

   public StopAdmiringIfItemTooFarAway(int var1) {
      super(ImmutableMap.of(MemoryModuleType.ADMIRING_ITEM, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryStatus.REGISTERED));
      this.maxDistanceToItem = var1;
   }

   protected boolean checkExtraStartConditions(ServerLevel var1, E var2) {
      if (!var2.getOffhandItem().isEmpty()) {
         return false;
      } else {
         Optional var3 = var2.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM);
         if (!var3.isPresent()) {
            return true;
         } else {
            return !((ItemEntity)var3.get()).closerThan(var2, (double)this.maxDistanceToItem);
         }
      }
   }

   protected void start(ServerLevel var1, E var2, long var3) {
      var2.getBrain().eraseMemory(MemoryModuleType.ADMIRING_ITEM);
   }

   // $FF: synthetic method
   // $FF: bridge method
   protected boolean checkExtraStartConditions(ServerLevel var1, LivingEntity var2) {
      return this.checkExtraStartConditions(var1, (Piglin)var2);
   }

   // $FF: synthetic method
   // $FF: bridge method
   protected void start(ServerLevel var1, LivingEntity var2, long var3) {
      this.start(var1, (Piglin)var2, var3);
   }
}
