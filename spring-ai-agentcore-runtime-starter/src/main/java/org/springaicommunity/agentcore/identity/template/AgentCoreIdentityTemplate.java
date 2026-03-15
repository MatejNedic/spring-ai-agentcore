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

package org.springaicommunity.agentcore.identity.template;

import org.springframework.util.Assert;
import software.amazon.awssdk.services.bedrockagentcore.BedrockAgentCoreClient;

/**
 * Template for exchanging JWTs for workload access tokens via the AgentCore Identity
 * service.
 *
 * @author Matej Nedic
 */
public class AgentCoreIdentityTemplate {

	private final BedrockAgentCoreClient client;

	public AgentCoreIdentityTemplate(BedrockAgentCoreClient client) {
		Assert.notNull(client, "BedrockAgentCoreClient must not be null");
		this.client = client;
	}

	/**
	 * Exchanges a JWT for an opaque workload access token.
	 * @param jwt the JWT to exchange
	 * @param workloadName the workloadName
	 * @return the opaque workload access token
	 */
	public String getWorkloadAccessToken(String jwt, String workloadName) {
		Assert.hasText(jwt, "jwt must not be null or empty");
		Assert.hasText(workloadName, "workloadName must not be null or empty");
		var response = this.client.getWorkloadAccessTokenForJWT(r -> r.userToken(jwt).workloadName(workloadName));
		return response.workloadAccessToken();
	}

}
