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
                "content", "Eres un asistente experto en Salesforce. Usa el contexto proporcionado y la conversación previa para responder claro, breve y natural. Si la pregunta depende de algo anterior, debes tenerlo en cuenta. No respondas nada fuera de Salesforce."
        ));
    }

    @Override
    public String ask(String question) {

        //  Normalizar
        question = normalize(question);

        // Buscar contexto
        String context = getContext(question);

        // Detectar si ya hay conversación previa
        boolean hasMemory = conversation.size() > 1;

        String userMessage;


        // HAY CONTEXTO

        if (!context.isEmpty()) {

            userMessage = "Contexto: " + context + "\nPregunta: " + question;
            return callOpenAI(userMessage);
        }


        //  HAY MEMORIA (pregunta encadenada)

        if (hasMemory) {
            return callOpenAI(question);
        }


        // SMALL TALK

        if (isSmallTalkOnly(question)) {
            return getSmallTalkResponse(question);
        }


       // FUERA DE DOMINIO

        return "Lo siento, solo puedo ayudarte con procesos de Salesforce como creación, consulta o cierre de casos.";
    }


    //  LLAMADA A OPENAI

    private String callOpenAI(String userMessage) {

        conversation.add(Map.of("role", "user", "content", userMessage));

        ObjectMapper mapper = new ObjectMapper();

        try {
            String messagesJson = mapper.writeValueAsString(conversation);

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


    // CONTEXTO

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


    //  NORMALIZAR TEXTO

    public String normalize(String text) {
        if (text == null) return "";

        text = text.trim();
        text = text.toLowerCase();
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        text = text.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        text = text.replaceAll("[^a-z0-9 ]", "");
        text = text.replaceAll("\\s+", " ");

        return text;
    }


    //  SMALL TALK DETECCIÓN

    public boolean isSmallTalkOnly(String question) {

        return question.equals("hola") ||
                question.equals("buenas") ||
                question.equals("gracias") ||
                question.equals("adios") ||
                question.equals("quien eres") ||
                question.equals("que eres");
    }


    //  SMALL TALK RESPUESTAS
  
    public String getSmallTalkResponse(String question) {

        if (question.equals("hola") || question.equals("buenas")) {
            return "Hola 👋 Soy un asistente que te ayuda con procesos de Salesforce. ¿En qué puedo ayudarte?";
        }

        if (question.equals("gracias")) {
            return "¡Con gusto! 😊";
        }

        if (question.equals("quien eres") || question.equals("que eres")) {
            return "Soy un asistente enfocado en ayudarte con procesos de Salesforce.";
        }

        if (question.equals("adios")) {
            return "¡Hasta luego! 👋";
        }

        return "Hola 👋 ¿En qué puedo ayudarte?";
    }
}