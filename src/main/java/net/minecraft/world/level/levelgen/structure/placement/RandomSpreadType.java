package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.RandomSource;

public enum RandomSpreadType implements StringRepresentable {
   LINEAR("linear"),
   TRIANGULAR("triangular");

   private static final RandomSpreadType[] VALUES = values();
   public static final Codec<RandomSpreadType> CODEC = StringRepresentable.fromEnum(() -> {
      return VALUES;
   }, RandomSpreadType::byName);
   private final String id;

   private RandomSpreadType(String var3) {
      this.id = var3;
   }

   public static RandomSpreadType byName(String var0) {
      RandomSpreadType[] var1 = VALUES;
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         RandomSpreadType var4 = var1[var3];
         if (var4.getSerializedName().equals(var0)) {
            return var4;
         }
      }

      throw new IllegalArgumentException("Unknown Random Spread type: " + var0);
   }

   public String getSerializedName() {
      return this.id;
   }

   public int evaluate(RandomSource var1, int var2) {
      int var10000;
      switch(this) {
      case LINEAR:
         var10000 = var1.nextInt(var2);
         break;
      case TRIANGULAR:
         var10000 = (var1.nextInt(var2) + var1.nextInt(var2)) / 2;
         break;
      default:
         throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   // $FF: synthetic method
   private static RandomSpreadType[] $values() {
      return new RandomSpreadType[]{LINEAR, TRIANGULAR};
   }
}
