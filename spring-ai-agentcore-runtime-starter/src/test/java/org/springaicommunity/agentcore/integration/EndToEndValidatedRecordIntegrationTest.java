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

package org.springaicommunity.agentcore.integration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agentcore.annotation.AgentCoreInvocation;
import org.springaicommunity.agentcore.context.AgentCoreContext;
import org.springaicommunity.agentcore.context.AgentCoreHeaders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test verifying the canonical {@code @AgentCoreInvocation} signature:
 *
 * <pre>{@code
 * &#64;AgentCoreInvocation
 * public String invoke(@Valid PersonQuestion question, AgentCoreContext context) { ... }
 * }</pre>
 *
 * <p>
 * Confirms that:
 * <ul>
 * <li>The typed body parameter is deserialized via Spring MVC's standard message
 * converters (one Jackson read, no annotation required).</li>
 * <li>{@code @Valid} triggers Jakarta Bean Validation natively.</li>
 * <li>{@link AgentCoreContext} is injected by
 * {@link org.springaicommunity.agentcore.service.AgentCoreContextArgumentResolver}.</li>
 * </ul>
 */
@SpringBootTest(classes = EndToEndValidatedRecordIntegrationTest.TestApp.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EndToEndValidatedRecordIntegrationTest {

	@SpringBootApplication(scanBasePackages = "org.springaicommunity.agentcore.autoconfigure")
	static class TestApp {

		@Service
		public static class TestAgentService {

			@AgentCoreInvocation
			public String invoke(@Valid PersonQuestion question, AgentCoreContext context) {
				String sessionId = context.getHeader(AgentCoreHeaders.SESSION_ID);
				return "Hello " + question.name() + ", you asked: '" + question.question() + "' (session=" + sessionId
						+ ")";
			}

		}

	}

	record PersonQuestion(@NotBlank String name, @NotBlank String question) {
	}

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void shouldDeserializeRecordInjectContextAndReturnString() {
		var headers = new HttpHeaders();
		headers.set(AgentCoreHeaders.SESSION_ID, "abc-123");

		var body = new PersonQuestion("Ada", "What is recursion?");
		var entity = new HttpEntity<>(body, headers);

		var response = restTemplate.postForEntity("http://localhost:" + port + "/invocations", entity, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo("Hello Ada, you asked: 'What is recursion?' (session=abc-123)");
	}

	@Test
	void shouldRejectInvalidPayloadWith400() {
		var body = new PersonQuestion("", "");
		var response = restTemplate.postForEntity("http://localhost:" + port + "/invocations", body, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

}
