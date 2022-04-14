package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class BackUpIfTooClose<E extends Mob> extends Behavior<E> {
   private final int tooCloseDistance;
   private final float strafeSpeed;

   public BackUpIfTooClose(int var1, float var2) {
      super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
      this.tooCloseDistance = var1;
      this.strafeSpeed = var2;
   }

   protected boolean checkExtraStartConditions(ServerLevel var1, E var2) {
      return this.isTargetVisible(var2) && this.isTargetTooClose(var2);
   }

   protected void start(ServerLevel var1, E var2, long var3) {
      var2.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, (Object)(new EntityTracker(this.getTarget(var2), true)));
      var2.getMoveControl().strafe(-this.strafeSpeed, 0.0F);
      var2.setYRot(Mth.rotateIfNecessary(var2.getYRot(), var2.yHeadRot, 0.0F));
   }

   private boolean isTargetVisible(E var1) {
      return ((NearestVisibleLivingEntities)var1.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get()).contains(this.getTarget(var1));
   }

   private boolean isTargetTooClose(E var1) {
      return this.getTarget(var1).closerThan(var1, (double)this.tooCloseDistance);
   }

   private LivingEntity getTarget(E var1) {
      return (LivingEntity)var1.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
   }

   // $FF: synthetic method
   // $FF: bridge method
   protected boolean checkExtraStartConditions(ServerLevel var1, LivingEntity var2) {
      return this.checkExtraStartConditions(var1, (Mob)var2);
   }

   // $FF: synthetic method
   // $FF: bridge method
   protected void start(ServerLevel var1, LivingEntity var2, long var3) {
      this.start(var1, (Mob)var2, var3);
   }
}