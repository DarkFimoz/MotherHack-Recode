package fun.motherhack.modules.impl.client;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventKey;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.utils.network.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.resource.language.I18n;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Panic extends Module {

    public Panic() {
        super("Panic", Category.Client);
    }

    private final List<Module> saved = new ArrayList<>();

    @Override
    public void onEnable() {
        super.onEnable();
        if (fullNullCheck()) return;
        ChatUtils.sendMessage(I18n.translate("modules.panic.unhookmessage"));
        for (Module module : MotherHack.getInstance().getModuleManager().getModules()) {
            if (module == this) continue;
            if (module.isToggled()) {
                saved.add(module);
                module.setToggled(false);
            }
        }

        new Thread(() -> {
            try {
                Thread.sleep(10000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mc.inGameHud.getChatHud().clear(false);
            try {
                File file = new File(mc.runDirectory + "/logs/" + "latest.log");
                if (!file.exists()) return;
                FileInputStream stream = new FileInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                ArrayList<String> lines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("MotherHack")) continue;
                    lines.add(line);
                }
                stream.close();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
                    for (String s : lines) writer.write(s + "\n");
                } catch (Exception ignored) {}
            } catch (Exception ignored) {}
            MotherHack.getInstance().setPanic(true);
        }).start();
    }

    @EventHandler
    public void onKey(EventKey e) {
        if (fullNullCheck()) return;

        if (e.getKey() == GLFW.GLFW_KEY_PAGE_DOWN && e.getAction() == 1 && MotherHack.getInstance().isPanic()) {
            for (Module module : saved) {
                if (module == this) continue;
                if (!module.isToggled()) module.setToggled(true);
            }

            ChatUtils.sendMessage(I18n.translate("modules.panic.hookmessage"));
            MotherHack.getInstance().setPanic(false);
            setToggled(false);
        }
    }
}