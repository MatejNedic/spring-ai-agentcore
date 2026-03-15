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

package org.springaicommunity.agentcore.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springaicommunity.agentcore.annotation.AgentCoreInvocation;
import org.springaicommunity.agentcore.controller.AgentCoreInvocationsController;
import org.springaicommunity.agentcore.controller.AgentCoreInvocationsHandler;
import org.springaicommunity.agentcore.controller.AgentCorePingController;
import org.springaicommunity.agentcore.controller.AgentCorePingHandler;
import org.springaicommunity.agentcore.identity.template.AgentCoreIdentityTemplate;
import org.springaicommunity.agentcore.identity.providers.AgentCorePrincipalProvider;
import org.springaicommunity.agentcore.identity.providers.HeaderAgentCorePrincipalProvider;
import org.springaicommunity.agentcore.identity.providers.JwtAgentCorePrincipalProvider;
import org.springaicommunity.agentcore.ping.AgentCorePingService;
import org.springaicommunity.agentcore.ping.AgentCoreTaskTracker;
import org.springaicommunity.agentcore.service.AgentCoreMethodInvoker;
import org.springaicommunity.agentcore.service.AgentCoreMethodRegistry;
import org.springaicommunity.agentcore.service.AgentCoreMethodScanner;
import org.springaicommunity.agentcore.throttle.ThrottleConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.bedrockagentcore.BedrockAgentCoreClient;
import software.amazon.awssdk.services.bedrockagentcore.BedrockAgentCoreClientBuilder;

/**
 * Auto-configuration for AgentCore runtime support. Automatically configures all
 * necessary beans when AgentCoreInvocation is on the classpath.
 */
@AutoConfiguration
@ConditionalOnClass({ AgentCoreInvocation.class, RestController.class })
@AutoConfigureAfter({ AwsCredentialsAndRegionAutoConfiguration.class, AgentCorePingAutoConfiguration.class,
		AgentCoreActuatorAutoConfiguration.class, ThrottleConfiguration.class })
public class AgentCoreAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public static AgentCoreMethodRegistry agentCoreMethodRegistry() {
		return new AgentCoreMethodRegistry();
	}

	@Bean
	@ConditionalOnMissingBean
	public static AgentCoreMethodScanner agentCoreMethodScanner(@Lazy AgentCoreMethodRegistry registry) {
		return new AgentCoreMethodScanner(registry);
	}

	@Bean
	@ConditionalOnMissingBean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean
	@ConditionalOnMissingBean
	public AgentCoreMethodInvoker agentCoreMethodInvoker(ObjectMapper mapper, AgentCoreMethodRegistry registry,
			AgentCorePrincipalProvider agentCorePrincipalProvider) {
		return new AgentCoreMethodInvoker(mapper, registry, agentCorePrincipalProvider);
	}

	@Bean
	@ConditionalOnMissingBean(AgentCoreInvocationsHandler.class)
	public AgentCoreInvocationsController agentCoreController(AgentCoreMethodInvoker invoker) {
		return new AgentCoreInvocationsController(invoker);
	}

	@Bean
	@ConditionalOnMissingBean
	public AgentCoreTaskTracker agentCoreTaskTracker() {
		return new AgentCoreTaskTracker();
	}

	@Bean
	@ConditionalOnMissingBean(AgentCorePingHandler.class)
	public AgentCorePingController agentCoreHealthController(AgentCorePingService agentCorePingService) {
		return new AgentCorePingController(agentCorePingService);
	}

	@Bean
	@ConditionalOnMissingBean
	public BedrockAgentCoreClient bedrockAgentCoreClient(AwsRegionProvider awsRegionProvider,
			AwsCredentialsProvider awsCredentialsProvider, AwsProperties awsProperties) {
		var builder = BedrockAgentCoreClient.builder()
			.region(awsRegionProvider.getRegion())
			.credentialsProvider(awsCredentialsProvider)
			.overrideConfiguration(c -> c.apiCallTimeout(awsProperties.getTimeout()));
		if (awsProperties.getEndpoint() != null) {
			builder.endpointOverride(awsProperties.getEndpoint());
		}
		configureSyncHttpClient(builder, awsProperties);
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public AgentCoreIdentityTemplate agentCoreIdentityTemplate(BedrockAgentCoreClient client) {
		return new AgentCoreIdentityTemplate(client);
	}

	@Configuration
	@ConditionalOnClass(name = { "org.springframework.security.core.context.SecurityContextHolder",
			"org.springframework.security.config.annotation.web.builders.HttpSecurity",
			"org.springframework.security.web.SecurityFilterChain" })
	@ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
	static class SpringSecurityAgentCorePrincipalConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public AgentCorePrincipalProvider agentCorePrincipalProvider() {
			return new JwtAgentCorePrincipalProvider();
		}

		@Bean
		@ConditionalOnMissingBean
		public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
			http.csrf(AbstractHttpConfigurer::disable);
			http.authorizeHttpRequests(
					auth -> auth.requestMatchers("/", "/*.js", "/*.css", "/*.json", "/*.svg", "/*.html")
						.permitAll()
						.requestMatchers("/actuator/**")
						.permitAll());

			http.authorizeHttpRequests(
					auth -> auth.requestMatchers("/invocations").authenticated().anyRequest().permitAll())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

			return http.build();
		}

	}

	@Configuration
	@ConditionalOnMissingClass(value = "org.springframework.security.core.context.SecurityContextHolder")
	static class HeaderAgentCorePrincipalConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public AgentCorePrincipalProvider agentCorePrincipalProvider(ObjectMapper objectMapper) {
			return new HeaderAgentCorePrincipalProvider(objectMapper);
		}

	}

	public static void configureSyncHttpClient(BedrockAgentCoreClientBuilder builder, AwsProperties awsProperties) {
		SyncClientProperties properties = awsProperties.getSyncClient();
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
