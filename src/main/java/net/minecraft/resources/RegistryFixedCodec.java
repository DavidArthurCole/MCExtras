package net.minecraft.resources;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;

public final class RegistryFixedCodec<E> implements Codec<Holder<E>> {
   private final ResourceKey<? extends Registry<E>> registryKey;

   public static <E> RegistryFixedCodec<E> create(ResourceKey<? extends Registry<E>> var0) {
      return new RegistryFixedCodec(var0);
   }

   private RegistryFixedCodec(ResourceKey<? extends Registry<E>> var1) {
      this.registryKey = var1;
   }

   public <T> DataResult<T> encode(Holder<E> var1, DynamicOps<T> var2, T var3) {
      if (var2 instanceof RegistryOps) {
         RegistryOps var4 = (RegistryOps)var2;
         Optional var5 = var4.registry(this.registryKey);
         if (var5.isPresent()) {
            if (!var1.isValidInRegistry((Registry)var5.get())) {
               return DataResult.error("Element " + var1 + " is not valid in current registry set");
            }

            return (DataResult)var1.unwrap().map((var2x) -> {
               return ResourceLocation.CODEC.encode(var2x.location(), var2, var3);
            }, (var1x) -> {
               return DataResult.error("Elements from registry " + this.registryKey + " can't be serialized to a value");
            });
         }
      }

      return DataResult.error("Can't access registry " + this.registryKey);
   }

   public <T> DataResult<Pair<Holder<E>, T>> decode(DynamicOps<T> var1, T var2) {
      if (var1 instanceof RegistryOps) {
         RegistryOps var3 = (RegistryOps)var1;
         Optional var4 = var3.registry(this.registryKey);
         if (var4.isPresent()) {
            return ResourceLocation.CODEC.decode(var1, var2).map((var2x) -> {
               return var2x.mapFirst((var2) -> {
                  return ((Registry)var4.get()).getOrCreateHolder(ResourceKey.create(this.registryKey, var2));
               });
            });
         }
      }

      return DataResult.error("Can't access registry " + this.registryKey);
   }

   public String toString() {
      return "RegistryFixedCodec[" + this.registryKey + "]";
   }

   // $FF: synthetic method
   public DataResult encode(Object var1, DynamicOps var2, Object var3) {
      return this.encode((Holder)var1, var2, var3);
   }
}
