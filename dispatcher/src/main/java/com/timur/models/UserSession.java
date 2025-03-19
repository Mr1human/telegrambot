package com.timur.models;

import com.timur.enums.UserState;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter @Setter
public class UserSession {
    private UserState state;
    private Map<String, String> answers = new HashMap<>();

    public void saveAnswer(String question, String answer){
        answers.put(question, answer);
    }

}
