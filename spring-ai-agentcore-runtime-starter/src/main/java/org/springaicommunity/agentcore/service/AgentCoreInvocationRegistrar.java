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

package org.springaicommunity.agentcore.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Registers the discovered {@code @AgentCoreInvocation} method as the Spring MVC handler
 * for {@code POST /invocations} after all singleton beans have been instantiated.
 *
 * <p>
 * Implements {@link SmartInitializingSingleton} so registration runs exactly once, after
 * all beans (including {@link RequestMappingHandlerMapping}) are fully initialized.
 *
 * <p>
 * If a {@code POST /invocations} mapping already exists (because the user registered
 * their own controller), registration is skipped to honour the override.
 *
 * <p>
 * The {@code requestMappingHandlerMapping} bean is looked up by name because in
 * applications with Actuator on the classpath, multiple
 * {@link RequestMappingHandlerMapping} beans exist; the canonical MVC mapping is
 * registered under the name {@code requestMappingHandlerMapping}.
 */
public class AgentCoreInvocationRegistrar implements SmartInitializingSingleton {

	private static final Logger logger = LoggerFactory.getLogger(AgentCoreInvocationRegistrar.class);

	private static final String INVOCATIONS_PATH = "/invocations";

	private static final String HANDLER_MAPPING_BEAN = "requestMappingHandlerMapping";

	private final AgentCoreMethodScanner scanner;

	private final ApplicationContext applicationContext;

	private final ObjectProvider<Validator> validatorProvider;

	public AgentCoreInvocationRegistrar(AgentCoreMethodScanner scanner, ApplicationContext applicationContext,
			ObjectProvider<Validator> validatorProvider) {
		this.scanner = scanner;
		this.applicationContext = applicationContext;
		this.validatorProvider = validatorProvider;
	}

	@Override
	public void afterSingletonsInstantiated() {
		InvocationTarget target = this.scanner.getTarget().orElse(null);
		if (target == null) {
			logger.debug("No @AgentCoreInvocation method found; skipping POST {} registration", INVOCATIONS_PATH);
			return;
		}

		assertValidatorAvailableIfNeeded(target);

		if (!this.applicationContext.containsBean(HANDLER_MAPPING_BEAN)) {
			logger.debug("No {} in context; skipping registration", HANDLER_MAPPING_BEAN);
			return;
		}

		RequestMappingHandlerMapping handlerMapping = this.applicationContext.getBean(HANDLER_MAPPING_BEAN,
				RequestMappingHandlerMapping.class);

		if (hasExistingInvocationsMapping(handlerMapping)) {
			logger.info("Existing POST {} mapping detected; user override in effect, skipping @AgentCoreInvocation"
					+ " registration", INVOCATIONS_PATH);
			return;
		}

		RequestMappingInfo mapping = RequestMappingInfo.paths(INVOCATIONS_PATH)
			.methods(RequestMethod.POST)
			.consumes(MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE)
			.produces(MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE,
					MediaType.APPLICATION_OCTET_STREAM_VALUE)
			.build();

		handlerMapping.registerMapping(mapping, target.bean(), target.method());
		logger.info("Registered POST {} -> {}#{}", INVOCATIONS_PATH, target.method().getDeclaringClass().getName(),
				target.method().getName());
	}

	private void assertValidatorAvailableIfNeeded(InvocationTarget target) {
		if (target.validationHints() == null) {
			return;
		}
		if (this.validatorProvider.getIfAvailable() == null) {
			throw new IllegalStateException("@AgentCoreInvocation method "
					+ target.method().getDeclaringClass().getName() + "#" + target.method().getName()
					+ " declares a @Valid/@Validated body parameter but no Validator bean is available. "
					+ "Add spring-boot-starter-validation.");
		}
	}

	private static boolean hasExistingInvocationsMapping(RequestMappingHandlerMapping handlerMapping) {
		return handlerMapping.getHandlerMethods().keySet().stream().anyMatch(info -> {
			var patterns = info.getPatternValues();
			var methods = info.getMethodsCondition().getMethods();
			return patterns.contains(INVOCATIONS_PATH) && methods.contains(RequestMethod.POST);
		});
	}

}
