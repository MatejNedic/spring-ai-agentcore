# Spring AI Browser Example

Demonstrates browser automation with Spring AI ChatClient and session-scoped artifact storage.

Uses an LLM to browse a URL, take a screenshot, and describe the page content. Screenshots are stored in the artifact store and saved to disk.

Defaults to **local mode** (headless Chromium via Playwright). Switch to AgentCore Browser by changing one property.

## Prerequisites

- Java 17+
- Maven
- AWS credentials configured (for Bedrock LLM and optionally AgentCore Browser)
- Playwright browsers installed (local mode only):
  ```bash
  mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
  ```

## Running

```bash
# Build the project first (from project root)
mvn clean install -DskipTests

# Run the example
cd examples/spring-ai-browser
mvn spring-boot:run
```

Output goes to `screenshots/screenshot.png`.

## Configuration

```properties
# Local browser mode (default) — uses Playwright
agentcore.browser.mode=local

# Remote mode — uses AgentCore Browser service
agentcore.browser.mode=agentcore

# Bedrock model (default: Claude Sonnet 4.5)
spring.ai.bedrock.converse.chat.options.model=global.anthropic.claude-sonnet-4-5-20250929-v1:0
```

Override the target URL:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--app.url=https://github.com"
```

## How It Works

1. ChatClient sends prompt to LLM with browser tools available
2. LLM decides to use `browseUrl` and `takeScreenshot` tools
3. `BrowserTools` captures screenshot and stores in `ArtifactStore<GeneratedFile>`
4. Application retrieves screenshots from artifact store and saves to disk
