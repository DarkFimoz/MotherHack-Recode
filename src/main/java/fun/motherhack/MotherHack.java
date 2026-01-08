package fun.motherhack;

import fun.motherhack.managers.*;
import fun.motherhack.protection.AntiDump;
import fun.motherhack.screen.clickgui.ClickGui;
import fun.motherhack.screen.csgui.MHackGui;
import fun.motherhack.screen.mainmenu.MainMenu;
import fun.motherhack.utils.mediaplayer.MediaPlayer;
import fun.motherhack.utils.render.fonts.Fonts;
import fun.motherhack.utils.sound.GuiSoundHelper;
import fun.motherhack.utils.Wrapper;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.IEventBus;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.fabricmc.api.ModInitializer;
import lombok.*;

import java.io.File;
import java.lang.invoke.MethodHandles;

@Getter
public class MotherHack implements ModInitializer, Wrapper {

    @Getter private static MotherHack instance;

    private IEventBus eventHandler;
    private NotifyManager notifyManager;
    private FriendManager friendManager;
    private ModuleManager moduleManager;
    private CommandManager commandManager;
    private ServerManager serverManager;
    private RotationManager rotationManager;
    private MacroManager macroManager;
    private HudManager hudManager;
    private ConfigManager configManager;
    private WaypointManager waypointManager;
    private AccountManager accountManager;
    private IgnoreManager ignoreManager;
    private ClickGui clickGui;
    private MHackGui mHackGui;
    private MainMenu mainMenu;
    private MediaPlayer mediaPlayer;
    @Setter private boolean panic = false;
    private long initTime;

    public static Logger LOGGER = LogManager.getLogger(MotherHack.class);
    private final File globalsDir = new File(mc.runDirectory, "motherhack");
    private final File configsDir = new File(globalsDir, "configs");
    private final File abItemsDir = new File(globalsDir, "abitems");

    @Override
    public void onInitialize() {
        // Защита от дампа и отладки
        AntiDump.check();
        
        LOGGER.info("[MotherHack] Starting initialization.");
        
        initTime = System.currentTimeMillis();
        instance = this;
        
        // Регистрируем звуки в самом начале (до загрузки ресурсов)
        GuiSoundHelper.init();
        
        createDirs(globalsDir, configsDir, abItemsDir);
        eventHandler = new EventBus();
        eventHandler.registerLambdaFactory("fun.motherhack", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
        notifyManager = new NotifyManager();
        friendManager = new FriendManager();
        moduleManager = new ModuleManager();
        commandManager = new CommandManager();
        serverManager = new ServerManager();
        rotationManager = new RotationManager();
        macroManager = new MacroManager();
        hudManager = new HudManager();
        configManager = new ConfigManager();
        waypointManager = new WaypointManager();
        accountManager = new AccountManager();
        ignoreManager = new IgnoreManager();
        clickGui = new ClickGui();
        mHackGui = new MHackGui();
        mainMenu = new MainMenu();
        mediaPlayer = new MediaPlayer();

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return id("font_reloader");
            }

            @Override
            public void reload(ResourceManager manager) {
                Fonts.initializeFonts();
                LOGGER.info("[MotherHack] Fonts reinitialized after resource reload");
            }
        });

        Fonts.initializeFonts();

        LOGGER.info("[MotherHack] Successfully initialized for {} ms.", System.currentTimeMillis() - initTime);
    }

    private void createDirs(File... file) {
        for (File f : file) f.mkdirs();
    }

    public static Identifier id(String texture) {
        return Identifier.of("motherhack", texture);
    }
    
    public IgnoreManager getIgnoreManager() {
        return ignoreManager;
    }
}

// гей