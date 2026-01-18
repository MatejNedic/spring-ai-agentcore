package org.springaicommunity.example;

import org.springaicommunity.agentcore.memory.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ChatController {

	private final ChatClient shortTermChatClient;
	private final ChatClient longTermChatClient;
	private final ChatMemory chatMemory;
	private final AgentCoreLongMemoryRetriever retriever;
	private final AgentCoreLongMemoryProperties config;

	private static final String CONVERSATION_ID = "testActor:testSession";

	public ChatController(
            ChatClient.Builder chatClientBuilder,
            AgentCoreMemory agentCoreMemory, ChatMemory chatMemory,
			AgentCoreLongMemoryRetriever retriever, AgentCoreLongMemoryProperties config,
			AgentCoreShortMemoryRepository memoryRepository) {
        this.chatMemory = chatMemory;
		this.retriever = retriever;
		this.config = config;

        this.shortTermChatClient = chatClientBuilder
			.defaultAdvisors(agentCoreMemory.shortMemoryAdvisor)
			.build();

		this.longTermChatClient = chatClientBuilder
				.defaultAdvisors(agentCoreMemory.advisors)
				.build();

		// NOTE! The memory events are removed on startup
		memoryRepository.deleteByConversationId(CONVERSATION_ID);
	}

	@PostMapping("/api/short")
	public ChatResponse shortChat(@RequestBody ChatRequest request) {
		String response = shortTermChatClient.prompt()
			.user(request.message())
			.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, CONVERSATION_ID))
			.call()
			.content();

		return new ChatResponse(response);
	}

	@PostMapping("/api/long")
	public ChatResponse longChat(@RequestBody ChatRequest request) {
		String response = longTermChatClient.prompt()
				.user(request.message())
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, CONVERSATION_ID))
				.call()
				.content();

		return new ChatResponse(response);
	}

	@GetMapping("/api/history")
	public List<Message> getHistory() {
		return chatMemory.get(CONVERSATION_ID);
	}

	@DeleteMapping("/api/history")
	public void clearHistory() {
		chatMemory.clear(CONVERSATION_ID);
	}

	@GetMapping("/api/memories")
	public List<AgentCoreLongMemoryRetriever.MemoryRecord> getMemories() {
		return retriever.listMemories(config.summary().strategyId(), "testActor");
	}

	public record ChatRequest(String message) {}
	public record ChatResponse(String response) {}

}
