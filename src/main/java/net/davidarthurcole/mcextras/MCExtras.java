package net.davidarthurcole.mcextras;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;

public class MCExtras implements ModInitializer {

	private static final String rscPath = MinecraftClient.getInstance().getResourcePackDir().toURI().toString();

	@Override
	public void onInitialize() {
		screenshotCmds();
		logCmds();
	}

	static void screenshotCmds(){
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			//Register both /openscreenshots and /oss
			dispatcher.register(ClientCommandManager.literal("oss").executes(MCExtras::loadScreenshots));
			dispatcher.register(ClientCommandManager.literal("openscreenshots").executes(MCExtras::loadScreenshots));
		});
	}

	static void logCmds(){
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			//Register both /openlogs and /ols
			dispatcher.register(ClientCommandManager.literal("ols").executes(MCExtras::loadLogs));
			dispatcher.register(ClientCommandManager.literal("openlogs").executes(MCExtras::loadLogs));

			//Register both /openlatest and /ol
			dispatcher.register(ClientCommandManager.literal("ol").executes(MCExtras::openLatest));
			dispatcher.register(ClientCommandManager.literal("openlatest").executes(MCExtras::openLatest));
		});
	}

	//Open the screenshots folder
	static int loadScreenshots(CommandContext<FabricClientCommandSource> commandContext) {
		//Didn't want to go through the hassle of trying to figure out the path myself, this works :shrug:
		Util.getOperatingSystem().open(rscPath.replace("resourcepacks", "screenshots"));
		return 1;
	}

	//Open the logs folder
	static int loadLogs(CommandContext<FabricClientCommandSource> commandContext) {
		//Didn't want to go through the hassle of trying to figure out the path myself, this works :shrug:
		Util.getOperatingSystem().open(rscPath.replace("resourcepacks", "logs"));
		return 1;
	}

	//Open the latest log
	static int openLatest(CommandContext<FabricClientCommandSource> commandContext) {
		//Didn't want to go through the hassle of trying to figure out the path myself, this works :shrug:
		Util.getOperatingSystem().open(rscPath.replace("resourcepacks", "logs/latest.log"));
		return 1;
	}

}
