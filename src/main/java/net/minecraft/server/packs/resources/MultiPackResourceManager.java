package net.minecraft.server.packs.resources;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;

public class MultiPackResourceManager implements CloseableResourceManager {
   private final Map<String, FallbackResourceManager> namespacedManagers;
   private final List<PackResources> packs;

   public MultiPackResourceManager(PackType var1, List<PackResources> var2) {
      this.packs = List.copyOf(var2);
      HashMap var3 = new HashMap();
      Iterator var4 = var2.iterator();

      while(var4.hasNext()) {
         PackResources var5 = (PackResources)var4.next();
         Iterator var6 = var5.getNamespaces(var1).iterator();

         while(var6.hasNext()) {
            String var7 = (String)var6.next();
            ((FallbackResourceManager)var3.computeIfAbsent(var7, (var1x) -> {
               return new FallbackResourceManager(var1, var1x);
            })).add(var5);
         }
      }

      this.namespacedManagers = var3;
   }

   public Set<String> getNamespaces() {
      return this.namespacedManagers.keySet();
   }

   public Resource getResource(ResourceLocation var1) throws IOException {
      ResourceManager var2 = (ResourceManager)this.namespacedManagers.get(var1.getNamespace());
      if (var2 != null) {
         return var2.getResource(var1);
      } else {
         throw new FileNotFoundException(var1.toString());
      }
   }

   public boolean hasResource(ResourceLocation var1) {
      ResourceManager var2 = (ResourceManager)this.namespacedManagers.get(var1.getNamespace());
      return var2 != null ? var2.hasResource(var1) : false;
   }

   public List<Resource> getResources(ResourceLocation var1) throws IOException {
      ResourceManager var2 = (ResourceManager)this.namespacedManagers.get(var1.getNamespace());
      if (var2 != null) {
         return var2.getResources(var1);
      } else {
         throw new FileNotFoundException(var1.toString());
      }
   }

   public Collection<ResourceLocation> listResources(String var1, Predicate<String> var2) {
      HashSet var3 = Sets.newHashSet();
      Iterator var4 = this.namespacedManagers.values().iterator();

      while(var4.hasNext()) {
         FallbackResourceManager var5 = (FallbackResourceManager)var4.next();
         var3.addAll(var5.listResources(var1, var2));
      }

      ArrayList var6 = Lists.newArrayList(var3);
      Collections.sort(var6);
      return var6;
   }

   public Stream<PackResources> listPacks() {
      return this.packs.stream();
   }

   public void close() {
      this.packs.forEach(PackResources::close);
   }
}
