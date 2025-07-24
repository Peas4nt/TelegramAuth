package com.minixomek;

import com.minixomek.config.ConfigManager;
import com.minixomek.data.UserRecord;
import com.minixomek.data.UserStorage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TelegramAuth implements ModInitializer {
    public static final String MOD_ID = "telegramauth";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private TelegramBot telegramBot;

    @Override
    public void onInitialize() {
        LOGGER.info("TestMod initializing...");

        ConfigManager.load(); // load config data
        UserStorage.load(); // load users data

        // Config data importing
        String token = ConfigManager.config.telegramApiKey;
        String botUsername = ConfigManager.config.telegramBotName;

        // Token checking
        if (token == null || token.isBlank() || token.equals("YOUR_DEFAULT_KEY")) {
            LOGGER.error("Token is empty");
            return;
        }

        // Bot username checking
        if (botUsername == null || botUsername.isBlank() || botUsername.equals("YOUR_BOT_USERNAME")) {
            LOGGER.error("Bot username is empty");
            return;
        }

        LOGGER.info("Telegram bot key loaded");
        LOGGER.info("Telegram bot ussername loaded");

        try {
            // Bot Starting
            this.telegramBot = TelegramBot.start(token, botUsername);
            LOGGER.info("Telegram bot successfully started");

        } catch (Exception e) {
            LOGGER.error("Failed to start Telegram bot (Incorrect bot username or bot api key)", e);
            return;
        }

        // initialize events
        this.initServersEvents();
    }

    private void initServersEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UserStorage.load(); // load users data

            ServerPlayerEntity player = handler.getPlayer();

            // get player ip and username
            String playerName = player.getName().getString();
            String ipAddress = player.getIp();

            // get data from config
            UserRecord userData = UserStorage.getByUsername(playerName);

            // if data null disconnect player
            if (userData == null) {
                handler.disconnect(Text.of("You're not registered. Please register your username via the Telegram bot."));
                return;
            }

            // if player join with new ip then disconnect player and send him confirm message in telegram
            if (!UserStorage.checkUserIp(userData, ipAddress))  {
                handler.disconnect(Text.of("Please confirm your login via Telegram to proceed."));
                telegramBot.sendLoginConfirmation(userData, ipAddress);
            }
        });
    }


}

