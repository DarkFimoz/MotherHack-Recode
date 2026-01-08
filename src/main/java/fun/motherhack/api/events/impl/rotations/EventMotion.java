package fun.motherhack.api.events.impl.rotations;

import fun.motherhack.api.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public class EventMotion extends Event {
    private float yaw, pitch;
    public static float lastYaw, lastPitch;
    
    public void setYaw(float yaw) {
    	this.yaw = yaw;
    	lastYaw = yaw;
    }
    
    public void setPitch(float pitch) {
    	this.pitch = pitch;
    	lastPitch = pitch;
    }
}