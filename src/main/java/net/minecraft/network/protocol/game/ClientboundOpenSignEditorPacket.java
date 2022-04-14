package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class ClientboundOpenSignEditorPacket implements Packet<ClientGamePacketListener> {
   private final BlockPos pos;

   public ClientboundOpenSignEditorPacket(BlockPos var1) {
      this.pos = var1;
   }

   public ClientboundOpenSignEditorPacket(FriendlyByteBuf var1) {
      this.pos = var1.readBlockPos();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeBlockPos(this.pos);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleOpenSignEditor(this);
   }

   public BlockPos getPos() {
      return this.pos;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
