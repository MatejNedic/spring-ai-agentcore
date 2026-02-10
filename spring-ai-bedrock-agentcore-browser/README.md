# Spring AI Bedrock AgentCore Browser

Spring AI integration with Amazon Bedrock AgentCore Browser. Headless browser automation for web page navigation, content extraction, screenshots, and page interaction using Playwright over CDP.

Supports two modes:
- **agentcore** (default) — uses AgentCore Browser managed service
- **local** — uses a locally launched Chromium browser for development and testing

## Features

- Browse web pages and extract text content
- Take screenshots with session-scoped storage
- Click elements, fill forms, execute JavaScript
- Configurable tool descriptions for LLM
- TTL-based cache cleanup

## Quick Start

Add the dependency:

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-bedrock-agentcore-browser</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Inject the tool provider:

```java
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final BrowserScreenshotStore screenshotStore;

    public ChatService(
            ChatClient.Builder chatClientBuilder,
            @Qualifier("browserToolCallbackProvider") ToolCallbackProvider browserTools,
            BrowserScreenshotStore screenshotStore) {

        this.screenshotStore = screenshotStore;
        this.chatClient = chatClientBuilder
            .defaultToolCallbacks(browserTools)
            .build();
    }

    public Flux<String> chat(String prompt, String sessionId) {
        return chatClient.prompt()
            .user(prompt)
            .stream()
            .content()
            .concatWith(Flux.defer(() -> appendScreenshots(sessionId)))
            // Store sessionId in Reactor context for tools
            .contextWrite(ctx -> ctx.put(BrowserTools.SESSION_ID_CONTEXT_KEY, sessionId));
    }

    private Flux<String> appendScreenshots(String sessionId) {
        List<BrowserScreenshot> screenshots = screenshotStore.retrieve(sessionId);
        if (screenshots == null || screenshots.isEmpty()) {
            return Flux.empty();
        }
        StringBuilder sb = new StringBuilder();
        for (BrowserScreenshot s : screenshots) {
            sb.append("\n\n![Screenshot of ").append(s.url()).append("](")
              .append(s.toDataUrl()).append(")");
        }
        return Flux.just(sb.toString());
    }
}
```

## Session ID Handling

Session ID is passed via Reactor context and read using Spring AI's `ToolCallReactiveContextHolder`. This is required because tool execution happens on `Schedulers.boundedElastic()` thread pool, not the HTTP request thread.

```java
// BrowserTools reads session ID from Reactor context
public class BrowserTools {
    public static final String SESSION_ID_CONTEXT_KEY = "sessionId";

    public String takeScreenshot(String url) {
        ContextView ctx = ToolCallReactiveContextHolder.getContext();
        String sessionId = ctx.getOrDefault(SESSION_ID_CONTEXT_KEY, DEFAULT_SESSION_ID);
        // ... store screenshot by sessionId
    }
}
```

## Tools

| Tool | Parameters | Returns |
|------|------------|---------|
| `browseUrl` | url | Page title + text content |
| `takeScreenshot` | url | Metadata (bytes stored in cache) |
| `clickElement` | url, selector | Result message |
| `fillForm` | url, selector, value | Result message |
| `evaluateScript` | url, script | Script result |

## Configuration

```properties
# All optional - defaults shown
agentcore.browser.mode=agentcore          # or "local" for local Chromium
agentcore.browser.session-timeout-seconds=900
agentcore.browser.browser-identifier=aws.browser.v1
agentcore.browser.viewport-width=1456
agentcore.browser.viewport-height=819
agentcore.browser.max-content-length=10000
agentcore.browser.screenshot-ttl-seconds=300

# Custom tool descriptions (optional)
agentcore.browser.browse-url-description=...
agentcore.browser.screenshot-description=...
agentcore.browser.click-description=...
agentcore.browser.fill-description=...
agentcore.browser.evaluate-description=...
```

## Local Development

For local development without AWS credentials, use local mode:

```properties
agentcore.browser.mode=local
```

This launches a headless Chromium browser locally via Playwright. Same tools, same behavior — just no AgentCore service dependency. Useful for development and testing.

## Example Application

See [`examples/spring-ai-browser`](../examples/spring-ai-browser) for a minimal standalone app that browses a URL, extracts content, takes a screenshot, and saves it to a local folder. Defaults to local mode.

## Integration Test

```bash
# AgentCore mode (requires AWS credentials)
AGENTCORE_IT=true mvn verify -pl spring-ai-bedrock-agentcore-browser

# Local mode (no AWS credentials needed)
mvn test -pl spring-ai-bedrock-agentcore-browser
```

## License

Apache License 2.0
