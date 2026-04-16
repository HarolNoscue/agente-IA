package com.chatbot.demo.service.impl;

import com.chatbot.demo.service.OpenAIService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiImp implements OpenAIService {

    private final String API_KEY = System.getenv("OPENAI_API_KEY");
    private final List<Map<String, String>> conversation = new ArrayList<>();


    private final Map<String, String> knowledge = Map.of(
            "crear caso", "Para crear un caso en Salesforce debes ir al módulo de Service Cloud, seleccionar 'Casos' y hacer clic en 'Nuevo'. Luego completas la información requerida.",
            "estado caso", "Puedes consultar el estado de un caso ingresando al módulo de casos y buscando por el ID del caso.",
            "cerrar caso", "Para cerrar un caso debes abrirlo y cambiar su estado a 'Cerrado', asegurándote de completar los campos obligatorios."
    );


    public OpenAiImp() {
        conversation.add(Map.of(
                "role", "system",
                "content", "Eres un asistente experto en Salesforce. Usa el contexto proporcionado para responder de forma clara, natural y explicativa. No copies el contexto literalmente, sino úsalo para construir una mejor respuesta. Si no hay contexto suficiente, di que no tienes información."
        ));
    }


    @Override
    public String ask(String question) {
        String context = getContext(question);

        String userMessage;

        if (!context.isEmpty()) {
            userMessage = "Contexto: " + context + "\nPregunta: " + question;
        } else {
            userMessage = question;
        }

        conversation.add(Map.of(
                "role", "user",
                "content", userMessage
        ));

        ObjectMapper mapper = new ObjectMapper();

        String messagesJson;
        try {
            messagesJson = mapper.writeValueAsString(conversation);
        } catch (Exception e) {
            return "Error creando conversación";
        }

        String body = """
    {
      "model": "gpt-5.4-mini",
      "messages": %s
    }
    """.formatted(messagesJson);

        String jsonResponse = WebClient.builder()
                .baseUrl("https://api.openai.com/v1/chat/completions")
                .defaultHeader("Authorization", "Bearer " + API_KEY)
                .defaultHeader("Content-Type", "application/json")
                .build()
                .post()
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            JsonNode root = mapper.readTree(jsonResponse);

            String response = root
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            conversation.add(Map.of("role", "assistant", "content", response));

            return response;

        } catch (Exception e) {
            return "Error procesando respuesta";
        }
    }


    public String getContext(String question) {

        String normalizedQuestion = normalize(question);

        for (String key : knowledge.keySet()) {

            String normalizedKey = normalize(key);
            String[] keyWords = normalizedKey.split(" ");

            int matchCount = 0;

            for (String word : keyWords) {
                if (normalizedQuestion.contains(word)) {
                    matchCount++;
                }
            }

            if (matchCount == keyWords.length) {
                return knowledge.get(key);
            }
        }

        return "";
    }

    public String normalize(String text) {
        text = text.toLowerCase();
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        text = text.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        text = text.replaceAll("[^a-z0-9 ]", "");
        return text;
    }
}

