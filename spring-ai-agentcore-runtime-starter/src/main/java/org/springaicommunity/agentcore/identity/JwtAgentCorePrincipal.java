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

package org.springaicommunity.agentcore.identity;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springaicommunity.agentcore.context.AgentCoreContext;
import org.springaicommunity.agentcore.context.AgentCoreHeaders;
import org.springframework.util.Assert;

/**
 * JWT AgentCorePrincipal implementation which helps extract UserId from JWT.
 */
public class JwtAgentCorePrincipal implements AgentCorePrincipal {

	private String jwt;

	private Map<String, Object> claims;

	private ObjectMapper objectMapper;

	public JwtAgentCorePrincipal(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.objectMapper = objectMapper;
	}

	public JwtAgentCorePrincipal(String tokenValue, Map<String, Object> claims) {
		this.jwt = tokenValue;
		this.claims = claims;
	}

	public void extract(AgentCoreContext context) {
		String token = context.getHeader(AgentCoreHeaders.WORKLOAD_ACCESS_TOKEN_RUNTIME);
		if (token == null) {
			token = context.getHeader(AgentCoreHeaders.AUTHORIZATION);
			if (token != null && token.startsWith("Bearer ")) {
				token = token.substring(7);
			}
		}
		Assert.hasText(token, "No JWT found in request headers");
		this.jwt = token;
		this.claims = Collections.unmodifiableMap(decodeJwtPayload(token));
	}

	@Override
	public String getUserId() {
		Object sub = this.claims.get("sub");
		return sub != null ? sub.toString() : null;
	}

	public Map<String, Object> getClaims() {
		return this.claims;
	}

	public String getJwt() {
		return this.jwt;
	}

	private Map<String, Object> decodeJwtPayload(String jwt) {
		String[] parts = jwt.split("\\.");
		Assert.isTrue(parts.length >= 2, "Invalid JWT format");
		try {
			byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
			return this.objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
			});
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Failed to decode JWT payload", ex);
		}
	}

}
