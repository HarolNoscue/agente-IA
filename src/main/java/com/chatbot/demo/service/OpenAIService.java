package com.chatbot.demo.service;

import org.springframework.stereotype.Service;

@Service
public interface OpenAIService {
    String ask(String question);
}