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

import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive {@link AgentCoreInvocationResultHandler} that captures the current
 * {@link ContextSnapshot} (including any registered thread-locals such as the workload
 * access token) at assembly time and pins it onto the returned {@code Flux}/{@code Mono}
 * via {@code contextWrite}.
 *
 * <p>
 * This is what makes a lazily evaluated streaming invocation see the invocation-scoped
 * thread-locals: the value is captured while it is still populated on the request thread,
 * carried on the immutable Reactor {@code Context}, and restored onto whatever scheduler
 * thread runs the operators (provided automatic context propagation is enabled).
 *
 * <p>
 * Only instantiated when Reactor and Micrometer context-propagation are on the classpath.
 *
 * @author Matej Nedic
 */
public class ReactiveContextPropagationResultHandler implements AgentCoreInvocationResultHandler {

	private static final ContextSnapshotFactory SNAPSHOT_FACTORY = ContextSnapshotFactory.builder().build();

	@Override
	public Object handleResult(Object result) {
		if (result instanceof Flux<?> flux) {
			ContextSnapshot snapshot = SNAPSHOT_FACTORY.captureAll();
			return flux.contextWrite(snapshot::updateContext);
		}
		if (result instanceof Mono<?> mono) {
			ContextSnapshot snapshot = SNAPSHOT_FACTORY.captureAll();
			return mono.contextWrite(snapshot::updateContext);
		}
		return result;
	}

}
