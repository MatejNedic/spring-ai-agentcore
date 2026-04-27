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
	public Map<String, String> chat(@RequestBody Map<String, String> request, @AuthenticationPrincipal Jwt jwt) {
		String workloadAccessToken = identityTemplate.getWorkloadAccessTokenForJwt(jwt.getTokenValue(), workloadName);

		return Map.of("workloadAccessToken", workloadAccessToken, "user", jwt.getSubject(), "message",
				request.getOrDefault("message", ""));
	}

	@PostMapping("/api-key")
	public Map<String, String> apiKey(@AuthenticationPrincipal Jwt jwt) {
		String workloadAccessToken = identityTemplate.getWorkloadAccessTokenForJwt(jwt.getTokenValue(), workloadName);
		String apiKey = identityTemplate.getApiKey(workloadAccessToken, resourceCredentialProviderName);

		return Map.of("apiKey", apiKey, "user", jwt.getSubject());
	}

	@PostMapping("/oauth-token")
	public Map<String, String> oauthToken(@AuthenticationPrincipal Jwt jwt) {
		String workloadAccessToken = identityTemplate.getWorkloadAccessTokenForJwt(jwt.getTokenValue(), workloadName);
		String oauthToken = identityTemplate.getOauthToken(c -> c.workloadIdentityToken(workloadAccessToken)
			.resourceCredentialProviderName(resourceCredentialProviderName));

		return Map.of("oauthToken", oauthToken, "user", jwt.getSubject());
	}

}
