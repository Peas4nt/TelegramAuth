package com.minixomek.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UserStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File("config/testmod_users.json");

    private static List<UserRecord> users = new ArrayList<>();

    public static void load() {
        if (FILE.exists()) {
            try (FileReader reader = new FileReader(FILE)) {
                UserRecord[] arr = GSON.fromJson(reader, UserRecord[].class);
                users = new ArrayList<>(List.of(arr));
            } catch (Exception e) {
                System.err.println("[TestMod] Failed to read user data: " + e.getMessage());
            }
        } else {
            save(); // create empty file
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(users, writer);
        } catch (Exception e) {
            System.err.println("[TestMod] Failed to save user data: " + e.getMessage());
        }
    }

    // create a new user
    public static void addUser(String username, String chatId) {
        users.add(new UserRecord(username, chatId));
        save();
    }

    // get data by username
    public static UserRecord getByUsername(String username) {
        for (UserRecord record : users) {
            if (record.username.equalsIgnoreCase(username)) {
                return record;
            }
        }
        return null;
    }

    // get data by telegram chatId
    public static UserRecord getByTelegramChatId(String chatId) {
        for (UserRecord record : users) {
            if (Objects.equals(record.telegramChatId, chatId)) {
                return record;
            }
        }
        return null;
    }

    // check registered ip
    public static boolean checkUserIp(UserRecord record, String ip) {
        for (String registeredIp : record.ip) {
            if (Objects.equals(registeredIp, ip)) {
                return true;
            }
        }
        return false;
    }

    // add ip to user
    public static void changeUserName(String chatId, String newUsername) {
        UserRecord record = getByTelegramChatId(chatId);
        if (record != null) {
            if (newUsername != null && !newUsername.isBlank()) {
                record.username = newUsername;
                save();
            }
        }
    }

    // add ip to user list
    public static void addIpToUser(String username, String ip) {
        UserRecord record = getByUsername(username);
        if (record != null) {
            if (ip != null && !ip.isBlank()) {
                record.addIp(ip);
                save();
            }
        }
    }

    public static void clearUserIp(String chatId) {
        UserRecord record = getByTelegramChatId(chatId);
        if (record != null) {
            record.ip.clear();
            save();
        }
    }
}
