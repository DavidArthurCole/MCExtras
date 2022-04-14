package net.minecraft.network.protocol.game;

import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.apache.commons.lang3.Validate;

public class ClientboundSoundPacket implements Packet<ClientGamePacketListener> {
   public static final float LOCATION_ACCURACY = 8.0F;
   private final SoundEvent sound;
   private final SoundSource source;
   private final int x;
   private final int y;
   private final int z;
   private final float volume;
   private final float pitch;

   public ClientboundSoundPacket(SoundEvent var1, SoundSource var2, double var3, double var5, double var7, float var9, float var10) {
      Validate.notNull(var1, "sound", new Object[0]);
      this.sound = var1;
      this.source = var2;
      this.x = (int)(var3 * 8.0D);
      this.y = (int)(var5 * 8.0D);
      this.z = (int)(var7 * 8.0D);
      this.volume = var9;
      this.pitch = var10;
   }

   public ClientboundSoundPacket(FriendlyByteBuf var1) {
      this.sound = (SoundEvent)Registry.SOUND_EVENT.byId(var1.readVarInt());
      this.source = (SoundSource)var1.readEnum(SoundSource.class);
      this.x = var1.readInt();
      this.y = var1.readInt();
      this.z = var1.readInt();
      this.volume = var1.readFloat();
      this.pitch = var1.readFloat();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeVarInt(Registry.SOUND_EVENT.getId(this.sound));
      var1.writeEnum(this.source);
      var1.writeInt(this.x);
      var1.writeInt(this.y);
      var1.writeInt(this.z);
      var1.writeFloat(this.volume);
      var1.writeFloat(this.pitch);
   }

   public SoundEvent getSound() {
      return this.sound;
   }

   public SoundSource getSource() {
      return this.source;
   }

   public double getX() {
      return (double)((float)this.x / 8.0F);
   }

   public double getY() {
      return (double)((float)this.y / 8.0F);
   }

   public double getZ() {
      return (double)((float)this.z / 8.0F);
   }

   public float getVolume() {
      return this.volume;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleSoundEvent(this);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}