package com.timur.services;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


import java.util.concurrent.TimeUnit;

@Service
public class UserStorageService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String USER_CHAT_TO_OPERATOR_KEY = "userChatToOperator ";
    private static final String USER_CHAT_TO_THREAD_KEY = "userChatToThread ";
    private static final String THREAD_TO_USER_CHAT_KEY = "threadToUser ";

    public UserStorageService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    // Методы для работы с userToOperator
    public void addUserChatToOperator(Long userChatId, Long operatorChatId) {
        String key = USER_CHAT_TO_OPERATOR_KEY + userChatId;
        redisTemplate.opsForValue().set(key, operatorChatId.toString(), 14, TimeUnit.DAYS);
    }

    public Long getOperatorByUserChatId(Long userChatId) {
        String key = USER_CHAT_TO_OPERATOR_KEY + userChatId;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.valueOf(value) : null;
    }

    public boolean existsOperatorByUserChatId(Long userChatId) {
        return getOperatorByUserChatId(userChatId) != null;
    }

    public void removeUserChatToOperator(Long userChatId) {
        String key = USER_CHAT_TO_OPERATOR_KEY + userChatId;
        redisTemplate.opsForValue().getOperations().delete(key);
    }

    // Методы для работы с userToThread
    public void addUserChatToThread(Long userChatId, Integer threadId) {
        String key = USER_CHAT_TO_THREAD_KEY + userChatId;
        redisTemplate.opsForValue().set(key, threadId.toString(), 14, TimeUnit.DAYS);
    }

    public Integer getThreadByUserChatId(Long userChatId) {
        String key = USER_CHAT_TO_THREAD_KEY + userChatId;
        String threadId = redisTemplate.opsForValue().get(key);
        return threadId != null ? Integer.valueOf(threadId) : null;
    }

    public void removeUserChatToThread(Long userChatId) {
        String key = USER_CHAT_TO_THREAD_KEY + userChatId;
        redisTemplate.opsForValue().getOperations().delete(key);
    }

    public boolean existThreadByUserChatId(Long userChatId) {
        return getThreadByUserChatId(userChatId) != null;
    }

    // Методы для работы с threadToUserChat
    public void addThreadToUserChat(Integer threadId, Long userChatId) {
        String key = THREAD_TO_USER_CHAT_KEY + threadId;
        redisTemplate.opsForValue().set(key, userChatId.toString(), 14, TimeUnit.DAYS);
    }

    public Long getUserChatByThread(Integer threadId) {
        String key = THREAD_TO_USER_CHAT_KEY + threadId;
        String userChatId = redisTemplate.opsForValue().get(key);
        return userChatId != null ? Long.valueOf(userChatId) : null;
    }

    public void removeThreadToUserChat(Integer threadId) {
        String key = THREAD_TO_USER_CHAT_KEY + threadId;
        redisTemplate.opsForValue().getOperations().delete(key);
    }
}
