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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.bedrockagentcore.BedrockAgentCoreClient;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Auto-configuration for AWS credentials, region, and {@link BedrockAgentCoreClient}.
 *
 * @author Matej Nedic
 */
@AutoConfiguration
@EnableConfigurationProperties(AgentCoreIdentityAwsProperties.class)
public class AwsCredentialsAndRegionAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public AwsRegionProvider regionProvider(AgentCoreIdentityAwsProperties properties) {
		if (StringUtils.hasText(properties.getRegion())) {
			return new StaticRegionProvider(properties.getRegion());
		}
		return DefaultAwsRegionProviderChain.builder().build();
	}

	@Bean
	@ConditionalOnMissingBean
	public AwsCredentialsProvider credentialsProvider(AgentCoreIdentityAwsProperties properties) {
		List<AwsCredentialsProvider> providers = new ArrayList<>();


		if (StringUtils.hasText(properties.getAccessKey()) && StringUtils.hasText(properties.getSecretKey())) {
			providers.add(createStaticCredentialsProvider(properties));
		}

		if (properties.isInstanceProfile()) {
			providers.add(InstanceProfileCredentialsProvider.create());
		}

		Profile profile = properties.getProfile();
		if (profile != null && profile.getName() != null) {
			providers.add(createProfileCredentialProvider(profile));
		}


		if (providers.isEmpty()) {
			return DefaultCredentialsProvider.builder().build();
		}

		return AwsCredentialsProviderChain.builder().credentialsProviders(providers).build();
	}

	private static ProfileCredentialsProvider createProfileCredentialProvider(Profile profile) {
		var builder = ProfileCredentialsProvider.builder().profileName(profile.getName());
		if (profile.getPath() != null) {
			var profileFile = ProfileFile.builder()
				.type(ProfileFile.Type.CREDENTIALS)
				.content(Paths.get(profile.getPath()))
				.build();
			builder.profileFile(profileFile);
		}
		return builder.build();
	}



	private AwsCredentialsProvider createStaticCredentialsProvider(AgentCoreIdentityAwsProperties properties) {
		if (StringUtils.hasText(properties.getSessionToken())) {
			return StaticCredentialsProvider.create(AwsSessionCredentials.create(properties.getAccessKey(),
					properties.getSecretKey(), properties.getSessionToken()));
		}
		return StaticCredentialsProvider
			.create(AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));
	}

	static class StaticRegionProvider implements AwsRegionProvider {

		private final Region region;

		StaticRegionProvider(String region) {
			try {
				this.region = Region.of(region);
			}
			catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("The region '" + region + "' is not a valid region!", e);
			}
		}

		@Override
		public Region getRegion() {
			return this.region;
		}

	}

}