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

package org.springaicommunity.agentcore.service;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springaicommunity.agentcore.annotation.AgentCoreInvocation;
import org.springaicommunity.agentcore.exception.AgentCoreInvocationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentCoreMethodScannerTest {

	@Test
	void shouldDiscoverSingleAnnotatedMethod() throws Exception {
		AgentCoreMethodScanner scanner = new AgentCoreMethodScanner();
		SingleAgent bean = new SingleAgent();

		scanner.postProcessAfterInitialization(bean, "singleAgent");

		assertThat(scanner.getTarget()).hasValueSatisfying(target -> {
			assertThat(target.bean()).isSameAs(bean);
			assertThat(target.method().getName()).isEqualTo("invoke");
		});
	}

	@Test
	void shouldDiscoverInheritedAnnotatedMethod() throws Exception {
		AgentCoreMethodScanner scanner = new AgentCoreMethodScanner();
		ChildAgent bean = new ChildAgent();

		scanner.postProcessAfterInitialization(bean, "childAgent");

		assertThat(scanner.getTarget()).hasValueSatisfying(target -> {
			assertThat(target.bean()).isSameAs(bean);
			assertThat(target.method().getName()).isEqualTo("invoke");
		});
	}

	@Test
	void shouldThrowWhenMultipleMethodsAnnotatedInSameBean() {
		AgentCoreMethodScanner scanner = new AgentCoreMethodScanner();
		DuplicateAgent bean = new DuplicateAgent();

		assertThatThrownBy(() -> scanner.postProcessAfterInitialization(bean, "duplicateAgent"))
			.isInstanceOf(AgentCoreInvocationException.class)
			.hasMessageContaining("Multiple @AgentCoreInvocation methods found");
	}

	@Test
	void shouldThrowWhenMultipleBeansEachHaveOneAnnotatedMethod() throws Exception {
		AgentCoreMethodScanner scanner = new AgentCoreMethodScanner();
		scanner.postProcessAfterInitialization(new SingleAgent(), "first");

		assertThatThrownBy(() -> scanner.postProcessAfterInitialization(new SingleAgent(), "second"))
			.isInstanceOf(AgentCoreInvocationException.class)
			.hasMessageContaining("Multiple @AgentCoreInvocation methods found");
	}

	@Test
	void shouldReturnEmptyWhenNoBeanIsAnnotated() throws Exception {
		AgentCoreMethodScanner scanner = new AgentCoreMethodScanner();
		scanner.postProcessAfterInitialization(new Object(), "plain");

		assertThat(scanner.getTarget()).isEmpty();
	}

	@Test
	void recordedMethodBelongsToBeanHierarchy() throws Exception {
		AgentCoreMethodScanner scanner = new AgentCoreMethodScanner();
		SingleAgent bean = new SingleAgent();
		scanner.postProcessAfterInitialization(bean, "singleAgent");

		Method recorded = scanner.getTarget().orElseThrow().method();
		assertThat(recorded.getDeclaringClass()).isAssignableFrom(bean.getClass());
	}

	static class SingleAgent {

		@AgentCoreInvocation
		public String invoke(String prompt) {
			return "ok " + prompt;
		}

	}

	static class ParentAgent {

		@AgentCoreInvocation
		public String invoke(String prompt) {
			return "parent " + prompt;
		}

	}

	static class ChildAgent extends ParentAgent {

		// inherits @AgentCoreInvocation from ParentAgent

	}

	static class DuplicateAgent {

		@AgentCoreInvocation
		public String first(String s) {
			return s;
		}

		@AgentCoreInvocation
		public String second(String s) {
			return s;
		}

	}

}
