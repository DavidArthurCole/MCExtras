package net.minecraft.data.tags;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;
import org.slf4j.Logger;

public abstract class TagsProvider<T> implements DataProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   protected final DataGenerator generator;
   protected final Registry<T> registry;
   private final Map<ResourceLocation, Tag.Builder> builders = Maps.newLinkedHashMap();

   protected TagsProvider(DataGenerator var1, Registry<T> var2) {
      this.generator = var1;
      this.registry = var2;
   }

   protected abstract void addTags();

   public void run(HashCache var1) {
      this.builders.clear();
      this.addTags();
      this.builders.forEach((var2, var3) -> {
         List var4 = var3.getEntries().filter((var1x) -> {
            Tag.Entry var10000 = var1x.entry();
            Registry var10001 = this.registry;
            Objects.requireNonNull(var10001);
            Predicate var2 = var10001::containsKey;
            Map var10002 = this.builders;
            Objects.requireNonNull(var10002);
            return !var10000.verifyIfPresent(var2, var10002::containsKey);
         }).toList();
         if (!var4.isEmpty()) {
            throw new IllegalArgumentException(String.format("Couldn't define tag %s as it is missing following references: %s", var2, var4.stream().map(Objects::toString).collect(Collectors.joining(","))));
         } else {
            JsonObject var5 = var3.serializeToJson();
            Path var6 = this.getPath(var2);

            try {
               String var7 = GSON.toJson(var5);
               String var8 = SHA1.hashUnencodedChars(var7).toString();
               if (!Objects.equals(var1.getHash(var6), var8) || !Files.exists(var6, new LinkOption[0])) {
                  Files.createDirectories(var6.getParent());
                  BufferedWriter var9 = Files.newBufferedWriter(var6);

                  try {
                     var9.write(var7);
                  } catch (Throwable var13) {
                     if (var9 != null) {
                        try {
                           var9.close();
                        } catch (Throwable var12) {
                           var13.addSuppressed(var12);
                        }
                     }

                     throw var13;
                  }

                  if (var9 != null) {
                     var9.close();
                  }
               }

               var1.putNew(var6, var8);
            } catch (IOException var14) {
               LOGGER.error("Couldn't save tags to {}", var6, var14);
            }

         }
      });
   }

   private Path getPath(ResourceLocation var1) {
      ResourceKey var2 = this.registry.key();
      Path var10000 = this.generator.getOutputFolder();
      String var10001 = var1.getNamespace();
      return var10000.resolve("data/" + var10001 + "/" + TagManager.getTagDir(var2) + "/" + var1.getPath() + ".json");
   }

   protected TagsProvider.TagAppender<T> tag(TagKey<T> var1) {
      Tag.Builder var2 = this.getOrCreateRawBuilder(var1);
      return new TagsProvider.TagAppender(var2, this.registry, "vanilla");
   }

   protected Tag.Builder getOrCreateRawBuilder(TagKey<T> var1) {
      return (Tag.Builder)this.builders.computeIfAbsent(var1.location(), (var0) -> {
         return new Tag.Builder();
      });
   }

   protected static class TagAppender<T> {
      private final Tag.Builder builder;
      private final Registry<T> registry;
      private final String source;

      TagAppender(Tag.Builder var1, Registry<T> var2, String var3) {
         this.builder = var1;
         this.registry = var2;
         this.source = var3;
      }

      public TagsProvider.TagAppender<T> add(T var1) {
         this.builder.addElement(this.registry.getKey(var1), this.source);
         return this;
      }

      @SafeVarargs
      public final TagsProvider.TagAppender<T> add(ResourceKey<T>... var1) {
         ResourceKey[] var2 = var1;
         int var3 = var1.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            ResourceKey var5 = var2[var4];
            this.builder.addElement(var5.location(), this.source);
         }

         return this;
      }

      public TagsProvider.TagAppender<T> addOptional(ResourceLocation var1) {
         this.builder.addOptionalElement(var1, this.source);
         return this;
      }

      public TagsProvider.TagAppender<T> addTag(TagKey<T> var1) {
         this.builder.addTag(var1.location(), this.source);
         return this;
      }

      public TagsProvider.TagAppender<T> addOptionalTag(ResourceLocation var1) {
         this.builder.addOptionalTag(var1, this.source);
         return this;
      }

      @SafeVarargs
      public final TagsProvider.TagAppender<T> add(T... var1) {
         Stream var10000 = Stream.of(var1);
         Registry var10001 = this.registry;
         Objects.requireNonNull(var10001);
         var10000.map(var10001::getKey).forEach((var1x) -> {
            this.builder.addElement(var1x, this.source);
         });
         return this;
      }
   }
}
