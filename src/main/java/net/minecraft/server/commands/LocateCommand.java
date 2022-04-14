package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrTagLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;

public class LocateCommand {
   private static final DynamicCommandExceptionType ERROR_FAILED = new DynamicCommandExceptionType((var0) -> {
      return new TranslatableComponent("commands.locate.failed", new Object[]{var0});
   });
   private static final DynamicCommandExceptionType ERROR_INVALID = new DynamicCommandExceptionType((var0) -> {
      return new TranslatableComponent("commands.locate.invalid", new Object[]{var0});
   });

   public LocateCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> var0) {
      var0.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("locate").requires((var0x) -> {
         return var0x.hasPermission(2);
      })).then(Commands.argument("structure", ResourceOrTagLocationArgument.resourceOrTag(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY)).executes((var0x) -> {
         return locate((CommandSourceStack)var0x.getSource(), ResourceOrTagLocationArgument.getStructureFeature(var0x, "structure"));
      })));
   }

   private static int locate(CommandSourceStack var0, ResourceOrTagLocationArgument.Result<ConfiguredStructureFeature<?, ?>> var1) throws CommandSyntaxException {
      Registry var2 = var0.getLevel().registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
      Either var10000 = var1.unwrap();
      Function var10001 = (var1x) -> {
         return var2.getHolder(var1x).map((var0) -> {
            return HolderSet.direct(var0);
         });
      };
      Objects.requireNonNull(var2);
      HolderSet var3 = (HolderSet)((Optional)var10000.map(var10001, var2::getTag)).orElseThrow(() -> {
         return ERROR_INVALID.create(var1.asPrintable());
      });
      BlockPos var4 = new BlockPos(var0.getPosition());
      ServerLevel var5 = var0.getLevel();
      Pair var6 = var5.getChunkSource().getGenerator().findNearestMapFeature(var5, var3, var4, 100, false);
      if (var6 == null) {
         throw ERROR_FAILED.create(var1.asPrintable());
      } else {
         return showLocateResult(var0, var1, var4, var6, "commands.locate.success");
      }
   }

   public static int showLocateResult(CommandSourceStack var0, ResourceOrTagLocationArgument.Result<?> var1, BlockPos var2, Pair<BlockPos, ? extends Holder<?>> var3, String var4) {
      BlockPos var5 = (BlockPos)var3.getFirst();
      String var6 = (String)var1.unwrap().map((var0x) -> {
         return var0x.location().toString();
      }, (var1x) -> {
         ResourceLocation var10000 = var1x.location();
         return "#" + var10000 + " (" + (String)((Holder)var3.getSecond()).unwrapKey().map((var0) -> {
            return var0.location().toString();
         }).orElse("[unregistered]") + ")";
      });
      int var7 = Mth.floor(dist(var2.getX(), var2.getZ(), var5.getX(), var5.getZ()));
      MutableComponent var8 = ComponentUtils.wrapInSquareBrackets(new TranslatableComponent("chat.coordinates", new Object[]{var5.getX(), "~", var5.getZ()})).withStyle((var1x) -> {
         Style var10000 = var1x.withColor(ChatFormatting.GREEN);
         ClickEvent.Action var10003 = ClickEvent.Action.SUGGEST_COMMAND;
         int var10004 = var5.getX();
         return var10000.withClickEvent(new ClickEvent(var10003, "/tp @s " + var10004 + " ~ " + var5.getZ())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableComponent("chat.coordinates.tooltip")));
      });
      var0.sendSuccess(new TranslatableComponent(var4, new Object[]{var6, var8, var7}), false);
      return var7;
   }

   private static float dist(int var0, int var1, int var2, int var3) {
      int var4 = var2 - var0;
      int var5 = var3 - var1;
      return Mth.sqrt((float)(var4 * var4 + var5 * var5));
   }
}
