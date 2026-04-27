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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.bedrockagentcore.BedrockAgentCoreClient;
import software.amazon.awssdk.services.bedrockagentcore.model.CompleteResourceTokenAuthResponse;
import software.amazon.awssdk.services.bedrockagentcore.model.GetResourceApiKeyResponse;
import software.amazon.awssdk.services.bedrockagentcore.model.GetResourceOauth2TokenResponse;
import software.amazon.awssdk.services.bedrockagentcore.model.GetWorkloadAccessTokenForJwtResponse;
import software.amazon.awssdk.services.bedrockagentcore.model.GetWorkloadAccessTokenForUserIdResponse;
import software.amazon.awssdk.services.bedrockagentcore.model.GetWorkloadAccessTokenResponse;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentCoreIdentityTemplateTest {

	@Mock
	private BedrockAgentCoreClient client;

	private AgentCoreIdentityTemplate template;

	@BeforeEach
	void setUp() {
		this.template = new AgentCoreIdentityTemplate(this.client);
	}

	@Test
	void getWorkloadAccessTokenForJwtReturnsToken() {
		when(this.client.getWorkloadAccessTokenForJWT(any(Consumer.class)))
			.thenReturn(GetWorkloadAccessTokenForJwtResponse.builder().workloadAccessToken("wat-123").build());

		String token = this.template.getWorkloadAccessTokenForJwt("my-jwt", "my-workload");
		assertThat(token).isEqualTo("wat-123");
	}

	@Test
	void getWorkloadAccessTokenForJwtRejectsNullJwt() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.template.getWorkloadAccessTokenForJwt(null, "workload"));
	}

	@Test
	void getWorkloadAccessTokenForJwtRejectsEmptyWorkloadName() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.template.getWorkloadAccessTokenForJwt("jwt", ""));
	}

	@Test
	void getApiKeyReturnsKey() {
		when(this.client.getResourceApiKey(any(Consumer.class)))
			.thenReturn(GetResourceApiKeyResponse.builder().apiKey("key-456").build());

		String apiKey = this.template.getApiKey("token", "my-provider");
		assertThat(apiKey).isEqualTo("key-456");
	}

	@Test
	void getApiKeyRejectsNullToken() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.template.getApiKey(null, "provider"));
	}

	@Test
	void getApiKeyRejectsEmptyResourceName() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.template.getApiKey("token", ""));
	}

	@Test
	void getOauthTokenReturnsAccessToken() {
		when(this.client.getResourceOauth2Token(any(Consumer.class)))
			.thenReturn(GetResourceOauth2TokenResponse.builder().accessToken("oauth-789").build());

		String token = this.template.getOauthToken(
				c -> c.workloadIdentityToken("wit").resourceCredentialProviderName("provider").scopes("read"));
		assertThat(token).isEqualTo("oauth-789");
	}

	@Test
	void constructorRejectsNullClient() {
		assertThatIllegalArgumentException().isThrownBy(() -> new AgentCoreIdentityTemplate(null));
	}

	@Test
	void getWorkloadAccessTokenForUserIdReturnsToken() {
		when(this.client.getWorkloadAccessTokenForUserId(any(Consumer.class)))
			.thenReturn(GetWorkloadAccessTokenForUserIdResponse.builder().workloadAccessToken("wat-user-456").build());

		String token = this.template.getWorkloadAccessTokenForUserId("user123", "my-workload");
		assertThat(token).isEqualTo("wat-user-456");
	}

	@Test
	void getWorkloadAccessTokenForUserIdRejectsNullUserId() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.template.getWorkloadAccessTokenForUserId(null, "workload"));
	}

	@Test
	void getWorkloadAccessTokenForUserIdRejectsEmptyWorkloadName() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.template.getWorkloadAccessTokenForUserId("user", ""));
	}

	@Test
	void getWorkloadAccessTokenReturnsToken() {
		when(this.client.getWorkloadAccessToken(any(Consumer.class)))
			.thenReturn(GetWorkloadAccessTokenResponse.builder().workloadAccessToken("wat-simple-789").build());

		String token = this.template.getWorkloadAccessToken("my-workload");
		assertThat(token).isEqualTo("wat-simple-789");
	}

	@Test
	void getWorkloadAccessTokenRejectsEmptyWorkloadName() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.template.getWorkloadAccessToken(""));
	}

	@Test
	void completeResourceTokenAuthCallsClient() {
		when(this.client.completeResourceTokenAuth(any(Consumer.class)))
			.thenReturn(CompleteResourceTokenAuthResponse.builder().build());

		this.template.completeResourceTokenAuth("https://callback.example.com/session/123", "my-jwt");
		verify(this.client).completeResourceTokenAuth(any(Consumer.class));
	}

	@Test
	void completeResourceTokenAuthRejectsNullSessionUri() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.template.completeResourceTokenAuth(null, "jwt"));
	}

	@Test
	void completeResourceTokenAuthRejectsEmptyUserToken() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.template.completeResourceTokenAuth("uri", ""));
	}

	@Test
	void completeResourceTokenAuthForUserIdCallsClient() {
		when(this.client.completeResourceTokenAuth(any(Consumer.class)))
			.thenReturn(CompleteResourceTokenAuthResponse.builder().build());

		this.template.completeResourceTokenAuthForUserId("https://callback.example.com/session/123", "user123");
		verify(this.client).completeResourceTokenAuth(any(Consumer.class));
	}

	@Test
	void completeResourceTokenAuthForUserIdRejectsNullSessionUri() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.template.completeResourceTokenAuthForUserId(null, "user"));
	}

	@Test
	void completeResourceTokenAuthForUserIdRejectsEmptyUserId() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.template.completeResourceTokenAuthForUserId("uri", ""));
	}

}
