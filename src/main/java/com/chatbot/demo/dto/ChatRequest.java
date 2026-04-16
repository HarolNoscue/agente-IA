package com.chatbot.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChatRequest {

    @NotBlank(message = "La pregunta no puede estar vacía")
    private String question;

}
