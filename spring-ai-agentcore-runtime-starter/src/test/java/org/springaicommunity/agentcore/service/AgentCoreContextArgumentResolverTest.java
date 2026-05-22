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
import org.springaicommunity.agentcore.context.AgentCoreContext;

import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AgentCoreContextArgumentResolverTest {

	private final AgentCoreContextArgumentResolver resolver = new AgentCoreContextArgumentResolver();

	@Test
	void supportsAgentCoreContextParameterOnly() throws Exception {
		Method method = Sample.class.getDeclaredMethod("withContext", AgentCoreContext.class, String.class);

		MethodParameter contextParam = new MethodParameter(method, 0);
		MethodParameter stringParam = new MethodParameter(method, 1);

		assertThat(resolver.supportsParameter(contextParam)).isTrue();
		assertThat(resolver.supportsParameter(stringParam)).isFalse();
	}

	@Test
	void resolvesContextWithRequestHeaders() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Session-Id", "session-42");
		request.addHeader("X-Multi", "a");
		request.addHeader("X-Multi", "b");

		Method method = Sample.class.getDeclaredMethod("withContext", AgentCoreContext.class, String.class);
		MethodParameter param = new MethodParameter(method, 0);

		Object result = resolver.resolveArgument(param, null, new ServletWebRequest(request), null);

		assertThat(result).isInstanceOf(AgentCoreContext.class);
		AgentCoreContext context = (AgentCoreContext) result;
		assertThat(context.getHeader("X-Session-Id")).isEqualTo("session-42");
		assertThat(context.getHeaders().get("X-Multi")).containsExactly("a", "b");
	}

	@SuppressWarnings("unused")
	static class Sample {

		void withContext(AgentCoreContext context, String body) {
		}

	}

}
