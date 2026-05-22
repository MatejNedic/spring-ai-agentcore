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

import java.util.List;

import org.springaicommunity.agentcore.annotation.AgentCoreInvocation;
import org.springaicommunity.agentcore.controller.AgentCorePingController;
import org.springaicommunity.agentcore.controller.AgentCorePingHandler;
import org.springaicommunity.agentcore.ping.AgentCorePingService;
import org.springaicommunity.agentcore.ping.AgentCoreTaskTracker;
import org.springaicommunity.agentcore.service.AgentCoreContextArgumentResolver;
import org.springaicommunity.agentcore.service.AgentCoreInvocationBodyArgumentResolver;
import org.springaicommunity.agentcore.service.AgentCoreInvocationRegistrar;
import org.springaicommunity.agentcore.service.AgentCoreMethodScanner;
import org.springaicommunity.agentcore.throttle.ThrottleConfiguration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuration for AgentCore runtime support.
 *
 * <p>
 * Wires:
 * <ul>
 * <li>{@link AgentCoreMethodScanner} — bean post-processor that discovers the user's
 * single {@code @AgentCoreInvocation}-annotated method.</li>
 * <li>{@link AgentCoreInvocationRegistrar} — registers the discovered method as the
 * Spring MVC handler for {@code POST /invocations}.</li>
 * <li>A {@link WebMvcConfigurer} bean that adds the AgentCore-specific
 * {@link HandlerMethodArgumentResolver}s ({@link AgentCoreContextArgumentResolver} and
 * {@link AgentCoreInvocationBodyArgumentResolver}).</li>
 * <li>{@link AgentCoreTaskTracker} — counter for in-flight background tasks.</li>
 * <li>{@link AgentCorePingController} — {@code GET /ping} endpoint (skipped if the user
 * provides their own {@link AgentCorePingHandler}).</li>
 * </ul>
 */
@Configuration
@ConditionalOnClass({ AgentCoreInvocation.class, RestController.class })
@Import({ AgentCorePingAutoConfiguration.class, AgentCoreActuatorAutoConfiguration.class, ThrottleConfiguration.class })
public class AgentCoreAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public static AgentCoreMethodScanner agentCoreMethodScanner() {
		return new AgentCoreMethodScanner();
	}

	@Bean
	@ConditionalOnMissingBean
	public AgentCoreInvocationRegistrar agentCoreInvocationRegistrar(AgentCoreMethodScanner scanner,
			org.springframework.context.ApplicationContext applicationContext,
			ObjectProvider<Validator> validatorProvider) {
		return new AgentCoreInvocationRegistrar(scanner, applicationContext, validatorProvider);
	}

	@Bean
	public WebMvcConfigurer agentCoreWebMvcConfigurer(AgentCoreMethodScanner scanner,
			ObjectProvider<Validator> validator) {
		return new WebMvcConfigurer() {
			@Override
			public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
				resolvers.add(new AgentCoreContextArgumentResolver());
				resolvers.add(new AgentCoreInvocationBodyArgumentResolver(scanner,
						AgentCoreInvocationBodyArgumentResolver.DEFAULT_MESSAGE_CONVERTERS,
						validator.getIfAvailable()));
			}
		};
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

}
