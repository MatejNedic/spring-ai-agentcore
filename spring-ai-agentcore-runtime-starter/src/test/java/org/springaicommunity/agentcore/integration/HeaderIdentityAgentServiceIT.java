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

import java.util.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agentcore.annotation.AgentCoreInvocation;
import org.springaicommunity.agentcore.identity.JwtAgentCorePrincipal;
import org.springaicommunity.agentcore.identity.providers.AgentCorePrincipalProvider;
import org.springaicommunity.agentcore.identity.providers.HeaderAgentCorePrincipalProvider;
import org.springaicommunity.agentcore.identity.template.AgentCoreIdentityTemplate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = { HeaderIdentityAgentServiceIT.TestApp.class, HeaderIdentityAgentServiceIT.TestConfig.class })
@AutoConfigureMockMvc
class HeaderIdentityAgentServiceIT {

	private static final String USER_ID = "test-user-123";

	private static final String JWT_TOKEN = createTestJwt(USER_ID);

	@Autowired
	MockMvc mockMvc;

	@SpringBootApplication(scanBasePackages = "org.springaicommunity.agentcore.autoconfigure",
			exclude = { SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class,
					ManagementWebSecurityAutoConfiguration.class })
	static class TestApp {

		@Service
		public static class HeaderIdentityAgentService {

			private final AgentCoreIdentityTemplate identityTemplate;

			public HeaderIdentityAgentService(AgentCoreIdentityTemplate identityTemplate) {
				this.identityTemplate = identityTemplate;
			}

			@AgentCoreInvocation
			public String handle(String prompt, JwtAgentCorePrincipal principal) {
				String workloadToken = identityTemplate.getWorkloadAccessToken(principal.getJwt(), "my-agent");
				return "User: " + principal.getUserId() + ", workload token obtained, prompt: " + prompt;
			}

		}

	}

	@TestConfiguration
	static class TestConfig {

		@Bean
		@Primary
		AgentCoreIdentityTemplate agentCoreIdentityTemplate() {
			AgentCoreIdentityTemplate mock = mock(AgentCoreIdentityTemplate.class);
			when(mock.getWorkloadAccessToken(anyString(), anyString())).thenReturn("mock-workload-token");
			return mock;
		}

		// Have to be like this since dependency is on path and autoconfiguration fails.
		@Bean
		@Primary
		AgentCorePrincipalProvider agentCorePrincipalProvider(ObjectMapper objectMapper) {
			return new HeaderAgentCorePrincipalProvider(objectMapper);
		}

	}

	@Test
	void shouldInvokeWithHeaderJwtAndGetWorkloadToken() throws Exception {
		mockMvc
			.perform(post("/invocations").contentType(MediaType.TEXT_PLAIN)
				.header("Authorization", "Bearer " + JWT_TOKEN)
				.content("hello"))
			.andExpect(status().isOk())
			.andExpect(content().string("User: " + USER_ID + ", workload token obtained, prompt: hello"));
	}

	private static String createTestJwt(String sub) {
		String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes());
		String payload = Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString(("{\"sub\":\"" + sub + "\"}").getBytes());
		return header + "." + payload + ".";
	}

}
