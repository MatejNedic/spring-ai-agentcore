# Spring AI Browser Example

Minimal example that navigates to a URL, extracts page content, takes a screenshot, and saves it to a local folder.

Defaults to **local mode** (headless Chromium via Playwright). Switch to AgentCore Browser by changing one property.

## Prerequisites

- Java 17+
- Maven
- Playwright browsers installed: `mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"` (local mode only)
- AWS credentials configured (agentcore mode only)

## Running

```bash
# Build the browser module first (from project root)
mvn clean install -pl spring-ai-bedrock-agentcore-browser

# Run the example
cd examples/spring-ai-browser
mvn spring-boot:run
```

Output goes to `screenshots/screenshot.png`.

## Configuration

```properties
# Local mode (default) — no AWS credentials needed
agentcore.browser.mode=local

# Remote mode — uses AgentCore Browser service
agentcore.browser.mode=agentcore
```

Override the target URL:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--app.url=https://github.com"
```
