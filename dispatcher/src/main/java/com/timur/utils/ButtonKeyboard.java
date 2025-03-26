package com.timur.utils;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;


@Component
public class ButtonKeyboard {

    public ReplyKeyboardMarkup marketplaceButton() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
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

    public ReplyKeyboardRemove keyboardRemove() {
        ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
        keyboardRemove.setRemoveKeyboard(true);
        return keyboardRemove;
    }
}
