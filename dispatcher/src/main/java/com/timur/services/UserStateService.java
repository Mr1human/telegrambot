package com.timur.services;

import com.timur.enums.UserState;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserStateService {
    private final Map<Long, UserState> chatIdUserStateMap = new HashMap<>();

    public void save(Long chatId, UserState userState){
        chatIdUserStateMap.put(chatId, userState);
    }

    public UserState getUserState(Long chatId){
        return chatIdUserStateMap.get(chatId);
    }

    public void clearUserSession(Long chatId) {
        chatIdUserStateMap.remove(chatId);
    }
}
