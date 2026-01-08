package fun.motherhack.screen;

import fun.motherhack.MotherHack;
import fun.motherhack.utils.render.Render2D;
import fun.motherhack.utils.render.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.session.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.awt.Color;

public class AccountSwitcherScreen extends Screen {
    private TextFieldWidget usernameField;
    private List<ButtonWidget> accountButtons = new ArrayList<>();

    public AccountSwitcherScreen() {
        super(Text.of("Account Switcher"));
    }

    @Override
    protected void init() {
        super.init();

        // Username input field
        this.usernameField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, this.height / 2 - 50, 200, 20, Text.of("Username"));
        this.usernameField.setText("Player");
        this.addSelectableChild(this.usernameField);
        this.addDrawableChild(this.usernameField);

        // Instructions
        this.addDrawableChild(ButtonWidget.builder(Text.of("Enter username above, then choose:"), button -> {
            // This is just a label, no action
        }).dimensions(this.width / 2 - 100, this.height / 2 - 75, 200, 20).build());

        // Add Account button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Add to Saved Accounts"), button -> {
            String username = usernameField.getText().trim();
            if (!username.isEmpty()) {
                MotherHack.getInstance().getAccountManager().addAccount(username);
                refreshAccountButtons();
                usernameField.setText(""); // Clear field after adding
            }
        }).dimensions(this.width / 2 - 100, this.height / 2 - 20, 200, 20).build());

        // Login button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Login Once (not saved)"), button -> {
            String username = usernameField.getText().trim();
            if (!username.isEmpty()) {
                switchToAccount(username);
            }
        }).dimensions(this.width / 2 - 100, this.height / 2 + 10, 200, 20).build());

        // Back button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Back"), button -> {
            this.close();
        }).dimensions(this.width / 2 - 100, this.height / 2 + 40, 200, 20).build());

        refreshAccountButtons();
    }

    private void refreshAccountButtons() {
        // Remove old account buttons
        for (ButtonWidget button : accountButtons) {
            this.remove(button);
        }
        accountButtons.clear();

        // Add new account buttons
        int yOffset = this.height / 2 + 80;
        for (String account : MotherHack.getInstance().getAccountManager().getAccounts()) {
            ButtonWidget accountButton = ButtonWidget.builder(Text.of("Login as: " + account), button -> {
                switchToAccount(account);
            }).dimensions(this.width / 2 - 100, yOffset, 150, 20).build();

            ButtonWidget deleteButton = ButtonWidget.builder(Text.of("Delete"), button -> {
                MotherHack.getInstance().getAccountManager().removeAccount(account);
                refreshAccountButtons();
            }).dimensions(this.width / 2 + 60, yOffset, 40, 20).build();

            accountButtons.add(accountButton);
            accountButtons.add(deleteButton);

            this.addDrawableChild(accountButton);
            this.addDrawableChild(deleteButton);

            yOffset += 25;
        }
    }

    private void switchToAccount(String username) {
        try {
            // Create offline session
            GameProfile profile = new GameProfile(UUID.randomUUID(), username);
            Session session = new Session(
                username,
                profile.getId(),
                "0", // token for offline mode
                Optional.empty(),
                Optional.empty(),
                Session.AccountType.LEGACY
            );

            // Set the session using reflection
            var mc = MinecraftClient.getInstance();
            
            try {
                // Try to set session field
                var sessionField = mc.getClass().getDeclaredField("session");
                sessionField.setAccessible(true);
                sessionField.set(mc, session);
                
                System.out.println("Account switcher: Successfully switched to " + username);
                
                // If in-game, disconnect from server
                if (mc.getNetworkHandler() != null) {
                    mc.world.disconnect();
                }
                
                // Close screen
                this.close();
                
            } catch (NoSuchFieldException e) {
                // Try alternative field name for different Minecraft versions
                try {
                    var sessionField = mc.getClass().getDeclaredField("field_1726"); // Obfuscated name
                    sessionField.setAccessible(true);
                    sessionField.set(mc, session);
                    
                    System.out.println("Account switcher: Successfully switched to " + username + " (obfuscated)");
                    this.close();
                } catch (Exception ex) {
                    System.err.println("Failed to switch account: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to switch account: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        // Title
        Render2D.drawFont(context.getMatrices(), Fonts.BOLD.getFont(24f),
            "Account Switcher", this.width / 2f - 80, this.height / 2f - 80, new Color(255, 255, 255));

        // Username label
        Render2D.drawFont(context.getMatrices(), Fonts.MEDIUM.getFont(12f),
            "Username:", this.width / 2f - 100, this.height / 2f - 65, new Color(204, 204, 204));

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}