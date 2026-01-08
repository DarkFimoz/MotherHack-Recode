package fun.motherhack.api.events.impl;

import fun.motherhack.api.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.packet.Packet;

@AllArgsConstructor @Getter @Setter
public class EventPacket extends Event {
    private Packet<?> packet;

    public static class Receive extends EventPacket {
        public Receive(Packet<?> packet) {
            super(packet);
        }
    }

    public static class Send extends EventPacket {
        public Send(Packet<?> packet) {
            super(packet);
        }
    }

    public static class All extends EventPacket {
        public All(Packet<?> packet) {
            super(packet);
        }
    }
}