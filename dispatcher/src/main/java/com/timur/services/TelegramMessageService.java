package com.timur.services;

import com.timur.bot.TelegramBot;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.forum.CreateForumTopic;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
@Log4j
public class TelegramMessageService implements MessageSender{

    private TelegramBot telegramBot;

    public void setTelegramBot(TelegramBot telegramBot){
        this.telegramBot = telegramBot;
    }

    @Override
    public void sendMessage(SendMessage message){
        if (message != null) {
            try {
                telegramBot.execute(message);
            } catch (TelegramApiException e) {
                log.error(e);
            }
        }
    }

    @Override
    public void sendPhoto(SendPhoto photo){
        if (photo != null) {
            try {
                telegramBot.execute(photo);
            } catch (TelegramApiException e) {
                log.error(e);
            }
        }
    }


    @Override
    public Integer createUserThread(String problem, String nameUser , Long operatorsChatId) {
        CreateForumTopic topic = CreateForumTopic.builder()
                .chatId(operatorsChatId)
                .name(problem.toUpperCase() + ", клиент: " + nameUser)
                .build();
        try {
            return telegramBot.execute(topic).getMessageThreadId();
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }
}
