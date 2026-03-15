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
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springaicommunity.agentcore.annotation.AgentCoreInvocation;
import org.springaicommunity.agentcore.identity.JwtAgentCorePrincipal;
import org.springaicommunity.agentcore.identity.template.AgentCoreIdentityTemplate;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ExplicitAuthFlowsType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.VerifiedAttributeType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = { IdentityAgentServiceIT.TestApp.class, IdentityAgentServiceIT.TestConfig.class })
@AutoConfigureMockMvc
@Testcontainers
@EnabledIfEnvironmentVariable(named = "LOCALSTACK_AUTH_TOKEN", matches = ".+",
		disabledReason = "Requires LocalStack Pro image")
class IdentityAgentServiceIT {

	private static final String api_key = System.getenv("LOCALSTACK_AUTH_TOKEN");

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack-pro:4.12.0"))
		.withEnv("LOCALSTACK_AUTH_TOKEN", api_key);

	static String jwtToken;

	static String userId;

	static String poolId;

	@Autowired
	MockMvc mockMvc;

	@SpringBootApplication(scanBasePackages = "org.springaicommunity.agentcore.autoconfigure")
	static class TestApp {

		@Service
		public static class IdentityAgentService {

			private final AgentCoreIdentityTemplate identityTemplate;

			public IdentityAgentService(AgentCoreIdentityTemplate identityTemplate) {
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

	}

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) throws Exception {
		registry.add("spring.agent-core.credentials.access-key", localstack::getAccessKey);
		registry.add("spring.agent-core.credentials.secret-key", localstack::getSecretKey);
		registry.add("spring.agent-core.credentials.region", localstack::getRegion);
		registry.add("spring.agent-core.credentials.endpoint", localstack::getEndpoint);

		var cognitoClient = CognitoIdentityProviderClient.builder()
			.endpointOverride(localstack.getEndpoint())
			.region(Region.of(localstack.getRegion()))
			.credentialsProvider(StaticCredentialsProvider
				.create(AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
			.build();

		var pool = cognitoClient
			.createUserPool(r -> r.poolName("test-pool").autoVerifiedAttributes(VerifiedAttributeType.EMAIL));
		poolId = pool.userPool().id();

		var appClient = cognitoClient.createUserPoolClient(r -> r.userPoolId(poolId)
			.clientName("test-client")
			.explicitAuthFlows(ExplicitAuthFlowsType.ALLOW_USER_PASSWORD_AUTH,
					ExplicitAuthFlowsType.ALLOW_REFRESH_TOKEN_AUTH));
		String clientId = appClient.userPoolClient().clientId();

		cognitoClient.adminCreateUser(r -> r.userPoolId(poolId).username("testuser").temporaryPassword("TempPass1!"));

		cognitoClient.adminSetUserPassword(
				r -> r.userPoolId(poolId).username("testuser").password("TestPass1!").permanent(true));

		var authResult = cognitoClient.initiateAuth(r -> r.authFlow(AuthFlowType.USER_PASSWORD_AUTH)
			.clientId(clientId)
			.authParameters(Map.of("USERNAME", "testuser", "PASSWORD", "TestPass1!")));

		jwtToken = authResult.authenticationResult().accessToken();

		String payload = new String(Base64.getUrlDecoder().decode(jwtToken.split("\\.")[1]));
		userId = new ObjectMapper().readTree(payload).get("sub").asText();

		String jwkSetUri = localstack.getEndpoint() + "/" + poolId + "/.well-known/jwks.json";
		registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> jwkSetUri);
	}

	@Test
	void shouldInvokeWithJwtAndGetWorkloadToken() throws Exception {
		mockMvc
			.perform(post("/invocations").contentType(MediaType.TEXT_PLAIN)
				.header("Authorization", "Bearer " + jwtToken)
				.content("hello"))
			.andExpect(status().isOk())
			.andExpect(content().string("User: " + userId + ", workload token obtained, prompt: hello"));
	}

}
