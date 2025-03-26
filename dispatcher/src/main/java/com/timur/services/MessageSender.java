package com.timur.services;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

public interface MessageSender {
    void sendMessage(SendMessage message);
    void sendPhoto(SendPhoto photo);
    Integer createUserThread (String problem, String nameUser , Long operatorsChatId);
}
