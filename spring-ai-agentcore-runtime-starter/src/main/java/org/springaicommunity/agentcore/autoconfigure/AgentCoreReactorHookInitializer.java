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

import reactor.core.publisher.Hooks;

import org.springframework.beans.factory.InitializingBean;

/**
 * Enables Reactor's automatic context propagation so thread-locals registered with the
 * global {@code ContextRegistry} (such as the workload access token) are restored onto
 * the scheduler threads that run reactive operators.
 *
 * <p>
 * {@link Hooks#enableAutomaticContextPropagation()} is a JVM-wide, idempotent hook. It is
 * isolated in this dedicated bean (rather than inlined into the auto-configuration) so
 * the {@code reactor.core.publisher.Hooks} reference is only loaded when Reactor is
 * present, and so it can be disabled via configuration.
 *
 * @author Matej Nedic
 */
public class AgentCoreReactorHookInitializer implements InitializingBean {

	@Override
	public void afterPropertiesSet() {
		Hooks.enableAutomaticContextPropagation();
	}

}
