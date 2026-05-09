package org.springaicommunity.agentcore.memory.longterm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springaicommunity.agentcore.memory.longterm.AgentCoreLongTermMemoryRetriever.MemoryRecord;
import org.springaicommunity.agentcore.memory.longterm.strategy.EpisodicMemoryStrategyHandler;
import org.springaicommunity.agentcore.memory.longterm.strategy.SemanticMemoryStrategyHandler;
import org.springaicommunity.agentcore.memory.longterm.strategy.SummaryMemoryStrategyHandler;
import org.springaicommunity.agentcore.memory.longterm.strategy.UserPreferenceMemoryStrategyHandler;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AgentCoreLongTermMemoryAdvisor}. Focused on the advisor's
 * orchestration with each built-in {@code MemoryStrategyHandler}: that the right handler
 * is called, memories are fetched with the expected parameters, and the enriched prompt
 * is injected into the correct message slot.
 */
@ExtendWith(MockitoExtension.class)
class AgentCoreLongTermMemoryAdvisorTest {

	private static final String ACTOR_NS = AgentCoreLongTermMemoryNamespace.ACTOR.getPattern();

	private static final String SESSION_NS = AgentCoreLongTermMemoryNamespace.SESSION.getPattern();

	@Mock
	private AgentCoreLongTermMemoryRetriever retriever;

	@Mock
	private CallAdvisorChain chain;

	private AgentCoreLongTermMemoryAdvisor semanticAdvisor;

	private AgentCoreLongTermMemoryAdvisor userPreferenceAdvisor;

	@BeforeEach
	void setUp() {
		this.semanticAdvisor = semanticAdvisor("strategy-123", ACTOR_NS, "Known facts", 100);
		this.userPreferenceAdvisor = userPreferenceAdvisor("strategy-456", ACTOR_NS, "User preferences", 101);
	}

	// ------------------------------------------------------------------
	// Basic orchestration
	// ------------------------------------------------------------------

	@Test
	void throwsWhenNoConversationId() {
		ChatClientRequest request = requestWith("Hello", Map.of());

		assertThatThrownBy(() -> semanticAdvisor.adviseCall(request, chain)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining(ChatMemory.CONVERSATION_ID);
	}

	@Test
	void passesRequestThroughUnchangedWhenNoMemoriesFound() {
		when(retriever.searchMemories(anyString(), anyString(), anyString(), anyString(), anyInt(), anyString()))
			.thenReturn(List.of());

		ChatClientRequest request = requestWith("Hello", Map.of(ChatMemory.CONVERSATION_ID, "user-456"));

		semanticAdvisor.adviseCall(request, chain);

		verify(chain).nextCall(request);
	}

	@Test
	void exposesNameAndOrderPerStrategy() {
		assertThat(semanticAdvisor.getName()).isEqualTo("AgentCoreLongTermMemoryAdvisor-SEMANTIC");
		assertThat(semanticAdvisor.getOrder()).isEqualTo(100);

		assertThat(userPreferenceAdvisor.getName()).isEqualTo("AgentCoreLongTermMemoryAdvisor-USER_PREFERENCE");
		assertThat(userPreferenceAdvisor.getOrder()).isEqualTo(101);
	}

	// ------------------------------------------------------------------
	// Per-strategy retrieval wiring
	// ------------------------------------------------------------------

	@Test
	void semanticSearchesMemoriesWithConfiguredParameters() {
		when(retriever.searchMemories(eq("strategy-123"), eq("user-456"), anyString(), eq("What do I like?"), eq(3),
				eq(ACTOR_NS)))
			.thenReturn(List.of(new MemoryRecord("1", "User likes coffee", 0.9),
					new MemoryRecord("2", "User is from Seattle", 0.85)));

		semanticAdvisor.adviseCall(requestWith("What do I like?", Map.of(ChatMemory.CONVERSATION_ID, "user-456")),
				chain);

		verify(retriever).searchMemories(eq("strategy-123"), eq("user-456"), anyString(), eq("What do I like?"), eq(3),
				eq(ACTOR_NS));
		SystemMessage enriched = (SystemMessage) captureEnrichedPrompt().getInstructions().get(0);
		assertThat(enriched.getText()).contains("Known facts", "User likes coffee", "User is from Seattle");
	}

	@Test
	void userPreferenceListsMemoriesWithoutQuery() {
		when(retriever.listMemories("strategy-456", "user-456", ACTOR_NS)).thenReturn(
				List.of(new MemoryRecord("1", "Dark mode enabled", 0.0), new MemoryRecord("2", "Metric units", 0.0)));

		userPreferenceAdvisor
			.adviseCall(requestWith("Show settings", Map.of(ChatMemory.CONVERSATION_ID, "user-456:session-1")), chain);

		verify(retriever).listMemories("strategy-456", "user-456", ACTOR_NS);
		SystemMessage enriched = (SystemMessage) captureEnrichedPrompt().getInstructions().get(0);
		assertThat(enriched.getText()).contains("User preferences", "Dark mode enabled");
	}

	@Test
	void summaryUsesSessionScopedNamespace() {
		AgentCoreLongTermMemoryAdvisor advisor = summaryAdvisor("sum-1", SESSION_NS, "Prior session summary");
		when(retriever.searchMemories(eq("sum-1"), eq("user-1"), eq("session-1"), eq("Continue"), anyInt(),
				eq(SESSION_NS)))
			.thenReturn(List.of(new MemoryRecord("1", "We discussed quantum physics.", 0.9)));

		advisor.adviseCall(requestWith("Continue", Map.of(ChatMemory.CONVERSATION_ID, "user-1:session-1")), chain);

		verify(retriever).searchMemories(eq("sum-1"), eq("user-1"), eq("session-1"), eq("Continue"), anyInt(),
				eq(SESSION_NS));
	}

	// ------------------------------------------------------------------
	// Injection target — SYSTEM vs USER
	// ------------------------------------------------------------------

	@Test
	void semanticMergesIntoExistingSystemMessage() {
		when(retriever.searchMemories(eq("strategy-123"), eq("user-1"), anyString(), eq("Q"), anyInt(), eq(ACTOR_NS)))
			.thenReturn(List.of(new MemoryRecord("1", "User likes coffee.", 0.9)));

		ChatClientRequest request = ChatClientRequest.builder()
			.prompt(new Prompt(List.of(new SystemMessage("Be concise."), new UserMessage("Q"))))
			.context(Map.of(ChatMemory.CONVERSATION_ID, "user-1"))
			.build();

		semanticAdvisor.adviseCall(request, chain);

		List<?> messages = captureEnrichedPrompt().getInstructions();
		assertThat(messages).hasSize(2);
		assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
		assertThat(((SystemMessage) messages.get(0)).getText()).contains("Be concise.", "User likes coffee.");
	}

	@Test
	void summaryReplacesUserMessageRatherThanAddingSystemMessage() {
		AgentCoreLongTermMemoryAdvisor advisor = summaryAdvisor("sum-1", SESSION_NS, "Prior session summary");
		when(retriever.searchMemories(eq("sum-1"), eq("user-1"), eq("session-1"), eq("Continue"), anyInt(),
				eq(SESSION_NS)))
			.thenReturn(List.of(new MemoryRecord("1", "We discussed quantum physics.", 0.9)));

		advisor.adviseCall(requestWith("Continue", Map.of(ChatMemory.CONVERSATION_ID, "user-1:session-1")), chain);

		List<?> messages = captureEnrichedPrompt().getInstructions();
		assertThat(messages).hasSize(1);
		assertThat(messages.get(0)).isInstanceOf(UserMessage.class);
		assertThat(((UserMessage) messages.get(0)).getText()).contains("We discussed quantum physics.", "Continue");
	}

	// ------------------------------------------------------------------
	// Empty-prompt short-circuit (handler.requiresUserPrompt)
	// ------------------------------------------------------------------

	@Test
	void semanticShortCircuitsWhenUserPromptIsEmpty() {
		ChatClientRequest request = requestWith("", Map.of(ChatMemory.CONVERSATION_ID, "user-1"));

		semanticAdvisor.adviseCall(request, chain);

		verifyNoInteractions(retriever);
		verify(chain).nextCall(request);
	}

	@Test
	void userPreferenceProceedsEvenWhenUserPromptIsEmpty() {
		when(retriever.listMemories("strategy-456", "user-1", ACTOR_NS))
			.thenReturn(List.of(new MemoryRecord("1", "Dark mode", 0.0)));

		userPreferenceAdvisor.adviseCall(requestWith("", Map.of(ChatMemory.CONVERSATION_ID, "user-1")), chain);

		verify(retriever).listMemories("strategy-456", "user-1", ACTOR_NS);
	}

	// ------------------------------------------------------------------
	// Episodic — modern namespace-based path and legacy strategy-based path
	// ------------------------------------------------------------------

	@Test
	void episodicModernPathQueriesSameStrategyWithSeparateNamespaces() {
		String reflectionsNs = "/strategy/{memoryStrategyId}/";
		AgentCoreLongTermMemoryAdvisor advisor = episodicAdvisor(EpisodicMemoryStrategyHandler.builder()
			.strategyId("ep-123")
			.namespacePattern(ACTOR_NS)
			.reflectionsNamespacePattern(reflectionsNs)
			.episodesTopK(3)
			.reflectionsTopK(2));

		when(retriever.searchMemories(eq("ep-123"), eq("user-456"), anyString(), eq("How's the weather?"), eq(3),
				eq(ACTOR_NS)))
			.thenReturn(List.of(new MemoryRecord("1", "User asked about weather yesterday", 0.9)));
		when(retriever.searchMemories(eq("ep-123"), eq("user-456"), anyString(), eq("How's the weather?"), eq(2),
				eq(reflectionsNs)))
			.thenReturn(List.of(new MemoryRecord("2", "User prefers detailed answers", 0.85)));

		advisor.adviseCall(requestWith("How's the weather?", Map.of(ChatMemory.CONVERSATION_ID, "user-456")), chain);

		SystemMessage enriched = (SystemMessage) captureEnrichedPrompt().getInstructions().get(0);
		assertThat(enriched.getText()).contains("Relevant past interactions", "User asked about weather yesterday",
				"Lessons learned", "User prefers detailed answers");
	}

	@Test
	void episodicLegacyPathQueriesTwoDifferentStrategies() {
		AgentCoreLongTermMemoryAdvisor advisor = episodicAdvisor(EpisodicMemoryStrategyHandler.builder()
			.strategyId("episodes-strategy")
			.namespacePattern(ACTOR_NS)
			.reflectionsStrategyId("reflections-strategy")
			.episodesTopK(3)
			.reflectionsTopK(2));

		when(retriever.searchMemories(eq("episodes-strategy"), eq("user-456"), anyString(), anyString(), eq(3),
				eq(ACTOR_NS)))
			.thenReturn(List.of(new MemoryRecord("1", "Previous interaction", 0.9)));
		when(retriever.searchMemories(eq("reflections-strategy"), eq("user-456"), anyString(), anyString(), eq(2),
				eq(ACTOR_NS)))
			.thenReturn(List.of(new MemoryRecord("2", "User prefers detailed answers", 0.85)));

		advisor.adviseCall(requestWith("Hello", Map.of(ChatMemory.CONVERSATION_ID, "user-456")), chain);

		verify(retriever).searchMemories(eq("episodes-strategy"), eq("user-456"), anyString(), anyString(), eq(3),
				eq(ACTOR_NS));
		verify(retriever).searchMemories(eq("reflections-strategy"), eq("user-456"), anyString(), anyString(), eq(2),
				eq(ACTOR_NS));
	}

	@Test
	void episodicModernPathTakesPrecedenceOverLegacyReflectionsStrategyId() {
		String reflectionsNs = "/strategy/{memoryStrategyId}/";
		AgentCoreLongTermMemoryAdvisor advisor = episodicAdvisor(EpisodicMemoryStrategyHandler.builder()
			.strategyId("ep-123")
			.namespacePattern(ACTOR_NS)
			.reflectionsStrategyId("legacy-reflections-strategy")
			.reflectionsNamespacePattern(reflectionsNs)
			.episodesTopK(3)
			.reflectionsTopK(2));

		when(retriever.searchMemories(eq("ep-123"), eq("user-456"), anyString(), eq("Q"), eq(3), eq(ACTOR_NS)))
			.thenReturn(List.of(new MemoryRecord("1", "episode", 0.9)));
		when(retriever.searchMemories(eq("ep-123"), eq("user-456"), anyString(), eq("Q"), eq(2), eq(reflectionsNs)))
			.thenReturn(List.of(new MemoryRecord("2", "reflection", 0.85)));

		advisor.adviseCall(requestWith("Q", Map.of(ChatMemory.CONVERSATION_ID, "user-456")), chain);

		verify(retriever, never()).searchMemories(eq("legacy-reflections-strategy"), anyString(), anyString(),
				anyString(), anyInt(), anyString());
	}

	@Test
	void episodicEmitsEpisodesOnlyWhenNoReflectionsConfigured() {
		AgentCoreLongTermMemoryAdvisor advisor = episodicAdvisor(EpisodicMemoryStrategyHandler.builder()
			.strategyId("episodes-strategy")
			.namespacePattern(ACTOR_NS)
			.episodesTopK(3)
			.reflectionsTopK(2));

		when(retriever.searchMemories(eq("episodes-strategy"), eq("user-456"), anyString(), eq("Hello"), eq(3),
				eq(ACTOR_NS)))
			.thenReturn(List.of(new MemoryRecord("1", "Previous interaction", 0.9)));

		advisor.adviseCall(requestWith("Hello", Map.of(ChatMemory.CONVERSATION_ID, "user-456")), chain);

		SystemMessage enriched = (SystemMessage) captureEnrichedPrompt().getInstructions().get(0);
		assertThat(enriched.getText()).contains("Relevant past interactions").doesNotContain("Lessons learned");
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	private AgentCoreLongTermMemoryAdvisor semanticAdvisor(String strategyId, String namespace, String contextLabel,
			int order) {
		return AgentCoreLongTermMemoryAdvisor.builder(retriever)
			.memoryStrategy(AgentCoreLongTermMemoryStrategyType.SEMANTIC)
			.order(order)
			.handler(SemanticMemoryStrategyHandler.builder()
				.strategyId(strategyId)
				.namespacePattern(namespace)
				.topK(3)
				.contextLabel(contextLabel)
				.build())
			.build();
	}

	private AgentCoreLongTermMemoryAdvisor userPreferenceAdvisor(String strategyId, String namespace,
			String contextLabel, int order) {
		return AgentCoreLongTermMemoryAdvisor.builder(retriever)
			.memoryStrategy(AgentCoreLongTermMemoryStrategyType.USER_PREFERENCE)
			.order(order)
			.handler(UserPreferenceMemoryStrategyHandler.builder()
				.strategyId(strategyId)
				.namespacePattern(namespace)
				.contextLabel(contextLabel)
				.build())
			.build();
	}

	private AgentCoreLongTermMemoryAdvisor summaryAdvisor(String strategyId, String namespace, String contextLabel) {
		return AgentCoreLongTermMemoryAdvisor.builder(retriever)
			.memoryStrategy(AgentCoreLongTermMemoryStrategyType.SUMMARY)
			.handler(SummaryMemoryStrategyHandler.builder()
				.strategyId(strategyId)
				.namespacePattern(namespace)
				.topK(3)
				.contextLabel(contextLabel)
				.build())
			.build();
	}

	private AgentCoreLongTermMemoryAdvisor episodicAdvisor(EpisodicMemoryStrategyHandler.Builder handlerBuilder) {
		return AgentCoreLongTermMemoryAdvisor.builder(retriever)
			.memoryStrategy(AgentCoreLongTermMemoryStrategyType.EPISODIC)
			.handler(handlerBuilder.build())
			.build();
	}

	private ChatClientRequest requestWith(String userMessage, Map<String, Object> context) {
		return ChatClientRequest.builder()
			.prompt(new Prompt(List.of(new UserMessage(userMessage))))
			.context(context)
			.build();
	}

	private Prompt captureEnrichedPrompt() {
		ArgumentCaptor<ChatClientRequest> captor = ArgumentCaptor.forClass(ChatClientRequest.class);
		verify(chain).nextCall(captor.capture());
		return captor.getValue().prompt();
	}

}
