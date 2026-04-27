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
package org.springaicommunity.agentcore.identity.autoconfiguration;

import org.junit.jupiter.api.Test;
import org.springaicommunity.agentcore.identity.core.AgentCoreIdentityTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.bedrockagentcore.BedrockAgentCoreClient;

import static org.assertj.core.api.Assertions.assertThat;

class AwsAgentCoreIdentityAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(AwsCredentialsAndRegionAutoConfiguration.class,
				AwsAgentCoreIdentityAutoConfiguration.class))
		.withPropertyValues("spring.agent-core.credentials.access-key=test",
				"spring.agent-core.credentials.secret-key=test",
				"spring.agent-core.credentials.endpoint=http://localhost:4566");

	@Test
	void createsBedrockAgentCoreClient() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(BedrockAgentCoreClient.class);
		});
	}

	@Test
	void createsAgentCoreIdentityTemplate() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(AgentCoreIdentityTemplate.class);
		});
	}

	@Test
	void doesNotOverrideExistingBedrockAgentCoreClientBean() {
		this.contextRunner
			.withBean(BedrockAgentCoreClient.class,
					() -> BedrockAgentCoreClient.builder()
						.region(software.amazon.awssdk.regions.Region.US_EAST_1)
						.credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
							.create(software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create("a", "b")))
						.endpointOverride(java.net.URI.create("http://localhost:4566"))
						.build())
			.run(context -> {
				assertThat(context).hasSingleBean(BedrockAgentCoreClient.class);
			});
	}

}
