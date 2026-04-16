package com.chatbot.demo.controller;

import com.chatbot.demo.dto.ChatRequest;
import com.chatbot.demo.service.OpenAIService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final OpenAIService openAIService;

    public ChatController(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }

    @PostMapping
    public String chat(@Valid @RequestBody ChatRequest request) {
        return openAIService.ask(request.getQuestion());
    }
}
