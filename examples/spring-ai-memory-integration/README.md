# Spring AI AgentCore Memory Example

A complete example demonstrating AWS Bedrock AgentCore Short-Term Memory integration with Spring AI for persistent conversation history.

## Prerequisites

- Java 17+
- Maven 3.6+
- AWS CLI configured with credentials
- Terraform 1.0+

## 🏗️ Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────────┐
│   REST Client   │───▶│  ChatController  │───▶│   Spring AI Chat   │
│   (curl/web)    │    │                  │    │      Client         │
└─────────────────┘    └──────────────────┘    └─────────────────────┘
                                ▲                          │
                                │                          ▼
                                │               ┌─────────────────────┐
                                │               │  Bedrock Converse   │
                                │               │       API           │
                                │               └─────────────────────┘
                                │
                       ┌──────────────────┐
                       │   ChatMemory     │
                       │  (Window-based)  │
                       └──────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │ AgentCore Memory │
                       │   Repository     │
                       └──────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │ AWS Bedrock      │
                       │ AgentCore Memory │
                       │ (Short-Term)     │
                       └──────────────────┘
```

## Quick Start

### Local Setup

1. Setup your local AWS credentials / auth

1. Create the AgentCore Memory:
    ```bash
    mvn spring-boot:test-run
    ```
1. Export the `AGENTCORE_MEMORY_ID` and `SUMMARY_STRATEGY_ID` env vars
1. Start the Spring web application
    ```bash
    mvn spring-boot:run
    ```
1. Test the application:
    ```bash
    # Tell your name
    curl -X POST http://localhost:8080/api/chat \
      -H "Content-Type: application/json" \
      -d '{"message": "My name is Andrei"}'
    
    # Ask for your name (memory recall)
    curl -X POST http://localhost:8080/api/chat \
      -H "Content-Type: application/json" \
      -d '{"message": "What is my name?"}'
    
    # Get conversation history
    curl http://localhost:8080/api/chat/history
    
    # Clear conversation
    curl -X DELETE http://localhost:8080/api/chat/history
    ```




### 1. Create Infrastructure

```bash
./deploy.sh
```

This creates an AgentCore short-term memory and waits for it to become ACTIVE (takes ~2-3 minutes).

### 2. Export Memory ID

```bash
export AGENTCORE_MEMORY_ID=$(cd terraform && terraform output -raw memory_id)
```

### 3. Run Application

```bash
mvn spring-boot:run
```

### 4. Test the API

```bash
# Tell your name
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "My name is Andrei"}'

# Ask for your name (memory recall)
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What is my name?"}'

# Get conversation history
curl http://localhost:8080/api/chat/history

# Clear conversation
curl -X DELETE http://localhost:8080/api/chat/history
```

## Cleanup

```bash
cd terraform
terraform destroy
```

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# AWS Bedrock Model
spring.ai.bedrock.converse.chat.options.model=global.amazon.nova-2-lite-v1:0

# AgentCore Memory
agentcore.memory.memory-id=${AGENTCORE_MEMORY_ID}
agentcore.memory.total-events-limit=100
agentcore.memory.page-size=50
agentcore.memory.ignore-unknown-roles=true
```

## How It Works

The application uses:
- **AgentCore Memory Repository** - Stores conversation history in AWS Bedrock AgentCore Memory
- **Spring AI ChatClient** - Handles chat interactions with Amazon Bedrock models
- **MessageChatMemoryAdvisor** - Automatically manages conversation context

Conversation history is persisted in AgentCore Memory with a unique conversation ID (`testActor/testSession`), allowing the AI to remember previous interactions.

## License

Apache License 2.0
