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

/**
 * Strategy for post-processing the value returned by an {@code @AgentCoreInvocation}
 * method, immediately after the method assembles its result and while any
 * invocation-scoped thread-locals (e.g. the workload access token) are still populated.
 *
 * <p>
 * The default, MVC/blocking implementation is a pass through: the result is returned
 * unchanged and any thread-local state set by an {@link AgentCoreInvocationCallback}
 * remains valid because the method body executes on the same thread.
 *
 * <p>
 * When Reactor is on the classpath, a reactive implementation overrides the default and
 * pins the current {@code ContextSnapshot} onto the returned {@code Flux}/{@code Mono}.
 * This is required because the invocation's {@code finally} block clears the thread-local
 * before the publisher is subscribed (and possibly on a different thread), so the value
 * must be captured at assembly time and carried on the Reactor {@code Context} instead.
 *
 * @author Matej Nedic
 */
@FunctionalInterface
public interface AgentCoreInvocationResultHandler {

	/**
	 * Post-processes the value returned by the invocation method.
	 * @param result the raw value returned by the {@code @AgentCoreInvocation} method
	 * @return the (possibly wrapped) result to hand back to the controller
	 */
	Object handleResult(Object result);

}
