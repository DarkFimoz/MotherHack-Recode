package fun.motherhack.managers;

import fun.motherhack.MotherHack;
import lombok.Getter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Getter
public class AccountManager {

    private final File accountsDir;
    private final List<String> accounts = new ArrayList<>();

    public AccountManager() {
        this.accountsDir = new File(MotherHack.getInstance().getGlobalsDir(), "Accounts");
        if (!accountsDir.exists()) {
            accountsDir.mkdirs();
        }
        loadAccounts();
    }

    public void addAccount(String username) {
        if (username == null || username.trim().isEmpty()) return;
        username = username.trim();

        if (!accounts.contains(username)) {
            accounts.add(username);
            saveAccount(username);
        }
    }

    public void removeAccount(String username) {
        if (username == null || username.trim().isEmpty()) return;
        username = username.trim();

        accounts.remove(username);
        deleteAccountFile(username);
    }

    public boolean hasAccount(String username) {
        if (username == null || username.trim().isEmpty()) return false;
        return accounts.contains(username.trim());
    }

    public List<String> getAccounts() {
        return new ArrayList<>(accounts);
    }

    private void saveAccount(String username) {
        try {
            File accountFile = new File(accountsDir, username + ".account");
            try (FileWriter writer = new FileWriter(accountFile)) {
                writer.write(username);
            }
        } catch (IOException e) {
            System.err.println("Failed to save account: " + username);
            e.printStackTrace();
        }
    }

    private void deleteAccountFile(String username) {
        try {
            File accountFile = new File(accountsDir, username + ".account");
            if (accountFile.exists()) {
                accountFile.delete();
            }
        } catch (Exception e) {
            System.err.println("Failed to delete account file: " + username);
            e.printStackTrace();
        }
    }

    private void loadAccounts() {
        try {
            Files.list(Paths.get(accountsDir.getAbsolutePath()))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".account"))
                    .forEach(path -> {
                        try {
                            String username = Files.readString(path).trim();
                            if (!username.isEmpty() && !accounts.contains(username)) {
                                accounts.add(username);
                            }
                        } catch (IOException e) {
                            System.err.println("Failed to load account from: " + path);
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            System.err.println("Failed to load accounts directory");
            e.printStackTrace();
        }
    }
}