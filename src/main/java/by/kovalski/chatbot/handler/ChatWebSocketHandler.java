package by.kovalski.chatbot.handler;

import by.kovalski.chatbot.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final ChatService chatService;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return session.send(
                session.receive()
                        .map(msg -> msg.getPayloadAsText())
                        .flatMap(chatService::getChatResponse)
                        .map(session::textMessage)
        );
    }
}
