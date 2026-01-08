package fun.motherhack.modules.impl.misc;

import fun.motherhack.MotherHack;
import fun.motherhack.api.events.impl.EventTick;
import fun.motherhack.managers.ModuleManager;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.impl.misc.MessageAppend;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.modules.settings.impl.StringSetting;
import fun.motherhack.utils.math.TimerUtils;
import meteordevelopment.orbit.EventHandler;

import java.util.Random;

public class Spammer extends Module {

    private final NumberSetting minDelay = new NumberSetting("settings.spammer.minDelay", 1.0f, 0.1f, 60.0f, 0.1f);
    private final NumberSetting maxDelay = new NumberSetting("settings.spammer.maxDelay", 5.0f, 0.1f, 60.0f, 0.1f);
    private final StringSetting message1 = new StringSetting("settings.spammer.message1", "!ЛУЧШАЯ КИЛКА НА 8 ПВП И НА 12 ПВП В ТГК MOTHERHACKRECODE", false);
    private final StringSetting message2 = new StringSetting("settings.spammer.message2", "!БЕСКОНЕЧНЫЙ И БЫСТРЫЙ ПОЛЁТ НА ЭЛИТРАХ НА СЕРВЕРАХ ОТ ДЖЕНРО В ТГК MOTHERHACKRECODE", false);
    private final StringSetting message3 = new StringSetting("settings.spammer.message3", "!НА ТЕБЕ ФАРМЯТ КИЛЫ? УБИВАЮТ ЗА СЕКУНДУ? ПОПРОБУЙ ДАТЬ ОТПОР С ЛУЧШИМ ЧИТОМ В ТГК MOTHERHACKRECODE", false);
    private final StringSetting message4 = new StringSetting("settings.spammer.message4", "!ОТКРЫВАЙ МАГАЗИН ХОТЬ В ЖОПЕ МИРА И ЗАКУПАЙСЯ ВЕЗДЕ С ЛУЧШИМ ЧИТОМ В ТГК MOTHERHACKRECODE", false);
    private final StringSetting message5 = new StringSetting("settings.spammer.message5", "!ТЫ РИЛ ТЕРПИШЬ БЕЗ СОФТА??? ПЕРЕСТАНЬ БЫТЬ ТЕРПИЛОЙ И ПОКУПАЙ ЛУЧШИЙ СОФТ В ТГК MOTHERHACKRECODE", false);
    private final StringSetting message6 = new StringSetting("settings.spammer.message6", "!ХОЧЕШЬ ТЕЛЛИ СКАФФУЛД? ПОКУПАЙ ЛУЧШИЙ СОФТ В ТГК MOTHERHACKRECODE", false);
    private final StringSetting message7 = new StringSetting("settings.spammer.message7", "", false);
    private final StringSetting message8 = new StringSetting("settings.spammer.message8", "", false);
    private final StringSetting message9 = new StringSetting("settings.spammer.message9", "", false);
    private final StringSetting message10 = new StringSetting("settings.spammer.message10", "", false);
    private final BooleanSetting disableMessageAppend = new BooleanSetting("settings.spammer.disableMessageAppend", false);

    private final TimerUtils timer = new TimerUtils();
    private final Random random = new Random();
    private String[] messageArray;
    private boolean wasMessageAppendEnabled;

    public Spammer() {
        super("Spammer", Category.Misc);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        updateMessages();
        if (disableMessageAppend.getValue()) {
            MessageAppend messageAppend = MotherHack.getInstance().getModuleManager().getModule(MessageAppend.class);
            if (messageAppend != null) {
                wasMessageAppendEnabled = messageAppend.isToggled();
                if (wasMessageAppendEnabled) {
                    messageAppend.setToggled(false);
                }
            }
        }
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        if (messageArray == null || messageArray.length == 0) return;

        long delayMs = (long) ((minDelay.getValue() + random.nextDouble() * (maxDelay.getValue() - minDelay.getValue())) * 1000L);
        if (timer.passed(delayMs)) {
            String message = messageArray[random.nextInt(messageArray.length)];
            if (message.length() > 256) {
                message = message.substring(0, 256);
            }
            mc.player.networkHandler.sendChatMessage(message);
            timer.reset();
        }
    }

    private void updateMessages() {
        String[] allMessages = {
            message1.getValue(),
            message2.getValue(),
            message3.getValue(),
            message4.getValue(),
            message5.getValue(),
            message6.getValue(),
            message7.getValue(),
            message8.getValue(),
            message9.getValue(),
            message10.getValue()
        };
        messageArray = java.util.Arrays.stream(allMessages).filter(s -> !s.trim().isEmpty()).toArray(String[]::new);
    }

    @Override
    public void onDisable() {
        if (disableMessageAppend.getValue() && wasMessageAppendEnabled) {
            MessageAppend messageAppend = MotherHack.getInstance().getModuleManager().getModule(MessageAppend.class);
            if (messageAppend != null) {
                messageAppend.setToggled(true);
            }
        }
        super.onDisable();
    }
}