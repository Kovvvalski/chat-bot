package by.kovalski.chatbot.config;

import by.kovalski.chatbot.handler.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.netty.http.client.HttpClient;

import java.util.Map;

@Configuration
public class ApplicationConfig {
    @Value("${openrouter.ai.key}")
    private String apiKey;

    @Value("${openrouter.ai.baseurl}")
    private String baseUrl;


    @Bean
    public HandlerMapping handlerMapping(ChatWebSocketHandler chatWebSocketHandler) {
        Map<String, ChatWebSocketHandler> handlerMap = Map.of(
                "/ws/chat", chatWebSocketHandler
        );
        return new SimpleUrlHandlerMapping(handlerMap, 1);
    }

    @Bean
    public WebClient chatWebClient() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("HTTP-Referer", "http://localhost")
                .defaultHeader("User-Agent", "spring-webflux-client")
                .build();
    }


    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
