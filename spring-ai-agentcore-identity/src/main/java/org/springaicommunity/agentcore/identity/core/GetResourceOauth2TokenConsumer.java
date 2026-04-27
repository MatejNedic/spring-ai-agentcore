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

import software.amazon.awssdk.services.bedrockagentcore.model.GetResourceOauth2TokenRequest;
import software.amazon.awssdk.services.bedrockagentcore.model.Oauth2FlowType;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Consumer impl for configuring a {@link GetResourceOauth2TokenRequest} via a fluent
 * consumer API.
 *
 * @author Matej Nedic
 */
public interface GetResourceOauth2TokenConsumer extends Consumer<GetResourceOauth2TokenRequest.Builder> {

	GetResourceOauth2TokenConsumer workloadIdentityToken(String workloadIdentityToken);

	GetResourceOauth2TokenConsumer resourceCredentialProviderName(String resourceCredentialProviderName);

	GetResourceOauth2TokenConsumer scopes(Collection<String> scopes);

	default GetResourceOauth2TokenConsumer scopes(String... scopes) {
		return scopes(java.util.Arrays.asList(scopes));
	}

	GetResourceOauth2TokenConsumer oauth2Flow(Oauth2FlowType oauth2Flow);

	GetResourceOauth2TokenConsumer sessionUri(String sessionUri);

	GetResourceOauth2TokenConsumer resourceOauth2ReturnUrl(String resourceOauth2ReturnUrl);

	GetResourceOauth2TokenConsumer forceAuthentication(Boolean forceAuthentication);

	GetResourceOauth2TokenConsumer customParameters(Map<String, String> customParameters);

	static GetResourceOauth2TokenConsumer of(Consumer<GetResourceOauth2TokenConsumer> consumer) {
		DefaultGetResourceOauth2TokenConsumerImpl spec = new DefaultGetResourceOauth2TokenConsumerImpl();
		consumer.accept(spec);
		return spec;
	}

}
