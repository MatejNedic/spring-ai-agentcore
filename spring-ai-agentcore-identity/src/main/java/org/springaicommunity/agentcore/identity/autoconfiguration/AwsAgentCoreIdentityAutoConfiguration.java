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

import org.springaicommunity.agentcore.identity.core.AgentCoreIdentityTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.bedrockagentcore.BedrockAgentCoreClient;
import software.amazon.awssdk.services.bedrockagentcore.BedrockAgentCoreClientBuilder;

/**
 * Autoconfiguration for Bedrock AWS SDK client and {@link AgentCoreIdentityTemplate}
 * @author Matej Nedic
 */
@AutoConfiguration
@AutoConfigureAfter({ AwsCredentialsAndRegionAutoConfiguration.class })
public class AwsAgentCoreIdentityAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public BedrockAgentCoreClient bedrockAgentCoreClient(AwsRegionProvider awsRegionProvider,
			AwsCredentialsProvider awsCredentialsProvider, AgentCoreIdentityAwsProperties agentCoreIdentityAwsProperties) {
		var builder = BedrockAgentCoreClient.builder()
			.region(awsRegionProvider.getRegion())
			.credentialsProvider(awsCredentialsProvider)
			.overrideConfiguration(c -> c.apiCallTimeout(agentCoreIdentityAwsProperties.getTimeout()));
		if (agentCoreIdentityAwsProperties.getEndpoint() != null) {
			builder.endpointOverride(agentCoreIdentityAwsProperties.getEndpoint());
		}
		configureSyncHttpClient(builder, agentCoreIdentityAwsProperties);
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public AgentCoreIdentityTemplate agentCoreIdentityTemplate(BedrockAgentCoreClient bedrockAgentCoreClient) {
		return new AgentCoreIdentityTemplate(bedrockAgentCoreClient);
	}

	public static void configureSyncHttpClient(BedrockAgentCoreClientBuilder builder, AgentCoreIdentityAwsProperties agentCoreIdentityAwsProperties) {
		SyncClientProperties properties = agentCoreIdentityAwsProperties.getSyncClient();
		if (properties != null) {
			var httpClientBuilder = ApacheHttpClient.builder();
			PropertyMapper propertyMapper = PropertyMapper.get();
			propertyMapper.from(properties::getConnectionAcquisitionTimeout)
				.to(httpClientBuilder::connectionAcquisitionTimeout);
			propertyMapper.from(properties::getConnectionTimeout).to(httpClientBuilder::connectionTimeout);
			propertyMapper.from(properties::getSocketTimeout).to(httpClientBuilder::socketTimeout);
			builder.httpClientBuilder(httpClientBuilder);
		}
	}

}
