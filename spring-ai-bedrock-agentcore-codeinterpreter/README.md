# Spring AI Bedrock AgentCore Code Interpreter

Spring AI integration with Amazon Bedrock AgentCore Code Interpreter. Execute Python, JavaScript, and TypeScript code in a secure sandbox with automatic file retrieval.

## Features

- Execute code in Python, JavaScript, or TypeScript
- Automatic file retrieval (charts, CSVs, PDFs)
- Session-scoped file storage for multi-user environments
- Configurable tool description for LLM
- TTL-based cache cleanup

## Quick Start

Add the dependency:

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-bedrock-agentcore-codeinterpreter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Inject the tool provider:

```java
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final CodeInterpreterFileStore fileStore;

    public ChatService(
            ChatClient.Builder chatClientBuilder,
            @Qualifier("codeInterpreterToolCallbackProvider") ToolCallbackProvider codeInterpreterTools,
            CodeInterpreterFileStore fileStore) {

        this.fileStore = fileStore;
        this.chatClient = chatClientBuilder
            .defaultToolCallbacks(codeInterpreterTools)
            .build();
    }

    public Flux<String> chat(String prompt, String sessionId) {
        return chatClient.prompt()
            .user(prompt)
            .toolContext(Map.of(CodeInterpreterFileStore.SESSION_ID_KEY, sessionId))
            .stream().content()
            .concatWith(Flux.defer(() -> appendGeneratedFiles(sessionId)));
    }

    private Flux<String> appendGeneratedFiles(String sessionId) {
        List<GeneratedFile> files = fileStore.retrieve(sessionId);
        if (files == null || files.isEmpty()) {
            return Flux.empty();
        }
        StringBuilder sb = new StringBuilder();
        for (GeneratedFile file : files) {
            if (file.isImage()) {
                sb.append("\n\n![").append(file.name()).append("](")
                  .append(file.toDataUrl()).append(")");
            } else {
                sb.append("\n\n[Download ").append(file.name()).append("](")
                  .append(file.toDataUrl()).append(")");
            }
        }
        return Flux.just(sb.toString());
    }
}
```

## Configuration

```properties
# All optional - defaults shown
agentcore.code-interpreter.session-timeout-seconds=900
agentcore.code-interpreter.async-timeout-seconds=300
agentcore.code-interpreter.file-store-ttl-seconds=300
agentcore.code-interpreter.code-interpreter-identifier=aws.codeinterpreter.v1
agentcore.code-interpreter.tool-description=Custom tool description...
```

## Integration Test

```bash
AGENTCORE_IT=true mvn verify -pl spring-ai-bedrock-agentcore-codeinterpreter
```

## License

Apache License 2.0
