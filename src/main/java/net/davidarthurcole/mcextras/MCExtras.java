package net.davidarthurcole.mcextras;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.natamus.collective_fabric.fabric.callbacks.CollectiveChatEvents;
import net.davidarthurcole.mcextras.events.MCExtrasDupeChatEvent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class MCExtras implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("mcextras");

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		//registerEvents();
		screenshotCmds();
	}

	static void screenshotCmds(){

		MinecraftClient client = MinecraftClient.getInstance();
		LiteralCommandNode<FabricClientCommandSource> openScreenshotsNode = ClientCommandManager.DISPATCHER.register(ClientCommandManager.literal("openscreenshots")
				.executes(context -> loadScreenshots(client)));

		ClientCommandManager.DISPATCHER.register(ClientCommandManager.literal("oss")
				.executes(context -> loadScreenshots(client)));
	}

	//Given a client, open the screenshots folder
	public static int loadScreenshots(MinecraftClient client) {
		//Didn't want to go through the hassle of trying to figure out the path myself, this works :shrug:
		Util.getOperatingSystem().open(client.getResourcePackDir().toURI().toString().replace("resourcepacks", "screenshots"));
		return 1;
	}
}
