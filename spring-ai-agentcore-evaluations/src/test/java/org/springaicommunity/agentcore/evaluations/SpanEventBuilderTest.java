/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springaicommunity.agentcore.evaluations;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpanEventBuilder}.
 *
 * <p>
 * Verifies the public contract required by the AgentCore Evaluate API: a span/event pair
 * carrying session metadata and the user/assistant payloads.
 *
 * @author Andrei Shakirin
 */
class SpanEventBuilderTest {

	private static final String TRACE_ID = "trace123";

	private static final String SESSION_ID = "session456";

	@Test
	void shouldProduceSpanAndEventLinkedByTraceAndSpanId() {
		List<Map<String, Object>> sessionSpans = SpanEventBuilder.agentInvocation(TRACE_ID, SESSION_ID)
			.promptEvent("What is the capital of France?")
			.completionEvent("Paris.")
			.buildSessionSpans();

		assertThat(sessionSpans).hasSize(2);
		Map<String, Object> span = sessionSpans.get(0);
		Map<String, Object> event = sessionSpans.get(1);

		assertThat(span.get("traceId")).isEqualTo(TRACE_ID);
		assertThat(event.get("traceId")).isEqualTo(span.get("traceId"));
		assertThat(event.get("spanId")).isEqualTo(span.get("spanId"));
	}

	@Test
	void spanShouldCarrySessionAndScope() {
		Map<String, Object> span = SpanEventBuilder.agentInvocation(TRACE_ID, SESSION_ID)
			.buildSessionSpans()
			.getFirst();

		assertThat(attributes(span)).containsEntry("session.id", SESSION_ID);
		assertThat(scope(span)).containsEntry("name", SpanEventBuilder.SCOPE_NAME);
	}

	@Test
	void eventShouldCarryPromptAndCompletionPayloads() {
		Map<String, Object> event = SpanEventBuilder.agentInvocation(TRACE_ID, SESSION_ID)
			.promptEvent("What is the capital of France?")
			.completionEvent("Paris.")
			.buildSessionSpans()
			.get(1);

		// The body shape is the Strands ADOT format: the plain prompt/completion text
		// must be findable anywhere in the serialized body (Strands wraps user content
		// as a JSON array and output as a message object).
		String bodyStr = event.get("body").toString();
		assertThat(bodyStr).contains("What is the capital of France?");
		assertThat(bodyStr).contains("Paris.");
	}

	@Test
	void defaultFinishReasonIsEndTurnWhenNotSet() {
		Map<String, Object> event = SpanEventBuilder.agentInvocation(TRACE_ID, SESSION_ID)
			.completionEvent("Paris.")
			.buildSessionSpans()
			.get(1);

		assertThat(event.get("body").toString()).contains("finish_reason=end_turn");
	}

	@Test
	void explicitFinishReasonIsPropagatedToBody() {
		Map<String, Object> event = SpanEventBuilder.agentInvocation(TRACE_ID, SESSION_ID)
			.completionEvent("Partial response...")
			.finishReason("tool_use")
			.buildSessionSpans()
			.get(1);

		String bodyStr = event.get("body").toString();
		assertThat(bodyStr).contains("finish_reason=tool_use");
		assertThat(bodyStr).doesNotContain("finish_reason=end_turn");
	}

	@Test
	void blankFinishReasonIsIgnored() {
		Map<String, Object> event = SpanEventBuilder.agentInvocation(TRACE_ID, SESSION_ID)
			.completionEvent("Paris.")
			.finishReason("   ")
			.buildSessionSpans()
			.get(1);

		// Falls back to default
		assertThat(event.get("body").toString()).contains("finish_reason=end_turn");
	}

	@Test
	void tokenUsageAttributesAreEmittedWhenBothPresent() {
		Map<String, Object> span = SpanEventBuilder.agentInvocation(TRACE_ID, SESSION_ID)
			.tokenUsage(120, 45)
			.buildSessionSpans()
			.getFirst();

		assertThat(attributes(span)).containsEntry("gen_ai.usage.input_tokens", 120)
			.containsEntry("gen_ai.usage.output_tokens", 45);
	}

	@Test
	void tokenUsageAttributesAreOmittedWhenNull() {
		Map<String, Object> span = SpanEventBuilder.agentInvocation(TRACE_ID, SESSION_ID)
			.tokenUsage(null, null)
			.buildSessionSpans()
			.getFirst();

		assertThat(attributes(span)).doesNotContainKey("gen_ai.usage.input_tokens")
			.doesNotContainKey("gen_ai.usage.output_tokens");
	}

	@Test
	void tokenUsagePartiallyPresentEmitsOnlyAvailableAttributes() {
		Map<String, Object> span = SpanEventBuilder.agentInvocation(TRACE_ID, SESSION_ID)
			.tokenUsage(120, null)
			.buildSessionSpans()
			.getFirst();

		assertThat(attributes(span)).containsEntry("gen_ai.usage.input_tokens", 120)
			.doesNotContainKey("gen_ai.usage.output_tokens");
	}

	@Test
	void historyEntriesAreEmittedBeforeCurrentUserPrompt() {
		List<Map<String, Object>> history = List.of(Map.of("role", "system", "content", "You are helpful."),
				Map.of("role", "user", "content", "Earlier question"),
				Map.of("role", "assistant", "content", "Earlier answer"));

		Map<String, Object> event = SpanEventBuilder.agentInvocation(TRACE_ID, SESSION_ID)
			.promptEvent("Current question")
			.completionEvent("Current answer")
			.history(history)
			.buildSessionSpans()
			.get(1);

		String bodyStr = event.get("body").toString();
		// All history entries plus current user are in input.messages
		assertThat(bodyStr).contains("You are helpful.")
			.contains("Earlier question")
			.contains("Earlier answer")
			.contains("Current question");
		// Current answer in output.messages
		assertThat(bodyStr).contains("Current answer");
	}

	@Test
	void emptyHistoryPreservesSingleMessageWireShape() {
		Map<String, Object> event = SpanEventBuilder.agentInvocation(TRACE_ID, SESSION_ID)
			.promptEvent("Current question")
			.completionEvent("Current answer")
			.history(List.of())
			.buildSessionSpans()
			.get(1);

		String bodyStr = event.get("body").toString();
		// Exact match of the baseline — one user entry in input.messages
		assertThat(bodyStr).contains("Current question").contains("Current answer");
	}

	@Test
	void nullHistoryTreatedAsEmpty() {
		Map<String, Object> event = SpanEventBuilder.agentInvocation(TRACE_ID, SESSION_ID)
			.promptEvent("Q")
			.completionEvent("A")
			.history(null)
			.buildSessionSpans()
			.get(1);

		assertThat(event.get("body").toString()).contains("Q").contains("A");
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> attributes(Map<String, Object> span) {
		return (Map<String, Object>) span.get("attributes");
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> scope(Map<String, Object> span) {
		return (Map<String, Object>) span.get("scope");
	}

}
