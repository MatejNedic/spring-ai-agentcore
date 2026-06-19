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

package org.springaicommunity.agentcore.identity.core;

import io.micrometer.context.ThreadLocalAccessor;

/**
 * Micrometer {@link ThreadLocalAccessor} that bridges the
 * {@link WorkloadAccessTokenHolder} thread-local with the Reactor {@code Context}.
 *
 * <p>
 * Registering this accessor with the global {@code ContextRegistry} lets Reactor capture
 * the workload access token into the {@code Context} at assembly time and restore it onto
 * whichever scheduler thread runs a reactive operator. This is what allows the blocking
 * {@link AgentCoreIdentityTemplate#getApiKey(String)} API to keep working when invoked
 * lazily inside a streaming {@code Flux}/{@code Mono}.
 *
 * @author Matej Nedic
 */
public class WorkloadAccessTokenAccessor implements ThreadLocalAccessor<String> {

	/**
	 * Stable key under which the workload access token is stored in the Reactor
	 * {@code Context}.
	 */
	public static final String KEY = "org.springaicommunity.agentcore.identity.workloadAccessToken";

	private final WorkloadAccessTokenHolder holder;

	public WorkloadAccessTokenAccessor(WorkloadAccessTokenHolder holder) {
		this.holder = holder;
	}

	@Override
	public Object key() {
		return KEY;
	}

	@Override
	public String getValue() {
		return this.holder.get();
	}

	@Override
	public void setValue(String value) {
		this.holder.set(value);
	}

	@Override
	public void setValue() {
		this.holder.clear();
	}

	@Override
	public void restore(String previousValue) {
		if (previousValue == null) {
			this.holder.clear();
		}
		else {
			this.holder.set(previousValue);
		}
	}

}
