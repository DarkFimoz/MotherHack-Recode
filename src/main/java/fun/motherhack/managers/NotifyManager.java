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
        
        Module notificationsModule = MotherHack.getInstance().getModuleManager().getModuleByClass(fun.motherhack.modules.impl.client.Notifications.class);
        if (notificationsModule == null || !notificationsModule.isToggled()) return;
        
        fun.motherhack.modules.impl.client.Notifications notifications = (fun.motherhack.modules.impl.client.Notifications) notificationsModule;
        if (!notifications.isEnabled()) return;
        
        if (notifies.isEmpty()) return;
        
        fun.motherhack.modules.impl.client.Notifications.NotificationPosition pos = notifications.getPosition();
        float startY;
        
        if (pos == fun.motherhack.modules.impl.client.Notifications.NotificationPosition.BottomCenter) {
            startY = mc.getWindow().getScaledHeight() / 2f + 26;
        } else {
            startY = mc.getWindow().getScaledHeight() - 40;
        }
        
        if (notifies.size() > 10) notifies.removeFirst();
        notifies.removeIf(Notify::expired);

        for (Notify notify : Lists.newArrayList(notifies)) {
            startY = (startY - 18f);
            notify.render(e, startY + (notifies.size() * 18f), pos);
        }
    }
}