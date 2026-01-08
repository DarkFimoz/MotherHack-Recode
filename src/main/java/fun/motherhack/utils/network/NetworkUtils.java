package fun.motherhack.utils.network;

import fun.motherhack.utils.Wrapper;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.network.packet.Packet;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class NetworkUtils implements Wrapper {

    @Getter private final List<Packet<?>> silentPackets = new ArrayList<>();

    public void sendSilentPacket(Packet<?> packet) {
        silentPackets.add(packet);
        mc.getNetworkHandler().sendPacket(packet);
    }

    public void sendPacket(Packet<?> packet) {
        mc.getNetworkHandler().sendPacket(packet);
    }
}