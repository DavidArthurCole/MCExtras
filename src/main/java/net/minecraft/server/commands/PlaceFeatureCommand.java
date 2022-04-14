package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class PlaceFeatureCommand {
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(new TranslatableComponent("commands.placefeature.failed"));

   public PlaceFeatureCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> var0) {
      var0.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("placefeature").requires((var0x) -> {
         return var0x.hasPermission(2);
      })).then(((RequiredArgumentBuilder)Commands.argument("feature", ResourceKeyArgument.key(Registry.CONFIGURED_FEATURE_REGISTRY)).executes((var0x) -> {
         return placeFeature((CommandSourceStack)var0x.getSource(), ResourceKeyArgument.getConfiguredFeature(var0x, "feature"), new BlockPos(((CommandSourceStack)var0x.getSource()).getPosition()));
      })).then(Commands.argument("pos", BlockPosArgument.blockPos()).executes((var0x) -> {
         return placeFeature((CommandSourceStack)var0x.getSource(), ResourceKeyArgument.getConfiguredFeature(var0x, "feature"), BlockPosArgument.getLoadedBlockPos(var0x, "pos"));
      }))));
   }

   public static int placeFeature(CommandSourceStack var0, Holder<ConfiguredFeature<?, ?>> var1, BlockPos var2) throws CommandSyntaxException {
      ServerLevel var3 = var0.getLevel();
      ConfiguredFeature var4 = (ConfiguredFeature)var1.value();
      if (!var4.place(var3, var3.getChunkSource().getGenerator(), var3.getRandom(), var2)) {
         throw ERROR_FAILED.create();
      } else {
         String var5 = (String)var1.unwrapKey().map((var0x) -> {
            return var0x.location().toString();
         }).orElse("[unregistered]");
         var0.sendSuccess(new TranslatableComponent("commands.placefeature.success", new Object[]{var5, var2.getX(), var2.getY(), var2.getZ()}), true);
         return 1;
      }
   }
}
