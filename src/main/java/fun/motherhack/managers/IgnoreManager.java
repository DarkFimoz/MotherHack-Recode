package fun.motherhack.managers;

import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Getter
public class IgnoreManager {
    private final Set<String> ignoredPlayers = new HashSet<>();
    
    public void addPlayer(String username) {
        ignoredPlayers.add(username.toLowerCase());
    }
    
    public void removePlayer(String username) {
        ignoredPlayers.remove(username.toLowerCase());
    }
    
    public boolean isIgnored(String username) {
        return ignoredPlayers.contains(username.toLowerCase());
    }
}
