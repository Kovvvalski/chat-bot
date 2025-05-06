package by.kovalski.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final WebClient webClient;

    @Value("${openrouter.ai.url}")
    private String openrouterUrl;

    public Mono<String> getChatResponse(String userMessage) {
        Map<String, Object> payload = Map.of(
                "model", "openai/gpt-3.5-turbo",
                "messages", List.of(
                        Map.of("role", "user", "content", userMessage)
                )
        );

        return webClient.post()
                .uri(openrouterUrl)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .map(json -> {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) json.get("choices");
                    if (choices == null || choices.isEmpty()) return "Empty response";
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return message != null ? message.get("content").toString() : "No content";
                });
    }
}
