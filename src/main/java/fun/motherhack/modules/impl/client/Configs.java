package fun.motherhack.modules.impl.client;

import fun.motherhack.MotherHack;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.ButtonSetting;
import fun.motherhack.modules.settings.impl.StringSetting;
import fun.motherhack.utils.notify.Notify;
import fun.motherhack.utils.notify.NotifyIcons;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Configs extends Module {

    private final StringSetting configName = new StringSetting("Config Name", "newconfig", false);
    private final ButtonSetting saveButton = new ButtonSetting("Save Config", () -> {
        try {
            MotherHack.getInstance().getConfigManager().saveConfig(configName.getValue());
            MotherHack.getInstance().getNotifyManager().add(new Notify(NotifyIcons.successIcon, "Config '" + configName.getValue() + "' saved!", 2000));
            refreshConfigList();
        } catch (Exception e) {
            MotherHack.getInstance().getNotifyManager().add(new Notify(NotifyIcons.failIcon, "Failed to save config!", 2000));
            e.printStackTrace();
        }
    });
    private final ButtonSetting loadButton = new ButtonSetting("Load Config", () -> {
        try {
            MotherHack.getInstance().getConfigManager().loadConfig(configName.getValue());
            MotherHack.getInstance().getNotifyManager().add(new Notify(NotifyIcons.successIcon, "Config '" + configName.getValue() + "' loaded!", 2000));
        } catch (Exception e) {
            MotherHack.getInstance().getNotifyManager().add(new Notify(NotifyIcons.failIcon, "Failed to load config!", 2000));
            e.printStackTrace();
        }
    });
    // Removed separate delete button, now each config has its own delete button
    private final ButtonSetting refreshButton = new ButtonSetting("Refresh List", this::refreshConfigList);
    private final ButtonSetting openFolderButton = new ButtonSetting("Open Folder", () -> {
        try {
            File configsDir = MotherHack.getInstance().getConfigsDir();
            if (!configsDir.exists()) {
                configsDir.mkdirs();
            }
            Desktop.getDesktop().open(configsDir);
            MotherHack.getInstance().getNotifyManager().add(new Notify(NotifyIcons.successIcon, "Opened configs folder!", 2000));
        } catch (IOException e) {
            MotherHack.getInstance().getNotifyManager().add(new Notify(NotifyIcons.failIcon, "Failed to open configs folder: " + e.getMessage(), 2000));
            e.printStackTrace();
        }
    });

    private final List<ButtonSetting> configLoadButtons = new ArrayList<>();

    public Configs() {
        super("Configs", Category.Client);
    }

    private void refreshConfigList() {
        if (MotherHack.getInstance().getConfigManager() == null) return; // Not initialized yet

        // Remove old config buttons
        getSettings().removeAll(configLoadButtons);
        configLoadButtons.clear();

        // Add new config buttons
        File configDir = MotherHack.getInstance().getConfigsDir();
        if (configDir.exists() && configDir.isDirectory()) {
            File[] files = configDir.listFiles((dir, name) -> name.endsWith(".mhack"));
            if (files != null) {
                String currentConfig = MotherHack.getInstance().getConfigManager().getCurrentConfig();
                for (File file : files) {
                    String configName = file.getName().replace(".mhack", "");
                    boolean isCurrent = configName.equals(currentConfig);

                    // Load button
                    ButtonSetting loadConfigButton = new ButtonSetting(
                        (isCurrent ? "Current: " : "Load: ") + configName,
                        () -> {
                            try {
                                MotherHack.getInstance().getConfigManager().loadConfig(configName);
                                MotherHack.getInstance().getNotifyManager().add(new Notify(NotifyIcons.successIcon, "Config '" + configName + "' loaded!", 2000));
                                refreshConfigList(); // Refresh to update current indicator
                            } catch (Exception e) {
                                MotherHack.getInstance().getNotifyManager().add(new Notify(NotifyIcons.failIcon, "Failed to load config!", 2000));
                                e.printStackTrace();
                            }
                        }
                    );
                    configLoadButtons.add(loadConfigButton);
                    getSettings().add(loadConfigButton);

                    // Delete button (only if not current)
                    if (!isCurrent) {
                        ButtonSetting deleteConfigButton = new ButtonSetting("Delete: " + configName, () -> {
                            try {
                                MotherHack.getInstance().getConfigManager().deleteConfig(configName);
                                MotherHack.getInstance().getNotifyManager().add(new Notify(NotifyIcons.successIcon, "Config '" + configName + "' deleted!", 2000));
                                refreshConfigList();
                            } catch (Exception e) {
                                MotherHack.getInstance().getNotifyManager().add(new Notify(NotifyIcons.failIcon, "Failed to delete config!", 2000));
                                e.printStackTrace();
                            }
                        });
                        configLoadButtons.add(deleteConfigButton);
                        getSettings().add(deleteConfigButton);
                    }
                }
            }
        }
    }
}