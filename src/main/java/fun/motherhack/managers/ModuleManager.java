package fun.motherhack.managers;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventMouse;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.api.events.impl.EventKey;
import fun.motherhack.modules.impl.combat.*;
import fun.motherhack.modules.impl.movement.*;
import fun.motherhack.modules.impl.render.*;
import fun.motherhack.modules.impl.render.Particles.ParticlesModule;
import fun.motherhack.modules.impl.render.motionblur.MotionBlur;
import fun.motherhack.modules.impl.misc.*;
import fun.motherhack.modules.impl.client.*;
import fun.motherhack.modules.settings.Setting;
import fun.motherhack.utils.Wrapper;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class ModuleManager implements Wrapper {

    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        MotherHack.getInstance().getEventHandler().subscribe(this);
        addModules(
                new Sprint(),
                new UI(),
                new MHACKGUI(),
                new Configs(),
                new CustomBackground(),
                new Cape(),
                new NameTags(),
                new GuiMove(),
                new RPC(),
                new MultiTask(),
                new Aura(),
                new NoPush(),
                new NoRender(),
                new FastUse(),
                new MoveFix(),
                new FakeLag(),
                new NoAttackCooldown(),
                new NoSlow(),
                new NoWeb(),
                new AutoVault(),
                new WindHop(),
                new TargetEsp(),
                new NoFriendDamage(),
                new NoEntityTrace(),
                new Panic(),
                new NoJumpDelay(),
                new ItemHelper(),
                new AutoBuy(),
                new AuctionHelper(),
                new OffHand(),
                new AutoTotem(),
                new AutoShield(),
                new AutoGapple(),
                new AntiBot(),
                new DamageParticles(),
                new Fullbright(),
                new ElytraHelper(),
                new ClickPearl(),
                new ElytraForward(),
                new ElytraBooster(),
                new Targets(),
                new Teams(),
                new TargetStrafe(),
                new Scaffold(),
                new FuntimeHelper(),
                new AutoAccept(),
                new PotionTracker(),
                new UseTracker(),
                new ScoreboardHealth(),
                new MessageAppend(),
                new BedTags(),
                new ChinaHat(),
                new JumpCircles(),
                new Trails(),
                new CrossHair(),
                new SpeedMine(),
                new ToxicBot(),
                new Spammer(),
                new NameProtect(),
                new InvseeExploit(),
                new AutoFlyme(),
                new FastHub(),
                new ElytraPlus(),
                new ElytraRecast(),
                new FreeLook(),
                new Zoom(),
                new InvSaver(),
                new AutoClicker(),
                new Freeze(),
                new ItemScroller(),
                new Flight(),
                new PacketFly(),
                new Speed(),
                new Spider(),
                new Fun(),
                new AutoWalk(),
                new Fucker(),
                new BedWarsHelper(),
                new AntiCheatDetector(),
                new MathSolver(),
                new PenisESP(),
                new Snow(),
                new ParticlesModule(),
                new NoServerRotate(),
                new Wings(),
                new HotBar(),
                new ItemPhysics(),
                new DamageTint(),
                new SwingAnimations(),
                new AspectRatio(),
                new NoCameraClip(),
                new MotionBlur(),
                new AutoLogin(),
                new FakePlayer(),
                new FreeCam(),
                new Arrows(),
                new Parkour(),
                new Recorder(),
                new LegitAura(),
                new Xray(),
                new ItemESP(),
                new AutoRespawn(), 
                new Models(), // тун сахур BY KISSEDWARRIOR (он фрик)
                new RWHelper(), 
                new VanillaDisabler(), // влагалище дизейблер
                new Bots(), 
                new Ezz(),
                new Notifications(), 
                new NoOpen(), 
                new DeathCoords(), 
                new AntiVomit(), 
                new HUD(),
                new Criticals(), 
                new Reach(), 
                new Blink(), 
                new AutoCasino(), 
                new FakeFine(),
                new AutoBuff(),
                new AntiAFK(),
                new AutoDuel(),
                new AutoFish(),
                new FakePing(),
                new TotemPopCounter(),
                new FOV(),
                new Strafe(),
                new WaterSpeed(),
                new BreakHighLight(),
                new AntiAttack(),
                new AmbienceModule(), 
                new SeeInvisibles(),
                new BowHelper(),
                new Trajectories(),
                new FastBreak(),
                new Pets(),
                new Phase(),
                new BaseFinder()
        );

// буду пастить пасту драгхака для пасты драгхака а потом перенесу на тх сурсы все пасты и спащу с тх сурсов пасту 
// пасты драгхака и Doickswag признает мою пасту пасты драгхака перенесенной с тх сурсов норм пастой пасты драгхакой 
// с тх сурсов, а потом я сделаю лоадер с AlisaAI потом солью сурсы в MAX и сделаю эксплоит на вагина дисейблер платным, 
// потом Алиса AI закончиться и я юзну GigaChat чтобы перенести пасту пасты тх перенесенную с тх сурсов, слитую в MAX в 
// сферум, а потом сделаю тун тун сахура с помощью GigaChat и солью сурсы в MAX, потом РКН запретит MAX и я плоки плоки 
// и начну пастить эту пасту в leet client а потом фантайм и рилик закроют и я с помощью GigaChat сделаю новый Фантайм.py — 
// лучший хвх сервер где будет паста под названием мазерхак рекод альфа бета тест паста пасты тх перенесенная в MAX потом 
// переделанная с помощью GigaChat под сферум и лит клиент пенить, люто пенить, лютейшая пена мазерхак рекод вагина дисейблер

// ctrl + /

// ctrl + c
// ctrl + v

// буду пастить пасту драгхака для пасты драгхака а потом перенесу на тх сурсы все пасты и спащу с тх сурсов пасту 
// пасты драгхака и Doickswag признает мою пасту пасты драгхака перенесенной с тх сурсов норм пастой пасты драгхакой 
// с тх сурсов, а потом я сделаю лоадер с AlisaAI потом солью сурсы в MAX и сделаю эксплоит на вагина дисейблер платным, 
// потом Алиса AI закончиться и я юзну GigaChat чтобы перенести пасту пасты тх перенесенную с тх сурсов, слитую в MAX в 
// сферум, а потом сделаю тун тун сахура с помощью GigaChat и солью сурсы в MAX, потом РКН запретит MAX и я плоки плоки 
// и начну пастить эту пасту в leet client а потом фантайм и рилик закроют и я с помощью GigaChat сделаю новый Фантайм.py — 
// лучший хвх сервер где будет паста под названием мазерхак рекод альфа бета тест паста пасты тх перенесенная в MAX потом 
// переделанная с помощью GigaChat под сферум и лит клиент пенить, люто пенить, лютейшая пена мазерхак рекод вагина дисейблер


// чо если сюда ратку добавить?


        for (Module module : modules) {
            try {
                for (Field field : module.getClass().getDeclaredFields()) {
                    if (!Setting.class.isAssignableFrom(field.getType())) continue;
                    field.setAccessible(true);
                    Setting<?> setting = (Setting<?>) field.get(module);
                    if (setting != null && !module.getSettings().contains(setting)) module.getSettings().add(setting);
                }
            } catch (Exception ignored) {}
        }
    }

    private void addModules(Module... module) {
        this.modules.addAll(List.of(module));
    }

    @EventHandler
    public void onKey(EventKey e) {
        if (Module.fullNullCheck() || mc.currentScreen != null) return;

        if (e.getAction() == 1)
            for (Module module : modules) {
                // Разрешаем Panic работать даже в режиме паники
                if (module instanceof fun.motherhack.modules.impl.client.Panic) {
                    if (module.getBind().getKey() == e.getKey() && !module.getBind().isMouse())
                        module.toggle();
                    continue;
                }
                
                // Блокируем остальные модули в режиме паники
                if (MotherHack.getInstance().isPanic()) continue;
                
                // Запрещаем toggle GUI модулей через keybind когда открыт любой GUI
                if ((module instanceof fun.motherhack.modules.impl.client.UI || 
                     module instanceof fun.motherhack.modules.impl.client.MHACKGUI) && 
                    mc.currentScreen != null) continue;
                
                if (module.getBind().getKey() == e.getKey() && !module.getBind().isMouse())
                    module.toggle();
            }
    }

    @EventHandler
    public void onMouse(EventMouse e) {
        if (Module.fullNullCheck() || mc.currentScreen != null) return;

        if (e.getAction() == 1)
            for (Module module : modules) {
                // Разрешаем Panic работать даже в режиме паники
                if (module instanceof fun.motherhack.modules.impl.client.Panic) {
                    if (module.getBind().getKey() == e.getButton() && module.getBind().isMouse())
                        module.toggle();
                    continue;
                }
                
                // Блокируем остальные модули в режиме паники
                if (MotherHack.getInstance().isPanic()) continue;
                
                // Запрещаем toggle GUI модулей через keybind когда открыт любой GUI
                if ((module instanceof fun.motherhack.modules.impl.client.UI || 
                     module instanceof fun.motherhack.modules.impl.client.MHACKGUI) && 
                    mc.currentScreen != null) continue;
                
                if (module.getBind().getKey() == e.getButton() && module.getBind().isMouse())
                    module.toggle();
            }
    }

    public List<Module> getModules(Category category) {
        return modules.stream().filter(m -> m.getCategory() == category).toList();
    }

    public List<Category> getCategories() {
        return Arrays.asList(Category.values());
    }

    public <T extends Module> T getModule(Class<T> clazz) {
        for (Module module : modules) {
            if (!clazz.isInstance(module)) continue;
            return (T) module;
        }
        return null;
    }

    public Module getModuleByClass(Class<? extends Module> clazz) {
        for (Module module : modules) {
            if (module.getClass() == clazz) return module;
        }
        return null;
    }
}