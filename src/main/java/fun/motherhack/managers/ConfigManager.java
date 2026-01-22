package fun.motherhack.managers;

import com.google.gson.*;
import fun.motherhack.MotherHack;
import fun.motherhack.hud.HudElement;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.client.Panic;
import fun.motherhack.modules.impl.client.UI;
import fun.motherhack.modules.settings.Setting;
import fun.motherhack.modules.settings.api.*;
import fun.motherhack.modules.settings.impl.*;
import fun.motherhack.utils.macro.Macro;
import fun.motherhack.utils.other.FileUtils;
import lombok.Cleanup;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigManager {

    private String currentConfig = "default";

    public String getCurrentConfig() {
        return currentConfig;
    }
    private final String extension = ".mhack";

    public ConfigManager() {
        loadAll();
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveAll));
    }

    private void loadAll() {
        try {
            loadGlobals();
            loadConfig(currentConfig);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    private void saveAll() {
        try {
            saveGlobals();
            saveConfig(currentConfig);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public void saveConfig(String config) throws Exception {
        File configsDir = MotherHack.getInstance().getConfigsDir();
        if (!configsDir.exists() && !configsDir.mkdirs()) {
            throw new IOException("Failed to create configs directory");
        }
        
        File configFile = new File(configsDir, config + extension);
        FileUtils.reset(configFile.getAbsolutePath());
        
        JsonObject object = new JsonObject();
        object.add("config", new JsonPrimitive(config));
        object.add("modules", serializeModules());
        
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(object.toString())));
            this.currentConfig = config;
            System.out.println("Config saved successfully: " + configFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Failed to save config: " + e.getMessage());
            throw e;
        }
    }

    public void loadConfig(String config) throws Exception {
        File configFile = new File(MotherHack.getInstance().getConfigsDir(), config + extension);
        if (!configFile.exists()) {
            System.err.println("Config file not found: " + configFile.getAbsolutePath());
            throw new FileNotFoundException("Config file not found: " + config);
        }
        
        try (InputStream stream = Files.newInputStream(configFile.toPath());
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            
            JsonElement element = JsonParser.parseReader(reader);
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                if (object.has("modules")) {
                    deserializeModules(object.get("modules").getAsJsonObject());
                    this.currentConfig = config;
                    System.out.println("Config loaded successfully: " + config);
                } else {
                    System.err.println("Invalid config format: missing 'modules' field");
                    throw new IOException("Invalid config format: missing 'modules' field");
                }
            } else {
                System.err.println("Invalid config format: not a JSON object");
                throw new IOException("Invalid config format: not a JSON object");
            }
        } catch (Exception e) {
            System.err.println("Failed to load config: " + e.getMessage());
            throw e;
        }
    }

    public void deleteConfig(String config) throws Exception {
        new File(MotherHack.getInstance().getConfigsDir() + "/" + config + extension).delete();
    }

    public void saveGlobals() throws Exception {
        FileUtils.reset(MotherHack.getInstance().getGlobalsDir() + "/" + "globals" + extension);
        JsonObject object = new JsonObject();
        object.add("config", new JsonPrimitive(currentConfig));
        JsonArray friendsArray = new JsonArray();
        MotherHack.getInstance().getFriendManager().getFriends().forEach(friendsArray::add);
        object.add("friends", friendsArray);
        JsonArray macrosArray = new JsonArray();
        MotherHack.getInstance().getMacroManager().getMacros().forEach(macro -> macrosArray.add(macro.getName() + ":" + macro.getCommand() + ":" + macro.getBind().getKey()));
        object.add("macros", macrosArray);

        // Save MHackGUI position
        JsonObject guiPos = new JsonObject();
        guiPos.add("x", new JsonPrimitive(MotherHack.getInstance().getMHackGui().getGuiX()));
        guiPos.add("y", new JsonPrimitive(MotherHack.getInstance().getMHackGui().getGuiY()));
        object.add("mhackgui_pos", guiPos);

        @Cleanup OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(MotherHack.getInstance().getGlobalsDir() + "/" + "globals" + extension), StandardCharsets.UTF_8);
        writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(object.toString())));
    }

    public void loadGlobals() throws Exception {
        if (!FileUtils.exists(MotherHack.getInstance().getGlobalsDir() + "/" + "globals" + extension)) return;
        InputStream stream = Files.newInputStream(Paths.get(MotherHack.getInstance().getGlobalsDir() + "/" + "globals" + extension));
        JsonObject object = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();

        if (object.has("config")) currentConfig = object.get("config").getAsString();
        if (object.has("friends")) {
            for (JsonElement element : object.get("friends").getAsJsonArray()) {
                if (MotherHack.getInstance().getFriendManager().isFriend(element.getAsString())) continue;
                MotherHack.getInstance().getFriendManager().add(element.getAsString());
            }
        }
        if (object.has("macros")) {
            for (JsonElement element : object.get("macros").getAsJsonArray()) {
                String[] split = element.getAsString().split(":", 3);
                if (split.length < 3) continue;
                String name =  split[0];
                String command = split[1];
                Bind bind = new Bind(Integer.parseInt(split[2]), false);
                MotherHack.getInstance().getMacroManager().add(new Macro(name, command, bind));
            }
        }

        // Load MHackGUI position
        if (object.has("mhackgui_pos") && MotherHack.getInstance().getMHackGui() != null) {
            JsonObject guiPos = object.get("mhackgui_pos").getAsJsonObject();
            if (guiPos.has("x") && guiPos.has("y")) {
                MotherHack.getInstance().getMHackGui().setGuiX(guiPos.get("x").getAsFloat());
                MotherHack.getInstance().getMHackGui().setGuiY(guiPos.get("y").getAsFloat());
            }
        }
    }

    private JsonObject serializeModules() {
        JsonObject modules = new JsonObject();
        for (Module module : MotherHack.getInstance().getModuleManager().getModules()) {
            JsonObject object = new JsonObject();
            object.add("toggled", new JsonPrimitive(module.isToggled()));
            object.add("bind", new JsonPrimitive(module.getBind().getKey() + ", " + module.getBind().isMouse()));

            JsonObject settings = new JsonObject();
            for (Setting<?> s : module.getSettings()) {
                if (s instanceof BooleanSetting) settings.add(s.getName(), new JsonPrimitive((Boolean) s.getValue()));
                else if (s instanceof NumberSetting) settings.add(s.getName(), new JsonPrimitive((Float) s.getValue()));
                else if (s instanceof StringSetting) settings.add(s.getName(), new JsonPrimitive((String) s.getValue()));
                else if (s instanceof EnumSetting<?> enums) settings.add(s.getName(), new JsonPrimitive(((Enum<?>) enums.getValue()).name()));
                else if (s instanceof BindSetting bind) settings.add(s.getName(), new JsonPrimitive(bind.getValue().getKey() + ", " + bind.getValue().isMouse()));
                else if (s instanceof ListSetting list) {
                    JsonObject list2 = new JsonObject();
                    for (BooleanSetting setting : list.getValue()) list2.add(setting.getName(), new JsonPrimitive(setting.getValue()));
                    settings.add(list.getName(), list2);
                }
            }

            object.add("settings", settings);
            modules.add(module.getName(), object);
        }

        for (HudElement element : MotherHack.getInstance().getHudManager().getHudElements()) {
            JsonObject object = new JsonObject();
            JsonObject settings = new JsonObject();
            for (Setting<?> s : element.getSettings()) {
             	if (s instanceof BooleanSetting) settings.add(s.getName(), new JsonPrimitive((Boolean) s.getValue()));
             	else if (s instanceof NumberSetting) settings.add(s.getName(), new JsonPrimitive((Float) s.getValue()));
             	else if (s instanceof PositionSetting pos) settings.add(s.getName(), new JsonPrimitive(pos.getValue().getX() + ", " + pos.getValue().getY()));
            }
            object.add("settings", settings);
            modules.add(element.getName(), object);
        }

        // Save HUD elements visibility settings
        JsonObject hudElementsVisibility = new JsonObject();
        ListSetting elements = MotherHack.getInstance().getHudManager().getElements();
        for (BooleanSetting setting : elements.getValue()) {
            hudElementsVisibility.add(setting.getName(), new JsonPrimitive(setting.getValue()));
        }
        modules.add("_HudElementsVisibility", hudElementsVisibility);

        return modules;
    }

    private void deserializeModules(JsonObject modules) {
        for (Module module : MotherHack.getInstance().getModuleManager().getModules()) {
            if (!modules.has(module.getName())) {
                module.setToggled(false, true);
                for (Setting<?> setting : module.getSettings()) setting.reset();
                continue;
            }

            JsonObject object = modules.get(module.getName()).getAsJsonObject();
            if (!(module instanceof UI || module instanceof Panic)) module.setToggled(object.has("toggled") && object.get("toggled").getAsBoolean(), true);

            if (object.has("bind")) {
                String[] data = object.get("bind").getAsString().split(", ");
                if (data.length == 2) {
                    int key = Integer.parseInt(data[0]);
                    boolean mouse = Boolean.parseBoolean(data[1]);
                    module.setBind(new Bind(key, mouse));
                }
            }

            if (!object.has("settings")) {
                for (Setting<?> setting : module.getSettings()) setting.reset();
                continue;
            }

            JsonObject settings = object.get("settings").getAsJsonObject();

            for (Setting<?> s : module.getSettings()) {
                if (!settings.has(s.getName())) {
                    s.reset();
                    continue;
                }

                JsonElement element = settings.get(s.getName());
                if (s instanceof BooleanSetting) ((BooleanSetting) s).setValue(element.getAsBoolean());
                else if (s instanceof NumberSetting) ((NumberSetting) s).setValue(element.getAsFloat());
                else if (s instanceof EnumSetting<?>) ((EnumSetting<?>) s).setEnumValue(element.getAsString());
                else if (s instanceof StringSetting) ((StringSetting) s).setValue(element.getAsString());
                else if (s instanceof BindSetting) {
                    String[] data = element.getAsString().split(", ");
                    if (data.length == 2) {
                        int key = Integer.parseInt(data[0]);
                        boolean mouse = Boolean.parseBoolean(data[1]);
                        ((BindSetting) s).setValue(new Bind(key, mouse));
                    }
                } else if (s instanceof ListSetting list) {
                    JsonObject list2 = element.getAsJsonObject();
                    for (BooleanSetting setting : list.getValue()) if (list2.has(setting.getName())) setting.setValue(list2.get(setting.getName()).getAsBoolean());
                }
            }
        }

        for (HudElement element : MotherHack.getInstance().getHudManager().getHudElements()) {
            if (!modules.has(element.getName())) {
                element.setToggled(false);
                for (Setting<?> setting : element.getSettings()) setting.reset();
                continue;
            }

            JsonObject object = modules.get(element.getName()).getAsJsonObject();

            if (!object.has("settings")) {
                for (Setting<?> setting : element.getSettings()) setting.reset();
                continue;
            }

            JsonObject settings = object.get("settings").getAsJsonObject();
            for (Setting<?> s : element.getSettings()) {
                if (!settings.has(s.getName())) {
                    s.reset();
                    continue;
                }

                JsonElement element2 = settings.get(s.getName());
                if (s instanceof BooleanSetting) ((BooleanSetting) s).setValue(element2.getAsBoolean());
                else if (s instanceof NumberSetting) ((NumberSetting) s).setValue(element2.getAsFloat());
                else if (s instanceof PositionSetting) {
                    String[] data = element2.getAsString().split(", ");
                    if (data.length == 2) {
                        float x = Float.parseFloat(data[0]);
                        float y = Float.parseFloat(data[1]);
                        ((PositionSetting) s).setValue(new Position(x, y));
                    }
                }
            }
        }

        // Load HUD elements visibility settings
        if (modules.has("_HudElementsVisibility")) {
            JsonObject hudElementsVisibility = modules.get("_HudElementsVisibility").getAsJsonObject();
            ListSetting elements = MotherHack.getInstance().getHudManager().getElements();
            for (BooleanSetting setting : elements.getValue()) {
                if (hudElementsVisibility.has(setting.getName())) {
                    setting.setValue(hudElementsVisibility.get(setting.getName()).getAsBoolean());
                }
            }
        }
    }
}