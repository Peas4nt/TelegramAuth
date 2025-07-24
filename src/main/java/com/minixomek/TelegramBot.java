package com.minixomek;

import com.minixomek.data.UserRecord;
import com.minixomek.data.UserStorage;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TelegramBot extends TelegramLongPollingBot {
    private final String token;
    private final String username;

    public TelegramBot(String token, String username) {
        this.token = token;
        this.username = username;
    }

    public static TelegramBot start(String token, String username) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

        TelegramBot telegramBot = new TelegramBot(token, username);
        botsApi.registerBot(telegramBot);

        return telegramBot;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            CallbackQuery callback = update.getCallbackQuery();
            String data = callback.getData();
            String chatId = callback.getMessage().getChatId().toString();

            // get data from button context
            String[] parts = data.split("\\s+", 2);
            String action = parts[0];
            String ip = parts[1];

            if (Objects.equals(action, "confirm:")) {
                handleLoginConfirmation(chatId, ip, true);
            } else if (Objects.equals(action, "deny:")) {
                handleLoginConfirmation(chatId, ip, false);
            }

            // send message
            try {
                AnswerCallbackQuery answer = new AnswerCallbackQuery();
                answer.setCallbackQueryId(callback.getId());
                answer.setText("✅ Response received");
                execute(answer);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        else if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String userText = update.getMessage().getText().trim();

            // split the message
            String[] parts = userText.split("\\s+", 2);
            String command = parts[0];
            String argument = parts.length > 1 ? parts[1] : "";

            // telegram bot commands
            String reply = switch (command.toLowerCase()) {
                case "/start" -> "Hi! Write /help, to help.";
                case "/help" -> handleHelpCommand();
                case "/register" -> handleRegisterCommand(chatId, argument);
                case "/status" -> handleStatusCommand(chatId);
                case "/changeusername" -> handleChangeUsernameCommand(chatId, argument);
                case "/closesessions" -> handleCloseSessionCommand(chatId);
                default -> "Unknown command. Write /help to get all commands.";
            };

            // sending the answer
            SendMessage message = new SendMessage(chatId, reply);
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private String handleHelpCommand() {
        return """
            /help - Show available commands.
            /register <username> - Register your Minecraft username.
            /status - View your account status.
            /changeusername <username> - Update your linked username.
            /closesessions - Clear saved IPs and require re-confirmation on login.
            """;
    }

    private String handleStatusCommand(String chatIdStr) {
        UserRecord user = UserStorage.getByTelegramChatId(chatIdStr);
        if (user == null) {
            return "no user data";
        }
        StringBuilder message = new StringBuilder();

        message.append("Username: ").append(user.username);
        message.append("\nIP Addresses:");

        if (user.ip.size() == 0) {
            message.append(" none");
        }

        for (String ip : user.ip) {
            message.append(" ").append(ip).append(",");
        }

        return message.toString();
    }

    private String handleChangeUsernameCommand(String chatIdStr, String username) {
        // check if username is empty
        if (username.isBlank()) {
            return "Please write the username. For example: /changeusername Player123";
        }

        // check if this username exists
        UserRecord usernameUserCheck = UserStorage.getByUsername(username);
        if (usernameUserCheck != null) {
            return "This player name already registered. \nIf its your username, please contact with server administrator.";
        }

        UserStorage.changeUserName(chatIdStr, username);

        return "You successfully changed username to " + username;
    }

    private String handleCloseSessionCommand(String chatIdStr) {
        UserStorage.clearUserIp(chatIdStr);

        return "You successfully closed all sessions.";
    }

    private String handleRegisterCommand(String chatIdStr, String username) {
        // check if username is empty
        if (username.isBlank()) {
            return "Please write the username. For example: /register Player123";
        }

        // check if this telegram account have a registered username
        UserRecord telegramUserCheck = UserStorage.getByTelegramChatId(chatIdStr);
        if (telegramUserCheck != null) {
            return "Cannot registered username. You already have registered username: " + telegramUserCheck.username;
        }

        // check if this username exists
        UserRecord usernameUserCheck = UserStorage.getByUsername(username);
        if (usernameUserCheck != null) {
            return "This player name already registered. \nIf its your username, please contact with server administrator.";
        }

        // add user data to json
        UserStorage.addUser(username, chatIdStr);

        return "You successfully registered: " + username;
    }

    // Login confirmation message
    public void sendLoginConfirmation(UserRecord user, String ip) {
        SendMessage message = new SendMessage();
        message.setChatId(user.telegramChatId);
        message.setText("New login attempt for user: " + user.username + " (" + ip + ")");

        // Message buttons
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // confirm button
        InlineKeyboardButton confirmButton = new InlineKeyboardButton("✅ Confirm");
        confirmButton.setCallbackData("confirm: " + ip);

        // deny button
        InlineKeyboardButton denyButton = new InlineKeyboardButton("❌ Deny");
        denyButton.setCallbackData("deny: " + ip);

        rows.add(List.of(confirmButton, denyButton));

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleLoginConfirmation(String chatId, String ip, boolean approved) {
        UserRecord user = UserStorage.getByTelegramChatId(chatId);

        if (approved) {
            TelegramAuth.LOGGER.info("Player {} approved join via telegram", user.username);
            UserStorage.addIpToUser(user.username, ip);
        } else {
            TelegramAuth.LOGGER.info("Player {} denied join via telegram", user.username);
        }

        String response = approved ? "✅ Access approved for " + ip
                : "❌ Access denied for " + ip;

        SendMessage message = new SendMessage(user.telegramChatId, response);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }
}