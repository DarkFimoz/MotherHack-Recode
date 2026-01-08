package fun.motherhack.modules.impl.misc;

import fun.motherhack.api.events.impl.EventPlayerTick;
import fun.motherhack.api.events.impl.EventRender3D;
import fun.motherhack.api.events.impl.EventRender2D;
import fun.motherhack.api.events.impl.EventPacket;
import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.api.Nameable;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import fun.motherhack.modules.settings.impl.EnumSetting;
import fun.motherhack.modules.settings.impl.NumberSetting;
import fun.motherhack.utils.math.TimerUtils;
import fun.motherhack.utils.render.Render3D;
import fun.motherhack.utils.render.Render2D;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import fun.motherhack.modules.impl.misc.MessageAppend;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import fun.motherhack.utils.rotations.RotationChanger;
import fun.motherhack.MotherHack;

import java.awt.*;
import java.util.Random;

public class Fun extends Module {
    // Doza settings
    private final BooleanSetting doza = new BooleanSetting("settings.fun.doza", false);
    private final NumberSetting dozaSpeed = new NumberSetting("settings.fun.dozaSpeed", 50.0f, 1.0f, 100.0f, 1.0f);
    private final EnumSetting<DozaMode> dozaMode = new EnumSetting<>("settings.fun.dozaMode", DozaMode.YAW);
    private final BooleanSetting dozaRandomize = new BooleanSetting("settings.fun.dozaRandomize", true);
    private float currentYaw = 0f;
    private float currentPitch = 0f;

    // Twerk settings
    private final BooleanSetting twerk = new BooleanSetting("settings.fun.twerk", false);
    private final NumberSetting twerkSpeed = new NumberSetting("settings.fun.twerkSpeed", 5, 1, 20, 1);
    private int twerkTickCounter = 0;
    private long lastToggleTime = 0;
    private boolean shiftPressed = false;

    // Pizdec settings
    private final BooleanSetting pizdec = new BooleanSetting("settings.fun.pizdec", false);
    private final NumberSetting pizdecIntensity = new NumberSetting("settings.fun.pizdecIntensity", 1.0f, 0.1f, 5.0f, 0.1f);
    
    private float pizdecAnimation = 0;
    private float pizdecScreenOffset = 0;
    private final Random random = new Random();

    // Rotation helper for safe camera changes
    private float[] rotations = new float[]{0f, 0f};
    private final RotationChanger changer = new RotationChanger(
            2000,
            () -> new Float[]{rotations[0], rotations[1]},
            () -> !doza.getValue() && !pizdec.getValue()
    );
    
    // AutoSex settings
    private final BooleanSetting autoSex = new BooleanSetting("settings.fun.autoSex", false);
    private final NumberSetting autoSexDelay = new NumberSetting("settings.fun.autoSexDelay", 5.0f, 1.0f, 30.0f, 1.0f);
    private final TimerUtils autoSexTimer = new TimerUtils();
    private long lastAutoSexMessageTime = 0;

    // AutoExamples settings
    private final BooleanSetting autoExamples = new BooleanSetting("settings.fun.autoExamples", false);
    private final String[] sexMessages = {
        "Оххх, сучка, давай быстрее!",
        "Твой член - просто бомба, детка!",
        "Я обожаю, когда ты так глубоко!",
        "Еби меня, как последнюю шлюху!",
        "Да, вот так, ебашь сильнее!",
        "О да, ебаный в рот, как же хорошо!",
        "Трахни меня в жопу, папочка!",
        "Я вся мокрая от твоих прикосновений!",
        "Выеби меня, как последнюю блядь!",
        "О мой блядский бог, как же хорошо!",
        "Ты ебешь меня, как бог!",
        "Да, еби меня в сраку, сука!",
        "О да, ебаный стон!",
        "Выебывай меня, как последнюю шлюху!",
        "Охуенно, не останавливайся!",
        "Сука, я кончаю!",
        "Еби меня в жопу, как последнюю шлюху!",
        "О да, трахни меня по-взрослому!",
        "Я вся твоя, еби меня, как хочешь!",
        "Сука, как же приятно!",
        "Охххх, глубже",
        "У тебя такой твёрдый стоячек",
        "Мне нравится твоя пиписечка",
        "Нассы мне в рот пожалуйста, дорогой",
        "Ты станешь сексуальней с читом MotherHack Recode",
        "Я хочу почувствовать твою горячую плоть",
        "Давай устроим ночь страсти",
        "Ты сводишь меня с ума",
        "Моя киска мокнет от твоего взгляда",
        "Я хочу, чтобы ты был моим господином",
        "Давай устроим горячий секс",
        "Ты такой сексуальный, что я не могу сдержаться",
        "Я хочу почувствовать твой член внутри себя",
        "Давай займемся любовью прямо здесь",
        "Ты заводишь меня одним своим видом",
        "Я хочу, чтобы ты трахнул меня как следует",
        "Мое тело дрожит от желания",
        "Ты сводишь меня с ума своей страстью",
        "Я хочу ощутить твою страсть внутри себя",
        "Давай устроим жаркую ночь",
        "Ты заставляешь мое сердце биться чаще",
        "Я хочу, чтобы ты был моим",
        "Ты такой страстный, что я таю",
        "Давай займемся любовью под луной",
        "Ты разжигаешь во мне огонь страсти",
        "Я хочу почувствовать твои нежные прикосновения",
        "Ты заводишь меня одним только взглядом",
        "Давай устроим страстную ночь",
        "Я хочу, чтобы ты был моим любовником",
        "Ты сводишь меня с ума своей страстью"
    };
    
    // Режимы отправки сообщений AutoSex
    public enum AutoSexMode implements Nameable {
        PRIVATE("Приватно"),
        PUBLIC("Публично");
        
        private final String name;
        
        AutoSexMode(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    private final EnumSetting<AutoSexMode> autoSexMode = new EnumSetting<>("settings.fun.autoSexMode", AutoSexMode.PRIVATE);

    // Dinnerbone settings
    public final BooleanSetting dinnerbone = new BooleanSetting("settings.fun.dinnerbone", false);
    
    // Enums
    public enum DozaMode implements Nameable {
        YAW("Yaw Only"),
        PITCH("Pitch Only"),
        BOTH("Both"),
        EPILEPSY("Epilepsy");
        
        private final String name;
        
        DozaMode(String name) {
            this.name = name;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }

    public Fun() {
        super("Fun", Category.Misc);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        
        if (mc.player != null) {
            currentYaw = mc.player.getYaw();
            currentPitch = mc.player.getPitch();
        } else {
            currentYaw = 0;
            currentPitch = 0;
        }
        shiftPressed = false;
        if (mc.options != null) {
            mc.options.sneakKey.setPressed(false);
        }
    }

    private boolean shouldControlPlayer() {
        return mc.currentScreen == null && // No open screens
               mc.player != null && 
               !mc.player.isSpectator() && 
               !mc.player.isCreative() &&
               mc.interactionManager != null &&
               mc.world != null;
    }

    @EventHandler
    public void onPacketReceive(EventPacket.Receive e) {
        if (fullNullCheck()) return;
        if (!autoExamples.getValue()) return;
        if (MotherHack.getInstance() == null) return;
        
        if (e.getPacket() instanceof GameMessageS2CPacket packet) {
            String message = packet.content().getString();
            
            // Удаляем форматирование и цвета из сообщения
            String cleanMessage = message.replaceAll("§[0-9a-fklmnor]", "").trim();
            
            // Удаляем имена игроков (формат [PlayerName: message] или <PlayerName> message)
            if (cleanMessage.contains(":")) {
                String[] parts = cleanMessage.split(":", 2);
                if (parts.length > 1) {
                    cleanMessage = parts[1].trim();
                }
            }
            
            if (cleanMessage.startsWith("<") && cleanMessage.contains(">")) {
                int endIndex = cleanMessage.indexOf(">");
                if (endIndex < cleanMessage.length() - 1) {
                    cleanMessage = cleanMessage.substring(endIndex + 1).trim();
                }
            }
            
            // Проверяем, является ли сообщение математическим выражением
            if (isMathExpression(cleanMessage)) {
                try {
                    double result = evaluateMathExpression(cleanMessage);
                    if (mc.player != null) {
                        String answer = String.valueOf(result);
                        // Убираем .0 для целых чисел
                        if (result == (long) result) {
                            answer = String.valueOf((long) result);
                        }
                        sendChatMessageDirect(answer);
                    }
                } catch (Exception ex) {
                    // Игнорируем невалидные выражения
                }
            }
        }
    }

    @EventHandler
    public void onPlayerTick(EventPlayerTick e) {
        if (fullNullCheck() || !shouldControlPlayer()) {
            // Reset states if we shouldn't be controlling the player
            if (mc.options != null) {
                mc.options.sneakKey.setPressed(false);
            }
            shiftPressed = false;
            return;
        }
         
        PlayerEntity player = mc.player;

        // Save original rotations
        float originalYaw = player.getYaw();
        float originalPitch = player.getPitch();
        
        try {
            // Handle Doza rotation
            if (doza.getValue()) {
                switch (dozaMode.getValue()) {
                    case YAW:
                        currentYaw += dozaSpeed.getValue();
                        if (currentYaw > 180) currentYaw = -180;
                        // Apply rotation via RotationManager (do not directly set player to avoid forcing client camera)
                        rotations[0] = currentYaw;
                        rotations[1] = player.getPitch();
                        MotherHack.getInstance().getRotationManager().addRotation(changer);
                        break;
                        
                    case PITCH:
                        currentPitch += dozaSpeed.getValue();
                        if (currentPitch > 90) currentPitch = -90;
                        else if (currentPitch < -90) currentPitch = 90;
                        rotations[1] = currentPitch;
                        rotations[0] = player.getYaw();
                        MotherHack.getInstance().getRotationManager().addRotation(changer);
                        break;
                        
                    case BOTH:
                        currentYaw += dozaSpeed.getValue();
                        currentPitch += dozaSpeed.getValue() * 0.5f;
                        
                        if (currentYaw > 180) currentYaw = -180;
                        if (currentPitch > 90) currentPitch = -90;
                        else if (currentPitch < -90) currentPitch = 90;
                        
                        rotations[0] = currentYaw;
                        rotations[1] = currentPitch;
                        MotherHack.getInstance().getRotationManager().addRotation(changer);
                        break;
                        
                    case EPILEPSY:
                        if (dozaRandomize.getValue()) {
                            rotations[0] = (float) (Math.random() * 360 - 180);
                            rotations[1] = (float) (Math.random() * 180 - 90);
                            MotherHack.getInstance().getRotationManager().addRotation(changer);
                        } else {
                            currentYaw += dozaSpeed.getValue() * 2;
                            currentPitch = (float) Math.sin(System.currentTimeMillis() * 0.01) * 90;
                            
                            if (currentYaw > 180) currentYaw = -180;
                            rotations[0] = currentYaw;
                            rotations[1] = currentPitch;
                            MotherHack.getInstance().getRotationManager().addRotation(changer);
                        }
                        break;
                }
            }

            // Handle Twerking
            if (twerk.getValue()) {
                twerkTickCounter++;
                if (twerkTickCounter >= twerkSpeed.getValue()) {
                    mc.options.sneakKey.setPressed(!mc.options.sneakKey.isPressed());
                    twerkTickCounter = 0;
                }
            }

            // Handle Pizdec effects
            if (pizdec.getValue()) {
                pizdecAnimation += 0.1f * pizdecIntensity.getValue();
                pizdecScreenOffset += 0.05f * pizdecIntensity.getValue();
                
                // Apply screen tilt and distortion via RotationManager
                if (mc.player != null) {
                    float nyaw = mc.player.getYaw() + (float)Math.sin(pizdecAnimation * 0.3) * 3 * pizdecIntensity.getValue();
                    float npitch = mc.player.getPitch() + (float)Math.sin(pizdecAnimation * 0.4) * 2 * pizdecIntensity.getValue();
                    rotations[0] = nyaw;
                    rotations[1] = npitch;
                    MotherHack.getInstance().getRotationManager().addRotation(changer);
                }
            }
            
            // Handle AutoSex
            if (autoSex.getValue() && !fullNullCheck() && mc.player != null) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastAutoSexMessageTime >= (autoSexDelay.getValue() * 1000)) {
                    PlayerEntity nearestPlayer = findNearestPlayer();
                    if (nearestPlayer != null) {
                        int messageIndex = random.nextInt(sexMessages.length);
                        String message = sexMessages[messageIndex];
                        
                        if (autoSexMode.getValue() == AutoSexMode.PRIVATE) {
                            mc.player.networkHandler.sendChatCommand("msg " + nearestPlayer.getName().getString() + " " + message);
                        } else {
                            mc.player.networkHandler.sendChatMessage(nearestPlayer.getName().getString() + ", " + message);
                        }
                        
                        lastAutoSexMessageTime = currentTime;
                    }
                }
            }
        } finally {
            // Restore original rotations if no effects are active
            if (!doza.getValue() && !pizdec.getValue()) {
                MotherHack.getInstance().getRotationManager().removeRotation(changer);
                player.setYaw(originalYaw);
                player.setPitch(originalPitch);
            }
        }
        
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck() || mc.player == null) return;

        try {
            // Visual effects for each function
            if (doza.getValue()) {
                // Draw spinning particles around player
                drawSpinningEffect(mc.player, e.getMatrixStack());
            }

            if (twerk.getValue() && pizdec.getValue()) {
                // Draw twerking motion effect only if pizdec is enabled
                drawTwerkEffect(mc.player, e.getMatrixStack());
            }
        } catch (Exception ex) {
            // Prevent crashes from rendering code
            ex.printStackTrace();
        }
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || !pizdec.getValue()) return;

        // Draw drug-induced psychedelic effects on screen
        drawDrugEffect(e.getContext().getMatrices(), mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
    }

    @Override
    public void onDisable() {
        super.onDisable();
        MotherHack.getInstance().getRotationManager().removeRotation(changer);
        
        // Reset player appearance when module is disabled
        if (mc.player != null) {
            // Reset rotation
            mc.player.setYaw(0);
            mc.player.setPitch(0);
            
            // Reset sneaking state if twerking was enabled
            if (twerk.getValue()) {
                mc.options.sneakKey.setPressed(false);
            }
        }
    }
    

    private void drawSpinningEffect(PlayerEntity player, net.minecraft.client.util.math.MatrixStack matrixStack) {
        // Эффект вращения без отрисовки частиц
    }

    private void drawTwerkEffect(PlayerEntity player, net.minecraft.client.util.math.MatrixStack matrixStack) {
        if (mc.world == null || mc.player == null) return;
        
        Vec3d playerPos = player.getPos();
        double time = System.currentTimeMillis() * 0.001;
        
        // Draw twerking motion lines and circles around the player
        double twerkPhase = Math.sin(time * 5);
        
        // Draw multiple circles to simulate twerking motion
        for (int i = 0; i < 3; i++) {
            double circleRadius = 0.5 + i * 0.3;
            double circleY = playerPos.y + 0.5 + Math.sin(time * 5 + i) * 0.2;
            
            // Draw circle segments
            for (int j = 0; j < 16; j++) {
                double angle = (j * Math.PI * 2 / 16) + time * 5;
                double x1 = playerPos.x + Math.cos(angle) * circleRadius;
                double z1 = playerPos.z + Math.sin(angle) * circleRadius;
                double x2 = playerPos.x + Math.cos(angle + 0.1) * circleRadius;
                double z2 = playerPos.z + Math.sin(angle + 0.1) * circleRadius;
                
                Box segmentBox = new Box(
                    Math.min(x1, x2) - 0.05, circleY - 0.05, Math.min(z1, z2) - 0.05,
                    Math.max(x1, x2) + 0.05, circleY + 0.05, Math.max(z1, z2) + 0.05
                );
                Render3D.renderBox(matrixStack, segmentBox, new Color(255, 100 + i * 50, 0, 150));
            }
        }
    }

    private void drawDrugEffect(net.minecraft.client.util.math.MatrixStack matrixStack, int width, int height) {
        float intensity = pizdecIntensity.getValue();
        
        // Draw warping background
        for (int i = 0; i < 5; i++) {
            float offset = (float) (Math.sin(pizdecAnimation * 0.2 + i) * 10 * intensity);
            float size = 200 + (float)Math.sin(pizdecAnimation * 0.3 + i) * 50 * intensity;
            float hue = (pizdecAnimation * 0.1f + i * 0.2f) % 1.0f;
            Color color = new Color(Color.HSBtoRGB(hue, 0.8f, 0.8f));
            
            // Save current transformation matrix
            matrixStack.push();
            
            // Move to center
            matrixStack.translate(width/2f, height/2f, 0);
            
            // Apply scaling
            float scaleX = 1.0f + (float)Math.sin(pizdecAnimation * 0.5f + i) * 0.1f * intensity;
            float scaleY = 1.0f + (float)Math.cos(pizdecAnimation * 0.6f + i) * 0.1f * intensity;
            
            // Apply rotation by calculating new coordinates
            float angle = (float)Math.toRadians((pizdecAnimation * 10 + i * 30) % 360);
            float sin = (float)Math.sin(angle);
            float cos = (float)Math.cos(angle);
            
            // Calculate rotated and scaled coordinates
            float halfSize = size / 2;
            float[] xCoords = {-halfSize, halfSize, halfSize, -halfSize};
            float[] yCoords = {-halfSize, -halfSize, halfSize, halfSize};
            
            // Apply rotation and scaling to each vertex
            for (int j = 0; j < 4; j++) {
                float x = xCoords[j] * scaleX;
                float y = yCoords[j] * scaleY;
                xCoords[j] = x * cos - y * sin;
                yCoords[j] = x * sin + y * cos;
            }
            
            // Draw the rotated rectangle as a polygon
            int[] xPoints = new int[4];
            int[] yPoints = new int[4];
            for (int j = 0; j < 4; j++) {
                xPoints[j] = (int)xCoords[j];
                yPoints[j] = (int)yCoords[j];
            }
            
            // Draw the rectangle using Render2D
            Render2D.drawRoundedRect(matrixStack, -size/2, -size/2, size, size, 10, 
                    new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
                    
            // Restore transformation matrix
            matrixStack.pop();
        }
        
        // Draw floating colorful particles
        for (int i = 0; i < 50; i++) {
            float angle = (pizdecAnimation * 0.5f + i * 0.2f) % (float)(Math.PI * 2);
            float dist = 50 + (float)Math.sin(pizdecAnimation * 0.3 + i * 0.5) * 30 * intensity;
            float x = width/2f + (float)Math.cos(angle) * dist * 2;
            float y = height/2f + (float)Math.sin(angle * 1.5f) * dist;
            
            float hue = (pizdecAnimation * 0.05f + i * 0.01f) % 1.0f;
            Color color = new Color(Color.HSBtoRGB(hue, 0.9f, 1.0f));
            
            float size = 5 + (float)Math.sin(pizdecAnimation * 2 + i) * 3 * intensity;
            float rotation = pizdecAnimation * 20 + i * 10;
            
            // Save current transformation matrix
            matrixStack.push();
            
            // Move to particle position
            matrixStack.translate(x, y, 0);
            
            // Apply rotation by calculating new coordinates
            float particleAngle = (float)Math.toRadians(rotation);
            float sin = (float)Math.sin(particleAngle);
            float cos = (float)Math.cos(particleAngle);
            
            // Calculate rotated coordinates
            float halfSize = size / 2;
            float[] xCoords = {-halfSize, halfSize, halfSize, -halfSize};
            float[] yCoords = {-halfSize, -halfSize, halfSize, halfSize};
            
            // Apply rotation to each vertex
            for (int j = 0; j < 4; j++) {
                float xp = xCoords[j];
                float yp = yCoords[j];
                xCoords[j] = xp * cos - yp * sin;
                yCoords[j] = xp * sin + yp * cos;
            }
            
            // Convert to integer coordinates
            int[] xPoints = new int[4];
            int[] yPoints = new int[4];
            for (int j = 0; j < 4; j++) {
                xPoints[j] = (int)xCoords[j];
                yPoints[j] = (int)yCoords[j];
            }
            
            // Draw the particle using Render2D
            Render2D.drawRoundedRect(matrixStack, -size/2, -size/2, size, size, 2, color);
            
            // Restore transformation matrix
            matrixStack.pop();
        }
        
        // Draw warping grid
        int gridSize = 20;
        float gridIntensity = 0.5f * intensity;
        for (int x = 0; x <= width; x += gridSize) {
            for (int y = 0; y <= height; y += gridSize) {
                float offsetX = (float)Math.sin(y * 0.02 + pizdecAnimation) * 10 * gridIntensity;
                float offsetY = (float)Math.cos(x * 0.02 + pizdecAnimation) * 10 * gridIntensity;
                
                if (x < width - gridSize) {
                    float hue = (pizdecAnimation + x * 0.001f) % 1.0f;
                    Color color = new Color(Color.HSBtoRGB(hue, 0.7f, 1.0f));
                    float endX = x + gridSize + (float)Math.sin((y + gridSize) * 0.02 + pizdecAnimation) * 10 * gridIntensity;
                    // Draw line using rectangles since drawLine might not be available
                    float lineWidth = 1f;
                    Render2D.drawRoundedRect(matrixStack, 
                            x + offsetX - lineWidth/2, y + offsetY - lineWidth/2, 
                            endX - (x + offsetX) + lineWidth, lineWidth, 
                            lineWidth/2, color);
                }
                
                if (y < height - gridSize) {
                    float hue = (pizdecAnimation + y * 0.001f + 0.5f) % 1.0f;
                    Color color = new Color(Color.HSBtoRGB(hue, 0.7f, 1.0f));
                    float endY = y + gridSize + (float)Math.cos((x + gridSize) * 0.02 + pizdecAnimation) * 10 * gridIntensity;
                    // Draw line using rectangles since drawLine might not be available
                    float lineWidth = 1f;
                    Render2D.drawRoundedRect(matrixStack, 
                            x + offsetX - lineWidth/2, y + offsetY - lineWidth/2, 
                            lineWidth, endY - (y + offsetY) + lineWidth, 
                            lineWidth/2, color);
                }
            }
            
            // Draw pulsing center circle
            float pulse = (float)Math.sin(pizdecAnimation * 2) * 0.2f + 0.8f;
            float circleSize = 50 + 20 * pulse * intensity;
            int alpha = (int)(100 * intensity);
            alpha = Math.min(255, Math.max(0, alpha));
            Color circleColor = new Color(255, 255, 255, alpha);
            Render2D.drawRoundedRect(matrixStack,
                width/2f - circleSize/2,
                height/2f - circleSize/2,
                circleSize, circleSize,
                circleSize/2,
                circleColor);
        }
        
        // Add screen distortion effect
        float distortionAlpha = (float)(Math.sin(pizdecScreenOffset * 2) + 1) * 0.1f;
        int alpha = (int)(distortionAlpha * 255);
        alpha = Math.min(255, Math.max(0, alpha));
        Color distortionColor = new Color(255, 0, 255, alpha);
        Render2D.drawRoundedRect(matrixStack, 0, 0, width, height, 0f, distortionColor);
    }


    
    private PlayerEntity findNearestPlayer() {
        if (mc.world == null || mc.player == null) return null;
        
        PlayerEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            
            double distance = mc.player.distanceTo(player);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        
        return nearest;
    }
    
    // Извлекает математическое выражение из сообщения
    private String extractMathExpression(String message) {
        if (message == null || message.isEmpty()) return null;
        
        // Паттерн для поиска математических выражений: числа с операторами
        // Ищем последовательности вида: число оператор число [оператор число...]
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "-?\\d+(?:\\.\\d+)?(?:\\s*[+\\-*/]\\s*-?\\d+(?:\\.\\d+)?)+");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        
        if (matcher.find()) {
            return matcher.group();
        }
        
        return null;
    }
    
    // Проверяет, является ли строка математическим выражением
    private boolean isMathExpression(String message) {
        if (message == null || message.trim().isEmpty()) return false;
        
        String trimmed = message.trim();
        
        // Игнорируем команды и сообщения с префиксами
        if (trimmed.startsWith("/") || trimmed.startsWith("!") || trimmed.startsWith(".")) return false;
        
        // Пытаемся извлечь математическое выражение из сообщения
        String extracted = extractMathExpression(trimmed);
        if (extracted == null) return false;
        
        // Проверяем наличие хотя бы одного математического оператора
        boolean hasOperator = extracted.contains("+") || extracted.contains("-") || 
                             extracted.contains("*") || extracted.contains("/");
        
        if (!hasOperator) return false;
        
        // Проверяем на валидные символы (цифры, операторы, точка)
        String cleanMessage = extracted.replaceAll("\\s+", ""); // Удаляем пробелы
        if (!cleanMessage.matches("-?[0-9+\\-*/\\.]+")) return false;
        
        // Проверяем, что выражение не заканчивается оператором
        if (cleanMessage.endsWith("+") || cleanMessage.endsWith("-") || 
            cleanMessage.endsWith("*") || cleanMessage.endsWith("/")) return false;
        
        if (cleanMessage.startsWith("*") || cleanMessage.startsWith("/") || 
            cleanMessage.startsWith("+")) return false;
        
        // Проверяем, что нет двух операторов подряд
        if (cleanMessage.matches(".*[+\\-*/]{2,}.*")) return false;
        
        // Дополнительная проверка - должно быть хотя бы одно число с каждой стороны оператора
        return true;
    }
    
    // Вычисляет простое математическое выражение
    private double evaluateMathExpression(String expression) throws Exception {
        // Извлекаем математическое выражение из сообщения
        String extracted = extractMathExpression(expression);
        if (extracted == null) {
            throw new Exception("No math expression found");
        }
        
        // Удаляем все пробелы
        extracted = extracted.replaceAll("\\s+", "");
        
        // Проверяем на валидность
        if (!extracted.matches("-?[0-9+\\-*/\\.]+")) {
            throw new Exception("Invalid characters");
        }
        
        // Простое вычисление (поддержка +, -, *, /)
        return evaluateSimple(extracted);
    }
    
    // Рекурсивное вычисление простых выражений с правильным приоритетом операторов
    private double evaluateSimple(String expression) throws Exception {
        expression = expression.trim();
        
        if (expression.isEmpty()) {
            throw new Exception("Empty expression");
        }
        
        // Ищем + или - справа налево (низший приоритет)
        // Это гарантирует правильный порядок вычислений
        int parenDepth = 0;
        for (int i = expression.length() - 1; i >= 0; i--) {
            char c = expression.charAt(i);
            
            if (c == ')') parenDepth++;
            else if (c == '(') parenDepth--;
            
            if (parenDepth == 0 && (c == '+' || c == '-')) {
                // Пропускаем если это унарный минус в начале
                if (i == 0) continue;
                
                // Проверяем, что это не часть числа после другого оператора (например 5*-3)
                char prev = expression.charAt(i - 1);
                if (prev == '*' || prev == '/' || prev == '+' || prev == '-') continue;
                
                String leftPart = expression.substring(0, i);
                String rightPart = expression.substring(i + 1);
                
                if (leftPart.isEmpty()) continue;
                
                double left = evaluateSimple(leftPart);
                double right = evaluateSimple(rightPart);
                
                return c == '+' ? left + right : left - right;
            }
        }
        
        // Ищем * или / справа налево (высший приоритет)
        for (int i = expression.length() - 1; i >= 0; i--) {
            char c = expression.charAt(i);
            
            if (c == ')') parenDepth++;
            else if (c == '(') parenDepth--;
            
            if (parenDepth == 0 && (c == '*' || c == '/')) {
                String leftPart = expression.substring(0, i);
                String rightPart = expression.substring(i + 1);
                
                double left = evaluateSimple(leftPart);
                double right = evaluateSimple(rightPart);
                
                if (c == '/') {
                    if (right == 0) throw new Exception("Division by zero");
                    return left / right;
                }
                return left * right;
            }
        }
        
        // Если дошли сюда, это просто число
        try {
            return Double.parseDouble(expression);
        } catch (NumberFormatException e) {
            throw new Exception("Invalid number: " + expression);
        }
    }
    
    // Отправляет сообщение в чат, обходя MessageAppend
    private void sendChatMessageDirect(String message) {
        if (mc.player == null || message == null) return;
        
        try {
            // Получаем MessageAppend и устанавливаем skip для обхода
            MessageAppend messageAppend = MotherHack.getInstance().getModuleManager().getModule(MessageAppend.class);
            
            if (messageAppend != null && messageAppend.isToggled()) {
                // Используем рефлексию для доступа к полю skip
                try {
                    java.lang.reflect.Field skipField = MessageAppend.class.getDeclaredField("skip");
                    skipField.setAccessible(true);
                    
                    // Устанавливаем skip равным нашему сообщению
                    skipField.set(messageAppend, message);
                    
                    // Отправляем сообщение
                    mc.player.networkHandler.sendChatMessage(message);
                    
                    // Очищаем skip после отправки
                    skipField.set(messageAppend, null);
                } catch (Exception ex) {
                    // Если не получилось, отправляем обычным способом
                    mc.player.networkHandler.sendChatMessage(message);
                }
            } else {
                // MessageAppend выключен, отправляем обычным способом
                mc.player.networkHandler.sendChatMessage(message);
            }
        } catch (Exception e) {
            // Если что-то пошло не так, отправляем обычным способом
            mc.player.networkHandler.sendChatMessage(message);
        }
    }
    
}