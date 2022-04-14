package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrTagLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.biome.Biome;

public class LocateBiomeCommand {
   private static final DynamicCommandExceptionType ERROR_BIOME_NOT_FOUND = new DynamicCommandExceptionType((var0) -> {
      return new TranslatableComponent("commands.locatebiome.notFound", new Object[]{var0});
   });
   private static final int MAX_SEARCH_RADIUS = 6400;
   private static final int SEARCH_STEP = 8;

   public LocateBiomeCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> var0) {
      var0.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("locatebiome").requires((var0x) -> {
         return var0x.hasPermission(2);
      })).then(Commands.argument("biome", ResourceOrTagLocationArgument.resourceOrTag(Registry.BIOME_REGISTRY)).executes((var0x) -> {
         return locateBiome((CommandSourceStack)var0x.getSource(), ResourceOrTagLocationArgument.getBiome(var0x, "biome"));
      })));
   }

   private static int locateBiome(CommandSourceStack var0, ResourceOrTagLocationArgument.Result<Biome> var1) throws CommandSyntaxException {
      BlockPos var2 = new BlockPos(var0.getPosition());
      Pair var3 = var0.getLevel().findNearestBiome(var1, var2, 6400, 8);
      if (var3 == null) {
         throw ERROR_BIOME_NOT_FOUND.create(var1.asPrintable());
      } else {
         return LocateCommand.showLocateResult(var0, var1, var2, var3, "commands.locatebiome.success");
      }
   }
}
