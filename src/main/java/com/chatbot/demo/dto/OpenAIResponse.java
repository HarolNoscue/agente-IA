package com.chatbot.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class OpenAIResponse {
    public List<Choice> choices;
}
