package net.minecraft.network.protocol.game;

import com.google.common.collect.Sets;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public record ClientboundLoginPacket(int a, boolean b, GameType c, @Nullable GameType d, Set<ResourceKey<Level>> e, RegistryAccess.Frozen f, Holder<DimensionType> g, ResourceKey<Level> h, long i, int j, int k, int l, boolean m, boolean n, boolean o, boolean p) implements Packet<ClientGamePacketListener> {
   private final int playerId;
   private final boolean hardcore;
   private final GameType gameType;
   @Nullable
   private final GameType previousGameType;
   private final Set<ResourceKey<Level>> levels;
   private final RegistryAccess.Frozen registryHolder;
   private final Holder<DimensionType> dimensionType;
   private final ResourceKey<Level> dimension;
   private final long seed;
   private final int maxPlayers;
   private final int chunkRadius;
   private final int simulationDistance;
   private final boolean reducedDebugInfo;
   private final boolean showDeathScreen;
   private final boolean isDebug;
   private final boolean isFlat;

   public ClientboundLoginPacket(FriendlyByteBuf var1) {
      this(var1.readInt(), var1.readBoolean(), GameType.byId(var1.readByte()), GameType.byNullableId(var1.readByte()), (Set)var1.readCollection(Sets::newHashSetWithExpectedSize, (var0) -> {
         return ResourceKey.create(Registry.DIMENSION_REGISTRY, var0.readResourceLocation());
      }), ((RegistryAccess)var1.readWithCodec(RegistryAccess.NETWORK_CODEC)).freeze(), (Holder)var1.readWithCodec(DimensionType.CODEC), ResourceKey.create(Registry.DIMENSION_REGISTRY, var1.readResourceLocation()), var1.readLong(), var1.readVarInt(), var1.readVarInt(), var1.readVarInt(), var1.readBoolean(), var1.readBoolean(), var1.readBoolean(), var1.readBoolean());
   }

   public ClientboundLoginPacket(int var1, boolean var2, GameType var3, @Nullable GameType var4, Set<ResourceKey<Level>> var5, RegistryAccess.Frozen var6, Holder<DimensionType> var7, ResourceKey<Level> var8, long var9, int var11, int var12, int var13, boolean var14, boolean var15, boolean var16, boolean var17) {
      this.playerId = var1;
      this.hardcore = var2;
      this.gameType = var3;
      this.previousGameType = var4;
      this.levels = var5;
      this.registryHolder = var6;
      this.dimensionType = var7;
      this.dimension = var8;
      this.seed = var9;
      this.maxPlayers = var11;
      this.chunkRadius = var12;
      this.simulationDistance = var13;
      this.reducedDebugInfo = var14;
      this.showDeathScreen = var15;
      this.isDebug = var16;
      this.isFlat = var17;
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeInt(this.playerId);
      var1.writeBoolean(this.hardcore);
      var1.writeByte(this.gameType.getId());
      var1.writeByte(GameType.getNullableId(this.previousGameType));
      var1.writeCollection(this.levels, (var0, var1x) -> {
         var0.writeResourceLocation(var1x.location());
      });
      var1.writeWithCodec(RegistryAccess.NETWORK_CODEC, this.registryHolder);
      var1.writeWithCodec(DimensionType.CODEC, this.dimensionType);
      var1.writeResourceLocation(this.dimension.location());
      var1.writeLong(this.seed);
      var1.writeVarInt(this.maxPlayers);
      var1.writeVarInt(this.chunkRadius);
      var1.writeVarInt(this.simulationDistance);
      var1.writeBoolean(this.reducedDebugInfo);
      var1.writeBoolean(this.showDeathScreen);
      var1.writeBoolean(this.isDebug);
      var1.writeBoolean(this.isFlat);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleLogin(this);
   }

   public int playerId() {
      return this.playerId;
   }

   public boolean hardcore() {
      return this.hardcore;
   }

   public GameType gameType() {
      return this.gameType;
   }

   @Nullable
   public GameType previousGameType() {
      return this.previousGameType;
   }

   public Set<ResourceKey<Level>> levels() {
      return this.levels;
   }

   public RegistryAccess.Frozen registryHolder() {
      return this.registryHolder;
   }

   public Holder<DimensionType> dimensionType() {
      return this.dimensionType;
   }

   public ResourceKey<Level> dimension() {
      return this.dimension;
   }

   public long seed() {
      return this.seed;
   }

   public int maxPlayers() {
      return this.maxPlayers;
   }

   public int chunkRadius() {
      return this.chunkRadius;
   }

   public int simulationDistance() {
      return this.simulationDistance;
   }

   public boolean reducedDebugInfo() {
      return this.reducedDebugInfo;
   }

   public boolean showDeathScreen() {
      return this.showDeathScreen;
   }

   public boolean isDebug() {
      return this.isDebug;
   }

   public boolean isFlat() {
      return this.isFlat;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
