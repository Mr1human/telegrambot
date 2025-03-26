package com.timur.services;

import com.timur.utils.ButtonKeyboard;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

@Service
public class SendMessageFabric {

    private final UserStorageService userStorageService;

    private final ButtonKeyboard buttonKeyboard;

    private final String QUESTION_DESCRIPTION = "Чтобы мои коллеги быстрее смогли вам помочь" +
            ", пожалуйста, подробно опишите ситуацию или задайте свой вопрос.";

    private final String TRANSFER_MESSAGE = "Благодарим вас за ответ! Наш оператор скоро свяжется с вами!";


    public SendMessageFabric(UserStorageService userStorageService, ButtonKeyboard buttonKeyboard) {
        this.userStorageService = userStorageService;
        this.buttonKeyboard = buttonKeyboard;
    }

    public SendMessage chooseMessage(Long chatId) {
        String text = "Какое направление вас интересует?";
        return createSendMessageWithButtons(chatId, text, buttonKeyboard.replyKeyboardMarkupWelcome());
    }

    public SendMessage welcomeMessage(Long chatId) {
        String text = "Здравствуйте! На связи чат-бот JEWEL. " +
                "Как мы можем к вам обращаться?";
        return createSendMessage(chatId, text);
    }

    public SendMessage handleDefectiveProductQ1(Long chatId) {
        return createSendMessageWithButtons(chatId,
                "С какого маркетплейса был заказан товар?",
                buttonKeyboard.marketplaceButton());
    }

    public SendMessage handleDefectiveProductQ2(Long chatId) {
        SendMessage sendMessage = createSendMessage(chatId, "Уточните номер заказа или отправления");
        sendMessage.setReplyMarkup(buttonKeyboard.keyboardRemove());
        return sendMessage;
    }

    public SendMessage handleBuyProduct(Long chatId) {
        SendMessage sendMessage = createSendMessage(chatId, "Просим прислать вас артикул, название или фото товара.");
        sendMessage.setReplyMarkup(buttonKeyboard.keyboardRemove());
        return sendMessage;
    }

    public SendMessage handleOtherQuestionAndWholeSaleDepartment(Long chatId) {
        SendMessage sendMessage = createSendMessage(chatId, QUESTION_DESCRIPTION);
        sendMessage.setReplyMarkup(buttonKeyboard.keyboardRemove());
        return sendMessage;
    }

    public SendMessage handleQuestionDescriptionAnswer(Long chatId) {
        SendMessage sendMessage = createSendMessage(chatId, QUESTION_DESCRIPTION);
        sendMessage.setReplyMarkup(buttonKeyboard.keyboardRemove());
        return sendMessage;
    }

    public SendMessage handleTransferMessageAnswer(Long chatId) {
        SendMessage sendMessage = createSendMessage(chatId, TRANSFER_MESSAGE);
        sendMessage.setReplyMarkup(buttonKeyboard.keyboardRemove());
        return sendMessage;
    }

    public SendPhoto sendPhotoToOperator(Long chatId, Integer threadId, String photoFileId) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(String.valueOf(chatId)); // ID чата оператора
        sendPhoto.setMessageThreadId(threadId);
        sendPhoto.setPhoto(new InputFile(photoFileId)); // fileId фото
        return sendPhoto;
    }

    public SendPhoto sendPhotoToUser(Long chatId, String photoFileId) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(String.valueOf(chatId)); // ID чата зера
        sendPhoto.setPhoto(new InputFile(photoFileId)); // fileId фото
        return sendPhoto;
    }

    public SendMessage forwardToUser(Long userId, String text) {
        return sendMessageToUser(userId, text);
    }

    public SendMessage forwardToOperator(Long userId, Integer threadId, String text) {

        if (userStorageService.existsOperatorByUserId(userId)) {
            Long operatorId = userStorageService.getOperatorByUserId(userId);
            return sendMessageToOperator(operatorId, threadId, "Сообщение от пользователя " + userId + ": " + "\n" + text);
        } else {
            return sendMessageToUser(userId, "Вы не подключены к техподдержке. Нажмите /start .");
        }

    }

    public SendMessage sendMessageToUser(Long userId, String text) {
        return createSendMessage(userId, text);
    }


    public SendMessage sendMessageToOperator(Long chatId, Integer threadId, String text) {
        SendMessage sendMessage = createSendMessage(chatId, text);

        if (threadId != null) {
            sendMessage.setMessageThreadId(threadId);
        }

        return sendMessage;
    }


    private SendMessage createSendMessageWithButtons(Long chatId, String text, ReplyKeyboardMarkup replyKeyboardMarkup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        return sendMessage;
    }

    private SendMessage createSendMessage(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        return sendMessage;
    }
}
