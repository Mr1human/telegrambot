package com.timur.services;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RequestUserService {
    private final Map<Long, List<String>> requestUser = new HashMap<>();

    public String getListRequestToString(Long chatId){
        return listToString(requestUser.get(chatId));
    }

    public List<String> getListRequest(Long chatId){
        return List.copyOf(requestUser.get(chatId));
    }

    public void addRequest(Long chatId, String text){
        requestUser.computeIfAbsent(chatId, k -> new ArrayList<>()).add(text);
    }

    public void removeRequestUser(Long chatId){
        requestUser.remove(chatId);
    }

    private String listToString(List<String> list) {
        return list.stream()
                .filter(item -> !item.startsWith("photo:")) // Игнорируем фото
                .map(item -> "- " + item) // Добавляем "- " к каждому элементу
                .collect(Collectors.joining("\n")); // Объединяем с переходом на новую строку
    }
}
