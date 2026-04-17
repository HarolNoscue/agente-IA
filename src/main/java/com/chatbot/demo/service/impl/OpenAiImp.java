package com.chatbot.demo.service.impl;

import com.chatbot.demo.service.OpenAIService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.text.Normalizer;
import java.util.*;

@Service
public class OpenAiImp implements OpenAIService {

    private final String API_KEY = System.getenv("OPENAI_API_KEY");

    private final List<Map<String, String>> conversation = new ArrayList<>();

    // 🔥más palabras clave
    private final Map<String, String> knowledge = Map.of(
            "crear caso", "Para crear un caso en Salesforce debes ir al módulo de Service Cloud, seleccionar 'Casos' y hacer clic en 'Nuevo'. Luego completas la información requerida.",
            "estado caso", "Puedes consultar el estado de un caso ingresando al módulo de casos y buscando por el ID del caso.",
            "cerrar caso", "Para cerrar un caso debes abrirlo y cambiar su estado a 'Cerrado', asegurándote de completar los campos obligatorios."
    );

    public OpenAiImp() {
        conversation.add(Map.of(
                "role", "system",
                "content", "Eres un asistente experto en Salesforce. Usa el contexto y la conversación previa. Responde claro, breve y natural. No respondas nada fuera de Salesforce."
        ));
    }

    @Override
    public String ask(String question) {

        question = normalize(question);

        String context = getContext(question);

        boolean hasMemory = conversation.size() > 1;

        // CONTEXTO DIRECTO
        if (!context.isEmpty()) {
            return callOpenAI("Contexto: " + context + "\nPregunta: " + question);
        }

        // MEMORIA (solo si sigue siendo Salesforce)
        if (hasMemory) {

            // combinar última interacción
            String lastContext = getLastAssistantMessage();

            String combined = lastContext + " " + question;

            if (isSalesforceRelated(combined)) {
                return callOpenAI(question);
            }
        }

        // SMALL TALK
        if (isSmallTalk(question)) {
            return getSmallTalkResponse(question);
        }

        // BLOQUEO
        return "Lo siento, solo puedo ayudarte con procesos de Salesforce como creación, consulta o cierre de casos.";
    }

     
    // OPENAI
     
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

     
    // CONTEXTO INTELIGENTE
     
    public String getContext(String question) {

        String normalizedQuestion = normalize(question);

        String bestKey = null;
        int bestScore = 0;

        for (String key : knowledge.keySet()) {

            String normalizedKey = normalize(key);
            String[] keyWords = normalizedKey.split(" ");

            int score = 0;

            for (String word : keyWords) {
                if (normalizedQuestion.contains(word)) {
                    score++;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestKey = key;
            }
        }

        // 🔥 mínimo 1 coincidencia
        if (bestScore >= 1) {
            return knowledge.get(bestKey);
        }

        return "";
    }

     
    // VALIDAR DOMINIO
     
    public boolean isSalesforceRelated(String question) {

        return question.contains("caso") ||
                question.contains("salesforce") ||
                question.contains("ticket") ||
                question.contains("cliente") ||
                question.contains("estado") ||
                question.contains("cerrar") ||
                question.contains("crear");
    }

     
    //  NORMALIZAR
     
    public String normalize(String text) {

        if (text == null) return "";

        text = text.trim().toLowerCase();
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        text = text.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        text = text.replaceAll("[^a-z0-9 ]", "");
        text = text.replaceAll("\\s+", " ");

        return text;
    }

     
    // SMALL TALK
     
    public boolean isSmallTalk(String question) {

        return question.equals("hola") ||
                question.equals("buenas") ||
                question.equals("gracias") ||
                question.equals("adios") ||
                question.equals("quien eres");
    }

    public String getSmallTalkResponse(String question) {

        if (question.equals("hola") || question.equals("buenas")) {
            return "Hola 👋 ¿En qué puedo ayudarte con Salesforce?";
        }

        if (question.equals("gracias")) {
            return "¡Con gusto! 😊";
        }

        if (question.equals("quien eres")) {
            return "Soy un asistente que te ayuda con procesos de Salesforce.";
        }

        if (question.equals("adios")) {
            return "¡Hasta luego! 👋";
        }

        return "Hola 👋";
    }

    private String getLastAssistantMessage() {

        for (int i = conversation.size() - 1; i >= 0; i--) {

            Map<String, String> msg = conversation.get(i);

            if ("assistant".equals(msg.get("role"))) {
                return normalize(msg.get("content"));
            }
        }

        return "";
    }
}