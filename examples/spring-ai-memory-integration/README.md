# Spring AI AgentCore Memory Example

A complete example demonstrating Amazon Bedrock AgentCore Short-Term & Long-Term Memory integration with Spring AI for persistent conversation history.

This example creates and uses the AgentCore Long-Term Memory Summary Strategy.

## Prerequisites

- Java 17+
- Maven 3.6+
- AWS credentials configured locally

## Architecture

```mermaid
sequenceDiagram
   autonumber
   participant CC as ChatClient (with Advisors)
   participant LMA as AgentCoreLongMemoryAdvisor
   participant SMA as MessageChatMemoryAdvisor
   participant Repos as AgentCoreShortMemoryRepository
   participant Retr as AgentCoreLongMemoryRetriever
   participant AWS as Bedrock AgentCore
   participant LLM as Model (e.g. Nova)

   rect rgb(240, 240, 255)
      Note right of LMA: LTM Advisor
      CC->>LMA: intercept chat message
      LMA->>Retr: search/listMemories
      Retr->>AWS: List/Search Events
      AWS-->>Retr: Memory Fragments (Semantic/Episodic/Summary/User Pref)
      Retr-->>LMA: List<MemoryRecord>
      LMA->>CC: Inject memories into System/User message
   end

   rect rgb(230, 255, 230)
      Note right of SMA: Short Memory Advisor
      CC->>SMA: intercept chat message
      SMA->>Repos: get memory history
      Repos->>AWS: listEvents
      AWS-->>Repos: AgentCore Events
      Repos-->>SMA: List<Message> (Linear History)
      SMA->>CC: Append history to messages
   end

   CC->>LLM: Augmented Prompt (History + LTM + Prompt)
   LLM-->>CC: Assistant Response

   rect rgb(230, 255, 230)
      Note right of SMA: Short Memory Save
      CC->>SMA: save message
      SMA->>Repos: save message
      Note right of Repos: Delta Detection
      Repos->>AWS: persist message
   end
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
    # --- Short-Term Memory (STM) ---
    # Tell your name
    curl -X POST http://localhost:8080/api/short \
        -H "Content-Type: application/json" \
        -d '{"message": "My name is Andrei"}'
    
    # Ask for your name (memory recall)
    curl -X POST http://localhost:8080/api/short \
        -H "Content-Type: application/json" \
        -d '{"message": "What is my name?"}'

    # --- Long-Term Memory (LTM) ---
    # Ask something to be persisted in LTM
    curl -X POST http://localhost:8080/api/long \
        -H "Content-Type: application/json" \
        -d '{"message": "I love hiking in the Alps"}'

    # Get conversation history
    curl http://localhost:8080/api/history
 
    # Get stored LTM memories
    curl http://localhost:8080/api/memories

    # Clear conversation
    curl -X DELETE http://localhost:8080/api/history
    ```

## Cleanup

With the `AGENTCORE_MEMORY_ID` env var set, run:
```bash
mvn spring-boot:test-run
```
