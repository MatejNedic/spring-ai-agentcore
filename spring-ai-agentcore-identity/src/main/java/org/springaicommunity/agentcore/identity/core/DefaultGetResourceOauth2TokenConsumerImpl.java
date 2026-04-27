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

import org.springframework.util.Assert;
import software.amazon.awssdk.services.bedrockagentcore.model.GetResourceOauth2TokenRequest;
import software.amazon.awssdk.services.bedrockagentcore.model.Oauth2FlowType;

import java.util.Collection;
import java.util.Map;

/**
 * Default implementation of {@link GetResourceOauth2TokenConsumer}.
 *
 * @author Matej Nedic
 */
class DefaultGetResourceOauth2TokenConsumerImpl implements GetResourceOauth2TokenConsumer {

	private final GetResourceOauth2TokenRequest.Builder builder = GetResourceOauth2TokenRequest.builder();

	@Override
	public GetResourceOauth2TokenConsumer workloadIdentityToken(String workloadIdentityToken) {
		Assert.hasText(workloadIdentityToken, "workloadIdentityToken must not be null or empty");
		this.builder.workloadIdentityToken(workloadIdentityToken);
		return this;
	}

	@Override
	public GetResourceOauth2TokenConsumer resourceCredentialProviderName(String resourceCredentialProviderName) {
		Assert.hasText(resourceCredentialProviderName, "resourceCredentialProviderName must not be null or empty");
		this.builder.resourceCredentialProviderName(resourceCredentialProviderName);
		return this;
	}

	@Override
	public GetResourceOauth2TokenConsumer scopes(Collection<String> scopes) {
		Assert.notNull(scopes, "scopes must not be null");
		this.builder.scopes(scopes);
		return this;
	}

	@Override
	public GetResourceOauth2TokenConsumer oauth2Flow(Oauth2FlowType oauth2Flow) {
		Assert.notNull(oauth2Flow, "oauth2Flow must not be null");
		this.builder.oauth2Flow(oauth2Flow);
		return this;
	}

	@Override
	public GetResourceOauth2TokenConsumer sessionUri(String sessionUri) {
		this.builder.sessionUri(sessionUri);
		return this;
	}

	@Override
	public GetResourceOauth2TokenConsumer resourceOauth2ReturnUrl(String resourceOauth2ReturnUrl) {
		this.builder.resourceOauth2ReturnUrl(resourceOauth2ReturnUrl);
		return this;
	}

	@Override
	public GetResourceOauth2TokenConsumer forceAuthentication(Boolean forceAuthentication) {
		this.builder.forceAuthentication(forceAuthentication);
		return this;
	}

	@Override
	public GetResourceOauth2TokenConsumer customParameters(Map<String, String> customParameters) {
		this.builder.customParameters(customParameters);
		return this;
	}

	@Override
	public void accept(GetResourceOauth2TokenRequest.Builder builder) {
		GetResourceOauth2TokenRequest request = this.builder.build();
		builder.workloadIdentityToken(request.workloadIdentityToken())
			.resourceCredentialProviderName(request.resourceCredentialProviderName())
			.scopes(request.scopes())
			.oauth2Flow(request.oauth2Flow())
			.sessionUri(request.sessionUri())
			.resourceOauth2ReturnUrl(request.resourceOauth2ReturnUrl())
			.forceAuthentication(request.forceAuthentication())
			.customParameters(request.customParameters());
	}

}
