package org.springaicommunity.agentcore.identity.providers;

import org.springaicommunity.agentcore.context.AgentCoreContext;
import org.springaicommunity.agentcore.identity.AgentCorePrincipal;
import org.springaicommunity.agentcore.identity.JwtAgentCorePrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * @author Matej Nedic
 */
public class JwtAgentCorePrincipalProvider implements AgentCorePrincipalProvider {

	@Override
	public AgentCorePrincipal resolve(AgentCoreContext context) {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof JwtAuthenticationToken jwtAuth) {
			Jwt jwt = jwtAuth.getToken();
			return new JwtAgentCorePrincipal(jwt.getTokenValue(), jwt.getClaims());
		}
		throw new IllegalStateException("No JWT authentication found in SecurityContext");

	}

}
