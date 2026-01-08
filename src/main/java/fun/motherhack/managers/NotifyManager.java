package fun.motherhack.managers;

import com.google.common.collect.Lists;
import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.modules.api.Module;
import fun.motherhack.utils.Wrapper;
import fun.motherhack.utils.notify.Notify;
import meteordevelopment.orbit.EventHandler;

import java.util.*;

public class NotifyManager implements Wrapper {

    public NotifyManager() {
        MotherHack.getInstance().getEventHandler().subscribe(this);
    }

    private final List<Notify> notifies = new ArrayList<>();

    public void add(Notify notify) {
        notifies.add(notify);
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (Module.fullNullCheck() || MotherHack.getInstance().isPanic()) return;
        if (notifies.isEmpty()) return;
        float startY = mc.getWindow().getScaledHeight() / 2f + 26;
        if (notifies.size() > 10) notifies.removeFirst();
        notifies.removeIf(Notify::expired);

        for (Notify notify : Lists.newArrayList(notifies)) {
            startY = (startY - 16f);
            notify.render(e, startY + (notifies.size() * 16f));
        }
    }
}