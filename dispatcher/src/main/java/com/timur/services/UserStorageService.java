package com.timur.services;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


import java.util.concurrent.TimeUnit;

@Service
public class UserStorageService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String USER_TO_OPERATOR_KEY = "userToOperator ";
    private static final String USER_TO_THREAD_KEY = "userToThread ";
    private static final String THREAD_TO_USER_KEY = "threadToUser ";
    private static final String CHAT_ID_TO_USER_ID_KEY = "chatIdToUserId ";

    public UserStorageService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    // Методы для работы с userToOperator
    public void addUserToOperator(Long userId, Long operatorChatId) {
        String key = USER_TO_OPERATOR_KEY + userId;
        redisTemplate.opsForValue().set(key, operatorChatId.toString(), 14, TimeUnit.DAYS);
    }

    public Long getOperatorByUserId(Long userId) {
        String key = USER_TO_OPERATOR_KEY + userId;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.valueOf(value) : null;
    }

    public boolean existsOperatorByUserId(Long userId) {
        return getOperatorByUserId(userId) != null;
    }

    public void removeUserToOperator(Long userId) {
        String key = USER_TO_OPERATOR_KEY + userId;
        redisTemplate.opsForValue().getOperations().delete(key);
    }

    // Методы для работы с userToThread
    public void addUserToThread(Long userId, Integer threadId) {
        String key = USER_TO_THREAD_KEY + userId;
        redisTemplate.opsForValue().set(key, threadId.toString(), 14, TimeUnit.DAYS);
    }

    public Integer getThreadByUserId(Long userId) {
        String key = USER_TO_THREAD_KEY + userId;
        String threadId = redisTemplate.opsForValue().get(key);
        return threadId != null ? Integer.valueOf(threadId) : null;
    }

    public void deleteUserToThread(Long userId) {
        String key = USER_TO_THREAD_KEY + userId;
        redisTemplate.opsForValue().getOperations().delete(key);
    }

    public void removeUserToThread(Long userId) {
        String key = USER_TO_THREAD_KEY + userId;
        redisTemplate.opsForValue().getOperations().delete(key);
    }

    public boolean existThreadByUserId(Long userId) {
        return getThreadByUserId(userId) != null;
    }

    // Методы для работы с threadToUser
    public void addThreadToUser(Integer threadId, Long userId) {
        String key = THREAD_TO_USER_KEY + threadId;
        redisTemplate.opsForValue().set(key, userId.toString(), 14, TimeUnit.DAYS);
    }

    public Long getUserIdByThread(Integer threadId) {
        String key = THREAD_TO_USER_KEY + threadId;
        String userId = redisTemplate.opsForValue().get(key);
        return userId != null ? Long.valueOf(userId) : null;
    }

    public void removeThreadToUser(Integer threadId) {
        String key = THREAD_TO_USER_KEY + threadId;
        redisTemplate.opsForValue().getOperations().delete(key);
    }

    // Методы для работы с chatIdAndUserId
    public void addChatIdToUserId(Long chatId, Long userId) {
        String key = CHAT_ID_TO_USER_ID_KEY + chatId;
        redisTemplate.opsForValue().set(key, userId.toString(), 14, TimeUnit.DAYS);
    }

    public Long getUserIdByChatId(Long chatId) {
        String key = CHAT_ID_TO_USER_ID_KEY + chatId;
        String userId = redisTemplate.opsForValue().get(key);
        return userId != null ? Long.valueOf(userId) : null;
    }

    public void removeChatIdToUserId(Long chatId) {
        String key = CHAT_ID_TO_USER_ID_KEY + chatId;
        redisTemplate.opsForValue().getOperations().delete(key);
    }
}
