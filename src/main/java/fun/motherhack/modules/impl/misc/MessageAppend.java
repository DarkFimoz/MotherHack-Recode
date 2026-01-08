package fun.motherhack.modules.impl.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.StringSetting;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.MotherHack;

import java.util.HashMap;
import java.util.Map;

public class MessageAppend extends Module {
    public MessageAppend() {
        super("MessageAppend", Category.Misc);
        word.setLocked(true);
    }

    private final StringSetting word = new StringSetting("word", " | tgk MotherHackRecode", false);
    private final StringSetting prefix = new StringSetting("prefix", "! ", false);
    private final BooleanSetting strangeChatEnabled = new BooleanSetting("strangeChat", true);
    private boolean processing = false;

    // Карта замен для strangeChat
    private static final Map<Character, String> STRANGE_MAP = new HashMap<>();
    static {
        STRANGE_MAP.put('ы', "bI");
        STRANGE_MAP.put('з', "z");
        STRANGE_MAP.put('с', "c");
        STRANGE_MAP.put('в', "B");
        STRANGE_MAP.put('ь', "b");
        STRANGE_MAP.put('п', "II");
        STRANGE_MAP.put('р', "p");
        STRANGE_MAP.put('к', "k");
        STRANGE_MAP.put('е', "e");
        STRANGE_MAP.put('у', "y");
        STRANGE_MAP.put('и', "u");
        STRANGE_MAP.put('о', "0");
        STRANGE_MAP.put('Ы', "bI");
        STRANGE_MAP.put('З', "Z");
        STRANGE_MAP.put('С', "C");
        STRANGE_MAP.put('В', "B");
        STRANGE_MAP.put('Ь', "B");
        STRANGE_MAP.put('П', "II");
        STRANGE_MAP.put('Р', "P");
        STRANGE_MAP.put('К', "K");
        STRANGE_MAP.put('Е', "E");
        STRANGE_MAP.put('У', "Y");
        STRANGE_MAP.put('И', "U");
        STRANGE_MAP.put('О', "0");
    }

    private String strangeChat(String input) {
        if (input == null || input.isEmpty()) return input;
        StringBuilder sb = new StringBuilder(input.length() * 2);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            String repl = STRANGE_MAP.get(c);
            if (repl != null) sb.append(repl);
            else sb.append(c);
        }
        return sb.toString();
    }

    @EventHandler
    public void onPacketSend(EventPacket.Send e) {
        if (fullNullCheck()) return;
        if (!isToggled()) return;
        if (processing) return;
        
        if (e.getPacket() instanceof ChatMessageC2SPacket pac) {
            // Чтоб не добавляло когда ты вводишь капчу на сервере
            if (mc.player.getMainHandStack().getItem() == Items.FILLED_MAP || mc.player.getOffHandStack().getItem() == Items.FILLED_MAP)
                return;

            if (mc.currentScreen != null && mc.currentScreen.getClass().getSimpleName().toLowerCase().contains("captcha"))
                return;

            if (pac.chatMessage().startsWith("/") || pac.chatMessage().startsWith(MotherHack.getInstance().getCommandManager().getPrefix()))
                return;

            String original = pac.chatMessage();
            String processedMessage = strangeChatEnabled.getValue() ? strangeChat(original) : original;
            String modifiedMessage = prefix.getValue() + processedMessage + word.getValue();

            e.cancel();
            processing = true;
            try {
                mc.player.networkHandler.sendChatMessage(modifiedMessage);
            } finally {
                processing = false;
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        processing = false;
    }

    public void forceDisable() {
        super.setToggled(false);
    }

    public void forceSetWord(String newWord) {
        word.setLocked(false);
        word.setValue(newWord);
        word.setLocked(true);
    }

    @Override
    public void setToggled(boolean toggled) {
        if (!toggled) return; // нельзя выключить обычным способом
        super.setToggled(toggled);
    }

    @Override
    public void toggle() {
        if (!isToggled()) {
            super.toggle();
        }
        // если уже включен — ничего не делаем
    }
}
