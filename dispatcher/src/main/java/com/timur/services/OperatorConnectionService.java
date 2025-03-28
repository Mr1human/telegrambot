package com.timur.services;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Service
public class OperatorConnectionService {
    private final SendMessageFabric sendMessageFabric;
    private final UserStorageService userStorageService;
    private final MessageSender messageSender;

    public OperatorConnectionService(SendMessageFabric sendMessageFabric, UserStorageService userStorageService, MessageSender messageSender) {
        this.sendMessageFabric = sendMessageFabric;
        this.userStorageService = userStorageService;
        this.messageSender = messageSender;
    }

    public void connectToOperator(Long userChatId, Long operatorsChatId, Integer threadId, String username) {

        userStorageService.addUserChatToOperator(userChatId, operatorsChatId);
        userStorageService.addUserChatToThread(userChatId, threadId);
        userStorageService.addThreadToUserChat(threadId, userChatId);

        SendMessage toOperator = sendMessageFabric
                .sendMessageToOperator(operatorsChatId, threadId, "Пользователь @"
                        + username + " запросил поддержку.");
        messageSender.sendMessage(toOperator);
    }
}
