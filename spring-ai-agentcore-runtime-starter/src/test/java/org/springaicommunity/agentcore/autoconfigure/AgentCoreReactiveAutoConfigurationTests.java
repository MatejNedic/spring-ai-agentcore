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

package org.springaicommunity.agentcore.autoconfigure;

import io.micrometer.context.ContextSnapshot;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agentcore.service.AgentCoreInvocationResultHandler;
import org.springaicommunity.agentcore.service.ReactiveContextPropagationResultHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the conditional wiring of {@link AgentCoreReactiveAutoConfiguration}: the
 * reactive {@link AgentCoreInvocationResultHandler} overrides the default passthrough
 * when Reactor and context-propagation are present, and the default applies otherwise.
 *
 * @author Matej Nedic
 */
class AgentCoreReactiveAutoConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(AgentCoreReactiveAutoConfiguration.class, AgentCoreAutoConfiguration.class));

	@AfterAll
	static void disableGlobalHook() {
		// The reactive auto-configuration enables a JVM-wide Reactor hook; reset it so it
		// does not leak into unrelated tests.
		Hooks.disableAutomaticContextPropagation();
	}

	@Test
	void reactiveHandlerOverridesDefaultWhenReactorPresent() {
		this.runner.run((context) -> {
			assertThat(context).hasSingleBean(AgentCoreInvocationResultHandler.class);
			assertThat(context.getBean(AgentCoreInvocationResultHandler.class))
				.isInstanceOf(ReactiveContextPropagationResultHandler.class);
			assertThat(context).hasSingleBean(AgentCoreReactorHookInitializer.class);
		});
	}

	@Test
	void hookInitializerCanBeDisabledByProperty() {
		this.runner.withPropertyValues("agentcore.reactor.context-propagation=false").run((context) -> {
			assertThat(context.getBean(AgentCoreInvocationResultHandler.class))
				.isInstanceOf(ReactiveContextPropagationResultHandler.class);
			assertThat(context).doesNotHaveBean(AgentCoreReactorHookInitializer.class);
		});
	}

	@Test
	void fallsBackToPassthroughWhenReactorAbsent() {
		this.runner.withClassLoader(new FilteredClassLoader(Flux.class, ContextSnapshot.class)).run((context) -> {
			assertThat(context).hasSingleBean(AgentCoreInvocationResultHandler.class);
			assertThat(context.getBean(AgentCoreInvocationResultHandler.class))
				.isNotInstanceOf(ReactiveContextPropagationResultHandler.class);
			assertThat(context).doesNotHaveBean(AgentCoreReactorHookInitializer.class);
		});
	}

}
