package by.kovalski.chatbot.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.tokenize.SimpleTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private static final double MIN_TRANSPORT_TOKENS = 0.05;

    private final WebClient webClient;

    @Value("${openrouter.ai.url}")
    private String openrouterUrl;

    @Value("${openrouter.ai.baseprompt}")
    private String basePrompt;

    @Value("${service.chat.themetokensfile}")
    private String themeTokensFilename;

    private Set<String> domainTokens;

    @PostConstruct
    @SneakyThrows
    private void loadThemeTokens() {
        var inputStream = getClass().getClassLoader().getResourceAsStream(themeTokensFilename);
        if (inputStream == null) {
            log.error("Theme tokens file not found in resources.");
            domainTokens = Set.of();
            return;
        }
        List<String> lines = new ArrayList<>();
        try (Scanner scanner = new Scanner(inputStream)) {
            while (scanner.hasNextLine()) {
                lines.add(scanner.nextLine().trim().toLowerCase());
            }
        }
        domainTokens = new HashSet<>(lines);
    }

    public Mono<String> getChatResponse(String userMessage) {
        if (!isDomainRelated(userMessage)) {
            return Mono.just("Your message doesn't appear to relate to configured subject domain, try another prompt.");
        }

        Map<String, Object> payload = Map.of(
                "model", "openai/gpt-3.5-turbo",
                "messages", List.of(
                        Map.of("role", "system", "content", basePrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );
        System.out.println(Thread.currentThread().getName());
        return webClient.post()
                .uri(openrouterUrl)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .map(json -> {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) json.get("choices");
                    if (choices == null || choices.isEmpty()) return "Empty response";
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = message != null ? message.get("content").toString() : "No content";

                    if (!isDomainRelated(content)) {
                        return "Sorry, the AI's response was not sufficiently related to your subject domain.";
                    }
                    System.out.println(Thread.currentThread().getName());
                    return content;
                });

    }

    private boolean isDomainRelated(String text) {
        SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
        String[] tokens = tokenizer.tokenize(text.toLowerCase());

        if (tokens.length == 0) return false;

        long domainTokenCount = Arrays.stream(tokens)
                .filter(domainTokens::contains)
                .count();

        double ratio = (double) domainTokenCount / tokens.length;

        return ratio >= MIN_TRANSPORT_TOKENS;
    }

}
