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
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;

import static org.assertj.core.api.Assertions.assertThat;

class AwsCredentialsAndRegionAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(AwsCredentialsAndRegionAutoConfiguration.class));

	@Test
	void createsRegionProviderWithConfiguredRegion() {
		this.contextRunner.withPropertyValues("spring.agent-core.credentials.region=eu-west-1").run(context -> {
			assertThat(context).hasSingleBean(AwsRegionProvider.class);
			assertThat(context.getBean(AwsRegionProvider.class).getRegion()).isEqualTo(Region.EU_WEST_1);
		});
	}

	@Test
	void createsCredentialsProviderWithStaticCredentials() {
		this.contextRunner
			.withPropertyValues("spring.agent-core.credentials.access-key=testKey",
					"spring.agent-core.credentials.secret-key=testSecret")
			.run(context -> {
				assertThat(context).hasSingleBean(AwsCredentialsProvider.class);
				var creds = context.getBean(AwsCredentialsProvider.class).resolveCredentials();
				assertThat(creds.accessKeyId()).isEqualTo("testKey");
				assertThat(creds.secretAccessKey()).isEqualTo("testSecret");
			});
	}

	@Test
	void createsDefaultCredentialsProviderWhenNoStaticCredentials() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(AwsCredentialsProvider.class);
		});
	}

	@Test
	void defaultRegionIsUsEast1() {
		this.contextRunner.run(context -> {
			assertThat(context.getBean(AwsRegionProvider.class).getRegion()).isEqualTo(Region.US_EAST_1);
		});
	}

}
