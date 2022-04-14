package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class ResourceKeyArgument<T> implements ArgumentType<ResourceKey<T>> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_ATTRIBUTE = new DynamicCommandExceptionType((var0) -> {
      return new TranslatableComponent("attribute.unknown", new Object[]{var0});
   });
   private static final DynamicCommandExceptionType ERROR_INVALID_FEATURE = new DynamicCommandExceptionType((var0) -> {
      return new TranslatableComponent("commands.placefeature.invalid", new Object[]{var0});
   });
   final ResourceKey<? extends Registry<T>> registryKey;

   public ResourceKeyArgument(ResourceKey<? extends Registry<T>> var1) {
      this.registryKey = var1;
   }

   public static <T> ResourceKeyArgument<T> key(ResourceKey<? extends Registry<T>> var0) {
      return new ResourceKeyArgument(var0);
   }

   private static <T> ResourceKey<T> getRegistryType(CommandContext<CommandSourceStack> var0, String var1, ResourceKey<Registry<T>> var2, DynamicCommandExceptionType var3) throws CommandSyntaxException {
      ResourceKey var4 = (ResourceKey)var0.getArgument(var1, ResourceKey.class);
      Optional var5 = var4.cast(var2);
      return (ResourceKey)var5.orElseThrow(() -> {
         return var3.create(var4);
      });
   }

   private static <T> Registry<T> getRegistry(CommandContext<CommandSourceStack> var0, ResourceKey<? extends Registry<T>> var1) {
      return ((CommandSourceStack)var0.getSource()).getServer().registryAccess().registryOrThrow(var1);
   }

   public static Attribute getAttribute(CommandContext<CommandSourceStack> var0, String var1) throws CommandSyntaxException {
      ResourceKey var2 = getRegistryType(var0, var1, Registry.ATTRIBUTE_REGISTRY, ERROR_UNKNOWN_ATTRIBUTE);
      return (Attribute)getRegistry(var0, Registry.ATTRIBUTE_REGISTRY).getOptional(var2).orElseThrow(() -> {
         return ERROR_UNKNOWN_ATTRIBUTE.create(var2.location());
      });
   }

   public static Holder<ConfiguredFeature<?, ?>> getConfiguredFeature(CommandContext<CommandSourceStack> var0, String var1) throws CommandSyntaxException {
      ResourceKey var2 = getRegistryType(var0, var1, Registry.CONFIGURED_FEATURE_REGISTRY, ERROR_INVALID_FEATURE);
      return (Holder)getRegistry(var0, Registry.CONFIGURED_FEATURE_REGISTRY).getHolder(var2).orElseThrow(() -> {
         return ERROR_INVALID_FEATURE.create(var2.location());
      });
   }

   public ResourceKey<T> parse(StringReader var1) throws CommandSyntaxException {
      ResourceLocation var2 = ResourceLocation.read(var1);
      return ResourceKey.create(this.registryKey, var2);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> var1, SuggestionsBuilder var2) {
      Object var4 = var1.getSource();
      if (var4 instanceof SharedSuggestionProvider) {
         SharedSuggestionProvider var3 = (SharedSuggestionProvider)var4;
         return var3.suggestRegistryElements(this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS, var2, var1);
      } else {
         return var2.buildFuture();
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   // $FF: synthetic method
   public Object parse(StringReader var1) throws CommandSyntaxException {
      return this.parse(var1);
   }

   public static class Serializer implements ArgumentSerializer<ResourceKeyArgument<?>> {
      public Serializer() {
      }

      public void serializeToNetwork(ResourceKeyArgument<?> var1, FriendlyByteBuf var2) {
         var2.writeResourceLocation(var1.registryKey.location());
      }

      public ResourceKeyArgument<?> deserializeFromNetwork(FriendlyByteBuf var1) {
         ResourceLocation var2 = var1.readResourceLocation();
         return new ResourceKeyArgument(ResourceKey.createRegistryKey(var2));
      }

      public void serializeToJson(ResourceKeyArgument<?> var1, JsonObject var2) {
         var2.addProperty("registry", var1.registryKey.location().toString());
      }

      // $FF: synthetic method
      // $FF: bridge method
      public void serializeToJson(ArgumentType var1, JsonObject var2) {
         this.serializeToJson((ResourceKeyArgument)var1, var2);
      }

      // $FF: synthetic method
      public ArgumentType deserializeFromNetwork(FriendlyByteBuf var1) {
         return this.deserializeFromNetwork(var1);
      }

      // $FF: synthetic method
      // $FF: bridge method
      public void serializeToNetwork(ArgumentType var1, FriendlyByteBuf var2) {
         this.serializeToNetwork((ResourceKeyArgument)var1, var2);
      }
   }
}
