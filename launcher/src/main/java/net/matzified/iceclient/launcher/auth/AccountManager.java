package net.matzified.iceclient.launcher.auth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.matzified.iceclient.launcher.profile.ProfileManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class AccountManager {

    /** Listener notified on the EDT whenever the active account changes. */
    public interface AccountChangeListener {
        void onAccountStateChanged();
    }


    private static AccountManager instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File accountsFile;

    private List<Account> accounts = new ArrayList<>();
    private Account activeAccount;

    private final List<AccountChangeListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(AccountChangeListener l)    { if (l != null) listeners.add(l); }
    public void removeListener(AccountChangeListener l) { listeners.remove(l); }
    private void notifyListeners() {
        for (AccountChangeListener l : listeners) {
            try { l.onAccountStateChanged(); } catch (Exception ignored) {}
        }
    }

    private AccountManager() {
        File rootDir = ProfileManager.getInstance().getRootDir();
        accountsFile = new File(rootDir, "accounts.json");
        loadAccounts();
        autoDetectMinecraftLauncherAccounts();
    }

    public static synchronized AccountManager getInstance() {
        if (instance == null) {
            instance = new AccountManager();
        }
        return instance;
    }

    private void loadAccounts() {
        accounts.clear();
        if (accountsFile.exists()) {
            try (FileReader reader = new FileReader(accountsFile)) {
                Type listType = new TypeToken<ArrayList<Account>>(){}.getType();
                List<Account> loaded = gson.fromJson(reader, listType);
                if (loaded != null) {
                    for (Account acc : loaded) {
                        if (!acc.getUsername().equalsIgnoreCase("MinecraftPlayer")) {
                            accounts.add(acc);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (accounts.isEmpty()) {
            Account defaultAcc = new Account("default_matzified", "Matzified", "00000000-0000-0000-0000-000000000000", "token_matzified_valid", "", true);
            accounts.add(defaultAcc);
            activeAccount = defaultAcc;
            saveAccounts();
        } else {
            for (Account acc : accounts) {
                if (acc.isActive()) {
                    activeAccount = acc;
                    break;
                }
            }
            if (activeAccount == null && !accounts.isEmpty()) {
                activeAccount = accounts.get(0);
                activeAccount.setActive(true);
                saveAccounts();
            }
        }
    }

    public boolean autoDetectMinecraftLauncherAccounts() {
        boolean importedAny = false;
        try {
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                File mcDir = new File(appData, ".minecraft");
                File mcAccountsFile = new File(mcDir, "launcher_accounts.json");
                if (mcAccountsFile.exists()) {
                    try (FileReader reader = new FileReader(mcAccountsFile)) {
                        JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                        if (obj.has("accounts")) {
                            JsonObject accMap = obj.getAsJsonObject("accounts");
                            for (Map.Entry<String, com.google.gson.JsonElement> entry : accMap.entrySet()) {
                                JsonObject accObj = entry.getValue().getAsJsonObject();
                                if (accObj.has("minecraftProfile")) {
                                    JsonObject prof = accObj.getAsJsonObject("minecraftProfile");
                                    String name = prof.get("name").getAsString();
                                    String id = prof.has("id") ? prof.get("id").getAsString() : "mc_uuid_" + System.currentTimeMillis();
                                    String accessToken = accObj.has("accessToken") ? accObj.get("accessToken").getAsString() : "token_mc_launcher";

                                    Account acc = new Account("mc_" + id, name, id, accessToken, "", true);
                                    addOrUpdateAccount(acc);
                                    importedAny = true;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return importedAny;
    }

    public void saveAccounts() {
        try (FileWriter writer = new FileWriter(accountsFile)) {
            gson.toJson(accounts, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public Account getActiveAccount() {
        return activeAccount;
    }

    public void setActiveAccount(Account account) {
        for (Account a : accounts) {
            a.setActive(a != null && a.getId().equals(account.getId()));
        }
        this.activeAccount = account;
        saveAccounts();
        notifyListeners();
    }

    public void addOrUpdateAccount(Account account) {
        Account existing = null;
        for (Account a : accounts) {
            if (a.getId().equals(account.getId()) || a.getUsername().equalsIgnoreCase(account.getUsername())) {
                existing = a;
                break;
            }
        }
        if (existing != null) {
            existing.setUsername(account.getUsername());
            existing.setUuid(account.getUuid());
            existing.setAccessToken(account.getAccessToken());
            existing.setSkinUrl(account.getSkinUrl());
            setActiveAccount(existing);
        } else {
            accounts.add(account);
            setActiveAccount(account);
        }
        notifyListeners();
    }

    public void addSession(MinecraftSession session) {
        Account acc = new Account("ms_" + session.getTrimmedUuid(), session.username(), session.getFormattedUuid(), session.accessToken(), "", true);
        addOrUpdateAccount(acc);
    }

    public void removeAccount(Account account) {
        if (account == null) return;
        accounts.removeIf(a -> a.getId().equals(account.getId()) || a.getUsername().equalsIgnoreCase(account.getUsername()));
        if (activeAccount != null && activeAccount.getId().equals(account.getId())) {
            activeAccount = accounts.isEmpty() ? null : accounts.get(0);
            if (activeAccount != null) {
                activeAccount.setActive(true);
            }
        }
        saveAccounts();
        notifyListeners();
    }

    /** Convenience overload — removes account by its ID string. */
    public void removeAccount(String accountId) {
        if (accountId == null) return;
        accounts.stream().filter(a -> a.getId().equals(accountId)).findFirst().ifPresent(this::removeAccount);
    }
}
