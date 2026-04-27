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

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.bedrockagentcore.model.GetResourceOauth2TokenRequest;
import software.amazon.awssdk.services.bedrockagentcore.model.Oauth2FlowType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class GetResourceOauth2TokenConsumerTest {

	@Test
	void ofBuildsConsumerWithAllFields() {
		GetResourceOauth2TokenConsumer consumer = GetResourceOauth2TokenConsumer
			.of(c -> c.workloadIdentityToken("wit-123")
				.resourceCredentialProviderName("my-provider")
				.scopes("read", "write")
				.oauth2Flow(Oauth2FlowType.M2_M)
				.sessionUri("https://session.example.com")
				.resourceOauth2ReturnUrl("https://return.example.com")
				.forceAuthentication(true)
				.customParameters(Map.of("key", "value")));

		GetResourceOauth2TokenRequest.Builder builder = GetResourceOauth2TokenRequest.builder();
		consumer.accept(builder);
		GetResourceOauth2TokenRequest request = builder.build();

		assertThat(request.workloadIdentityToken()).isEqualTo("wit-123");
		assertThat(request.resourceCredentialProviderName()).isEqualTo("my-provider");
		assertThat(request.scopes()).containsExactly("read", "write");
		assertThat(request.oauth2Flow()).isEqualTo(Oauth2FlowType.M2_M);
		assertThat(request.sessionUri()).isEqualTo("https://session.example.com");
		assertThat(request.resourceOauth2ReturnUrl()).isEqualTo("https://return.example.com");
		assertThat(request.forceAuthentication()).isTrue();
		assertThat(request.customParameters()).containsEntry("key", "value");
	}

	@Test
	void scopesVarargsDelegatesToCollection() {
		GetResourceOauth2TokenConsumer consumer = GetResourceOauth2TokenConsumer
			.of(c -> c.workloadIdentityToken("wit").resourceCredentialProviderName("p").scopes("a", "b", "c"));

		GetResourceOauth2TokenRequest.Builder builder = GetResourceOauth2TokenRequest.builder();
		consumer.accept(builder);
		assertThat(builder.build().scopes()).isEqualTo(List.of("a", "b", "c"));
	}

	@Test
	void rejectsNullWorkloadIdentityToken() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> GetResourceOauth2TokenConsumer.of(c -> c.workloadIdentityToken(null)));
	}

	@Test
	void rejectsEmptyResourceCredentialProviderName() {
		assertThatIllegalArgumentException().isThrownBy(() -> GetResourceOauth2TokenConsumer
			.of(c -> c.workloadIdentityToken("t").resourceCredentialProviderName("")));
	}

	@Test
	void rejectsNullScopes() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> GetResourceOauth2TokenConsumer.of(c -> c.workloadIdentityToken("t")
				.resourceCredentialProviderName("p")
				.scopes((java.util.Collection<String>) null)));
	}

}
