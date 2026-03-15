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

import java.lang.reflect.InvocationTargetException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springaicommunity.agentcore.context.AgentCoreContext;
import org.springaicommunity.agentcore.exception.AgentCoreInvocationException;
import org.springaicommunity.agentcore.identity.AgentCorePrincipal;

import org.springaicommunity.agentcore.identity.providers.AgentCorePrincipalProvider;
import org.springframework.http.HttpHeaders;

public class AgentCoreMethodInvoker {

	private final ObjectMapper objectMapper;

	private final AgentCoreMethodRegistry registry;

	private final AgentCorePrincipalProvider principalProvider;

	public AgentCoreMethodInvoker(ObjectMapper objectMapper, AgentCoreMethodRegistry registry,
			AgentCorePrincipalProvider principalProvider) {
		this.objectMapper = objectMapper;
		this.registry = registry;
		this.principalProvider = principalProvider;
	}

	public Object invokeAgentMethod(Object request, HttpHeaders headers) throws Exception {
		if (!registry.hasAgentMethod()) {
			throw new AgentCoreInvocationException("No @AgentCoreInvocation method found");
		}

		var method = registry.getAgentMethod();
		var bean = registry.getAgentBean();
		var paramTypes = method.getParameterTypes();

		Object[] args = prepareArguments(request, headers, paramTypes);

		try {
			return method.invoke(bean, args);
		}

		catch (InvocationTargetException e) {
			if (e.getCause() instanceof Exception exception) {
				throw exception;
			}
			throw new AgentCoreInvocationException("Method invocation failed", e);
		}
	}

	public Object invokeAgentMethod(Object request) throws Exception {
		return invokeAgentMethod(request, new HttpHeaders());
	}

	private Object[] prepareArguments(Object request, HttpHeaders headers, Class<?>[] paramTypes) {
		if (paramTypes.length == 0) {
			return new Object[0];
		}

		AgentCoreContext context = new AgentCoreContext(headers);
		Object[] args = new Object[paramTypes.length];
		int requestIndex = -1;

		for (int i = 0; i < paramTypes.length; i++) {
			if (paramTypes[i] == AgentCoreContext.class) {
				args[i] = context;
			}
			else if (AgentCorePrincipal.class.isAssignableFrom(paramTypes[i])) {
				args[i] = this.principalProvider.resolve(context);
			}
			else {
				if (requestIndex != -1) {
					throw new AgentCoreInvocationException("Unsupported parameter combination");
				}
				requestIndex = i;
			}
		}

		if (requestIndex != -1) {
			Class<?> requestType = paramTypes[requestIndex];
			if (requestType.isAssignableFrom(request.getClass())) {
				args[requestIndex] = request;
			}
			else {
				args[requestIndex] = convertRequest(request, requestType);
			}
		}

		return args;
	}

	private Object convertRequest(Object request, Class<?> targetType) {
		try {
			if (request instanceof String json) {
				return objectMapper.readValue(json, targetType);
			}

			// Object to JSON to target type conversion
			String json = objectMapper.writeValueAsString(request);
			return objectMapper.readValue(json, targetType);
		}

		catch (Exception e) {
			throw new AgentCoreInvocationException("Type conversion failed", e);
		}
	}

}
