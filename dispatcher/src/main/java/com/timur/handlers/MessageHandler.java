package com.timur.handlers;

import com.timur.enums.UserState;
import com.timur.services.*;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;


import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageHandler {

    private final String MESSAGE_FEEDBACK = "Если Ваш вопрос был решен, просим Вас изменить негативный отзыв на нашем " +
            "Маркетплейсе. Мы будем благодарны!";
    private final String COMPLETED_ANSWER = "Благодарим вас за обращение! Надеемся, что ваш вопрос был успешно решен.";


    private static final long OPERATORS_CHAT_ID = -1002668115843L;

    private final SendMessageFabric sendMessageFabric;
    private final OperatorConnectionService operatorConnectionService;
    private final UserStorageService userStorageService;
    private final UserStateService userStateService;
    private final MessageSender messageSender;
    private final RequestUserService requestUserService;

    private final TelegramMessageService telegramMessageService;

    public MessageHandler(SendMessageFabric sendMessageFabric, OperatorConnectionService operatorConnectionService,
                          UserStorageService userStorageService, UserStateService userStateService,
                          MessageSender messageSender, RequestUserService requestUserService, TelegramMessageService telegramMessageService) {
        this.sendMessageFabric = sendMessageFabric;
        this.operatorConnectionService = operatorConnectionService;
        this.userStorageService = userStorageService;
        this.userStateService = userStateService;
        this.messageSender = messageSender;
        this.requestUserService = requestUserService;
        this.telegramMessageService = telegramMessageService;
    }


    public void handleState(Message message) {
        Long chatId = message.getChatId();
        UserState state = userStateService.getUserState(chatId);
        String text = "";

        if (message.hasText()) {
            text = message.getText();
        }

        switch (state) {
            case NAME:
                handleCaseName(chatId, text);
                break;

            case START:
                handleStart(chatId, text);
                break;

            case DEFECTIVE_PRODUCT_Q1:
                handleCaseDefectiveProductQ1(chatId, text);
                break;

            case DEFECTIVE_PRODUCT_Q2:
                handleCaseDefectiveProductQ2(chatId, text);
                break;

            case DEFECTIVE_PRODUCT_Q3:
                handleCaseDefectiveProductQ3(message);
                break;

            case BUY_PRODUCT_Q1:
                handleCaseBuyProductQ1(message);
                break;


            case WHOLESALE_SALES_DEPARTMENT_Q1:
                handleCaseWhoSalesDepartmentQ1AndOtherQuestionQ1(message);
                break;

            case OTHER_QUESTION_Q1:
                handleCaseWhoSalesDepartmentQ1AndOtherQuestionQ1(message);
                break;

            case CONNECT_OPERATOR:
                handleCaseConnectOperator(message);
                break;

            default:
                handleNoStateUserMessage(chatId);
                break;
        }
    }

    private void handleStart(Long chatId, String text) {
        switch (text) {
            case "Товар пришел с браком":
                requestUserService.addRequest(chatId, text);
                userStateService.save(chatId, UserState.DEFECTIVE_PRODUCT_Q1);
                messageSender.sendMessage(sendMessageFabric.handleDefectiveProductQ1(chatId));
                break;
            case "Хочу докупить товар":
                requestUserService.addRequest(chatId, text);
                userStateService.save(chatId, UserState.BUY_PRODUCT_Q1);
                messageSender.sendMessage(sendMessageFabric.handleBuyProduct(chatId));
                break;
            case "Связь с отделом оптовых продаж":
                requestUserService.addRequest(chatId, text);
                userStateService.save(chatId, UserState.WHOLESALE_SALES_DEPARTMENT_Q1);
                messageSender.sendMessage(sendMessageFabric.handleOtherQuestionAndWholeSaleDepartment(chatId));
                break;
            case "Другой вопрос":
                requestUserService.addRequest(chatId, text);
                userStateService.save(chatId, UserState.OTHER_QUESTION_Q1);
                messageSender.sendMessage(sendMessageFabric.handleOtherQuestionAndWholeSaleDepartment(chatId));
                break;
            default:
                break;
        }
    }

    private void handleCaseName(Long chatId, String text) {
        requestUserService.addRequest(chatId, text);
        userStateService.save(chatId, UserState.START);
        start(chatId);
    }

    public void name(Long chatId, Long userId) {
        SendMessage sendMessage = sendMessageFabric.welcomeMessage(chatId);
        messageSender.sendMessage(sendMessage);
        userStateService.save(chatId, UserState.NAME);
        userStorageService.addChatIdToUserId(chatId, userId);
    }

    public void start(Long chatId) {
        SendMessage sendMessage = sendMessageFabric.chooseMessage(chatId);
        messageSender.sendMessage(sendMessage);
    }

    public void handleResponseOperator(Message message) {

        Integer threadId = message.getMessageThreadId();
        Long userId1 = userStorageService.getUserIdByThread(threadId);
        Long


        if(userId1 == null){
            SendMessage sendMessage = sendMessageFabric
                    .sendMessageToOperator(OPERATORS_CHAT_ID, threadId, "Вы не подключены к пользователю.");
            messageSender.sendMessage(sendMessage);
            return;
        }

        if (message.hasText() && message.getText().equals("/end_chat@jewel_posuda_bot")) {
            disconnect(userId1, threadId);
        } else if ((message.hasText() && message.getText().equals("/send_message@jewel_posuda_bot"))) {
            SendMessage sendMessage = sendMessageFabric.forwardToUser(userId1, MESSAGE_FEEDBACK);
            messageSender.sendMessage(sendMessage);
        } else if (message.hasPhoto()) { // отправка фото с описанием
            String photoId = message.getPhoto().get(0).getFileId();
            if (message.getCaption() != null) {
                SendMessage sendMessage = sendMessageFabric.sendMessageToUser(userId1, message.getCaption());
                telegramMessageService.sendMessage(sendMessage);
            }

            SendPhoto sendPhoto = sendMessageFabric.sendPhotoToUser(userId1, photoId);
            telegramMessageService.sendPhoto(sendPhoto);

        } else { //отправка текста
            SendMessage sendMessage = sendMessageFabric.forwardToUser(userId1, message.getText());
            messageSender.sendMessage(sendMessage);
        }

    }

    private void disconnect(Long userId1, Integer threadId) {
        userStateService.clearUserSession(userId1); //chatId то же самое что и userId у пользователя
        userStorageService.removeUserToOperator(userId1);
        userStorageService.removeUserToThread(userId1);
        userStorageService.removeChatIdToUserId(userChatId);
        userStorageService.removeThreadToUser(threadId);

        SendMessage sendMessageToUser = sendMessageFabric
                .forwardToUser(userId1, COMPLETED_ANSWER);

        SendMessage sendMessageToOperator = sendMessageFabric
                .sendMessageToOperator(OPERATORS_CHAT_ID, threadId, "Чат c пользователем " + userId1 + " завершен.");

        messageSender.sendMessage(sendMessageToUser);
        messageSender.sendMessage(sendMessageToOperator);
    }

    public void handleNoStateUserMessage(Long userId) {
        SendMessage sendMessage = sendMessageFabric
                .forwardToUser(userId, "Чтобы связаться с поддержкой нажмите: /start ");
        messageSender.sendMessage(sendMessage);
    }


    private void handleCaseConnectOperator(Message message) {
        Long userId = userStorageService.getUserIdByChatId(message.getChatId());
        Integer threadIdOperator = userStorageService.getThreadByUserId(userId);

        if (message.hasPhoto()) { //проверяю есть ли фото
            SendPhoto sendPhoto = sendMessageFabric.sendPhotoToOperator(OPERATORS_CHAT_ID, threadIdOperator,
                    message.getPhoto().get(0).getFileId());
            if (message.getCaption()!=null){
                telegramMessageService.sendMessage(sendMessageFabric
                        .sendMessageToOperator(OPERATORS_CHAT_ID, threadIdOperator, message.getCaption()));
            }
            telegramMessageService.sendPhoto(sendPhoto);
        } else { //если нет, то текст
            SendMessage sendMessage = sendMessageFabric.forwardToOperator(userId, threadIdOperator, message.getText());
            messageSender.sendMessage(sendMessage);
        }
    }

    private void handleCaseWhoSalesDepartmentQ1AndOtherQuestionQ1(Message message) {

        Long chatId = message.getChatId();
        String username = message.getFrom().getUserName();
        String text = message.getText();
        List<PhotoSize> photos = message.getPhoto();
        String caption = message.getCaption();

        if (photos != null && !photos.isEmpty()) { //проверяю есть ли фото
            if (caption != null) {
                requestUserService.addRequest(chatId, " описание фото : " + caption);
            }
            String photoFileId = photos.get(0).getFileId();
            requestUserService.addRequest(chatId, "photo:" + photoFileId);
        } else {
            requestUserService.addRequest(chatId, text);
        }

        userStateService.save(chatId, UserState.CONNECT_OPERATOR);
        SendMessage sendMessage = sendMessageFabric.handleTransferMessageAnswer(chatId);
        messageSender.sendMessage(sendMessage);

        List<String> request = requestUserService.getListRequest(chatId);
        String name = request.get(0);
        String problem = request.get(1);
        Integer threadId = messageSender.createUserThread(problem, name, OPERATORS_CHAT_ID);

        createConnectionUserToOperator(chatId, username, threadId);
        requestUserSendToOperator(chatId, threadId);
    }

    private void handleCaseBuyProductQ1(Message message) {

        Long chatId = message.getChatId();
        String username = message.getFrom().getUserName();
        List<PhotoSize> photos = message.getPhoto();
        String caption = message.getCaption();
        String text = message.getText();

        if (photos != null && !photos.isEmpty()) { //проверяю есть ли фото
            if (caption != null) {
                requestUserService.addRequest(chatId, " описание фото : " + caption);
            }
            String photoFileId = photos.get(0).getFileId();
            requestUserService.addRequest(chatId, "photo:" + photoFileId);
        } else {
            requestUserService.addRequest(chatId, text);
        }

        userStateService.save(chatId, UserState.CONNECT_OPERATOR); //сохраняю состояние и отправляю сообщение о подключении к оператору
        SendMessage sendMessage = sendMessageFabric.handleTransferMessageAnswer(chatId);
        messageSender.sendMessage(sendMessage);

        List<String> request = requestUserService.getListRequest(chatId);
        String name = request.get(0);
        String problem = request.get(1);
        Integer threadId = messageSender.createUserThread(problem, name, OPERATORS_CHAT_ID);

        createConnectionUserToOperator(chatId, username, threadId);

        requestUserSendToOperator(chatId, threadId);
    }

    private void handleCaseDefectiveProductQ3(Message message) {
        Long chatId = message.getChatId();
        String username = message.getFrom().getUserName();
        String text = message.getText();
        List<PhotoSize> photos = message.getPhoto();
        String caption = message.getCaption();

        if (photos != null && !photos.isEmpty()) { //проверяю есть ли фото
            if (caption != null) {
                requestUserService.addRequest(chatId, " описание фото : " + caption);
            }
            String photoFileId = photos.get(0).getFileId();
            requestUserService.addRequest(chatId, "photo:" + photoFileId);
        } else {
            requestUserService.addRequest(chatId, text);
        }

        userStateService.save(chatId, UserState.CONNECT_OPERATOR);
        SendMessage sendMessage = sendMessageFabric.handleTransferMessageAnswer(chatId);
        messageSender.sendMessage(sendMessage);

        List<String> request = requestUserService.getListRequest(chatId);
        String name = request.get(0);
        String problem = request.get(1);
        Integer threadId = messageSender.createUserThread(problem, name, OPERATORS_CHAT_ID);

        createConnectionUserToOperator(chatId, username, threadId);
        requestUserSendToOperator(chatId, threadId);

    }

    private void createConnectionUserToOperator(Long chatId, String username, Integer threadId) {
        Long userId = userStorageService.getUserIdByChatId(chatId);
        operatorConnectionService.connectToOperator(userId, OPERATORS_CHAT_ID, threadId, username);
    }

    private void requestUserSendToOperator(Long chatId, Integer threadId) {
        List<String> messageToOperator = requestUserService.getListRequest(chatId);

        Long userId = userStorageService.getUserIdByChatId(chatId);
        SendMessage sendMessage = sendMessageFabric
                .forwardToOperator(userId, threadId, listToString(messageToOperator));
        messageSender.sendMessage(sendMessage);

        for (String item : messageToOperator) {
            if (item.startsWith("photo:")) {
                String photoFileId = item.substring(6); // Убираем префикс "photo:"
                SendPhoto sendPhoto = sendMessageFabric.sendPhotoToOperator(OPERATORS_CHAT_ID, threadId, photoFileId);
                messageSender.sendPhoto(sendPhoto);
            }
        }

        requestUserService.removeRequestUser(chatId);
    }

    private void handleCaseDefectiveProductQ2(Long chatId, String text) {
        requestUserService.addRequest(chatId, text);
        userStateService.save(chatId, UserState.DEFECTIVE_PRODUCT_Q3);
        SendMessage sendMessage = sendMessageFabric.handleQuestionDescriptionAnswer(chatId);
        messageSender.sendMessage(sendMessage);
    }

    private void handleCaseDefectiveProductQ1(Long chatId, String text) {
        requestUserService.addRequest(chatId, text);
        userStateService.save(chatId, UserState.DEFECTIVE_PRODUCT_Q2);
        SendMessage sendMessage = sendMessageFabric.handleDefectiveProductQ2(chatId);
        messageSender.sendMessage(sendMessage);
    }

    private String listToString(List<String> list) {
        return list.stream()
                .filter(item -> !item.startsWith("photo:")) // Игнорируем фото
                .map(item -> "- " + item) // Добавляем "- " к каждому элементу
                .collect(Collectors.joining("\n")); // Объединяем с переходом на новую строку
    }
}
