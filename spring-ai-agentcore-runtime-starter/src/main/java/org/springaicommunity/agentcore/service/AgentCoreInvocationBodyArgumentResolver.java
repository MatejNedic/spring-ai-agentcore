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
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springaicommunity.agentcore.context.AgentCoreContext;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves the request-body parameter on the discovered
 * {@link org.springaicommunity.agentcore.annotation.AgentCoreInvocation @AgentCoreInvocation}
 * method.
 *
 * <p>
 * The body type and {@code @Valid} flag are precomputed at startup by
 * {@link AgentCoreMethodScanner} and read from the {@link InvocationTarget}; nothing is
 * inspected per request beyond the {@code Content-Type} header.
 *
 * <p>
 * Body deserialization picks the first {@link HttpMessageConverter} that
 * {@link HttpMessageConverter#canRead(Class, MediaType) canRead} the body type for the
 * request's content type. When validation is enabled the resolved value is passed to the
 * supplied {@link Validator}; failures throw {@link MethodArgumentNotValidException},
 * which Spring MVC's default exception handler maps to {@code 400 Bad Request}.
 */
public class AgentCoreInvocationBodyArgumentResolver implements HandlerMethodArgumentResolver {

	public static final List<HttpMessageConverter<?>> DEFAULT_MESSAGE_CONVERTERS = List
		.of(new MappingJackson2HttpMessageConverter(), new StringHttpMessageConverter());

	private final AgentCoreMethodScanner scanner;

	private final List<HttpMessageConverter<?>> messageConverters;

	@Nullable
	private final Validator validator;

	public AgentCoreInvocationBodyArgumentResolver(AgentCoreMethodScanner scanner,
			List<HttpMessageConverter<?>> messageConverters, @Nullable Validator validator) {
		this.scanner = scanner;
		this.messageConverters = messageConverters;
		this.validator = validator;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Method method = parameter.getMethod();
		if (method == null) {
			return false;
		}
		return this.scanner.getTarget()
			.map(target -> target.bodyParameter() != null && method.equals(target.method())
					&& !hasSpringMvcBindingAnnotation(parameter)
					&& !AgentCoreContext.class.equals(parameter.getParameterType()))
			.orElse(false);
	}

	private static boolean hasSpringMvcBindingAnnotation(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(org.springframework.web.bind.annotation.RequestHeader.class)
				|| parameter.hasParameterAnnotation(org.springframework.web.bind.annotation.RequestParam.class)
				|| parameter.hasParameterAnnotation(org.springframework.web.bind.annotation.PathVariable.class)
				|| parameter.hasParameterAnnotation(org.springframework.web.bind.annotation.CookieValue.class)
				|| parameter.hasParameterAnnotation(org.springframework.web.bind.annotation.RequestAttribute.class)
				|| parameter.hasParameterAnnotation(org.springframework.web.bind.annotation.MatrixVariable.class)
				|| parameter.hasParameterAnnotation(org.springframework.web.bind.annotation.SessionAttribute.class)
				|| parameter.hasParameterAnnotation(org.springframework.web.bind.annotation.RequestBody.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
		InvocationTarget target = this.scanner.getTarget().orElseThrow();
		Class<?> bodyType = parameter.getParameterType();

		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		HttpInputMessage inputMessage = new ServletServerHttpRequest(servletRequest);
		MediaType contentType = inputMessage.getHeaders().getContentType();
		if (contentType == null) {
			contentType = MediaType.APPLICATION_JSON;
		}

		Object value = read(bodyType, contentType, inputMessage);

		Object[] hints = target.validationHints();
		if (hints != null) {
			BeanPropertyBindingResult errors = new BeanPropertyBindingResult(value, parameter.getParameterName());
			if (this.validator instanceof SmartValidator smart) {
				smart.validate(value, errors, hints);
			}
			else {
				this.validator.validate(value, errors);
			}
			if (errors.hasErrors()) {
				throw new MethodArgumentNotValidException(parameter, errors);
			}
		}
		return value;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object read(Class<?> bodyType, MediaType contentType, HttpInputMessage inputMessage) {
		for (HttpMessageConverter converter : this.messageConverters) {
			if (converter.canRead(bodyType, contentType)) {
				try {
					return converter.read(bodyType, inputMessage);
				}
				catch (Exception ex) {
					throw new HttpMessageNotReadableException("Failed to read request body as " + bodyType.getName(),
							ex, inputMessage);
				}
			}
		}
		throw new HttpMessageNotReadableException(
				"No HttpMessageConverter for " + bodyType.getName() + " and content type " + contentType, inputMessage);
	}

}
