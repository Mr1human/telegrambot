package com.timur.handlers;

import com.timur.enums.UserState;
import com.timur.services.UserStateService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StateHandler {


    private final String QUESTION_DESCRIPTION = "Чтобы мои коллеги быстрее смогли вам помочь" +
            ", пожалуйста, подробно опишите ситуацию или задайте свой вопрос.";

    private final String TRANSFER_MESSAGE = "Благодарим вас за ответ! Наш оператор скоро свяжется с вами!";
    private final String COMPLETED_ANSWER = "Благодарим вас за обращение! Надеемся, что ваш вопрос был успешно решен.";
    private final UserStateService userStateService;

    public StateHandler(UserStateService userStateService) {
        this.userStateService = userStateService;
    }

    public SendMessage handleState(Long chatId, String text) {
        UserState state = userStateService.getUserState(chatId);

        if (state == null) {
            userStateService.save(chatId, UserState.START);
            return welcomeMessage(chatId);
        }

        switch (state) {
            case START:
                handleStart(chatId, text);
                return handleState(chatId, text);

            case DEFECTIVE_PRODUCT_Q1:
                userStateService.save(chatId, UserState.DEFECTIVE_PRODUCT_Q2);
                return handleDefectiveProductQ1(chatId);
            case DEFECTIVE_PRODUCT_Q2:
                userStateService.save(chatId, UserState.DEFECTIVE_PRODUCT_Q3);
                return handleDefectiveProductQ2(chatId);
            case DEFECTIVE_PRODUCT_Q3:
                userStateService.save(chatId, UserState.TRANSFER_OPERATOR);
                return handleDefaultAnswer(chatId, QUESTION_DESCRIPTION);

            case BUY_PRODUCT_Q1:
                userStateService.save(chatId, UserState.TRANSFER_OPERATOR);
                return handleBuyProduct(chatId);

            case WHOLESALE_SALES_DEPARTMENT_Q1:
                userStateService.save(chatId, UserState.TRANSFER_OPERATOR);
                return handleDefaultAnswer(chatId, QUESTION_DESCRIPTION);

            case OTHER_QUESTION_Q1:
                userStateService.save(chatId, UserState.TRANSFER_OPERATOR);
                return handleDefaultAnswer(chatId, QUESTION_DESCRIPTION);

            case TRANSFER_OPERATOR:
                userStateService.save(chatId, UserState.CONNECT_OPERATOR);

                return handleDefaultAnswer(chatId, TRANSFER_MESSAGE);

            case COMPLETED:
                userStateService.save(chatId, UserState.START);
                return handleDefaultAnswer(chatId, COMPLETED_ANSWER);
            default:
                return handleDefaultAnswer(chatId, "error...");
        }
    }


    private void handleStart(Long chatId, String text) {
        switch (text) {
            case "Товар пришел с браком":
                userStateService.save(chatId, UserState.DEFECTIVE_PRODUCT_Q1);
                break;
            case "Хочу докупить товар":
                userStateService.save(chatId, UserState.BUY_PRODUCT_Q1);
                break;
            case "Связь с отделом оптовых продаж":
                userStateService.save(chatId, UserState.WHOLESALE_SALES_DEPARTMENT_Q1);
                break;
            case "Другой вопрос":
                userStateService.save(chatId, UserState.OTHER_QUESTION_Q1);
                break;
            default:
//                userStateService.save(chatId, UserState.START);
                break;
        }
    }

    private SendMessage handleDefectiveProductQ1(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("С какого маркетплейса был заказан товар?");
        sendMessage.setReplyMarkup(marketplaceButton());
        return sendMessage;
    }

    private SendMessage handleDefectiveProductQ2(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Уточните номер заказа или отправления");
        return sendMessage;
    }

    private ReplyKeyboardMarkup marketplaceButton() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);  // Делаем клавиатуру компактной
        keyboardMarkup.setOneTimeKeyboard(true);

        KeyboardButton wb = new KeyboardButton("WB");
        KeyboardButton ozon = new KeyboardButton("OZON");
        KeyboardButton ya = new KeyboardButton("Яндекс Маркет");

        KeyboardRow row1 = new KeyboardRow();
        row1.add(wb);
        row1.add(ozon);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(ya);

        List<KeyboardRow> keyboard = List.of(row1, row2);
        keyboardMarkup.setKeyboard(keyboard);

        return keyboardMarkup;
    }

    private SendMessage handleBuyProduct(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Просим прислать вас артикул, название или фото товара.");
        return sendMessage;
    }

    private SendMessage handleOtherQuestionAndWholeSaleDepartment(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(QUESTION_DESCRIPTION);
        return sendMessage;
    }

    private SendMessage handleDefaultAnswer(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        return sendMessage;
    }

    private SendMessage welcomeMessage(Long chatId) {
        SendMessage welcomeMessage = new SendMessage();
        welcomeMessage.setChatId(chatId.toString());
        welcomeMessage.setText("Здравствуйте! На связи чат-бот JEWEL." +
                " Какое направление вас интересует?");
        welcomeMessage.setReplyMarkup(replyKeyboardMarkupWelcome());
        return welcomeMessage;
    }

    public ReplyKeyboardMarkup replyKeyboardMarkupWelcome() {

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);  // Делаем клавиатуру компактной
        keyboardMarkup.setOneTimeKeyboard(true);

        KeyboardButton defectiveProduct = new KeyboardButton("Товар пришел с браком");
        KeyboardButton buyProduct = new KeyboardButton("Хочу докупить товар");
        KeyboardButton wholeSales = new KeyboardButton("Связь с отделом оптовых продаж");
        KeyboardButton otherQuestion = new KeyboardButton("Другой вопрос");

        // Группируем по две кнопки в ряд
        KeyboardRow row1 = new KeyboardRow();
        row1.add(defectiveProduct);
        row1.add(buyProduct);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(wholeSales);
        row2.add(otherQuestion);

        List<KeyboardRow> keyboard = List.of(row1, row2);
        keyboardMarkup.setKeyboard(keyboard);

        return keyboardMarkup;
    }

}
