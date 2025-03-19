package com.timur.handlers;

import com.timur.services.UserStateService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

@Service
public class ButtonHandler {

    private final UserStateService userStateService;

    public ButtonHandler(UserStateService userStateService) {
        this.userStateService = userStateService;
    }

//    public SendMessage handleButtonClick(CallbackQuery callbackQuery) {
//        Long chatId = callbackQuery.getMessage().getChatId();
//        String callbackData = callbackQuery.getData();
//
//
//    }
}
