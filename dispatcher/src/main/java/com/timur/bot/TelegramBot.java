package com.timur.bot;

import com.timur.enums.UserState;
import com.timur.handlers.StateHandler;
import com.timur.services.UserStateService;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.forum.CreateForumTopic;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Log4j
public class TelegramBot extends TelegramLongPollingBot {


    private final Map<Long, Long> userToOperator = new HashMap<>();
    private final Map<Long, Integer> userToThread = new HashMap<>();
    private final Map<Long, Long> chatIdAndUserId = new HashMap<>();
    private Map<Long, List<String>> requestUser = new HashMap<>();

    private final UserStateService userStateService;
    private StateHandler stateHandler;

    private static final long OPERATORS_CHAT_ID = -1002668115843L;
    private final String QUESTION_DESCRIPTION = "Чтобы мои коллеги быстрее смогли вам помочь" +
            ", пожалуйста, подробно опишите ситуацию или задайте свой вопрос.";

    private final String TRANSFER_MESSAGE = "Благодарим вас за ответ! Наш оператор скоро свяжется с вами!";
    private final String COMPLETED_ANSWER = "Благодарим вас за обращение! Надеемся, что ваш вопрос был успешно решен.";

    @Value("${telegram.bot.username}")
    private String botName;


    public TelegramBot(@Value("${telegram.bot.token}") String botToken, UserStateService userStateService) {
        super(botToken);
        this.userStateService = userStateService;
    }

    @Override
    public String getBotUsername() {
        return botName;
    }


    @Override
    public void onUpdateReceived(Update update) {


        if (update.hasMessage()) {
            Message message = update.getMessage();
            Long userId = message.getFrom().getId();
            String username = message.getFrom().getUserName();

            Long chatId = message.getChatId();

            if (message.hasText() || message.hasPhoto()) {


                if (chatId > 0) {

                    if (message.hasText() && message.getText().equals("/start")) {
                        SendMessage sendMessage = welcomeMessage(chatId);
                        sendResponseMessage(sendMessage);

                        userStateService.save(chatId, UserState.START);
                        chatIdAndUserId.put(chatId, userId);

                        List<String> words = new ArrayList<>();
                        requestUser.put(chatId, words);
                    } else {
                        handleState(message);
                    }
                } else {
                    Integer threadId = message.getMessageThreadId();
                    for (Map.Entry<Long, Integer> entry : userToThread.entrySet()) {
                        if (entry.getValue().equals(threadId)) {
                            userId = entry.getKey();
                            break;
                        }
                    }

                    forwardToUser(userId, message.getText());
                }
            }

        }

    }

    private void sendPhotoToOperator(long chatId, Integer threadId, String photoFileId, String caption) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(String.valueOf(chatId)); // ID чата оператора
        sendPhoto.setMessageThreadId(threadId);
        sendPhoto.setPhoto(new InputFile(photoFileId)); // fileId фото
        sendPhoto.setCaption(caption); // Описание фото

        try {
            execute(sendPhoto); // Отправляем фото
        } catch (TelegramApiException e) {
            e.printStackTrace(); // Обработка ошибок
        }
    }

    private String listToString(List<String> list) {
        return list.stream()
                .filter(item -> !item.startsWith("photo:")) // Игнорируем фото
                .map(item -> "- " + item) // Добавляем "- " к каждому элементу
                .collect(Collectors.joining("\n")); // Объединяем с переходом на новую строку
    }


    public void handleState(Message message) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        UserState state = userStateService.getUserState(chatId);
        String username = message.getFrom().getUserName();
        Integer threadId = 0;
        List<String> messageToOperator = new ArrayList<>();
        String text = "";

        if (message.hasText()) {
            text = message.getText();
        }

        SendMessage sendMessage = new SendMessage();
        switch (state) {

            case START:
                handleStart(chatId, text);
                break;

            case DEFECTIVE_PRODUCT_Q1:
                requestUser.get(chatId).add(text);
                userStateService.save(chatId, UserState.DEFECTIVE_PRODUCT_Q2);
                sendMessage = handleDefectiveProductQ2(chatId);
                sendResponseMessage(sendMessage);
                break;

            case DEFECTIVE_PRODUCT_Q2:
                requestUser.get(chatId).add(text);
                userStateService.save(chatId, UserState.DEFECTIVE_PRODUCT_Q3);
                sendMessage = handleDefaultAnswer(chatId, QUESTION_DESCRIPTION);
                sendResponseMessage(sendMessage);
                break;

            case DEFECTIVE_PRODUCT_Q3:
                requestUser.get(chatId).add(text);
                userStateService.save(chatId, UserState.CONNECT_OPERATOR);
                sendMessage = handleDefaultAnswer(chatId, TRANSFER_MESSAGE);
                sendResponseMessage(sendMessage);

                threadId = createUserThread(username);
                chatIdAndUserId.put(chatId, userId);
                connectToOperator(userId, threadId, username);

                //Long operatorId = userToOperator.get(userId);
                messageToOperator = requestUser.get(chatId);

                forwardToOperator(userId, threadId, listToString(messageToOperator), "");
                requestUser.remove(chatId);

                break;

            case BUY_PRODUCT_Q1:

                if(message.getPhoto()!=null){
                    List<PhotoSize> photos = message.getPhoto();
                    PhotoSize photo = photos.get(0); // Берем самое большое фото
                    String photoFileId = photo.getFileId();
                    requestUser.get(chatId).add("photo:" + photoFileId); // Добавляем в заявку
                }else{
                    requestUser.get(chatId).add(text);
                }

                userStateService.save(chatId, UserState.CONNECT_OPERATOR);
                sendMessage = handleDefaultAnswer(chatId, TRANSFER_MESSAGE);
                sendResponseMessage(sendMessage);

                threadId = createUserThread(username);
                chatIdAndUserId.put(chatId, userId);
                connectToOperator(userId, threadId, username);

                //Long operatorId = userToOperator.get(userId);
                messageToOperator = requestUser.get(chatId);

                forwardToOperator(userId, threadId, listToString(messageToOperator), "");

                // Отправляем фото, если оно есть
                for (String item : messageToOperator) {
                    if (item.startsWith("photo:")) {
                        String photoFileId = item.substring(6); // Убираем префикс "photo:"
                        sendPhotoToOperator(OPERATORS_CHAT_ID, threadId, photoFileId, "Фото от пользователя: " + userId);
                    }
                }

                requestUser.remove(chatId);
                break;


            case WHOLESALE_SALES_DEPARTMENT_Q1:
                requestUser.get(chatId).add(text);
                userStateService.save(chatId, UserState.CONNECT_OPERATOR);
                sendMessage = handleDefaultAnswer(chatId, TRANSFER_MESSAGE);
                sendResponseMessage(sendMessage);

                threadId = createUserThread(username);
                chatIdAndUserId.put(chatId, userId);
                connectToOperator(userId, threadId, username);

                //Long operatorId = userToOperator.get(userId);
                messageToOperator = requestUser.get(chatId);

                forwardToOperator(userId, threadId, listToString(messageToOperator), "");
                requestUser.remove(chatId);
                break;


            case OTHER_QUESTION_Q1:
                requestUser.get(chatId).add(text);
                userStateService.save(chatId, UserState.CONNECT_OPERATOR);
                sendMessage = handleDefaultAnswer(chatId, TRANSFER_MESSAGE);
                sendResponseMessage(sendMessage);

                threadId = createUserThread(username);
                chatIdAndUserId.put(chatId, userId);
                connectToOperator(userId, threadId, username);

                //Long operatorId = userToOperator.get(userId);
                messageToOperator = requestUser.get(chatId);

                forwardToOperator(userId, threadId, listToString(messageToOperator), "");
                requestUser.remove(chatId);
                break;


//            case TRANSFER_OPERATOR:
//                userStateService.save(chatId, UserState.CONNECT_OPERATOR);
//                sendMessage = handleDefaultAnswer(chatId, "оператор подклюачется");
//                sendResponseMessage(sendMessage);
//                break;


            case CONNECT_OPERATOR:
                userId = chatIdAndUserId.get(chatId);
                Integer threadIdOperator = userToThread.get(userId);
                forwardToOperator(userId, threadIdOperator, text, username);
                break;


            case COMPLETED:
                break;


            default:
                break;
        }
    }


    private void handleStart(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        switch (text) {
            case "Товар пришел с браком":
                requestUser.get(chatId).add(text);
                userStateService.save(chatId, UserState.DEFECTIVE_PRODUCT_Q1);
                sendMessage = handleDefectiveProductQ1(chatId);
                sendResponseMessage(sendMessage);
                break;
            case "Хочу докупить товар":
                requestUser.get(chatId).add(text);
                userStateService.save(chatId, UserState.BUY_PRODUCT_Q1);
                sendMessage = handleBuyProduct(chatId);
                sendResponseMessage(sendMessage);
                break;
            case "Связь с отделом оптовых продаж":
                requestUser.get(chatId).add(text);
                userStateService.save(chatId, UserState.WHOLESALE_SALES_DEPARTMENT_Q1);
                sendMessage = handleOtherQuestionAndWholeSaleDepartment(chatId);
                sendResponseMessage(sendMessage);
                break;
            case "Другой вопрос":
                requestUser.get(chatId).add(text);
                userStateService.save(chatId, UserState.OTHER_QUESTION_Q1);
                sendMessage = handleOtherQuestionAndWholeSaleDepartment(chatId);
                sendResponseMessage(sendMessage);
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

    private void connectToOperator(Long userId, Integer threadId, String username) {
        userToOperator.put(userId, OPERATORS_CHAT_ID);
        userToThread.put(userId, threadId);
        sendMessageToUser(userId, "Вы подключены к техподдержке. Оператор скоро ответит.");
        sendMessageToOperator(OPERATORS_CHAT_ID, threadId, "Пользователь @" + username + " запросил поддержку.");
    }


    private void forwardToUser(Long userId, String text) {
        sendMessageToUser(userId, "Сообщение от оператора: " + text);
    }

    private void forwardToOperator(Long userId, Integer threadId, String text, String s) {
        if (userToOperator.containsKey(userId)) {
            Long operatorId = userToOperator.get(userId);
            sendMessageToOperator(operatorId, threadId, "Сообщение от пользователя " + userId + ": " + "\n" + text);
        } else {
            sendMessageToUser(userId, "Вы не подключены к техподдержке. Нажмите \"Связать с техподдержкой\".");
        }
    }

    private Integer createUserThread(String username) {
        CreateForumTopic topic = CreateForumTopic.builder()
                .chatId(OPERATORS_CHAT_ID)
                .name("Чат с @" + username)
                .build();
        try {
            return execute(topic).getMessageThreadId();
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendMessageToUser(Long userId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(userId.toString()) // Отправляем в личный чат
                .text(text)
                .build();
        sendResponseMessage(message);
    }


    private void sendMessageToOperator(Long chatId, Integer threadId, String text) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text).build();

        if (threadId != null) {
            sendMessage.setMessageThreadId(threadId);
        }

        sendResponseMessage(sendMessage);
    }


    public void sendResponseMessage(SendMessage message) {
        if (message != null) {
            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error(e);
            }
        }
    }
}
