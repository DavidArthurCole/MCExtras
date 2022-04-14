package net.minecraft.resources;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;

public class RegistryLoader {
   private final RegistryResourceAccess resources;
   private final Map<ResourceKey<? extends Registry<?>>, RegistryLoader.ReadCache<?>> readCache = new IdentityHashMap();

   RegistryLoader(RegistryResourceAccess var1) {
      this.resources = var1;
   }

   public <E> DataResult<? extends Registry<E>> overrideRegistryFromResources(WritableRegistry<E> var1, ResourceKey<? extends Registry<E>> var2, Codec<E> var3, DynamicOps<JsonElement> var4) {
      Collection var5 = this.resources.listResources(var2);
      DataResult var6 = DataResult.success(var1, Lifecycle.stable());

      ResourceKey var8;
      for(Iterator var7 = var5.iterator(); var7.hasNext(); var6 = var6.flatMap((var5x) -> {
         return this.overrideElementFromResources(var5x, var2, var3, var8, var4).map((var1) -> {
            return var5x;
         });
      })) {
         var8 = (ResourceKey)var7.next();
      }

      return var6.setPartial(var1);
   }

   <E> DataResult<Holder<E>> overrideElementFromResources(WritableRegistry<E> var1, ResourceKey<? extends Registry<E>> var2, Codec<E> var3, ResourceKey<E> var4, DynamicOps<JsonElement> var5) {
      RegistryLoader.ReadCache var6 = this.readCache(var2);
      DataResult var7 = (DataResult)var6.values.get(var4);
      if (var7 != null) {
         return var7;
      } else {
         Holder var8 = var1.getOrCreateHolder(var4);
         var6.values.put(var4, DataResult.success(var8));
         Optional var9 = this.resources.parseElement(var5, var2, var4, var3);
         DataResult var10;
         if (var9.isEmpty()) {
            if (var1.containsKey(var4)) {
               var10 = DataResult.success(var8, Lifecycle.stable());
            } else {
               var10 = DataResult.error("Missing referenced custom/removed registry entry for registry " + var2 + " named " + var4.location());
            }
         } else {
            DataResult var11 = (DataResult)var9.get();
            Optional var12 = var11.result();
            if (var12.isPresent()) {
               RegistryResourceAccess.ParsedEntry var13 = (RegistryResourceAccess.ParsedEntry)var12.get();
               var1.registerOrOverride(var13.fixedId(), var4, var13.value(), var11.lifecycle());
            }

            var10 = var11.map((var1x) -> {
               return var8;
            });
         }

         var6.values.put(var4, var10);
         return var10;
      }
   }

   private <E> RegistryLoader.ReadCache<E> readCache(ResourceKey<? extends Registry<E>> var1) {
      return (RegistryLoader.ReadCache)this.readCache.computeIfAbsent(var1, (var0) -> {
         return new RegistryLoader.ReadCache();
      });
   }

   public RegistryLoader.Bound bind(RegistryAccess.Writable var1) {
      return new RegistryLoader.Bound(var1, this);
   }

   private static final class ReadCache<E> {
      final Map<ResourceKey<E>, DataResult<Holder<E>>> values = Maps.newIdentityHashMap();

      ReadCache() {
      }
   }

   public static record Bound(RegistryAccess.Writable a, RegistryLoader b) {
      private final RegistryAccess.Writable access;
      private final RegistryLoader loader;

      public Bound(RegistryAccess.Writable var1, RegistryLoader var2) {
         this.access = var1;
         this.loader = var2;
      }

      public <E> DataResult<? extends Registry<E>> overrideRegistryFromResources(ResourceKey<? extends Registry<E>> var1, Codec<E> var2, DynamicOps<JsonElement> var3) {
         WritableRegistry var4 = this.access.ownedWritableRegistryOrThrow(var1);
         return this.loader.overrideRegistryFromResources(var4, var1, var2, var3);
      }

      public <E> DataResult<Holder<E>> overrideElementFromResources(ResourceKey<? extends Registry<E>> var1, Codec<E> var2, ResourceKey<E> var3, DynamicOps<JsonElement> var4) {
         WritableRegistry var5 = this.access.ownedWritableRegistryOrThrow(var1);
         return this.loader.overrideElementFromResources(var5, var1, var2, var3, var4);
      }

      public RegistryAccess.Writable access() {
         return this.access;
      }

      public RegistryLoader loader() {
         return this.loader;
      }
   }
}
