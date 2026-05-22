package com.unicorn.agents;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Custom controller that overrides the AgentCore-provided {@code POST /invocations}
 * endpoint.
 *
 * <p>The runtime starter detects an existing {@code POST /invocations} mapping at
 * application startup and skips registering its own {@code @AgentCoreInvocation}
 * handler, so this controller takes precedence automatically — no marker interface
 * or extra configuration required.
 */
@RestController
public class CustomAgentCoreInvocationsController {

    private final ChatClient chatClient;

    public CustomAgentCoreInvocationsController(ChatClient.Builder chatClient) {
        this.chatClient = chatClient
                .defaultTools(new DateTimeTools())
                .build();
    }

    @PostMapping(value = "/invocations", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> handleJsonInvocation(@RequestBody String request, @RequestHeader HttpHeaders headers) {
        return chatClient.prompt().user(request).stream().content();
    }
}
