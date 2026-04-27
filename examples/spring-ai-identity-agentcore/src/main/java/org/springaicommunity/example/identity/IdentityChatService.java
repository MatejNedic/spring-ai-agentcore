package org.springaicommunity.example.identity;

import java.util.Map;

import org.springaicommunity.agentcore.annotation.AgentCoreInvocation;
import org.springaicommunity.agentcore.context.AgentCoreContext;
import org.springaicommunity.agentcore.context.AgentCoreHeaders;
import org.springaicommunity.agentcore.identity.core.AgentCoreIdentityTemplate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IdentityChatService {

	private final AgentCoreIdentityTemplate identityTemplate;

	private final String resourceName;

	public IdentityChatService(AgentCoreIdentityTemplate identityTemplate,
			@Value("${app.resource-credential-provider-name}") String resourceName) {
		this.identityTemplate = identityTemplate;
		this.resourceName = resourceName;
	}

	@AgentCoreInvocation
	public Map<String, String> chat(Map<String, String> request, AgentCoreContext context) {
		String workloadAccessToken = context.getWorkloadAccessToken();
		String apiKey = identityTemplate.getApiKey(workloadAccessToken, resourceName);

		return Map.of("apiKey", apiKey, "sessionId", context.getHeader(AgentCoreHeaders.SESSION_ID), "message",
				request.getOrDefault("message", ""));
	}

}
