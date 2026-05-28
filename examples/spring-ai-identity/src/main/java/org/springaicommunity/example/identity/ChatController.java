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
package org.springaicommunity.example.identity;

import java.util.Map;

import org.springaicommunity.agentcore.identity.core.AgentCoreIdentityTemplate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

	private final AgentCoreIdentityTemplate identityTemplate;

	private final String workloadName;

	private final String resourceCredentialProviderName;

	public ChatController(AgentCoreIdentityTemplate identityTemplate,
			@Value("${app.workload-name}") String workloadName,
			@Value("${app.resource-credential-provider-name}") String resourceCredentialProviderName) {
		this.identityTemplate = identityTemplate;
		this.workloadName = workloadName;
		this.resourceCredentialProviderName = resourceCredentialProviderName;
	}

	@PostMapping("/chat")
	public String chat(@RequestBody Map<String, String> request, @AuthenticationPrincipal Jwt jwt) {
		identityTemplate.getWorkloadAccessTokenForJwt(jwt.getTokenValue(), workloadName);
		return "successfully retrieved workload access token";
	}

	@PostMapping("/api-key")
	public String apiKey(@AuthenticationPrincipal Jwt jwt) {
		String workloadAccessToken = identityTemplate.getWorkloadAccessTokenForJwt(jwt.getTokenValue(), workloadName);
		identityTemplate.getApiKey(workloadAccessToken, resourceCredentialProviderName);
		return "successfully retrieved api key";
	}

	@PostMapping("/oauth-token")
	public String oauthToken(@AuthenticationPrincipal Jwt jwt) {
		String workloadAccessToken = identityTemplate.getWorkloadAccessTokenForJwt(jwt.getTokenValue(), workloadName);
		identityTemplate.getOauthToken(c -> c.workloadIdentityToken(workloadAccessToken)
			.resourceCredentialProviderName(resourceCredentialProviderName));
		return "successfully retrieved oauth token";
	}

}
