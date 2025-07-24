package com.minixomek.data;

import java.util.ArrayList;
import java.util.List;

public class UserRecord {
    public String username;
    public List<String> ip = new ArrayList<>();
    public String telegramChatId;

    public UserRecord(String username, String telegramChatId) {
        this.username = username;
        this.telegramChatId = telegramChatId;
    }

    public void addIp(String newIp) {
        if (!ip.contains(newIp)) {
            ip.add(newIp);
        }
    }
}
