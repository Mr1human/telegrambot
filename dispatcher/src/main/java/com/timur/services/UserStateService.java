package com.timur.services;

import com.timur.enums.UserState;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class UserStateService {
    private final RedisTemplate<String, String> redisTemplate;

    private final String USER_STATE_KEY = "userState: ";

    public UserStateService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(Long chatId, UserState userState) {
        String key = USER_STATE_KEY + chatId;
        redisTemplate.opsForValue().set(key, userState.name(), 14, TimeUnit.DAYS);
    }

    public UserState getUserState(Long chatId) {
        String key = USER_STATE_KEY + chatId;
        String state = redisTemplate.opsForValue().get(key);
        return state != null ? UserState.valueOf(state) : null;
    }

    public boolean isUserInActiveState(Long chatId) {
        return getUserState(chatId) != null;
    }


    public void clearUserSession(Long chatId) {
        String key = USER_STATE_KEY + chatId;
        redisTemplate.opsForValue().getOperations().delete(key);
    }
}
