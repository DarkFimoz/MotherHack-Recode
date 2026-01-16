package fun.motherhack.api.mixins.accessors;

import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandshakeC2SPacket.class)
public interface IHandshakeC2SPacket {
    
    @Accessor("protocolVersion")
    @Mutable
    void setProtocolVersion(int version);
    
    @Accessor("protocolVersion")
    int getProtocolVersion();
}
