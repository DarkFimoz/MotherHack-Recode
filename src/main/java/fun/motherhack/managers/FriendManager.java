package fun.motherhack.managers;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class FriendManager {

    @Getter private final List<String> friends = new ArrayList<>();

    public void add(String friend) {
        if (!friend.isEmpty()) friends.add(friend);
    }

    public void remove(String friend) {
        friends.remove(friend);
    }

    public void clear() {
        friends.clear();
    }

    public boolean isFriend(String friend) {
        return friends.contains(friend);
    }

    public boolean isEmpty() {
        return friends.isEmpty();
    }
}