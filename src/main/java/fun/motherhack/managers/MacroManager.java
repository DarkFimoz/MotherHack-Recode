package fun.motherhack.managers;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventKey;
import fun.motherhack.modules.api.Module;
import fun.motherhack.utils.Wrapper;
import fun.motherhack.utils.macro.Macro;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;

import java.util.ArrayList;
import java.util.List;

public class MacroManager implements Wrapper {

    public MacroManager() {
        MotherHack.getInstance().getEventHandler().subscribe(this);
    }

    @Getter private final List<Macro> macros = new ArrayList<>();
    @Getter private final List<String> names = new ArrayList<>();

    public void add(Macro macro) {
        macros.add(macro);
        names.add(macro.getName());
    }

    public void remove(Macro macro) {
        macros.remove(macro);
        names.remove(macro.getName());
    }

    public void clear() {
        if (!macros.isEmpty()) macros.clear();
        if (!names.isEmpty()) names.clear();
    }

    public boolean isEmpty() {
        return macros.isEmpty() || names.isEmpty();
    }

    public Macro getMacro(String name) {
        for (Macro macro : macros) {
            if (!macro.getName().equalsIgnoreCase(name)) continue;
            return macro;
        }
        
        return null;
    }

    @EventHandler
    public void onKey(EventKey e) {
        if (Module.fullNullCheck() || mc.currentScreen != null || MotherHack.getInstance().isPanic() || macros.isEmpty()) return;

        if (e.getAction() == 1)
            for (Macro macro : macros)
                if (macro.getBind().getKey() == e.getKey()) {
                    if (macro.getCommand().startsWith("/")) mc.player.networkHandler.sendChatCommand(macro.getCommand().replace("/", ""));
                    else mc.player.networkHandler.sendChatMessage(macro.getCommand());
                }
    }
}