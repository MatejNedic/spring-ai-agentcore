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
import org.springaicommunity.agentcore.service.AgentCoreInvocationResultHandler;
import org.springaicommunity.agentcore.service.ReactiveContextPropagationResultHandler;
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that activates reactive context propagation for streaming
 * {@code @AgentCoreInvocation} methods. Only applies when both Reactor and Micrometer
 * context-propagation are on the classpath.
 *
 * <p>
 * It overrides the default passthrough {@link AgentCoreInvocationResultHandler} with one
 * that pins the captured {@code ContextSnapshot} onto the returned {@code Flux}/{@code
 * Mono}, and enables Reactor's automatic context propagation so invocation-scoped
 * thread-locals are restored on operator threads. Ordered before
 * {@link AgentCoreAutoConfiguration} so its handler wins over the default.
 *
 * @author Matej Nedic
 */
@AutoConfiguration(before = AgentCoreAutoConfiguration.class)
@ConditionalOnClass({ Flux.class, ContextSnapshot.class })
public class AgentCoreReactiveAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public AgentCoreInvocationResultHandler agentCoreInvocationResultHandler() {
		return new ReactiveContextPropagationResultHandler();
	}

	@Bean
	@ConditionalOnProperty(prefix = "agentcore.reactor", name = "context-propagation", havingValue = "true",
			matchIfMissing = true)
	public AgentCoreReactorHookInitializer agentCoreReactorHookInitializer() {
		return new AgentCoreReactorHookInitializer();
	}

}
