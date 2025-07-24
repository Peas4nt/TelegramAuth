package com.minixomek.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/testmod_bot_config.json");

    public static BotConfig config = new BotConfig();

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                config = GSON.fromJson(reader, BotConfig.class);
            } catch (IOException e) {
                System.err.println("Failed to read bot config: " + e.getMessage());
            }
        } else {
            save(); // create default
        }
    }

    public static void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save bot config: " + e.getMessage());
        }
    }
}
