# Spring AI Override Invocations

This example demonstrates how to override the auto-configured `POST /invocations`
endpoint from `spring-ai-agentcore-runtime-starter` while still benefiting from the
ping endpoint and other starter features.

## Features

- **Custom Controller Override**: Provides a hand-written `@PostMapping("/invocations")`
  in place of the auto-configured handler.
- **Ping Endpoint Reuse**: Leverages the built-in `/ping` endpoint for health monitoring.
- **Spring AI Integration**: Direct integration with `ChatClient` for AI responses.

## How It Works

The runtime starter inspects Spring MVC's `RequestMappingHandlerMapping` at startup. If
it finds an existing `POST /invocations` mapping, it skips registering the
`@AgentCoreInvocation` handler — your controller wins automatically.

```java
@RestController
public class CustomController {

    @PostMapping(value = "/invocations", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> handleJsonInvocation(@RequestBody String request, @RequestHeader HttpHeaders headers) {
        return chatClient.prompt().user(request).stream().content();
    }
}
```

No marker interface, no `@ConditionalOnMissingBean` — just declare the mapping.

## API Endpoints

- **Custom invocations**: `POST /invocations` — your custom AI processing with
  streaming response.
- **Health monitoring**: `GET /ping` — built-in health check from the starter.

## Benefits

- **Zero configuration**: Add `@RestController` + `@PostMapping("/invocations")` and you're done.
- **No inheritance or interface constraints**: Complete freedom in method signatures and mappings.
- **Selective feature usage**: Keep using `/ping`, throttling, and other starter features.
- **Full control**: Complete control over request/response handling.
- **Direct Spring AI access**: Direct `ChatClient` integration without going through the starter's annotation.

## Requirements

- Java 21+
- Spring Boot 3.x
- Spring AI
- Amazon Bedrock access
