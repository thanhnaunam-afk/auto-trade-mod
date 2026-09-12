package com.thanh.autotrade.blacklist;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Blacklist Manager:
 * - Quản lý danh sách player bị cấm
 * - Nếu phát hiện player trong blacklist → /pay froglighter + /rtp
 * - Lưu/load từ file config
 */
public class BlacklistManager {
    private static final Path BLACKLIST_FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("autotrade-blacklist.json");
    private static final Gson GSON = new Gson();

    private final List<String> blacklist = new CopyOnWriteArrayList<>();
    private final List<BlacklistListener> listeners = new CopyOnWriteArrayList<>();

    public interface BlacklistListener {
        void onBlacklistChanged();
    }

    public BlacklistManager() {
        loadBlacklist();
    }

    /**
     * Thêm player vào blacklist
     */
    public void addPlayer(String playerName) {
        if (!blacklist.contains(playerName)) {
            blacklist.add(playerName);
            saveBlacklist();
            notifyListeners();
        }
    }

    /**
     * Xóa player khỏi blacklist
     */
    public void removePlayer(String playerName) {
        if (blacklist.remove(playerName)) {
            saveBlacklist();
            notifyListeners();
        }
    }

    /**
     * Kiểm tra player có trong blacklist không
     */
    public boolean isBlacklisted(String playerName) {
        return blacklist.contains(playerName);
    }

    /**
     * Lấy tất cả player trong blacklist
     */
    public List<String> getBlacklist() {
        return new ArrayList<>(blacklist);
    }

    /**
     * Clear tất cả blacklist
     */
    public void clearBlacklist() {
        blacklist.clear();
        saveBlacklist();
        notifyListeners();
    }

    private void saveBlacklist() {
        try {
            JsonObject root = new JsonObject();
            JsonArray playersArray = new JsonArray();
            for (String player : blacklist) {
                playersArray.add(player);
            }
            root.add("players", playersArray);

            Files.createDirectories(BLACKLIST_FILE.getParent());
            try (Writer w = Files.newBufferedWriter(BLACKLIST_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
        } catch (IOException e) {
            System.err.println("[AutoTrade] Failed to save blacklist: " + e.getMessage());
        }
    }

    private void loadBlacklist() {
        if (!Files.exists(BLACKLIST_FILE)) return;

        try (Reader r = Files.newBufferedReader(BLACKLIST_FILE, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root != null && root.has("players")) {
                JsonArray players = root.getAsJsonArray("players");
                for (int i = 0; i < players.size(); i++) {
                    blacklist.add(players.get(i).getAsString());
                }
            }
        } catch (IOException e) {
            System.err.println("[AutoTrade] Failed to load blacklist: " + e.getMessage());
        }
    }

    public void addListener(BlacklistListener listener) {
        listeners.add(listener);
    }

    public void removeListener(BlacklistListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (BlacklistListener listener : listeners) {
            listener.onBlacklistChanged();
        }
    }
}
