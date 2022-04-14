package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.inventory.MenuType;

public class ClientboundOpenScreenPacket implements Packet<ClientGamePacketListener> {
   private final int containerId;
   private final int type;
   private final Component title;

   public ClientboundOpenScreenPacket(int var1, MenuType<?> var2, Component var3) {
      this.containerId = var1;
      this.type = Registry.MENU.getId(var2);
      this.title = var3;
   }

   public ClientboundOpenScreenPacket(FriendlyByteBuf var1) {
      this.containerId = var1.readVarInt();
      this.type = var1.readVarInt();
      this.title = var1.readComponent();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeVarInt(this.containerId);
      var1.writeVarInt(this.type);
      var1.writeComponent(this.title);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleOpenScreen(this);
   }

   public int getContainerId() {
      return this.containerId;
   }

   @Nullable
   public MenuType<?> getType() {
      return (MenuType)Registry.MENU.byId(this.type);
   }

   public Component getTitle() {
      return this.title;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
