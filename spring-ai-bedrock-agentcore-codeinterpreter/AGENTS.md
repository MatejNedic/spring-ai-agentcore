# AGENTS.md

Context for AI coding assistants working on this module.

## Module Overview

Spring AI integration with Amazon Bedrock AgentCore Code Interpreter. Executes Python, JavaScript, and TypeScript code in a secure sandbox with automatic file retrieval.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     CodeInterpreterTools                        │
│                   (Tool implementation logic)                   │
├─────────────────────────────────────────────────────────────────┤
│  executeCode(language, code, toolContext) → String              │
│    - Validates language (python, javascript, typescript)        │
│    - Executes code via client                                   │
│    - Stores files in FileStore                                  │
│    - Returns text-only result to LLM                            │
└───────────────────────────┬─────────────────────────────────────┘
                            │ uses
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                  AgentCoreCodeInterpreterClient                 │
│                    (Low-level SDK wrapper)                      │
├─────────────────────────────────────────────────────────────────┤
│  startSession(name) → sessionId                                 │
│  executeCode(sessionId, language, code) → CodeExecutionResult   │
│  listFiles(sessionId, path) → List<String>                      │
│  readFiles(sessionId, paths) → List<GeneratedFile>              │
│  stopSession(sessionId)                                         │
│  executeInEphemeralSession(language, code) → CodeExecutionResult│
└─────────────────────────────────────────────────────────────────┘
```

## Key Classes

| Class | Purpose |
|-------|---------|
| `AgentCoreCodeInterpreterAutoConfiguration` | Spring Boot auto-config with `ToolCallbackProvider` |
| `AgentCoreCodeInterpreterClient` | Low-level SDK wrapper with configurable timeouts |
| `AgentCoreCodeInterpreterConfiguration` | Config properties (timeouts, TTL, identifier, description) |
| `CodeInterpreterTools` | Tool implementation logic (no annotations) |
| `CodeInterpreterFileStore` | Session-scoped file storage with Caffeine cache and TTL |
| `CodeExecutionResult` | Record for execution results with null-safe defaults |
| `GeneratedFile` | Record for file data with defensive copy and helper methods |
| `ExecuteCodeRequest` | Input schema record for the tool (language, code) |

## Design Decisions

1. **ToolCallbackProvider pattern** - Programmatic tool registration with configurable description
2. **File handling in ChatService** - Files appended after stream completes, outside memory flow
3. **Session-scoped file storage** - Uses `ToolContext` to pass session ID for multi-user support
4. **No advisor** - Avoids files being stored in conversation memory (context overflow)
5. **Null-safe records** - `CodeExecutionResult` and `GeneratedFile` use defensive copies
6. **TTL-based cleanup** - Caffeine cache with 5-minute TTL prevents memory leaks

## Request Flow

```
1. User: "Create a chart showing Q1 sales"
2. ChatService passes sessionId via toolContext
3. LLM calls executeCode tool
4. CodeInterpreterTools.executeCode():
   a. Extract sessionId from toolContext
   b. client.executeInEphemeralSession(language, code)
   c. fileStore.store(sessionId, files)
   d. Return text-only result to LLM
5. Memory stores: user message + LLM response (NO files)
6. ChatService.appendGeneratedFiles(sessionId)
7. User sees: LLM response + chart image
```

## Configuration

```properties
agentcore.code-interpreter.session-timeout-seconds=900
agentcore.code-interpreter.async-timeout-seconds=300
agentcore.code-interpreter.file-store-ttl-seconds=300
agentcore.code-interpreter.code-interpreter-identifier=aws.codeinterpreter.v1
agentcore.code-interpreter.tool-description=Custom tool description...
```

## Build & Test

```bash
# Compile
mvn compile -pl spring-ai-bedrock-agentcore-codeinterpreter

# Format (required before commit)
mvn spring-javaformat:apply -pl spring-ai-bedrock-agentcore-codeinterpreter

# Integration test (requires AWS credentials)
AGENTCORE_IT=true mvn verify -pl spring-ai-bedrock-agentcore-codeinterpreter
```

## Not Implemented

SDK capabilities not yet exposed:

| Feature | SDK Operation | Use Case |
|---------|---------------|----------|
| File upload | `writeFiles` | Upload user files for processing |
| File deletion | `deleteFiles` | Clean up session files |
| Persistent sessions | Session reuse | Multi-turn code execution with shared state |
| Session listing | `listCodeInterpreterSessions` | Manage active sessions |
