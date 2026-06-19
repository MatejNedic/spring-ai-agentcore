/*
 * Copyright 2025-2026 the original author or authors.
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

package org.springaicommunity.agentcore.service;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ThreadLocalAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agentcore.annotation.AgentCoreInvocation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Verifies that an invocation-scoped thread-local set by an
 * {@link AgentCoreInvocationCallback} is propagated into a streaming {@code Flux} result
 * and restored on the scheduler thread that runs the operators, even after the
 * thread-local has been cleared on the request thread.
 *
 * @author Matej Nedic
 */
class ReactiveContextPropagationResultHandlerTests {

	private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();

	private static final String KEY = "test.workloadToken";

	private static final String HEADER = "X-Test-Token";

	private final ContextRegistry registry = ContextRegistry.getInstance();

	private final TestTokenAccessor accessor = new TestTokenAccessor();

	@BeforeEach
	void setUp() {
		this.registry.registerThreadLocalAccessor(this.accessor);
		Hooks.enableAutomaticContextPropagation();
	}

	@AfterEach
	void tearDown() {
		this.registry.removeThreadLocalAccessor(KEY);
		Hooks.disableAutomaticContextPropagation();
		TOKEN.remove();
	}

	@Test
	void tokenPropagatesIntoStreamingResultAcrossThreads() throws Exception {
		var registry = mock(AgentCoreMethodRegistry.class);
		var bean = new StreamingBean();
		var method = StreamingBean.class.getDeclaredMethod("stream", String.class);
		given(registry.hasAgentMethod()).willReturn(true);
		given(registry.getAgentMethod()).willReturn(method);
		given(registry.getAgentBean()).willReturn(bean);

		// Callback mirrors WorkloadAccessTokenCallback: sets the token from a header
		// before invocation and clears it afterwards (in a finally block).
		var callbacks = new AgentCoreInvocationCallbackRegistry(java.util.List.of(new TokenCallback()));
		var invoker = new AgentCoreMethodInvoker(new com.fasterxml.jackson.databind.ObjectMapper(), registry, callbacks,
				new ReactiveContextPropagationResultHandler());

		var headers = new HttpHeaders();
		headers.add(HEADER, "secret-token");

		Object result = invoker.invokeAgentMethod("prompt", headers);

		// After invokeAgentMethod returns, the request-thread thread-local has already
		// been cleared by the callback's afterInvocation.
		assertThat(TOKEN.get()).isNull();
		assertThat(result).isInstanceOf(Flux.class);

		@SuppressWarnings("unchecked")
		Flux<String> flux = (Flux<String>) result;
		StepVerifier.create(flux).expectNext("secret-token").verifyComplete();
	}

	static class StreamingBean {

		@AgentCoreInvocation
		Flux<String> stream(String prompt) {
			// Reads the thread-local lazily, on a different scheduler thread.
			return Flux.defer(() -> Flux.just(prompt)).publishOn(Schedulers.boundedElastic()).map((p) -> {
				String token = TOKEN.get();
				return (token != null) ? token : "MISSING";
			});
		}

	}

	static class TokenCallback implements AgentCoreInvocationCallback {

		@Override
		public void beforeInvocation(Object request, HttpHeaders headers) {
			TOKEN.set(headers.getFirst(HEADER));
		}

		@Override
		public void afterInvocation(Object request, HttpHeaders headers) {
			TOKEN.remove();
		}

	}

	static class TestTokenAccessor implements ThreadLocalAccessor<String> {

		@Override
		public Object key() {
			return KEY;
		}

		@Override
		public String getValue() {
			return TOKEN.get();
		}

		@Override
		public void setValue(String value) {
			TOKEN.set(value);
		}

		@Override
		public void setValue() {
			TOKEN.remove();
		}

	}

}
