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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;

import org.springaicommunity.agentcore.annotation.AgentCoreInvocation;
import org.springaicommunity.agentcore.context.AgentCoreContext;
import org.springaicommunity.agentcore.exception.AgentCoreInvocationException;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.annotation.ValidationAnnotationUtils;

/**
 * {@link BeanPostProcessor} that discovers the single
 * {@link AgentCoreInvocation @AgentCoreInvocation} method in the application and builds
 * an {@link InvocationTarget} containing the bean, the method, the resolved body
 * parameter (the non-{@link AgentCoreContext} parameter, if any), and any precomputed
 * validation hints derived from {@code @Valid} or {@code @Validated}.
 *
 * <p>
 * Throws {@link AgentCoreInvocationException} if more than one annotated method is found
 * across the application.
 */
public class AgentCoreMethodScanner implements BeanPostProcessor {

	private volatile InvocationTarget target;

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		Set<Method> annotated = MethodIntrospector.selectMethods(targetClass,
				(ReflectionUtils.MethodFilter) method -> AnnotatedElementUtils.hasAnnotation(method,
						AgentCoreInvocation.class));

		for (Method method : annotated) {
			if (this.target != null) {
				throw new AgentCoreInvocationException(
						"Multiple @AgentCoreInvocation methods found. Only one is allowed per application. "
								+ "Existing: " + format(this.target.method()) + ", new: " + format(method));
			}
			MethodParameter bodyParameter = findBodyParameter(method);
			Object[] validationHints = (bodyParameter != null) ? resolveValidationHints(bodyParameter) : null;
			this.target = new InvocationTarget(bean, method, bodyParameter, validationHints);
		}
		return bean;
	}

	/**
	 * Returns the discovered target, or {@link Optional#empty()} if no annotated method
	 * was found.
	 */
	public Optional<InvocationTarget> getTarget() {
		return Optional.ofNullable(this.target);
	}

	@Nullable
	private static MethodParameter findBodyParameter(Method method) {
		Class<?>[] paramTypes = method.getParameterTypes();
		for (int i = 0; i < paramTypes.length; i++) {
			if (AgentCoreContext.class.equals(paramTypes[i])) {
				continue;
			}
			MethodParameter candidate = new MethodParameter(method, i);
			if (hasSpringMvcAnnotation(candidate)) {
				continue;
			}
			return candidate;
		}
		return null;
	}

	private static boolean hasSpringMvcAnnotation(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(org.springframework.web.bind.annotation.RequestBody.class)
				|| parameter.hasParameterAnnotation(org.springframework.web.bind.annotation.RequestHeader.class)
				|| parameter.hasParameterAnnotation(org.springframework.web.bind.annotation.RequestParam.class)
				|| parameter.hasParameterAnnotation(org.springframework.web.bind.annotation.PathVariable.class)
				|| parameter.hasParameterAnnotation(org.springframework.web.bind.annotation.CookieValue.class)
				|| parameter.hasParameterAnnotation(org.springframework.web.bind.annotation.RequestAttribute.class)
				|| parameter.hasParameterAnnotation(org.springframework.web.bind.annotation.MatrixVariable.class)
				|| parameter.hasParameterAnnotation(org.springframework.web.bind.annotation.SessionAttribute.class);
	}

	/**
	 * Resolves the validation hints for the parameter using the same utility Spring MVC's
	 * built-in {@code @RequestBody} resolver uses.
	 * @return the hints array if {@code @Valid}, {@code @Validated} or any
	 * {@code @Validated}-meta-annotated annotation is present, {@code null} otherwise
	 */
	@Nullable
	private static Object[] resolveValidationHints(MethodParameter parameter) {
		for (Annotation annotation : parameter.getParameterAnnotations()) {
			Object[] hints = ValidationAnnotationUtils.determineValidationHints(annotation);
			if (hints != null) {
				return hints;
			}
		}
		return null;
	}

	private static String format(Method method) {
		return method.getDeclaringClass().getName() + "#" + method.getName();
	}

}
