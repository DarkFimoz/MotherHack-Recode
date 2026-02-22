package fun.motherhack.managers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CapeManager {
    private static final String CAPES_PATH = "assets/motherhack/capes/";
    private final List<Cape> capes = new ArrayList<>();
    private Cape selectedCape = null;

    public CapeManager() {
        loadCapes();
    }

    private void loadCapes() {
        // Добавляем доступные плащи
        capes.add(new Cape("Dev", Identifier.of("motherhack", "capes/dev.png")));
        capes.add(new Cape("Tester", Identifier.of("motherhack", "capes/tester.png")));
        capes.add(new Cape("Star", Identifier.of("motherhack", "capes/starcape.png")));
        capes.add(new Cape("BK Group", Identifier.of("motherhack", "capes/bkgroup.png")));
        capes.add(new Cape("FB Group", Identifier.of("motherhack", "capes/fbgroup.png")));
    }

    public List<Cape> getCapes() {
        return capes;
    }

    public Cape getSelectedCape() {
        return selectedCape;
    }

    public void setSelectedCape(Cape cape) {
        this.selectedCape = cape;
    }

    public void setSelectedCape(String name) {
        for (Cape cape : capes) {
            if (cape.getName().equalsIgnoreCase(name)) {
                this.selectedCape = cape;
                return;
            }
        }
    }

    public Cape getCapeByName(String name) {
        for (Cape cape : capes) {
            if (cape.getName().equalsIgnoreCase(name)) {
                return cape;
            }
        }
        return null;
    }

    public List<String> getCapeNames() {
        List<String> names = new ArrayList<>();
        for (Cape cape : capes) {
            names.add(cape.getName());
        }
        return names;
    }

    public static class Cape {
        private final String name;
        private final Identifier texture;

        public Cape(String name, Identifier texture) {
            this.name = name;
            this.texture = texture;
        }

        public String getName() {
            return name;
        }

        public Identifier getTexture() {
            return texture;
        }
    }
}
