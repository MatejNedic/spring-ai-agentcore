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

import java.lang.reflect.Method;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;

/**
 * The discovered {@code @AgentCoreInvocation} target with its body-parameter metadata
 * computed once at scan time.
 *
 * @param bean the bean instance that owns the method
 * @param method the annotated method
 * @param bodyParameter the request-body parameter of the method, or {@code null} if the
 * method has no body parameter (e.g. only an {@code AgentCoreContext} parameter)
 * @param validationHints the validation hints produced by
 * {@link org.springframework.validation.annotation.ValidationAnnotationUtils#determineValidationHints}
 * for the body parameter (e.g. an empty array for {@code @Valid}, the
 * {@code @Validated.value()} groups for {@code @Validated}), or {@code null} if no
 * validation annotation is present
 */
public record InvocationTarget(Object bean, Method method, @Nullable MethodParameter bodyParameter,
		@Nullable Object[] validationHints) {
}
