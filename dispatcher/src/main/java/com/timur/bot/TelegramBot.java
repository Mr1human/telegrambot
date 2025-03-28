package com.timur.bot;

import com.timur.handlers.MessageHandler;
import com.timur.services.TelegramMessageService;
import com.timur.services.UserStateService;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeChat;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Component
@Log4j
public class TelegramBot extends TelegramLongPollingBot {

    @Value("${telegram.operators.chat.id}")
    private Long OPERATORS_CHAT_ID;

    private final UserStateService userStateService;

    @Autowired
    private TelegramMessageService telegramMessageService;

    @Value("${telegram.bot.username}")
    private String botName;

    private final MessageHandler messageHandler;


    public TelegramBot(@Value("${telegram.bot.token}") String botToken, UserStateService userStateService,
                       MessageHandler messageHandler) {
        super(botToken);
        this.userStateService = userStateService;
        this.messageHandler = messageHandler;
    }

    @PostConstruct
    public void init() {
        telegramMessageService.setTelegramBot(this);
    }

    @Override
    public String getBotUsername() {
        return botName;
    }


    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();

            if (message.hasText() || message.hasPhoto()) {

                if (chatId > 0) {
                    if (message.hasText() && message.getText().equals("/start")) {
                        messageHandler.name(chatId);
                    } else if(!userStateService.isUserInActiveState(chatId)) {
                        messageHandler.handleNoStateUserMessage(chatId);
                    }else{
                        messageHandler.handleState(message);
                    }
                } else {
                    messageHandler.handleResponseOperator(message);
                }
            }
        }
    }

    @Override
    public void onRegister() {
        setCommandsForUser();
        setCommandsForOperator();
    }

    private void setCommandsForOperator() {
        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("welcome", "Приветсвтие пользователя"));
        commands.add(new BotCommand("send_message", "Отправляет сообщение пользователю " +
                "с просьбой удалить негативный отзыв"));
        commands.add(new BotCommand("end_chat", "Завершение чата с пользователем"));

        SetMyCommands setCommands = new SetMyCommands();
        setCommands.setCommands(commands);
        setCommands.setScope(new BotCommandScopeChat(String.valueOf(OPERATORS_CHAT_ID)));

        try {
            execute(setCommands);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void setCommandsForUser() {
        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("start", "Начать чат с поддержкой."));

        SetMyCommands setCommands = new SetMyCommands();
        setCommands.setCommands(commands);
        setCommands.setScope(new BotCommandScopeDefault());

        try {
            execute(setCommands);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
