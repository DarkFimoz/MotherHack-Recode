package fun.motherhack.modules.impl.client;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.StringSetting;
import fun.motherhack.modules.settings.impl.BooleanSetting;

public class VersionSpoof extends Module {

    public EnumSetting<Version> version = new EnumSetting<>("settings.versionspoof.version", Version.V1_21_4);
    public BooleanSetting customBrand = new BooleanSetting("settings.versionspoof.custombrand", false);
    public StringSetting brandName = new StringSetting("settings.versionspoof.brandname", "MotherHack 1.21.4", false);

    public VersionSpoof() {
        super("VersionSpoof", Category.Client);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    public enum Version implements Nameable {
        V1_21_4("1.21.4", 769),
        V1_20_1("1.20.1", 763),
        V1_17_1("1.17.1", 756),
        V1_11_1("1.11.1", 316);

        private final String name;
        private final int protocol;

        Version(String name, int protocol) {
            this.name = name;
            this.protocol = protocol;
        }

        @Override
        public String getName() {
            return name;
        }

        public int getProtocol() {
            return protocol;
        }
    }
}
