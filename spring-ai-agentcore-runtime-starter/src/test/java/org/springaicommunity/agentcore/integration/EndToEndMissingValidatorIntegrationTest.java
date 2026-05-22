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

package org.springaicommunity.agentcore.integration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agentcore.annotation.AgentCoreInvocation;
import org.springaicommunity.agentcore.service.AgentCoreInvocationRegistrar;
import org.springaicommunity.agentcore.service.AgentCoreMethodScanner;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.validation.Validator;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that the registrar fails fast at startup when {@code @Valid} is used on the
 * {@code @AgentCoreInvocation} body parameter but no {@link Validator} bean is available.
 */
class EndToEndMissingValidatorIntegrationTest {

	@SuppressWarnings("unchecked")
	@Test
	void shouldFailFastWhenValidatorMissing() {
		AgentCoreMethodScanner scanner = new AgentCoreMethodScanner();
		scanner.postProcessAfterInitialization(new AgentWithValidation(), "agent");

		ObjectProvider<Validator> emptyProvider = mock(ObjectProvider.class);
		when(emptyProvider.getIfAvailable()).thenReturn(null);

		StaticApplicationContext context = new StaticApplicationContext();
		context.refresh();

		AgentCoreInvocationRegistrar registrar = new AgentCoreInvocationRegistrar(scanner, context, emptyProvider);

		assertThatThrownBy(registrar::afterSingletonsInstantiated).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("@Valid/@Validated")
			.hasMessageContaining("spring-boot-starter-validation");
	}

	static class AgentWithValidation {

		@AgentCoreInvocation
		public String handle(@Valid Request request) {
			return request.prompt();
		}

	}

	record Request(@NotBlank String prompt) {
	}

}
