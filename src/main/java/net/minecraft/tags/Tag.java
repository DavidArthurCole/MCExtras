package net.minecraft.tags;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class Tag<T> {
   private static final Tag<?> EMPTY = new Tag(List.of());
   final List<T> elements;

   public Tag(Collection<T> var1) {
      this.elements = List.copyOf(var1);
   }

   public List<T> getValues() {
      return this.elements;
   }

   public static <T> Tag<T> empty() {
      return EMPTY;
   }

   static class OptionalTagEntry implements Tag.Entry {
      private final ResourceLocation id;

      public OptionalTagEntry(ResourceLocation var1) {
         this.id = var1;
      }

      public <T> boolean build(Function<ResourceLocation, Tag<T>> var1, Function<ResourceLocation, T> var2, Consumer<T> var3) {
         Tag var4 = (Tag)var1.apply(this.id);
         if (var4 != null) {
            var4.elements.forEach(var3);
         }

         return true;
      }

      public void serializeTo(JsonArray var1) {
         JsonObject var2 = new JsonObject();
         var2.addProperty("id", "#" + this.id);
         var2.addProperty("required", false);
         var1.add(var2);
      }

      public String toString() {
         return "#" + this.id + "?";
      }

      public void visitOptionalDependencies(Consumer<ResourceLocation> var1) {
         var1.accept(this.id);
      }

      public boolean verifyIfPresent(Predicate<ResourceLocation> var1, Predicate<ResourceLocation> var2) {
         return true;
      }
   }

   private static class TagEntry implements Tag.Entry {
      private final ResourceLocation id;

      public TagEntry(ResourceLocation var1) {
         this.id = var1;
      }

      public <T> boolean build(Function<ResourceLocation, Tag<T>> var1, Function<ResourceLocation, T> var2, Consumer<T> var3) {
         Tag var4 = (Tag)var1.apply(this.id);
         if (var4 == null) {
            return false;
         } else {
            var4.elements.forEach(var3);
            return true;
         }
      }

      public void serializeTo(JsonArray var1) {
         var1.add("#" + this.id);
      }

      public String toString() {
         return "#" + this.id;
      }

      public boolean verifyIfPresent(Predicate<ResourceLocation> var1, Predicate<ResourceLocation> var2) {
         return var2.test(this.id);
      }

      public void visitRequiredDependencies(Consumer<ResourceLocation> var1) {
         var1.accept(this.id);
      }
   }

   static class OptionalElementEntry implements Tag.Entry {
      private final ResourceLocation id;

      public OptionalElementEntry(ResourceLocation var1) {
         this.id = var1;
      }

      public <T> boolean build(Function<ResourceLocation, Tag<T>> var1, Function<ResourceLocation, T> var2, Consumer<T> var3) {
         Object var4 = var2.apply(this.id);
         if (var4 != null) {
            var3.accept(var4);
         }

         return true;
      }

      public void serializeTo(JsonArray var1) {
         JsonObject var2 = new JsonObject();
         var2.addProperty("id", this.id.toString());
         var2.addProperty("required", false);
         var1.add(var2);
      }

      public boolean verifyIfPresent(Predicate<ResourceLocation> var1, Predicate<ResourceLocation> var2) {
         return true;
      }

      public String toString() {
         return this.id + "?";
      }
   }

   static class ElementEntry implements Tag.Entry {
      private final ResourceLocation id;

      public ElementEntry(ResourceLocation var1) {
         this.id = var1;
      }

      public <T> boolean build(Function<ResourceLocation, Tag<T>> var1, Function<ResourceLocation, T> var2, Consumer<T> var3) {
         Object var4 = var2.apply(this.id);
         if (var4 == null) {
            return false;
         } else {
            var3.accept(var4);
            return true;
         }
      }

      public void serializeTo(JsonArray var1) {
         var1.add(this.id.toString());
      }

      public boolean verifyIfPresent(Predicate<ResourceLocation> var1, Predicate<ResourceLocation> var2) {
         return var1.test(this.id);
      }

      public String toString() {
         return this.id.toString();
      }
   }

   public interface Entry {
      <T> boolean build(Function<ResourceLocation, Tag<T>> var1, Function<ResourceLocation, T> var2, Consumer<T> var3);

      void serializeTo(JsonArray var1);

      default void visitRequiredDependencies(Consumer<ResourceLocation> var1) {
      }

      default void visitOptionalDependencies(Consumer<ResourceLocation> var1) {
      }

      boolean verifyIfPresent(Predicate<ResourceLocation> var1, Predicate<ResourceLocation> var2);
   }

   public static class Builder {
      private final List<Tag.BuilderEntry> entries = new ArrayList();

      public Builder() {
      }

      public static Tag.Builder tag() {
         return new Tag.Builder();
      }

      public Tag.Builder add(Tag.BuilderEntry var1) {
         this.entries.add(var1);
         return this;
      }

      public Tag.Builder add(Tag.Entry var1, String var2) {
         return this.add(new Tag.BuilderEntry(var1, var2));
      }

      public Tag.Builder addElement(ResourceLocation var1, String var2) {
         return this.add(new Tag.ElementEntry(var1), var2);
      }

      public Tag.Builder addOptionalElement(ResourceLocation var1, String var2) {
         return this.add(new Tag.OptionalElementEntry(var1), var2);
      }

      public Tag.Builder addTag(ResourceLocation var1, String var2) {
         return this.add(new Tag.TagEntry(var1), var2);
      }

      public Tag.Builder addOptionalTag(ResourceLocation var1, String var2) {
         return this.add(new Tag.OptionalTagEntry(var1), var2);
      }

      public <T> Either<Collection<Tag.BuilderEntry>, Tag<T>> build(Function<ResourceLocation, Tag<T>> var1, Function<ResourceLocation, T> var2) {
         com.google.common.collect.ImmutableSet.Builder var3 = ImmutableSet.builder();
         ArrayList var4 = new ArrayList();
         Iterator var5 = this.entries.iterator();

         while(var5.hasNext()) {
            Tag.BuilderEntry var6 = (Tag.BuilderEntry)var5.next();
            Tag.Entry var10000 = var6.entry();
            Objects.requireNonNull(var3);
            if (!var10000.build(var1, var2, var3::add)) {
               var4.add(var6);
            }
         }

         return var4.isEmpty() ? Either.right(new Tag(var3.build())) : Either.left(var4);
      }

      public Stream<Tag.BuilderEntry> getEntries() {
         return this.entries.stream();
      }

      public void visitRequiredDependencies(Consumer<ResourceLocation> var1) {
         this.entries.forEach((var1x) -> {
            var1x.entry.visitRequiredDependencies(var1);
         });
      }

      public void visitOptionalDependencies(Consumer<ResourceLocation> var1) {
         this.entries.forEach((var1x) -> {
            var1x.entry.visitOptionalDependencies(var1);
         });
      }

      public Tag.Builder addFromJson(JsonObject var1, String var2) {
         JsonArray var3 = GsonHelper.getAsJsonArray(var1, "values");
         ArrayList var4 = new ArrayList();
         Iterator var5 = var3.iterator();

         while(var5.hasNext()) {
            JsonElement var6 = (JsonElement)var5.next();
            var4.add(parseEntry(var6));
         }

         if (GsonHelper.getAsBoolean(var1, "replace", false)) {
            this.entries.clear();
         }

         var4.forEach((var2x) -> {
            this.entries.add(new Tag.BuilderEntry(var2x, var2));
         });
         return this;
      }

      private static Tag.Entry parseEntry(JsonElement var0) {
         String var1;
         boolean var2;
         if (var0.isJsonObject()) {
            JsonObject var3 = var0.getAsJsonObject();
            var1 = GsonHelper.getAsString(var3, "id");
            var2 = GsonHelper.getAsBoolean(var3, "required", true);
         } else {
            var1 = GsonHelper.convertToString(var0, "id");
            var2 = true;
         }

         ResourceLocation var4;
         if (var1.startsWith("#")) {
            var4 = new ResourceLocation(var1.substring(1));
            return (Tag.Entry)(var2 ? new Tag.TagEntry(var4) : new Tag.OptionalTagEntry(var4));
         } else {
            var4 = new ResourceLocation(var1);
            return (Tag.Entry)(var2 ? new Tag.ElementEntry(var4) : new Tag.OptionalElementEntry(var4));
         }
      }

      public JsonObject serializeToJson() {
         JsonObject var1 = new JsonObject();
         JsonArray var2 = new JsonArray();
         Iterator var3 = this.entries.iterator();

         while(var3.hasNext()) {
            Tag.BuilderEntry var4 = (Tag.BuilderEntry)var3.next();
            var4.entry().serializeTo(var2);
         }

         var1.addProperty("replace", false);
         var1.add("values", var2);
         return var1;
      }
   }

   public static record BuilderEntry(Tag.Entry a, String b) {
      final Tag.Entry entry;
      private final String source;

      public BuilderEntry(Tag.Entry var1, String var2) {
         this.entry = var1;
         this.source = var2;
      }

      public String toString() {
         return this.entry + " (from " + this.source + ")";
      }

      public Tag.Entry entry() {
         return this.entry;
      }

      public String source() {
         return this.source;
      }
   }
}
