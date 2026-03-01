package fun.motherhack.modules.impl.client;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventKey;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.utils.WindowIconManager;
import fun.motherhack.utils.network.ChatUtils;
import net.minecraft.text.Text;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Panic extends Module {
    
    public final EnumSetting<PanicMode> mode;
    
    private static final List<String> VISUAL_MODULES = Arrays.asList(
        "ChinaHat", "DamageParticles", "JumpCircles", "Snow", "Trails", "Wings",
        "ItemPhysics", "Pets", "Models", "SwingAnimations", "Arrows",
        "BreakHighLight", "Particles", "PenisESP", "TargetEsp", "ItemESP",
        "NameTags", "BedTags", "Trajectories"
    );
    
    private final List<Module> saved;
    private String savedConfig;
    
    public Panic() {
        super("Panic", Category.Client);
        this.mode = new EnumSetting<>("Mode", PanicMode.Full);
        this.saved = new ArrayList<>();
        this.savedConfig = null;
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        
        if (fullNullCheck()) {
            return;
        }
        
        PanicMode currentMode = this.mode.getValue();
        
        if (currentMode == PanicMode.Full) {
            ChatUtils.sendMessage(Text.translatable("modules.panic.unhookmessage").getString());
            WindowIconManager.restoreDefaultIcon();
            
            this.savedConfig = MotherHack.getInstance().getConfigManager().getCurrentConfig();
            
            for (Module module : MotherHack.getInstance().getModuleManager().getModules()) {
                if (module == this) {
                    continue;
                }
                
                if (module.isToggled()) {
                    this.saved.add(module);
                    module.setToggled(false);
                }
            }
            
            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                mc.mouse.unlockCursor();
                
                File optionsFile = new File(mc.runDirectory + "/options.txt");
                if (!optionsFile.exists()) {
                    return;
                }
                
                try {
                    FileInputStream fis = new FileInputStream(optionsFile);
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(fis, StandardCharsets.UTF_8));
                    
                    ArrayList<String> lines = new ArrayList<>();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("MotherHack")) {
                            continue;
                        }
                        lines.add(line);
                    }
                    
                    fis.close();
                    
                    BufferedWriter writer = new BufferedWriter(
                        new FileWriter(optionsFile, StandardCharsets.UTF_8));
                    
                    for (String l : lines) {
                        writer.write(l + "\n");
                    }
                    
                    writer.close();
                } catch (Exception e) {
                    // Ignore
                }
                
                MotherHack.getInstance().setPanic(true);
            }).start();
            
        } else if (currentMode == PanicMode.HideCategory) {
            ChatUtils.sendMessage("§7[§5Panic§7] §fРежим скрытия категорий активирован");
            
            for (Module module : MotherHack.getInstance().getModuleManager().getModules()) {
                if (module == this) {
                    continue;
                }
                
                if (module.isToggled()) {
                    if (module.getCategory() != Category.Render) {
                        this.saved.add(module);
                        module.setToggled(false);
                    } else if (!VISUAL_MODULES.contains(module.getName())) {
                        this.saved.add(module);
                        module.setToggled(false);
                    }
                }
            }
            
            MotherHack.getInstance().setPanic(true);
        }
    }
    
    public void onKey(EventKey event) {
        if (fullNullCheck() || mc.currentScreen != null) {
            return;
        }
        
        if (event.getKey() == 267 && event.getAction() == 1) { // Slash key
            if (!MotherHack.getInstance().isPanic()) {
                return;
            }
            
            PanicMode currentMode = this.mode.getValue();
            
            if (currentMode == PanicMode.Full) {
                WindowIconManager.setCustomIcon();
                
                if (this.savedConfig != null) {
                    try {
                        MotherHack.getInstance().getConfigManager().loadConfig(this.savedConfig);
                        ChatUtils.sendMessage(Text.translatable("modules.panic.hookmessage").getString() + 
                            " " + this.savedConfig);
                    } catch (Exception e) {
                        ChatUtils.sendMessage("Ошибка загрузки конфига: " + e.getMessage());
                        
                        for (Module module : this.saved) {
                            if (module == this) {
                                continue;
                            }
                            if (!module.isToggled()) {
                                module.setToggled(true);
                            }
                        }
                        
                        ChatUtils.sendMessage(Text.translatable("modules.panic.hookmessage").getString());
                    }
                } else {
                    for (Module module : this.saved) {
                        if (module == this) {
                            continue;
                        }
                        if (!module.isToggled()) {
                            module.setToggled(true);
                        }
                    }
                    
                    ChatUtils.sendMessage(Text.translatable("modules.panic.hookmessage").getString());
                }
                
            } else if (currentMode == PanicMode.HideCategory) {
                for (Module module : this.saved) {
                    if (module == this) {
                        continue;
                    }
                    if (!module.isToggled()) {
                        module.setToggled(true);
                    }
                }
                
                ChatUtils.sendMessage("§7[§5Panic§7] §fКатегории восстановлены");
            }
            
            MotherHack.getInstance().setPanic(false);
            setToggled(false);
            this.saved.clear();
            this.savedConfig = null;
        }
    }
    
    public enum PanicMode implements Nameable {
        Full("Full"),
        HideCategory("HideCategory");
        
        private final String name;
        
        PanicMode(String name) {
            this.name = name;
        }
        
        @Override
        public String getName() {
            return this.name;
        }
    }
}
